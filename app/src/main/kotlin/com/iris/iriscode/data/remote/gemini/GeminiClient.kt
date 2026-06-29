// Inspired by: https://ai.google.dev/gemini-api/docs/function-calling (Interactions API REST)
// UNTESTED — verify before use

package com.iris.iriscode.data.remote.gemini

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-level Gemini Interactions API client.
 * Handles SSE streaming, function call event parsing, and history serialization.
 *
 * All public methods return Flow<GeminiSseEvent> — caller (AgentLoop) drives the loop.
 */
@Singleton
class GeminiClient @Inject constructor() {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Send a streaming request to the Interactions API.
     *
     * @param apiKey   User's Gemini API key
     * @param model    Model string, e.g. "gemini-2.5-flash"
     * @param history  Full conversation history (stateless — we manage it)
     * @param tools    Tool declarations to expose to the model
     * @param systemPrompt  Optional system instruction
     */
    fun streamInteraction(
        apiKey: String,
        model: String = GeminiApi.MODEL_FLASH,
        history: List<GeminiStep>,
        tools: List<GeminiTool>,
        systemPrompt: String? = null
    ): Flow<GeminiSseEvent> = callbackFlow {

        val body = buildRequestBody(model, history, tools, systemPrompt)
        val url = "${GeminiApi.INTERACTIONS_ENDPOINT}?alt=sse"

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("x-goog-api-key", apiKey)
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        // Accumulate streaming function call args per index
        val pendingCalls = mutableMapOf<Int, MutableMap<String, Any>>()

        val listener = object : EventSourceListener() {

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data == "[DONE]") return
                try {
                    val json = JSONObject(data)
                    parseEvent(json, pendingCalls)?.let { event ->
                        trySend(event)
                    }
                } catch (e: Exception) {
                    trySend(GeminiSseEvent.StreamError("Parse error: ${e.message}"))
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                val msg = t?.message ?: response?.let {
                    "HTTP ${it.code}: ${it.body?.string()?.take(200)}"
                } ?: "Unknown SSE failure"
                trySend(GeminiSseEvent.StreamError(msg))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = EventSources.createFactory(httpClient)
            .newEventSource(request, listener)

        awaitClose {
            eventSource.cancel()
        }
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun buildRequestBody(
        model: String,
        history: List<GeminiStep>,
        tools: List<GeminiTool>,
        systemPrompt: String?
    ): JSONObject = JSONObject().apply {
        put("model", model)
        put("store", false)   // stateless — we own the history
        put("stream", true)

        // History as input array
        val inputArray = JSONArray()
        history.forEach { step -> inputArray.put(step.toJson()) }
        put("input", inputArray)

        // Tool declarations
        if (tools.isNotEmpty()) {
            val toolsArray = JSONArray()
            tools.forEach { tool -> toolsArray.put(tool.toJson()) }
            put("tools", toolsArray)
        }

        // System instruction
        if (!systemPrompt.isNullOrBlank()) {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemPrompt)
                    })
                })
            })
        }
    }

    /**
     * Parse a single SSE JSON event into a GeminiSseEvent.
     * Returns null for events we don't need to surface (e.g. ping, thought steps).
     */
    private fun parseEvent(
        json: JSONObject,
        pendingCalls: MutableMap<Int, MutableMap<String, Any>>
    ): GeminiSseEvent? {
        val eventType = json.optString("event_type")

        return when (eventType) {

            "step.start" -> {
                val step = json.optJSONObject("step") ?: return null
                val type = step.optString("type")
                val index = json.optInt("index", -1)

                if (type == "function_call") {
                    val callId = step.optString("id")
                    val name = step.optString("name")
                    pendingCalls[index] = mutableMapOf(
                        "id" to callId,
                        "name" to name,
                        "args" to ""
                    )
                    // Check if args arrived with step.start
                    val argsObj = step.optJSONObject("arguments")
                    if (argsObj != null) {
                        pendingCalls[index]!!["args"] = argsObj.toString()
                    }
                    GeminiSseEvent.FunctionCallStarted(callId, name)
                } else null
            }

            "step.delta" -> {
                val delta = json.optJSONObject("delta") ?: return null
                val type = delta.optString("type")
                val index = json.optInt("index", -1)

                when (type) {
                    "text" -> {
                        val text = delta.optString("text")
                        if (text.isNotEmpty()) GeminiSseEvent.TextDelta(text) else null
                    }
                    "arguments" -> {
                        val partial = delta.optString("partial_arguments", "")
                        if (partial.isNotEmpty()) {
                            pendingCalls[index]?.let { call ->
                                call["args"] = (call["args"] as? String ?: "") + partial
                            }
                            GeminiSseEvent.FunctionCallArgsDelta(index, partial)
                        } else null
                    }
                    else -> null
                }
            }

            "interaction.completed", "interaction.complete" -> {
                // Assemble completed tool calls
                val toolCalls = pendingCalls.values.mapNotNull { call ->
                    val id = call["id"] as? String ?: return@mapNotNull null
                    val name = call["name"] as? String ?: return@mapNotNull null
                    val argsStr = call["args"] as? String ?: "{}"
                    val argsMap = try {
                        val obj = JSONObject(argsStr)
                        obj.keys().asSequence().associateWith { obj.get(it) }
                    } catch (e: Exception) {
                        emptyMap()
                    }
                    GeminiStep.FunctionCall(id, name, argsMap)
                }

                // Token usage
                val usage = json.optJSONObject("usage_metadata")
                val inputTokens = usage?.optInt("prompt_token_count", 0) ?: 0
                val outputTokens = usage?.optInt("candidates_token_count", 0) ?: 0

                GeminiSseEvent.InteractionCompleted(toolCalls, inputTokens, outputTokens)
            }

            else -> null
        }
    }
}