package com.iris.iriscode.domain.model

data class ChatSession(
    val id: String,
    val projectId: Long,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

sealed class ChatMessage {
    abstract val id: String
    abstract val timestamp: Long

    data class UserText(
        override val id: String,
        val text: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    data class AgentText(
        override val id: String,
        val text: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    data class BashCommand(
        override val id: String,
        val command: String,
        val output: String = "",
        val isRunning: Boolean = true,
        val exitCode: Int? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    data class FileDiff(
        override val id: String,
        val filePath: String,
        val diff: String,
        val isApproved: Boolean? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    data class AskUser(
        override val id: String,
        val question: String,
        val options: List<String> = emptyList(),
        val answer: String? = null,
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()

    data class ReadFile(
        override val id: String,
        val filePath: String,
        val content: String = "",
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatMessage()
}
