package com.iris.iriscode.data.remote.gemini

import com.iris.iriscode.domain.model.ChatMessage
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

    fun streamChat(
        apiKey: String,
        model: String = GeminiApi.MODEL_FLASH,
        history: List<ChatMessage>,
        systemPrompt: String? = null
    ): Flow<String> = callbackFlow {

        val url = "${GeminiApi.BASE_URL}/models/${model}:streamGenerateContent?alt=sse&key=$apiKey"

        val contents = JSONArray()
        for (msg in history) {
            val role = when (msg) {
                is ChatMessage.UserText -> "user"
                is ChatMessage.AgentText -> if (msg.text.isEmpty()) continue else "model"
                else -> continue
            }
            contents.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
            })
        }

        val body = JSONObject().apply {
            put("contents", contents)
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

        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()

        var previousText = ""

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
                    val candidates = json.optJSONArray("candidates")
                    val parts = candidates
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                    val fullText = parts?.optJSONObject(0)?.optString("text", "") ?: ""

                    if (fullText.length > previousText.length) {
                        val delta = fullText.substring(previousText.length)
                        previousText = fullText
                        trySend(delta)
                    }
                } catch (e: Exception) {
                    trySend("\n\n[Parse error: ${e.message}]")
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                val msg = t?.message ?: response?.let {
                    "HTTP ${it.code}: ${it.body?.string()?.take(200)}"
                } ?: "Unknown error"
                trySend("\n\n[Connection error: $msg]")
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = EventSources.createFactory(httpClient)
            .newEventSource(request, listener)

        awaitClose { eventSource.cancel() }
    }

    fun streamText(
        apiKey: String,
        model: String = GeminiApi.MODEL_FLASH,
        prompt: String,
        systemPrompt: String? = null
    ): Flow<String> = streamChat(apiKey, model, listOf(
        ChatMessage.UserText(id = "prompt", text = prompt)
    ), systemPrompt)
}
