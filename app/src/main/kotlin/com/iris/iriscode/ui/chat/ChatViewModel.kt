package com.iris.iriscode.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iris.iriscode.agent.AgentLoop
import com.iris.iriscode.agent.AgentSystemPrompt
import com.iris.iriscode.data.local.OnboardingPreferences
import com.iris.iriscode.data.remote.gemini.GeminiApi
import com.iris.iriscode.data.remote.gemini.GeminiClient
import com.iris.iriscode.data.remote.gemini.GeminiStep
import com.iris.iriscode.domain.agent.AgentEvent
import com.iris.iriscode.domain.agent.ToolResult
import com.iris.iriscode.domain.model.ChatMessage
import com.iris.iriscode.domain.model.WorkMode
import com.iris.iriscode.terminal.BootstrapState
import com.iris.iriscode.terminal.TerminalManager
import com.iris.iriscode.terminal.TermuxBootstrap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class ChatTab { Chat, Terminal, Files }

data class ModelOption(
    val id: String,
    val displayName: String,
    val provider: String
)

val availableModels = listOf(
    ModelOption("flash", "Gemini 2.5 Flash", "Google"),
    ModelOption("pro", "Gemini 2.5 Pro", "Google"),
    ModelOption("claude-sonnet", "Claude Sonnet", "Anthropic"),
    ModelOption("gpt-4o", "GPT-4o", "OpenAI")
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isProcessing: Boolean = false,
    val isTyping: Boolean = false,
    val bootstrapState: BootstrapState = BootstrapState.Checking,
    val workMode: WorkMode = WorkMode.DEFAULT,
    val currentModel: String = "flash",
    val showSlashMenu: Boolean = false,
    val slashQuery: String = "",
    val selectedTab: ChatTab = ChatTab.Chat,
    val showOptionsSheet: Boolean = false,
    val thinkingEnabled: Boolean = true,
    val webSearchEnabled: Boolean = false,
    val effortLevel: String = "med",
    val modelDropdownExpanded: Boolean = false,
    val showMoreMenu: Boolean = false,
    val projectPath: String? = null,
    val projectName: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val geminiClient: GeminiClient,
    private val preferences: OnboardingPreferences,
    private val agentLoop: AgentLoop,
    application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val termuxBootstrap = TermuxBootstrap(application)
    val terminalManager = TerminalManager(termuxBootstrap)

    private val geminiStepHistory = mutableListOf<GeminiStep>()

    init {
        viewModelScope.launch {
            termuxBootstrap.install { state ->
                _state.value = _state.value.copy(bootstrapState = state)
            }
        }
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    fun setProjectInfo(path: String, name: String) {
        _state.value = _state.value.copy(projectPath = path, projectName = name)
    }

    fun updateInput(text: String) {
        _state.value = _state.value.copy(inputText = text)
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isEmpty() || _state.value.isProcessing) return

        val apiKey = preferences.getApiKey()
        if (apiKey.isNullOrBlank()) {
            addSystemMessage("Please set your Gemini API key in settings to start chatting.")
            return
        }

        val userMsg = ChatMessage.UserText(id = UUID.randomUUID().toString(), text = text)
        _state.value = _state.value.copy(
            messages = _state.value.messages + userMsg,
            inputText = "",
            isProcessing = true,
            isTyping = true
        )

        val projectPath = _state.value.projectPath

        viewModelScope.launch {
            if (projectPath != null) {
                runAgentLoop(apiKey, text, projectPath)
            } else {
                runSimpleChat(apiKey, text)
            }
        }
    }

    // ─── Agent Loop Path ─────────────────────────────────────────────────────

    private suspend fun runAgentLoop(apiKey: String, userMessage: String, projectPath: String) {
        val modelName = resolveModelName()
        val agentMsgId = UUID.randomUUID().toString()
        var agentTextBuilder = StringBuilder()
        var currentAgentMsgId: String? = agentMsgId

        addPendingMessage(agentMsgId)

        val systemPrompt = buildSystemPrompt()

        agentLoop.runTurn(
            apiKey = apiKey,
            model = modelName,
            userMessage = userMessage,
            history = geminiStepHistory,
            workMode = _state.value.workMode,
            projectPath = projectPath,
            systemPrompt = systemPrompt
        ).collect { event ->
            when (event) {
                is AgentEvent.TextChunk -> {
                    agentTextBuilder.append(event.text)
                    val msgId = currentAgentMsgId
                        ?: UUID.randomUUID().toString().also { currentAgentMsgId = it }
                    replaceMessage(msgId, agentTextBuilder.toString())
                    _state.value = _state.value.copy(isTyping = true)
                }

                is AgentEvent.ToolCallStarted -> {
                    // Could show a brief tool indicator — for now just track
                }

                is AgentEvent.ToolCallCompleted -> { }

                is AgentEvent.BashStarted -> {
                    val bashMsg = ChatMessage.BashCommand(
                        id = event.eventId,
                        command = event.command,
                        output = "",
                        isRunning = true
                    )
                    addMessage(bashMsg)
                }

                is AgentEvent.BashOutput -> {
                    updateBashOutput(event.eventId, event.line)
                }

                is AgentEvent.BashCompleted -> {
                    updateBashExit(event.eventId, event.exitCode)
                }

                is AgentEvent.DiffApprovalRequired -> {
                    removeVoidAgentText(currentAgentMsgId, agentTextBuilder)
                    val diffMsg = ChatMessage.FileDiff(
                        id = event.eventId,
                        filePath = event.filePath,
                        diff = event.diff
                    )
                    addMessage(diffMsg)
                }

                is AgentEvent.AskUserRequired -> {
                    val askMsg = ChatMessage.AskUser(
                        id = event.eventId,
                        question = event.question,
                        options = event.options
                    )
                    addMessage(askMsg)
                }

                is AgentEvent.TurnComplete -> {
                    removeVoidAgentText(currentAgentMsgId, agentTextBuilder)
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        isTyping = false
                    )
                }

                is AgentEvent.FatalError -> {
                    removeVoidAgentText(currentAgentMsgId, agentTextBuilder)
                    addSystemMessage(event.message)
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        isTyping = false
                    )
                }

                is AgentEvent.TokenUsageUpdate -> { }
            }
        }
    }

    // ─── Simple Chat Path (no project) ──────────────────────────────────────

    private suspend fun runSimpleChat(apiKey: String, text: String) {
        val modelName = resolveModelName()
        val agentMsgId = UUID.randomUUID().toString()
        val pendingMsg = ChatMessage.AgentText(id = agentMsgId, text = "")
        addMessage(pendingMsg)

        val responseText = StringBuilder()
        var hasContent = false
        var isError = false

        geminiClient.streamChat(
            apiKey = apiKey,
            model = modelName,
            history = _state.value.messages,
            systemPrompt = buildSystemPrompt()
        ).collect { delta ->
            if (delta.startsWith("\n\n[") && (delta.contains("Error") || delta.contains("error"))) {
                isError = true
                responseText.append(delta.trimStart('\n'))
            } else {
                hasContent = true
                responseText.append(delta)
            }
            replaceMessage(agentMsgId, responseText.toString())
        }

        if (isError && !hasContent) {
            removeMessage(agentMsgId)
            addSystemMessage(responseText.toString().trimStart('\n'))
        }

        _state.value = _state.value.copy(
            isProcessing = false,
            isTyping = false
        )
    }

    // ─── Approval signals (called from UI) ──────────────────────────────────

    fun approveDiff(messageId: String) {
        val updated = _state.value.messages.map { msg ->
            if (msg is ChatMessage.FileDiff && msg.id == messageId) {
                msg.copy(isApproved = true)
            } else msg
        }
        _state.value = _state.value.copy(messages = updated)
        agentLoop.approveWrite(messageId)
    }

    fun rejectDiff(messageId: String) {
        val updated = _state.value.messages.map { msg ->
            if (msg is ChatMessage.FileDiff && msg.id == messageId) {
                msg.copy(isApproved = false)
            } else msg
        }
        _state.value = _state.value.copy(messages = updated)
        agentLoop.rejectWrite(messageId)
    }

    fun answerAsk(messageId: String, answer: String) {
        val updated = _state.value.messages.map { msg ->
            if (msg is ChatMessage.AskUser && msg.id == messageId) {
                msg.copy(answer = answer)
            } else msg
        }
        _state.value = _state.value.copy(messages = updated)
        agentLoop.deliverAnswer(messageId, answer)
    }

    // ─── Message helpers ────────────────────────────────────────────────────

    private fun addMessage(msg: ChatMessage) {
        _state.value = _state.value.copy(
            messages = _state.value.messages + msg
        )
    }

    private fun addPendingMessage(id: String) {
        addMessage(ChatMessage.AgentText(id = id, text = ""))
    }

    private fun addSystemMessage(text: String) {
        addMessage(ChatMessage.AgentText(
            id = UUID.randomUUID().toString(),
            text = text
        ))
    }

    private fun replaceMessage(id: String, text: String) {
        _state.value = _state.value.copy(
            messages = _state.value.messages.map { msg ->
                if (msg is ChatMessage.AgentText && msg.id == id) msg.copy(text = text) else msg
            }
        )
    }

    private fun removeMessage(id: String) {
        _state.value = _state.value.copy(
            messages = _state.value.messages.filter { it.id != id }
        )
    }

    private fun updateBashOutput(eventId: String, line: String) {
        _state.value = _state.value.copy(
            messages = _state.value.messages.map { msg ->
                if (msg is ChatMessage.BashCommand && msg.id == eventId) {
                    msg.copy(output = msg.output + line)
                } else msg
            }
        )
    }

    private fun updateBashExit(eventId: String, exitCode: Int) {
        _state.value = _state.value.copy(
            messages = _state.value.messages.map { msg ->
                if (msg is ChatMessage.BashCommand && msg.id == eventId) {
                    msg.copy(isRunning = false, exitCode = exitCode)
                } else msg
            }
        )
    }

    /**
     * Remove the pending agent text message if it has no content.
     * Called before inserting a tool card so we don't show an empty bubble.
     */
    private fun removeVoidAgentText(msgId: String?, builder: StringBuilder) {
        if (msgId != null && builder.isEmpty()) {
            removeMessage(msgId)
        }
    }

    // ─── Utilities ──────────────────────────────────────────────────────────

    private fun resolveModelName(): String = when (_state.value.currentModel) {
        "flash" -> GeminiApi.MODEL_FLASH
        "pro" -> "gemini-2.5-pro"
        else -> GeminiApi.MODEL_FLASH
    }

    private fun buildSystemPrompt(): String {
        val path = _state.value.projectPath
        val name = _state.value.projectName
        val modelId = resolveModelName()
        if (path != null && name != null) {
            return AgentSystemPrompt.build(path, name, modelId)
        }
        return "You are Iris, a helpful coding assistant. Read files before editing them. Show clear reasoning."
    }

    // ─── UI actions ─────────────────────────────────────────────────────────

    fun setWorkMode(mode: WorkMode) {
        _state.value = _state.value.copy(workMode = mode)
    }

    fun toggleSlashMenu(show: Boolean) {
        _state.value = _state.value.copy(showSlashMenu = show)
    }

    fun updateSlashQuery(query: String) {
        _state.value = _state.value.copy(slashQuery = query)
    }

    fun handleSlashCommand(command: String) {
        toggleSlashMenu(false)
        when (command) {
            "plan" -> setWorkMode(WorkMode.PLAN)
            "build" -> setWorkMode(WorkMode.BUILD)
            "auto" -> setWorkMode(WorkMode.AUTO)
            "models" -> { }
            "new" -> clearChat()
            else -> {
                val message = ChatMessage.UserText(
                    id = UUID.randomUUID().toString(),
                    text = "/$command"
                )
                val response = ChatMessage.AgentText(
                    id = UUID.randomUUID().toString(),
                    text = "Unknown command: /$command"
                )
                _state.value = _state.value.copy(
                    messages = _state.value.messages + message + response
                )
            }
        }
    }

    fun toggleModelDropdown() {
        _state.value = _state.value.copy(modelDropdownExpanded = !_state.value.modelDropdownExpanded)
    }

    fun dismissModelDropdown() {
        _state.value = _state.value.copy(modelDropdownExpanded = false)
    }

    fun selectModel(modelId: String) {
        _state.value = _state.value.copy(
            currentModel = modelId,
            modelDropdownExpanded = false
        )
    }

    fun toggleMoreMenu() {
        _state.value = _state.value.copy(showMoreMenu = !_state.value.showMoreMenu)
    }

    fun dismissMoreMenu() {
        _state.value = _state.value.copy(showMoreMenu = false)
    }

    fun setTab(tab: ChatTab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    fun toggleOptionsSheet() {
        _state.value = _state.value.copy(showOptionsSheet = !_state.value.showOptionsSheet)
    }

    fun dismissOptionsSheet() {
        _state.value = _state.value.copy(showOptionsSheet = false)
    }

    fun setThinking(enabled: Boolean) {
        _state.value = _state.value.copy(thinkingEnabled = enabled)
    }

    fun setWebSearch(enabled: Boolean) {
        _state.value = _state.value.copy(webSearchEnabled = enabled)
    }

    fun setEffortLevel(level: String) {
        _state.value = _state.value.copy(effortLevel = level)
    }

    fun retryBootstrap() {
        termuxBootstrap.retry()
        _state.value = _state.value.copy(bootstrapState = BootstrapState.Checking)
        viewModelScope.launch {
            termuxBootstrap.install { state ->
                _state.value = _state.value.copy(bootstrapState = state)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        terminalManager.destroy()
    }

    private fun clearChat() {
        geminiStepHistory.clear()
        _state.value = _state.value.copy(messages = emptyList())
    }
}
