// Inspired by: anomalyco/opencode agent tool system
// UNTESTED — verify before use

package com.iris.iriscode.domain.agent

import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.Schema

/**
 * Base interface for all agent tools.
 * Each tool maps to a Gemini FunctionDeclaration.
 * domain/ layer — no Android imports except firebase-ai Schema types.
 */
interface IrisTool {
    val name: String
    val description: String

    /** Build the Gemini FunctionDeclaration for this tool. */
    fun toFunctionDeclaration(): FunctionDeclaration

    /**
     * Execute the tool with the given arguments.
     * Called by ToolRegistry when Gemini emits a function_call for this tool's name.
     * Must be called from a coroutine context.
     *
     * @param args Map of parameter name → value as parsed from Gemini's function_call response.
     * @return ToolResult describing the outcome.
     */
    suspend fun execute(args: Map<String, Any>): ToolResult
}