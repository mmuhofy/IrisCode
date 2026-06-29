// Inspired by: anomalyco/opencode packages/core/src/tool/write.ts
// UNTESTED — verify before use

package com.iris.iriscode.agent.tool

import com.iris.iriscode.data.remote.gemini.GeminiTool
import com.iris.iriscode.data.remote.gemini.GeminiToolParameters
import com.iris.iriscode.data.remote.gemini.GeminiToolProperty
import com.iris.iriscode.domain.agent.IrisTool
import com.iris.iriscode.domain.agent.ToolResult
import com.iris.iriscode.domain.agent.WriteFileRequest
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * write_file tool.
 *
 * Does NOT write directly — emits ToolResult.AwaitingApproval.
 * AgentLoop catches this, emits DiffApprovalRequired event to UI.
 * User approves → AgentLoop calls WriteFileTool.commitWrite().
 * User rejects → AgentLoop calls WriteFileTool.cancelWrite().
 */
class WriteFileTool @Inject constructor() : IrisTool {

    override val name = "write_file"
    override val description =
        "Write content to a file. Creates the file if it does not exist, " +
        "overwrites if it does. Always shows a diff for user approval before writing."

    // Pending writes waiting for user approval — keyed by eventId
    private val pendingWrites = mutableMapOf<String, WriteFileRequest>()

    override fun toGeminiTool() = GeminiTool(
        name = name,
        description = description,
        parameters = GeminiToolParameters(
            properties = mapOf(
                "path" to GeminiToolProperty(
                    type = "string",
                    description = "Absolute or project-relative file path to write"
                ),
                "content" to GeminiToolProperty(
                    type = "string",
                    description = "Full content to write to the file"
                )
            ),
            required = listOf("path", "content")
        )
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult {
        val path = args["path"] as? String
            ?: return ToolResult.Error("Missing required argument: path")
        val content = args["content"] as? String
            ?: return ToolResult.Error("Missing required argument: content")

        val file = File(path)
        val existed = file.exists()
        val currentContent = if (existed) {
            try { file.readText() } catch (e: Exception) { "" }
        } else ""

        val diff = buildDiff(path, currentContent, content, existed)
        val eventId = UUID.randomUUID().toString()

        pendingWrites[eventId] = WriteFileRequest(
            eventId = eventId,
            path = path,
            content = content,
            existed = existed
        )

        // Signal to AgentLoop: block, show DiffCard, wait for user
        return ToolResult.AwaitingApproval(eventId)
    }

    /**
     * Called by AgentLoop when user taps Approve on the DiffCard.
     * Returns ToolResult.Success or ToolResult.Error to resume the agent.
     */
    fun commitWrite(eventId: String): ToolResult {
        val request = pendingWrites.remove(eventId)
            ?: return ToolResult.Error("No pending write found for eventId: $eventId")

        return try {
            val file = File(request.path)
            file.parentFile?.mkdirs()
            file.writeText(request.content)
            val verb = if (request.existed) "Wrote" else "Created"
            ToolResult.Success("$verb file successfully: ${request.path}")
        } catch (e: SecurityException) {
            ToolResult.Error("Permission denied writing to: ${request.path}")
        } catch (e: Exception) {
            ToolResult.Error("Failed to write ${request.path}: ${e.message}")
        }
    }

    /**
     * Called by AgentLoop when user taps Reject on the DiffCard.
     */
    fun cancelWrite(eventId: String): ToolResult {
        pendingWrites.remove(eventId)
        return ToolResult.Cancelled("User rejected file write")
    }

    fun getPendingRequest(eventId: String): WriteFileRequest? = pendingWrites[eventId]

    /**
     * Minimal unified diff — new file vs overwrite.
     * Full diff engine (java-diff-utils) wired in DiffEngine in Parça 4.
     * For now produces a simple header + content preview.
     */
    private fun buildDiff(
        path: String,
        oldContent: String,
        newContent: String,
        existed: Boolean
    ): String {
        val sb = StringBuilder()
        if (!existed) {
            sb.appendLine("--- /dev/null")
            sb.appendLine("+++ b/$path")
            newContent.lines().forEach { line -> sb.appendLine("+$line") }
        } else {
            sb.appendLine("--- a/$path")
            sb.appendLine("+++ b/$path")
            // Simple line diff — replaced in Parça 4 with java-diff-utils
            val oldLines = oldContent.lines().toSet()
            val newLines = newContent.lines()
            newLines.forEach { line ->
                if (!oldLines.contains(line)) sb.appendLine("+$line")
                else sb.appendLine(" $line")
            }
        }
        return sb.toString()
    }
}