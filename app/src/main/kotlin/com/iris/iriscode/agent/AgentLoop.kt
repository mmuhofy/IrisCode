// Inspired by: anomalyco/opencode packages/core/src/session/prompt.ts — loop, tool dispatch, history
// UNTESTED — verify before use

package com.iris.iriscode.agent

import com.iris.iriscode.agent.tool.AskUserTool
import com.iris.iriscode.agent.tool.BashTool
import com.iris.iriscode.agent.tool.WriteFileTool
import com.iris.iriscode.data.remote.gemini.GeminiClient
import com.iris.iriscode.data.remote.gemini.GeminiSseEvent
import com.iris.iriscode.data.remote.gemini.GeminiStep
import com.iris.iriscode.domain.agent.AgentEvent
import com.iris.iriscode.domain.agent.ToolResult
import com.iris.iriscode.domain.model.WorkMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core agent loop.
 *
 * Lifecycle of one user turn:
 *   1. Append UserInput step to history
 *   2. Stream Gemini response
 *   3. On TextDelta → emit TextChunk event
 *   4. On InteractionCompleted:
 *      a. If no tool calls → emit TurnComplete, done
 *      b. For each tool call:
 *         - Emit ToolCallStarted
 *         - Execute via ToolRegistry
 *         - Handle ToolResult:
 *           · Success/Error → append FunctionResult to history, continue loop
 *           · Cancelled     → append cancellation note, continue loop
 *           · AwaitingApproval → suspend, emit DiffApprovalRequired or AskUserRequired,
 *                                wait for resume signal via approvalChannel
 *      c. Append all FunctionResults to history
 *      d. Loop — send updated history to Gemini for next turn
 *   5. MAX_STEPS guard to prevent infinite loops (mirrors opencode doom-loop guard)
 */
@Singleton
class AgentLoop @Inject constructor(
    private val geminiClient: GeminiClient,
    private val toolRegistry: ToolRegistry
) {

    companion object {
        private const val MAX_STEPS = 20
    }

    // ─── Approval channels ────────────────────────────────────────────────────
    // When a tool returns AwaitingApproval, the loop suspends and waits here.
    // UI calls approveWrite() / rejectWrite() / deliverAnswer() to resume.

    private val approvalChannel = Channel<ApprovalSignal>(Channel.UNLIMITED)

    sealed class ApprovalSignal {
        data class Approved(val eventId: String) : ApprovalSignal()
        data class Rejected(val eventId: String) : ApprovalSignal()
        data class Answer(val eventId: String, val answer: String) : ApprovalSignal()
    }

    /** Called by ViewModel when user taps Approve on a DiffCard */
    fun approveWrite(eventId: String) {
        approvalChannel.trySend(ApprovalSignal.Approved(eventId))
    }

    /** Called by ViewModel when user taps Reject on a DiffCard */
    fun rejectWrite(eventId: String) {
        approvalChannel.trySend(ApprovalSignal.Rejected(eventId))
    }

    /** Called by ViewModel when user submits answer on an AskCard */
    fun deliverAnswer(eventId: String, answer: String) {
        approvalChannel.trySend(ApprovalSignal.Answer(eventId, answer))
    }

    // ─── Main entry point ─────────────────────────────────────────────────────

    /**
     * Run one user turn of the agent loop.
     *
     * @param apiKey        User's Gemini API key
     * @param model         Model string
     * @param userMessage   Raw user text
     * @param history       Full conversation history (mutable — updated in place)
     * @param workMode      Current WorkMode — gates tool use
     * @param projectPath   Absolute path of the current project (for bash working dir)
     * @param systemPrompt  System instruction for this session
     *
     * @return Flow<AgentEvent> — collect in ViewModel, map to ChatMessage updates
     */
    fun runTurn(
        apiKey: String,
        model: String,
        userMessage: String,
        history: MutableList<GeminiStep>,
        workMode: WorkMode,
        projectPath: String,
        systemPrompt: String
    ): Flow<AgentEvent> = flow {

        // Configure bash working directory for this project
        toolRegistry.bashTool.workingDirectory = projectPath
        toolRegistry.bashTool.onOutputLine = { eventId, line ->
            // Can't emit from callback directly — BashTool buffers, AgentLoop emits after
            // Full streaming wired when PTY is ready (Parça 7)
        }

        // Step 1: append user message to history
        history.add(GeminiStep.UserInput(userMessage))

        var steps = 0

        // ── Agent loop ────────────────────────────────────────────────────────
        while (steps < MAX_STEPS) {
            steps++

            val toolDeclarations = toolRegistry.getToolDeclarations(workMode)
            val modeSystemPrompt = buildSystemPrompt(workMode, systemPrompt)

            // Step 2: stream one Gemini turn
            val toolCallsThisTurn = mutableListOf<GeminiStep.FunctionCall>()
            var hadTextOutput = false
            var inputTokens = 0
            var outputTokens = 0

            geminiClient.streamInteraction(
                apiKey = apiKey,
                model = model,
                history = history,
                tools = toolDeclarations,
                systemPrompt = modeSystemPrompt
            ).collect { event ->
                when (event) {
                    is GeminiSseEvent.TextDelta -> {
                        hadTextOutput = true
                        emit(AgentEvent.TextChunk(event.text))
                    }

                    is GeminiSseEvent.FunctionCallStarted -> {
                        emit(AgentEvent.ToolCallStarted(event.name, emptyMap()))
                    }

                    is GeminiSseEvent.InteractionCompleted -> {
                        toolCallsThisTurn.addAll(event.toolCalls)
                        inputTokens = event.inputTokens
                        outputTokens = event.outputTokens
                        emit(AgentEvent.TokenUsageUpdate(inputTokens, outputTokens, inputTokens + outputTokens))
                    }

                    is GeminiSseEvent.StreamError -> {
                        emit(AgentEvent.FatalError(event.message))
                        return@collect
                    }

                    else -> Unit
                }
            }

            // Step 3: no tool calls → done
            if (toolCallsThisTurn.isEmpty()) {
                emit(AgentEvent.TurnComplete)
                break
            }

            // Step 4: execute tool calls, collect results for next history turn
            val functionResults = mutableListOf<GeminiStep.FunctionResult>()

            for (toolCall in toolCallsThisTurn) {
                val result = dispatchToolCall(
                    toolCall = toolCall,
                    workMode = workMode,
                    flowCollector = this
                )

                emit(AgentEvent.ToolCallCompleted(toolCall.name, result))

                // Append model's function_call step + our function_result to history
                history.add(toolCall)
                history.add(
                    GeminiStep.FunctionResult(
                        callId = toolCall.id,
                        name = toolCall.name,
                        result = result.toResponseString()
                    )
                )

                // If fatal — abort loop
                if (result is ToolResult.Error && result.cause != null) {
                    emit(AgentEvent.FatalError(result.message))
                    return@flow
                }
            }

            // Loop continues — Gemini will receive function results and respond
        }

        if (steps >= MAX_STEPS) {
            emit(AgentEvent.FatalError("Agent reached maximum step limit ($MAX_STEPS). Stopping to prevent loop."))
        }
    }

    // ─── Tool dispatch ────────────────────────────────────────────────────────

    private suspend fun dispatchToolCall(
        toolCall: GeminiStep.FunctionCall,
        workMode: WorkMode,
        flowCollector: FlowCollector<AgentEvent>
    ): ToolResult {

        val initialResult = toolRegistry.execute(
            toolName = toolCall.name,
            args = toolCall.arguments,
            mode = workMode
        )

        return when (initialResult) {

            is ToolResult.Success,
            is ToolResult.Error,
            is ToolResult.Cancelled -> initialResult

            is ToolResult.AwaitingApproval -> {
                // Determine what kind of approval is needed
                val eventId = initialResult.eventId

                when (toolCall.name) {
                    "write_file" -> {
                        val request = toolRegistry.writeFileTool.getPendingRequest(eventId)
                            ?: return ToolResult.Error("Lost pending write for eventId: $eventId")

                        // Emit diff event — UI shows DiffCard
                        flowCollector.emit(
                            AgentEvent.DiffApprovalRequired(
                                eventId = eventId,
                                filePath = request.path,
                                diff = buildString {
                                    // Simple diff summary — full diff via java-diff-utils in Parça C
                                    appendLine("File: ${request.path}")
                                    appendLine("Operation: ${if (request.existed) "overwrite" else "create"}")
                                    appendLine("---")
                                    append(request.content.take(2000))
                                    if (request.content.length > 2000) appendLine("\n... (truncated)")
                                }
                            )
                        )

                        // Suspend — wait for user signal
                        awaitApproval(eventId) { signal ->
                            when (signal) {
                                is ApprovalSignal.Approved ->
                                    toolRegistry.writeFileTool.commitWrite(eventId)
                                is ApprovalSignal.Rejected ->
                                    toolRegistry.writeFileTool.cancelWrite(eventId)
                                else -> ToolResult.Cancelled("Unexpected signal for write_file")
                            }
                        }
                    }

                    "ask_user" -> {
                        val question = toolRegistry.askUserTool.getPendingQuestion(eventId)
                            ?: return ToolResult.Error("Lost pending question for eventId: $eventId")
                        val options = toolRegistry.askUserTool.getPendingOptions(eventId)

                        // Emit ask event — UI shows AskCard
                        flowCollector.emit(
                            AgentEvent.AskUserRequired(
                                eventId = eventId,
                                question = question,
                                options = options
                            )
                        )

                        // Suspend — wait for user answer
                        awaitApproval(eventId) { signal ->
                            when (signal) {
                                is ApprovalSignal.Answer ->
                                    toolRegistry.askUserTool.deliverAnswer(eventId, signal.answer)
                                else -> ToolResult.Cancelled("Unexpected signal for ask_user")
                            }
                        }
                    }

                    else -> ToolResult.Error("Unknown AwaitingApproval tool: ${toolCall.name}")
                }
            }
        }
    }

    // ─── Approval suspension ──────────────────────────────────────────────────

    /**
     * Drain approvalChannel until we get a signal for our eventId.
     * Any signals for other eventIds are re-queued (shouldn't happen in serial flow,
     * but guards against race conditions).
     */
    private suspend fun awaitApproval(
        eventId: String,
        handler: (ApprovalSignal) -> ToolResult
    ): ToolResult {
        val requeue = mutableListOf<ApprovalSignal>()

        try {
            while (true) {
                val signal = approvalChannel.receive()
                val signalId = when (signal) {
                    is ApprovalSignal.Approved -> signal.eventId
                    is ApprovalSignal.Rejected -> signal.eventId
                    is ApprovalSignal.Answer -> signal.eventId
                }

                if (signalId == eventId) {
                    return handler(signal)
                } else {
                    // Signal for a different event — re-queue
                    requeue.add(signal)
                }
            }
        } finally {
            // Re-send any mis-routed signals
            requeue.forEach { approvalChannel.trySend(it) }
        }
    }

    // ─── System prompt builder ────────────────────────────────────────────────

    private fun buildSystemPrompt(mode: WorkMode, base: String): String {
        val modeInstruction = when (mode) {
            WorkMode.PLAN ->
                "\n\n[MODE: PLAN] You are in read-only mode. " +
                "Analyze and suggest only. Do NOT write files or run commands. " +
                "write_file and bash tools are disabled."

            WorkMode.BUILD ->
                "\n\n[MODE: BUILD] Full tool use enabled. " +
                "Always show a diff before writing files. " +
                "Ask the user before running destructive commands."

            WorkMode.AUTO ->
                "\n\n[MODE: AUTO] Full autonomy. " +
                "Proceed without asking for approval. " +
                "Write files and run commands as needed to complete the task."
        }

        return base + modeInstruction
    }
}