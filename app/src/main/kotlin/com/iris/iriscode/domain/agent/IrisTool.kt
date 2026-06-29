// UNTESTED — verify before use

package com.iris.iriscode.domain.agent

import com.iris.iriscode.data.remote.gemini.GeminiTool

/**
 * Base interface for all agent tools.
 * domain/ katmanı — GeminiTool data class'ı domain'den geçiyor,
 * ancak GeminiTool pure Kotlin (JSONObject kullanmıyor burada).
 */
interface IrisTool {
    val name: String
    val description: String

    /** Build the GeminiTool descriptor for this tool (sent to Gemini as function declaration). */
    fun toGeminiTool(): GeminiTool

    /**
     * Execute this tool with the given arguments.
     * @param args Map parsed from Gemini's function_call arguments JSON.
     */
    suspend fun execute(args: Map<String, Any>): ToolResult
}