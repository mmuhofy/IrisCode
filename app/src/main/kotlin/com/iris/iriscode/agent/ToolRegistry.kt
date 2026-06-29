// UNTESTED — verify before use

package com.iris.iriscode.agent

import com.iris.iriscode.agent.tool.AskUserTool
import com.iris.iriscode.agent.tool.BashTool
import com.iris.iriscode.agent.tool.ReadFileTool
import com.iris.iriscode.agent.tool.WriteFileTool
import com.iris.iriscode.data.remote.gemini.GeminiTool
import com.iris.iriscode.domain.agent.IrisTool
import com.iris.iriscode.domain.agent.ToolResult
import com.iris.iriscode.domain.model.WorkMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(
    val readFileTool: ReadFileTool,
    val writeFileTool: WriteFileTool,
    val bashTool: BashTool,
    val askUserTool: AskUserTool
) {
    private val tools: Map<String, IrisTool> = mapOf(
        readFileTool.name to readFileTool,
        writeFileTool.name to writeFileTool,
        bashTool.name to bashTool,
        askUserTool.name to askUserTool
    )

    /**
     * Returns the list of GeminiTool declarations to send to the model.
     * Filters based on WorkMode:
     * - PLAN: only read_file and ask_user exposed
     * - BUILD/AUTO: all tools exposed
     */
    fun getToolDeclarations(mode: WorkMode): List<GeminiTool> {
        return tools.values
            .filter { tool -> isAllowed(tool.name, mode) }
            .map { it.toGeminiTool() }
    }

    /**
     * Execute a tool call from Gemini.
     * Enforces WorkMode gate before execution.
     */
    suspend fun execute(
        toolName: String,
        args: Map<String, Any>,
        mode: WorkMode
    ): ToolResult {
        if (!isAllowed(toolName, mode)) {
            // PLAN mode — silently cancel destructive tools
            return ToolResult.Cancelled(
                reason = "Tool '$toolName' is disabled in ${mode.displayName} mode"
            )
        }

        val tool = tools[toolName]
            ?: return ToolResult.Error("Unknown tool: $toolName")

        return tool.execute(args)
    }

    fun getTool(name: String): IrisTool? = tools[name]

    private fun isAllowed(toolName: String, mode: WorkMode): Boolean = when (mode) {
        WorkMode.PLAN -> toolName == "read_file" || toolName == "ask_user"
        WorkMode.BUILD, WorkMode.AUTO -> true
    }
}