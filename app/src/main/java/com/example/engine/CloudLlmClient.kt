package com.example.engine

import com.example.model.CloudNoteEvent
import com.example.model.LlmProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CloudLlmClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun testConnection(
        provider: LlmProvider,
        apiKey: String,
        customModel: String = ""
    ): Result<Long> = withContext(Dispatchers.IO) {
        val model = if (customModel.isNotBlank()) customModel.trim() else provider.defaultModel
        val startTime = System.currentTimeMillis()

        try {
            val testPrompt = "Generate 1 musical test note in JSON format: [{\"subInBar\":0,\"midiNote\":60,\"noteName\":\"C4\",\"durationBeats\":0.5,\"velocity\":0.8}]"
            val request = buildRequest(provider, apiKey, model, testPrompt, maxTokens = 60)

            client.newCall(request).execute().use { response ->
                val duration = System.currentTimeMillis() - startTime
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val errMsg = extractErrorMessage(body) ?: "HTTP ${response.code}: ${response.message}"
                    return@withContext Result.failure(Exception(errMsg))
                }

                val text = extractGeneratedText(provider, body)
                if (text.isBlank()) {
                    return@withContext Result.failure(Exception("Received empty response from ${provider.displayName}"))
                }

                Result.success(duration)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLookaheadBatch(
        provider: LlmProvider,
        apiKey: String,
        model: String,
        prompt: String
    ): Result<Pair<List<CloudNoteEvent>, String>> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest(provider, apiKey, model, prompt, maxTokens = 1200)

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val errMsg = extractErrorMessage(body) ?: "HTTP ${response.code}: ${response.message}"
                    return@withContext Result.failure(Exception(errMsg))
                }

                val text = extractGeneratedText(provider, body)
                val notes = parseNotesFromJson(text)
                if (notes.isEmpty()) {
                    return@withContext Result.failure(Exception("No parseable note events found in response: $text"))
                }

                Result.success(Pair(notes, text))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildRequest(
        provider: LlmProvider,
        apiKey: String,
        model: String,
        prompt: String,
        maxTokens: Int
    ): Request {
        return when (provider) {
            LlmProvider.GEMINI -> {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val jsonPayload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                        put("maxOutputTokens", maxTokens)
                        put("responseMimeType", "application/json")
                        put("thinkingConfig", JSONObject().apply {
                            put("thinkingBudget", 0)
                        })
                    })
                }
                Request.Builder()
                    .url(url)
                    .post(jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
            }
            LlmProvider.OPENAI -> {
                val url = "https://api.openai.com/v1/chat/completions"
                val jsonPayload = JSONObject().apply {
                    put("model", model)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "You are an elite music improvisation engine that outputs structured JSON note sequences.")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("temperature", 0.7)
                    put("max_tokens", maxTokens)
                    put("response_format", JSONObject().put("type", "json_object"))
                }
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
            }
            LlmProvider.CLAUDE -> {
                val url = "https://api.anthropic.com/v1/messages"
                val jsonPayload = JSONObject().apply {
                    put("model", model)
                    put("max_tokens", maxTokens)
                    put("system", "You are an elite musical improviser. Respond with valid JSON only.")
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("temperature", 0.7)
                }
                Request.Builder()
                    .url(url)
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .post(jsonPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
            }
        }
    }

    private fun extractGeneratedText(provider: LlmProvider, body: String): String {
        return try {
            val json = JSONObject(body)
            when (provider) {
                LlmProvider.GEMINI -> {
                    val candidates = json.optJSONArray("candidates") ?: return ""
                    if (candidates.length() == 0) return ""
                    val content = candidates.getJSONObject(0).optJSONObject("content") ?: return ""
                    val parts = content.optJSONArray("parts") ?: return ""
                    if (parts.length() == 0) return ""
                    parts.getJSONObject(0).optString("text", "")
                }
                LlmProvider.OPENAI -> {
                    val choices = json.optJSONArray("choices") ?: return ""
                    if (choices.length() == 0) return ""
                    val message = choices.getJSONObject(0).optJSONObject("message") ?: return ""
                    message.optString("content", "")
                }
                LlmProvider.CLAUDE -> {
                    val contents = json.optJSONArray("content") ?: return ""
                    if (contents.length() == 0) return ""
                    contents.getJSONObject(0).optString("text", "")
                }
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun extractErrorMessage(body: String): String? {
        return try {
            val json = JSONObject(body)
            if (json.has("error")) {
                val err = json.get("error")
                if (err is JSONObject) {
                    err.optString("message", "").takeIf { it.isNotEmpty() }
                } else {
                    err.toString()
                }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun parseNotesFromJson(rawJsonText: String): List<CloudNoteEvent> {
        val notes = mutableListOf<CloudNoteEvent>()
        if (rawJsonText.isBlank()) return notes

        // Strip markdown code fences if LLM wrapped in ```json ... ```
        var clean = rawJsonText.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json").trim()
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```").trim()
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```").trim()
        }

        try {
            if (clean.startsWith("[")) {
                val array = JSONArray(clean)
                parseJsonArray(array, notes)
            } else if (clean.startsWith("{")) {
                val obj = JSONObject(clean)
                val array = obj.optJSONArray("notes")
                    ?: obj.optJSONArray("sequence")
                    ?: obj.optJSONArray("events")
                if (array != null) {
                    parseJsonArray(array, notes)
                }
            }
        } catch (_: Exception) {
            // Fallback: try to find substring between '[' and ']'
            val startIdx = clean.indexOf('[')
            val endIdx = clean.lastIndexOf(']')
            if (startIdx >= 0 && endIdx > startIdx) {
                try {
                    val slice = clean.substring(startIdx, endIdx + 1)
                    val array = JSONArray(slice)
                    parseJsonArray(array, notes)
                } catch (_: Exception) {}
            }
        }

        return notes
    }

    private fun parseJsonArray(array: JSONArray, result: MutableList<CloudNoteEvent>) {
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val barOffset = item.optInt("barOffset", item.optInt("bar", 0))
            val subInBar = item.optInt("subInBar", item.optInt("step", item.optInt("subdivision", 0))).coerceIn(0, 15)
            val midiNote = item.optInt("midiNote", item.optInt("pitch", item.optInt("note", 60))).coerceIn(36, 96)
            val noteName = item.optString("noteName", "N/A")
            val durationBeats = item.optDouble("durationBeats", item.optDouble("duration", 0.5)).toFloat().coerceIn(0.1f, 4.0f)
            val velocity = item.optDouble("velocity", 0.85).toFloat().coerceIn(0.2f, 1.0f)
            val isBend = item.optBoolean("isBend", item.optBoolean("bend", false))

            result.add(
                CloudNoteEvent(
                    barOffset = barOffset,
                    subInBar = subInBar,
                    midiNote = midiNote,
                    noteName = noteName,
                    durationBeats = durationBeats,
                    velocity = velocity,
                    isBend = isBend
                )
            )
        }
    }
}
