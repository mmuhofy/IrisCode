package com.iris.iriscode.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iris.iriscode.domain.model.ChatMessage
import com.iris.iriscode.domain.model.WorkMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class ChatTab { Chat, Terminal }

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isProcessing: Boolean = false,
    val isTyping: Boolean = false,
    val workMode: WorkMode = WorkMode.DEFAULT,
    val currentModel: String = "flash",
    val showSlashMenu: Boolean = false,
    val slashQuery: String = "",
    val showModelSheet: Boolean = false,
    val selectedTab: ChatTab = ChatTab.Chat,
    val showExpandedPanel: Boolean = false,
    val thinkingEnabled: Boolean = true,
    val webSearchEnabled: Boolean = false,
    val effortLevel: String = "med"
)

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    fun updateInput(text: String) {
        _state.value = _state.value.copy(inputText = text)
    }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isEmpty() || _state.value.isProcessing) return

        val message = ChatMessage.UserText(
            id = UUID.randomUUID().toString(),
            text = text
        )

        _state.value = _state.value.copy(
            messages = _state.value.messages + message,
            inputText = "",
            isProcessing = true,
            isTyping = true
        )

        viewModelScope.launch {
            delay(800)
            _state.value = _state.value.copy(isTyping = false)
            delay(200)
            val response = ChatMessage.AgentText(
                id = UUID.randomUUID().toString(),
                text = "I received: \"$text\""
            )
            _state.value = _state.value.copy(
                messages = _state.value.messages + response,
                isProcessing = false
            )
        }
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
            "models" -> showModelSheet()
            "new" -> clearChat()
            "settings" -> { /* navigate to settings - handled by parent */ }
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

    fun showModelSheet() {
        _state.value = _state.value.copy(showModelSheet = true)
    }

    fun hideModelSheet() {
        _state.value = _state.value.copy(showModelSheet = false)
    }

    fun selectModel(model: String) {
        _state.value = _state.value.copy(
            currentModel = model,
            showModelSheet = false
        )
    }

    fun setTab(tab: ChatTab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    fun toggleExpandedPanel() {
        _state.value = _state.value.copy(showExpandedPanel = !_state.value.showExpandedPanel)
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

        viewModelScope.launch {
            delay(500)
            val response = ChatMessage.AgentText(
                id = UUID.randomUUID().toString(),
                text = "Thanks for your answer: \"$answer\""
            )
            _state.value = _state.value.copy(
                messages = _state.value.messages + response
            )
        }
    }

    private fun clearChat() {
        _state.value = _state.value.copy(messages = emptyList())
    }
}
