package com.iris.iriscode.data.remote.gemini

import com.iris.iriscode.domain.model.ChatMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
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

@Singleton
class GeminiClient @Inject constructor() {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Raw streaming of Gemini streamGenerateContent SSE events.
     * Each emission is the JSON payload of a single `data: ...` line.
     * "[DONE]" is filtered out.
     */
    private fun streamRaw(
        apiKey: String,
        model: String,
        body: JSONObject
    ): Flow<String> = callbackFlow {

        val url = "${GeminiApi.BASE_URL}/models/${model}:streamGenerateContent?alt=sse&key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        val listener = object : EventSourceListener() {

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data == "[DONE]") return
                if (trySend(data).isFailure) {
                    close(IOException("Stream buffer overflow"))
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                if (t != null) {
                    trySend("\n\n[Connection error: ${t.message}]")
                }
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = EventSources.createFactory(httpClient)
            .newEventSource(request, listener)

        awaitClose { eventSource.cancel() }
    }.buffer(Channel.UNLIMITED)

    // ─── Build request body ──────────────────────────────────────────────────

    private fun buildRequestBody(
        contents: JSONArray,
        tools: List<GeminiTool> = emptyList(),
        systemPrompt: String? = null
    ): JSONObject = JSONObject().apply {
        put("contents", contents)
        if (tools.isNotEmpty()) {
            val functionDeclarations = JSONArray()
            tools.forEach { functionDeclarations.put(it.toFunctionDeclaration()) }
            put("tools", JSONArray().put(JSONObject().apply {
                put("functionDeclarations", functionDeclarations)
            }))
        }
        if (!systemPrompt.isNullOrBlank()) {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
        }
        put("generationConfig", JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 8192)
        })
    }

    // ─── streamChat — text-only streaming ───────────────────────────────────

    fun streamChat(
        apiKey: String,
        model: String = GeminiApi.MODEL_FLASH,
        history: List<ChatMessage>,
        systemPrompt: String? = null
    ): Flow<String> = flow {
        val contents = chatMessagesToContents(history)
        val body = buildRequestBody(contents, emptyList(), systemPrompt)
        var previousText = ""

        streamRaw(apiKey, model, body).collect { dataString ->
            if (dataString.startsWith("\n\n[")) {
                emit(dataString)
                return@collect
            }
            try {
                val json = JSONObject(dataString)
                val parts = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                val fullText = parts?.optJSONObject(0)?.optString("text", "") ?: ""
                if (fullText.length > previousText.length) {
                    val delta = fullText.substring(previousText.length)
                    previousText = fullText
                    emit(delta)
                }
            } catch (e: Exception) {
                emit("\n\n[Parse error: ${e.message}]")
            }
        }
    }

    fun streamText(
        apiKey: String,
        model: String = GeminiApi.MODEL_FLASH,
        prompt: String,
        systemPrompt: String? = null
    ): Flow<String> = streamChat(apiKey, model, listOf(
        ChatMessage.UserText(id = "prompt", text = prompt)
    ), systemPrompt)

    // ─── streamInteraction — full streaming with function call detection ──

    fun streamInteraction(
        apiKey: String,
        model: String = GeminiApi.MODEL_FLASH,
        history: List<GeminiStep>,
        tools: List<GeminiTool> = emptyList(),
        systemPrompt: String? = null
    ): Flow<GeminiSseEvent> = flow {

        val contents = geminiStepsToContents(history)
        val body = buildRequestBody(contents, tools, systemPrompt)
        var previousText = ""
        val functionCalls = mutableListOf<GeminiStep.FunctionCall>()
        var aborted = false

        streamRaw(apiKey, model, body).collect { dataString ->
            if (dataString.startsWith("\n\n[")) {
                emit(GeminiSseEvent.StreamError(dataString.trimStart('\n')))
                aborted = true
                return@collect
            }
            try {
                val json = JSONObject(dataString)
                val candidates = json.optJSONArray("candidates") ?: return@collect
                val content = candidates
                    .optJSONObject(0)
                    ?.optJSONObject("content") ?: return@collect
                val parts = content.optJSONArray("parts") ?: return@collect

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)

                    if (part.has("text")) {
                        val fullText = part.getString("text")
                        if (fullText.length > previousText.length) {
                            val delta = fullText.substring(previousText.length)
                            previousText = fullText
                            emit(GeminiSseEvent.TextDelta(delta))
                        }
                    }

                    if (part.has("functionCall")) {
                        val fc = part.getJSONObject("functionCall")
                        val name = fc.getString("name")
                        val argsObj = fc.optJSONObject("args") ?: JSONObject()
                        val args = argsObj.keys().asSequence().associateWith { key ->
                            argsObj.get(key)
                        }
                        val fcStep = GeminiStep.FunctionCall(
                            id = "fc_${System.currentTimeMillis()}_${functionCalls.size}",
                            name = name,
                            arguments = args
                        )
                        functionCalls.add(fcStep)
                        emit(GeminiSseEvent.FunctionCallStarted(fcStep.id, fcStep.name))
                    }
                }
            } catch (e: Exception) {
                emit(GeminiSseEvent.StreamError("Parse error: ${e.message}"))
                aborted = true
            }
        }

        if (!aborted) {
            emit(GeminiSseEvent.InteractionCompleted(
                toolCalls = functionCalls.toList(),
                inputTokens = 0,
                outputTokens = previousText.length
            ))
        }
    }

    // ─── Content builders ──────────────────────────────────────────────────

    private fun geminiStepsToContents(history: List<GeminiStep>): JSONArray {
        val contents = JSONArray()
        for (step in history) {
            when (step) {
                is GeminiStep.UserInput -> {
                    contents.put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", step.text)))
                    })
                }
                is GeminiStep.ModelOutput -> {
                    if (step.text.isNotBlank()) {
                        contents.put(JSONObject().apply {
                            put("role", "model")
                            put("parts", JSONArray().put(JSONObject().put("text", step.text)))
                        })
                    }
                }
                is GeminiStep.FunctionCall -> {
                    contents.put(JSONObject().apply {
                        put("role", "model")
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("functionCall", JSONObject().apply {
                                put("name", step.name)
                                put("args", JSONObject(step.arguments))
                            })
                        }))
                    })
                }
                is GeminiStep.FunctionResult -> {
                    contents.put(JSONObject().apply {
                        put("role", "function")
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("functionResponse", JSONObject().apply {
                                put("name", step.name)
                                put("response", JSONObject().apply {
                                    put("output", step.result)
                                })
                            })
                        }))
                    })
                }
            }
        }
        return contents
    }

    private fun chatMessagesToContents(history: List<ChatMessage>): JSONArray {
        val contents = JSONArray()
        for (msg in history) {
            val role: String
            val text: String
            when (msg) {
                is ChatMessage.UserText -> {
                    role = "user"
                    text = msg.text
                }
                is ChatMessage.AgentText -> {
                    if (msg.text.isEmpty()) continue
                    role = "model"
                    text = msg.text
                }
                else -> continue
            }
            contents.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", text)))
            })
        }
        return contents
    }
}
