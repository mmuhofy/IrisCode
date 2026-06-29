// Inspired by: https://ai.google.dev/gemini-api/docs/function-calling (Interactions API REST)
// UNTESTED — verify before use

package com.iris.iriscode.data.remote.gemini

import org.json.JSONArray
import org.json.JSONObject

// ─── Constants ────────────────────────────────────────────────────────────────

object GeminiApi {
    const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    const val INTERACTIONS_ENDPOINT = "$BASE_URL/interactions"
    const val MODEL_FLASH = "gemini-2.5-flash"

    // SSE event prefix
    const val SSE_DATA_PREFIX = "data: "
}

// ─── Request models ───────────────────────────────────────────────────────────

/**
 * A single step in the conversation history.
 * Maps to the Interactions API step shape.
 */
sealed class GeminiStep {

    /** User message turn */
    data class UserInput(val text: String) : GeminiStep()

    /** Model text response */
    data class ModelOutput(val text: String) : GeminiStep()

    /** Model decided to call a function */
    data class FunctionCall(
        val id: String,
        val name: String,
        val arguments: Map<String, Any>
    ) : GeminiStep()

    /** We are returning the result of a function call to the model */
    data class FunctionResult(
        val callId: String,
        val name: String,
        val result: String
    ) : GeminiStep()

    fun toJson(): JSONObject = when (this) {
        is UserInput -> JSONObject().apply {
            put("type", "user_input")
            put("content", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", text)
                })
            })
        }

        is ModelOutput -> JSONObject().apply {
            put("type", "model_output")
            put("content", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", text)
                })
            })
        }

        is FunctionCall -> JSONObject().apply {
            put("type", "function_call")
            put("id", id)
            put("name", name)
            put("arguments", JSONObject(arguments))
        }

        is FunctionResult -> JSONObject().apply {
            put("type", "function_result")
            put("name", name)
            put("call_id", callId)
            put("result", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "text")
                    put("text", result)
                })
            })
        }
    }
}

/**
 * Tool definition sent to Gemini.
 * Corresponds to a FunctionDeclaration in the Interactions API.
 */
data class GeminiTool(
    val name: String,
    val description: String,
    val parameters: GeminiToolParameters
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", "function")
        put("name", name)
        put("description", description)
        put("parameters", parameters.toJson())
    }
}

data class GeminiToolParameters(
    val properties: Map<String, GeminiToolProperty>,
    val required: List<String> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", "object")
        val props = JSONObject()
        properties.forEach { (key, value) -> props.put(key, value.toJson()) }
        put("properties", props)
        put("required", JSONArray(required))
    }
}

data class GeminiToolProperty(
    val type: String,         // "string", "integer", "boolean", "array"
    val description: String,
    val enumValues: List<String> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type)
        put("description", description)
        if (enumValues.isNotEmpty()) put("enum", JSONArray(enumValues))
    }
}

// ─── SSE Event models ─────────────────────────────────────────────────────────

/**
 * Parsed SSE event from the Interactions API streaming response.
 */
sealed class GeminiSseEvent {

    /** A text chunk streamed from a model_output step */
    data class TextDelta(val text: String) : GeminiSseEvent()

    /**
     * A function_call step arrived (may be partial args while streaming).
     * Once interaction.completed fires, args are fully assembled.
     */
    data class FunctionCallStarted(
        val id: String,
        val name: String,
        val partialArgs: String = ""
    ) : GeminiSseEvent()

    /** Partial function call arguments delta */
    data class FunctionCallArgsDelta(
        val index: Int,
        val partial: String
    ) : GeminiSseEvent()

    /** Streaming is complete — full tool calls are ready */
    data class InteractionCompleted(
        val toolCalls: List<GeminiStep.FunctionCall>,
        val inputTokens: Int,
        val outputTokens: Int
    ) : GeminiSseEvent()

    /** Non-streaming complete response (no tool calls) */
    data class TextCompleted(
        val fullText: String,
        val inputTokens: Int,
        val outputTokens: Int
    ) : GeminiSseEvent()

    data class StreamError(val message: String) : GeminiSseEvent()
}