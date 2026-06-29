// UNTESTED — verify before use

package com.iris.iriscode.agent.tool

import com.iris.iriscode.data.remote.gemini.GeminiTool
import com.iris.iriscode.data.remote.gemini.GeminiToolParameters
import com.iris.iriscode.data.remote.gemini.GeminiToolProperty
import com.iris.iriscode.domain.agent.IrisTool
import com.iris.iriscode.domain.agent.ToolResult
import java.util.UUID
import javax.inject.Inject

/**
 * ask_user tool.
 *
 * Surfaces an AskCard in chat. Agent blocks until user answers.
 * AgentLoop calls deliverAnswer() when user submits.
 */
class AskUserTool @Inject constructor() : IrisTool {

    override val name = "ask_user"
    override val description =
        "Ask the user a question and wait for their answer. " +
        "Use when you need clarification or a decision before proceeding. " +
        "Optionally provide multiple choice options."

    // Pending questions waiting for user answer — keyed by eventId
    private val pendingAnswers = mutableMapOf<String, (String) -> Unit>()

    override fun toGeminiTool() = GeminiTool(
        name = name,
        description = description,
        parameters = GeminiToolParameters(
            properties = mapOf(
                "question" to GeminiToolProperty(
                    type = "string",
                    description = "The question to ask the user"
                ),
                "options" to GeminiToolProperty(
                    type = "string",
                    description = "Optional comma-separated list of choices, e.g. 'Yes,No,Cancel'"
                )
            ),
            required = listOf("question")
        )
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult {
        val question = args["question"] as? String
            ?: return ToolResult.Error("Missing required argument: question")
        val optionsRaw = args["options"] as? String ?: ""
        val options = if (optionsRaw.isNotBlank())
            optionsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        else emptyList()

        val eventId = UUID.randomUUID().toString()

        // Store eventId + options so AgentLoop can surface the AskCard
        pendingOptions[eventId] = options
        pendingQuestion[eventId] = question

        return ToolResult.AwaitingApproval(eventId)
    }

    /**
     * Called by AgentLoop when user submits an answer via AskCard.
     * @return ToolResult.Success with the answer, ready to send back to Gemini.
     */
    fun deliverAnswer(eventId: String, answer: String): ToolResult {
        pendingOptions.remove(eventId)
        pendingQuestion.remove(eventId)
        return ToolResult.Success("User answered: $answer")
    }

    fun getPendingQuestion(eventId: String): String? = pendingQuestion[eventId]
    fun getPendingOptions(eventId: String): List<String> = pendingOptions[eventId] ?: emptyList()

    private val pendingQuestion = mutableMapOf<String, String>()
    private val pendingOptions = mutableMapOf<String, List<String>>()
}