// UNTESTED — verify before use

package com.iris.iriscode.domain.agent

/**
 * Result returned from IrisTool.execute().
 *
 * Success       → tool ran, output is available for Gemini context
 * Error         → tool failed, message is surfaced to agent
 * Cancelled     → tool was blocked (e.g. PLAN mode blocks write_file/bash)
 * AwaitingApproval → tool triggered diff/approve flow, agent must pause until resolved
 */
sealed class ToolResult {

    /** Tool completed successfully. [output] is sent back to Gemini as function response. */
    data class Success(
        val output: String
    ) : ToolResult()

    /** Tool encountered an error. [message] is sent back to Gemini as function response. */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : ToolResult()

    /**
     * Tool was rejected before execution.
     * Not sent back to Gemini as a normal response — agent loop surfaces a system message.
     * Example: write_file in PLAN mode.
     */
    data class Cancelled(
        val reason: String
    ) : ToolResult()

    /**
     * Tool triggered a blocking UI event (diff/approve or ask_user).
     * Agent loop must suspend until the user resolves the event.
     * [eventId] is used to correlate the UI response with the pending tool call.
     */
    data class AwaitingApproval(
        val eventId: String
    ) : ToolResult()

    /** Convenience: was this a terminal success or failure? */
    val isTerminal: Boolean
        get() = this is Success || this is Error || this is Cancelled

    /** Serialize result to a string suitable for Gemini's function_response content. */
    fun toResponseString(): String = when (this) {
        is Success -> output
        is Error -> "ERROR: $message"
        is Cancelled -> "CANCELLED: $reason"
        is AwaitingApproval -> "AWAITING_APPROVAL: $eventId"
    }
}