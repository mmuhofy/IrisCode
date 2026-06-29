// UNTESTED — verify before use

package com.iris.iriscode.domain.agent

/**
 * Events emitted by the AgentLoop to the ViewModel/UI layer.
 * Collected as a Flow<AgentEvent> in ChatViewModel.
 */
sealed class AgentEvent {

    /** A text chunk streamed from Gemini. Append to current agent message. */
    data class TextChunk(val text: String) : AgentEvent()

    /** Agent finished generating a complete response turn. */
    object TurnComplete : AgentEvent()

    /** Agent is about to call a tool. */
    data class ToolCallStarted(val toolName: String, val args: Map<String, Any>) : AgentEvent()

    /** A tool finished executing. */
    data class ToolCallCompleted(val toolName: String, val result: ToolResult) : AgentEvent()

    /** write_file was called — triggers DiffCard in chat. */
    data class DiffApprovalRequired(
        val eventId: String,
        val filePath: String,
        val diff: String
    ) : AgentEvent()

    /** ask_user was called — triggers AskCard in chat. */
    data class AskUserRequired(
        val eventId: String,
        val question: String,
        val options: List<String>
    ) : AgentEvent()

    /** bash tool started — BashCard should appear in chat. */
    data class BashStarted(
        val eventId: String,
        val command: String
    ) : AgentEvent()

    /** bash tool produced output chunk. */
    data class BashOutput(
        val eventId: String,
        val line: String
    ) : AgentEvent()

    /** bash tool finished. */
    data class BashCompleted(
        val eventId: String,
        val exitCode: Int
    ) : AgentEvent()

    /** Agent loop encountered an unrecoverable error. */
    data class FatalError(val message: String, val cause: Throwable? = null) : AgentEvent()

    /** Token/cost update for /info card. */
    data class TokenUsageUpdate(
        val inputTokens: Int,
        val outputTokens: Int,
        val totalTokens: Int
    ) : AgentEvent()
}