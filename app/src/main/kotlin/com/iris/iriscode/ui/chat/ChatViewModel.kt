package com.iris.iriscode.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iris.iriscode.data.local.OnboardingPreferences
import com.iris.iriscode.data.remote.gemini.GeminiApi
import com.iris.iriscode.data.remote.gemini.GeminiClient
import com.iris.iriscode.domain.model.ChatMessage
import com.iris.iriscode.domain.model.WorkMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class ChatTab { Chat, Terminal }

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
    val workMode: WorkMode = WorkMode.DEFAULT,
    val currentModel: String = "flash",
    val showSlashMenu: Boolean = false,
    val slashQuery: String = "",
    val selectedTab: ChatTab = ChatTab.Chat,
    val showOptionsSheet: Boolean = false,
    val thinkingEnabled: Boolean = true,
    val webSearchEnabled: Boolean = false,
    val effortLevel: String = "med",
    val modelDropdownExpanded: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val geminiClient: GeminiClient,
    private val preferences: OnboardingPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun updateInput(text: String) {
        _state.value = _state.value.copy(inputText = text)
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isEmpty() || _state.value.isProcessing) return

        val apiKey = preferences.getApiKey()
        if (apiKey.isNullOrBlank()) {
            val errorMsg = ChatMessage.AgentText(
                id = UUID.randomUUID().toString(),
                text = "Please set your Gemini API key in settings to start chatting."
            )
            _state.value = _state.value.copy(messages = _state.value.messages + errorMsg)
            return
        }

        val userMsg = ChatMessage.UserText(id = UUID.randomUUID().toString(), text = text)
        val agentMsgId = UUID.randomUUID().toString()
        val pendingMsg = ChatMessage.AgentText(id = agentMsgId, text = "")

        _state.value = _state.value.copy(
            messages = _state.value.messages + userMsg + pendingMsg,
            inputText = "",
            isProcessing = true,
            isTyping = true
        )

        viewModelScope.launch {
            val modelName = when (_state.value.currentModel) {
                "flash" -> GeminiApi.MODEL_FLASH
                "pro" -> "gemini-2.5-pro"
                else -> GeminiApi.MODEL_FLASH
            }

            val responseText = StringBuilder()
            var hasContent = false
            var isError = false

            geminiClient.streamChat(
                apiKey = apiKey,
                model = modelName,
                history = _state.value.messages,
                systemPrompt = "You are Iris, a helpful coding assistant."
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
                val errorMsg = ChatMessage.AgentText(
                    id = UUID.randomUUID().toString(),
                    text = responseText.toString().trimStart('\n')
                )
                _state.value = _state.value.copy(
                    messages = _state.value.messages + errorMsg
                )
            }

            _state.value = _state.value.copy(
                isProcessing = false,
                isTyping = false
            )
        }
    }

    private fun replaceMessage(id: String, text: String) {
        val updated = _state.value.messages.map { msg ->
            if (msg is ChatMessage.AgentText && msg.id == id) {
                msg.copy(text = text)
            } else msg
        }
        _state.value = _state.value.copy(messages = updated)
    }

    private fun removeMessage(id: String) {
        _state.value = _state.value.copy(
            messages = _state.value.messages.filter { it.id != id }
        )
    }

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

    fun approveDiff(messageId: String) {
        val updated = _state.value.messages.map { msg ->
            if (msg is ChatMessage.FileDiff && msg.id == messageId) {
                msg.copy(isApproved = true)
            } else msg
        }
        _state.value = _state.value.copy(messages = updated)
    }

    fun rejectDiff(messageId: String) {
        val updated = _state.value.messages.map { msg ->
            if (msg is ChatMessage.FileDiff && msg.id == messageId) {
                msg.copy(isApproved = false)
            } else msg
        }
        _state.value = _state.value.copy(messages = updated)
    }

    fun answerAsk(messageId: String, answer: String) {
        val updated = _state.value.messages.map { msg ->
            if (msg is ChatMessage.AskUser && msg.id == messageId) {
                msg.copy(answer = answer)
            } else msg
        }
        _state.value = _state.value.copy(messages = updated)
    }

    private fun clearChat() {
        _state.value = _state.value.copy(messages = emptyList())
    }
}
