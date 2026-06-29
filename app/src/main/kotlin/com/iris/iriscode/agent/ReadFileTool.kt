// Inspired by: anomalyco/opencode packages/core/src/tool/read-filesystem.ts
// UNTESTED — verify before use

package com.iris.iriscode.agent.tool

import com.iris.iriscode.data.remote.gemini.GeminiTool
import com.iris.iriscode.data.remote.gemini.GeminiToolParameters
import com.iris.iriscode.data.remote.gemini.GeminiToolProperty
import com.iris.iriscode.domain.agent.IrisTool
import com.iris.iriscode.domain.agent.ToolResult
import java.io.File
import javax.inject.Inject

class ReadFileTool @Inject constructor() : IrisTool {

    override val name = "read_file"
    override val description =
        "Read the contents of a file or list a directory. " +
        "For large files, use offset and limit to page through content."

    // Mirrors opencode MAX_READ_LINES / MAX_READ_BYTES
    private val MAX_READ_LINES = 2_000
    private val MAX_READ_BYTES = 50 * 1024  // 50 KB
    private val MAX_LINE_LENGTH = 2_000

    // Binary extensions — same set as opencode read-filesystem.ts
    private val BINARY_EXTENSIONS = setOf(
        ".zip", ".tar", ".gz", ".exe", ".dll", ".so", ".class", ".jar", ".war",
        ".7z", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".odt", ".ods",
        ".odp", ".bin", ".dat", ".obj", ".o", ".a", ".lib", ".wasm", ".pyc", ".pyo",
        ".pdf", ".png", ".jpg", ".jpeg", ".gif", ".webp", ".mp3", ".mp4", ".apk"
    )

    override fun toGeminiTool() = GeminiTool(
        name = name,
        description = description,
        parameters = GeminiToolParameters(
            properties = mapOf(
                "path" to GeminiToolProperty(
                    type = "string",
                    description = "Absolute or project-relative path to file or directory"
                ),
                "offset" to GeminiToolProperty(
                    type = "integer",
                    description = "Line number to start reading from (1-based). Omit for start of file."
                ),
                "limit" to GeminiToolProperty(
                    type = "integer",
                    description = "Max lines to read (max $MAX_READ_LINES). Omit for default."
                )
            ),
            required = listOf("path")
        )
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult {
        val path = args["path"] as? String
            ?: return ToolResult.Error("Missing required argument: path")
        val offset = (args["offset"] as? Number)?.toInt() ?: 1
        val limit = ((args["limit"] as? Number)?.toInt() ?: MAX_READ_LINES)
            .coerceIn(1, MAX_READ_LINES)

        return try {
            val file = File(path)
            when {
                !file.exists() -> ToolResult.Error("Path does not exist: $path")
                file.isDirectory -> listDirectory(file, offset, limit)
                file.isFile -> readFile(file, path, offset, limit)
                else -> ToolResult.Error("Path is not a file or directory: $path")
            }
        } catch (e: SecurityException) {
            ToolResult.Error("Permission denied: $path")
        } catch (e: Exception) {
            ToolResult.Error("Failed to read $path: ${e.message}")
        }
    }

    private fun readFile(file: File, resource: String, offset: Int, limit: Int): ToolResult {
        val ext = file.extension.lowercase().let { if (it.isNotEmpty()) ".$it" else "" }
        if (BINARY_EXTENSIONS.contains(ext)) {
            return ToolResult.Error("Cannot read binary file: $resource")
        }

        val bytes = file.readBytes()

        // Quick binary heuristic — null byte or >30% non-printable
        if (isBinary(bytes)) {
            return ToolResult.Error("Cannot read binary file (binary content detected): $resource")
        }

        val allLines = bytes.toString(Charsets.UTF_8).lines()
        val totalLines = allLines.size

        if (offset > totalLines) {
            return ToolResult.Error("Offset $offset is out of range (file has $totalLines lines)")
        }

        val startIdx = (offset - 1).coerceAtLeast(0)
        val selectedLines = allLines.drop(startIdx).take(limit)
        val truncated = startIdx + selectedLines.size < totalLines

        val content = selectedLines.joinToString("\n") { line ->
            if (line.length > MAX_LINE_LENGTH)
                line.take(MAX_LINE_LENGTH) + "... (line truncated to $MAX_LINE_LENGTH chars)"
            else
                line
        }

        val sb = StringBuilder()
        sb.appendLine("File: $resource")
        sb.appendLine("Lines: $offset–${offset + selectedLines.size - 1} of $totalLines")
        if (truncated) sb.appendLine("(truncated — use offset=${offset + selectedLines.size} to continue)")
        sb.appendLine("---")
        sb.append(content)

        return ToolResult.Success(sb.toString())
    }

    private fun listDirectory(dir: File, offset: Int, limit: Int): ToolResult {
        val entries = dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
            ?: return ToolResult.Error("Cannot list directory: ${dir.absolutePath}")

        val total = entries.size
        val startIdx = (offset - 1).coerceAtLeast(0)
        val selected = entries.drop(startIdx).take(limit)
        val truncated = startIdx + selected.size < total

        val sb = StringBuilder()
        sb.appendLine("Directory: ${dir.absolutePath}")
        sb.appendLine("Entries: $offset–${offset + selected.size - 1} of $total")
        if (truncated) sb.appendLine("(truncated — use offset=${offset + selected.size} to continue)")
        sb.appendLine("---")
        selected.forEach { entry ->
            val suffix = if (entry.isDirectory) "/" else ""
            val size = if (entry.isFile) " (${entry.length()} bytes)" else ""
            sb.appendLine("${entry.name}$suffix$size")
        }

        return ToolResult.Success(sb.toString())
    }

    private fun isBinary(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        var nonPrintable = 0
        for (byte in bytes.take(8192)) {  // sample first 8KB
            val b = byte.toInt() and 0xFF
            if (b == 0) return true
            if (b < 9 || (b > 13 && b < 32)) nonPrintable++
        }
        return nonPrintable.toDouble() / bytes.size.coerceAtMost(8192) > 0.3
    }
}