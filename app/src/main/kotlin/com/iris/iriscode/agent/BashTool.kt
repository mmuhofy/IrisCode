// UNTESTED — verify before use
// Note: Full PTY impl is Parça 7 (terminal spike). 
// This tool currently runs via ProcessBuilder — no interactive PTY.
// PTY integration wired when terminal/ layer is ready.

package com.iris.iriscode.agent.tool

import com.iris.iriscode.data.remote.gemini.GeminiTool
import com.iris.iriscode.data.remote.gemini.GeminiToolParameters
import com.iris.iriscode.data.remote.gemini.GeminiToolProperty
import com.iris.iriscode.domain.agent.IrisTool
import com.iris.iriscode.domain.agent.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class BashTool @Inject constructor() : IrisTool {

    override val name = "bash"
    override val description =
        "Execute a shell command in the project directory. " +
        "Output is streamed to chat. Use for grep, git, ls, tests, builds. " +
        "Avoid interactive commands or long-running processes."

    // Injected by AgentLoop to set the working directory per project
    var workingDirectory: String? = null

    // Callback invoked per output line — AgentLoop wires this to emit BashOutput events
    var onOutputLine: ((eventId: String, line: String) -> Unit)? = null

    override fun toGeminiTool() = GeminiTool(
        name = name,
        description = description,
        parameters = GeminiToolParameters(
            properties = mapOf(
                "command" to GeminiToolProperty(
                    type = "string",
                    description = "Shell command to execute"
                ),
                "timeout_seconds" to GeminiToolProperty(
                    type = "integer",
                    description = "Max seconds to wait (default 30, max 120)"
                )
            ),
            required = listOf("command")
        )
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult = withContext(Dispatchers.IO) {
        val command = args["command"] as? String
            ?: return@withContext ToolResult.Error("Missing required argument: command")
        val timeoutSec = ((args["timeout_seconds"] as? Number)?.toLong() ?: 30L)
            .coerceIn(1L, 120L)

        val eventId = UUID.randomUUID().toString()
        val workDir = workingDirectory?.let { File(it) }

        try {
            val process = ProcessBuilder("sh", "-c", command)
                .apply { workDir?.let { directory(it) } }
                .redirectErrorStream(true)
                .start()

            val outputBuilder = StringBuilder()

            // Read stdout+stderr line by line, emit via callback
            process.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line!!
                    outputBuilder.appendLine(l)
                    onOutputLine?.invoke(eventId, l)
                }
            }

            val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext ToolResult.Error(
                    "Command timed out after ${timeoutSec}s: $command"
                )
            }

            val exitCode = process.exitValue()
            val output = outputBuilder.toString().trim()

            if (exitCode == 0) {
                ToolResult.Success(
                    buildString {
                        if (output.isNotEmpty()) append(output)
                        else append("(no output)")
                        appendLine()
                        append("exit 0")
                    }
                )
            } else {
                ToolResult.Error(
                    buildString {
                        if (output.isNotEmpty()) appendLine(output)
                        append("exit $exitCode")
                    }
                )
            }
        } catch (e: Exception) {
            ToolResult.Error("Failed to execute command: ${e.message}")
        }
    }
}