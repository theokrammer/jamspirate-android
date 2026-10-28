package com.example.ai

import com.example.model.ChordContext
import com.example.model.MusicianProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

enum class LlmProvider(val displayName: String, val defaultModel: String, val docsUrl: String) {
    LOCAL_THEORY("Local Theory Engine", "On-Device Algorithmic", ""),
    GEMINI("Google Gemini", "gemini-2.5-flash", "https://aistudio.google.com/app/apikey"),
    OPENAI("ChatGPT (OpenAI)", "gpt-4o-mini", "https://platform.openai.com/api-keys"),
    CLAUDE("Claude (Anthropic)", "claude-3-5-haiku-20241022", "https://console.anthropic.com/settings/keys")
}

enum class AudioOutputDestination(val displayName: String) {
    LOCAL("Local Theory Engine"),
    CLOUD("Cloud AI (Gemini)")
}

data class LookaheadNote(
    val barIndex: Int,
    val subInBar: Int, // 0 to 15 (16th note subdivisions)
    val midiPitch: Int, // e.g. 48 to 84
    val durationSubdivisions: Int, // 1 to 8
    val velocity: Float // 0.3 to 1.0
)

data class LlmGenerationResult(
    val provider: LlmProvider,
    val providerModel: String = "",
    val notes: List<LookaheadNote>,
    val latencyMs: Long,
    val rawSummary: String
)

class CloudLlmClient {

    @Volatile
    private var cachedGeminiModel: String = "gemini-2.5-flash"

    @Volatile
    private var discoveredGeminiModels: List<String> = emptyList()

    fun getResolvedGeminiModel(): String = cachedGeminiModel

    fun getDiscoveredGeminiModels(): List<String> = discoveredGeminiModels

    fun setPreferredGeminiModel(model: String) {
        if (model.isNotBlank()) {
            cachedGeminiModel = model.trim()
        }
    }

    /**
     * Calls ModelService.ListModels (GET /v1beta/models) to inspect all models
     * available to the user's API key that support generateContent.
     */
    fun fetchAvailableGeminiModels(apiKey: String): List<String> {
        val cleanKey = apiKey.trim()
        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$cleanKey"
        val req = Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val errMsg = parseErrorMessage(body)
                throw Exception("ListModels HTTP ${resp.code}: $errMsg")
            }
            val json = JSONObject(body)
            val modelsArr = json.optJSONArray("models") ?: JSONArray()
            val list = mutableListOf<String>()
            for (i in 0 until modelsArr.length()) {
                val obj = modelsArr.optJSONObject(i) ?: continue
                val name = obj.optString("name")
                val clean = name.removePrefix("models/").trim()
                val methods = obj.optJSONArray("supportedGenerationMethods")
                var supportsGen = false
                if (methods != null) {
                    for (m in 0 until methods.length()) {
                        if (methods.optString(m) == "generateContent") {
                            supportsGen = true
                            break
                        }
                    }
                }
                if (supportsGen && clean.isNotEmpty()) {
                    list.add(clean)
                }
            }
            return list
        }
    }

    /**
     * Ranks Gemini models by inference and first-token speed:
     * Flash-lite & Flash-8b are the lowest latency (~100-150ms),
     * followed by Gemini 2.0 Flash (~150-200ms), 2.5 Flash, 1.5 Flash,
     * and heavy Pro models last.
     */
    fun rankModelSpeed(modelName: String): Int {
        val lower = modelName.lowercase()
        return when {
            lower.contains("flash-lite") -> 100
            lower.contains("flash-8b") -> 95
            lower.contains("2.0-flash") -> 90
            lower.contains("2.5-flash") -> 88
            lower.contains("1.5-flash") -> 85
            lower.contains("flash") -> 80
            lower.contains("2.0-pro") -> 50
            lower.contains("2.5-pro") -> 48
            lower.contains("1.5-pro") -> 45
            lower.contains("pro") -> 40
            else -> 10
        }
    }

    /**
     * Chooses the fastest model from the list returned by ModelService.ListModels.
     */
    fun selectFastestGeminiModel(availableModels: List<String>): String {
        if (availableModels.isEmpty()) return cachedGeminiModel
        return availableModels.maxByOrNull { rankModelSpeed(it) } ?: availableModels.first()
    }

    private fun getGeminiModelsToTry(): List<String> {
        val list = mutableListOf<String>()
        if (cachedGeminiModel.isNotBlank()) {
            list.add(cachedGeminiModel)
        }
        if (discoveredGeminiModels.isNotEmpty()) {
            val sorted = discoveredGeminiModels.sortedByDescending { rankModelSpeed(it) }
            for (m in sorted) {
                if (!list.contains(m)) list.add(m)
            }
        } else {
            val defaults = listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-2.0-flash-lite", "gemini-1.5-flash-8b", "gemini-2.5-flash")
            for (m in defaults) {
                if (!list.contains(m)) list.add(m)
            }
        }
        return list
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(provider: LlmProvider, apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be empty."))
        }
        val startTime = System.currentTimeMillis()
        try {
            when (provider) {
                LlmProvider.LOCAL_THEORY -> {
                    Result.success("Local theory engine is built-in and ready.")
                }
                LlmProvider.GEMINI -> {
                    val cleanKey = apiKey.trim()
                    var discoveryInfo = ""
                    try {
                        val available = fetchAvailableGeminiModels(cleanKey)
                        if (available.isNotEmpty()) {
                            discoveredGeminiModels = available
                            val fastest = selectFastestGeminiModel(available)
                            cachedGeminiModel = fastest
                            discoveryInfo = " [Fastest: $fastest (${available.size} models)]"
                        }
                    } catch (_: Exception) {}

                    val result = callGeminiDirect(cleanKey, cachedGeminiModel, "Respond with exact word: PONG", isTest = true)
                    cachedGeminiModel = result.first
                    val latency = System.currentTimeMillis() - startTime
                    Result.success("Connected to Google Gemini (${result.first})$discoveryInfo in ${latency}ms")
                }
                LlmProvider.OPENAI -> {
                    val url = "https://api.openai.com/v1/chat/completions"
                    val bodyJson = JSONObject().apply {
                        put("model", provider.defaultModel)
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", "Ping")
                            })
                        })
                        put("max_tokens", 4)
                    }
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer ${apiKey.trim()}")
                        .post(bodyJson.toString().toRequestBody(jsonMediaType))
                        .build()
                    val response = httpClient.newCall(request).execute()
                    val latency = System.currentTimeMillis() - startTime
                    if (response.isSuccessful) {
                        Result.success("Connected to OpenAI (${provider.defaultModel}) in ${latency}ms")
                    } else {
                        val errBody = response.body?.string() ?: ""
                        Result.failure(Exception("OpenAI HTTP ${response.code}: ${parseErrorMessage(errBody)}"))
                    }
                }
                LlmProvider.CLAUDE -> {
                    val url = "https://api.anthropic.com/v1/messages"
                    val bodyJson = JSONObject().apply {
                        put("model", provider.defaultModel)
                        put("max_tokens", 5)
                        put("messages", JSONArray().apply {
                            put(JSONObject().apply {
                                put("role", "user")
                                put("content", "Ping")
                            })
                        })
                    }
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("x-api-key", apiKey.trim())
                        .addHeader("anthropic-version", "2023-06-01")
                        .post(bodyJson.toString().toRequestBody(jsonMediaType))
                        .build()
                    val response = httpClient.newCall(request).execute()
                    val latency = System.currentTimeMillis() - startTime
                    if (response.isSuccessful) {
                        Result.success("Connected to Claude (${provider.defaultModel}) in ${latency}ms")
                    } else {
                        val errBody = response.body?.string() ?: ""
                        Result.failure(Exception("Claude HTTP ${response.code}: ${parseErrorMessage(errBody)}"))
                    }
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Connection timed out reaching ${provider.displayName} (after 25s). Please check your internet connection or verify your API key."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun buildPrompt(
        musician: MusicianProfile,
        bpm: Int,
        key: String,
        upcomingBars: List<Pair<Int, ChordContext>>,
        customDirective: String = "",
        useFamousPieceMode: Boolean = false,
        timeSignature: String = "4/4",
        beatsPerBar: Int = 4,
        trackTitle: String = "",
        trackStyle: String = "",
        variantLabel: String = ""
    ): String {
        val barsDescription = upcomingBars.joinToString("\n") { (bar, chord) ->
            "- Measure ${bar + 1}: Chord ${chord.chordName} (${if (chord.isMinor) "minor" else "major"}, root pitch class ${chord.rootPitchClass})"
        }

        val trackHeader = if (trackTitle.isNotBlank()) {
            "BACKING TRACK CONTEXT:\nSong Title: \"$trackTitle\"${if (trackStyle.isNotBlank()) " | Style: $trackStyle" else ""}\n"
        } else "BACKING TRACK CONTEXT:\n"

        val variantHeader = if (variantLabel.isNotBlank()) {
            "IMPROVISATION VARIANT: \"$variantLabel\"\n"
        } else ""

        val customBlock = if (customDirective.isNotBlank()) {
            "\nSPECIFIC ARTISTIC / RHYTHMIC DIRECTIVES:\n$customDirective\n"
        } else ""

        val artistStyleBlock = if (useFamousPieceMode && musician.famousPiece.isNotBlank()) {
            """
            SOLOIST IDENTITY:
            You are performing the authentic melody, phrasing, signature tone, and spirit of ${musician.name}'s famous masterpiece:
            "${musician.famousPiece}" (${musician.eraOrGenre}).
            Channel the unmistakable melodic hooks, rhythmic syncopations, and vocal phrasing of "${musician.famousPiece}".
            """.trimIndent()
        } else {
            """
            SOLOIST IDENTITY:
            You are an elite lead soloist performing in the signature style of ${musician.name} (${musician.eraOrGenre}).
            Signature style: ${musician.signatureStyle}
            Target Note Density: ${(musician.noteDensity * 100).roundToInt()}% | Swing: ${(musician.swingFactor * 100).roundToInt()}% | Rest tendency: ${(musician.restProbability * 100).roundToInt()}%
            """.trimIndent()
        }

        val subdivisionsPerBar = (beatsPerBar * 4).coerceAtLeast(4)
        val beatTicks = (0 until beatsPerBar).joinToString(", ") { b -> "${b * 4} = beat ${b + 1}" }

        return """
$trackHeader$variantHeader$artistStyleBlock
$customBlock
HARMONIC & RHYTHMIC SETTING:
Tempo: $bpm BPM | Meter: $timeSignature ($beatsPerBar beats per measure) | Center Key: $key
Full Chord Progression across these measures:
$barsDescription

CRITICAL MUSICAL DIVERSITY & PHRASING RULES:
1. MULTI-MEASURE ARC: Form a coherent musical arc across the measures (e.g. initial motif in Measure 1, variation in Measure 2, climax/peak in Measure 3, melodic resolution in Measure 4).
2. RHYTHMIC VARIANCE: Do NOT repeat the exact same rhythm or pitch pattern in every bar! Combine syncopations, offbeat accents, triplets, eighth-note runs, and sustained expressive notes with breathing rests.
3. HARMONIC TARGETING: Dynamically adapt to each measure's chord tones (roots, 3rds, 7ths) and distinctive color extensions (9ths, 11ths).
4. Subdivisions per bar: $subdivisionsPerBar (4 subdivisions per beat: $beatTicks).
Midi pitches: 48 to 88. dur (16th subdivisions): 1 to 6. vel (velocity): 0.50 to 0.98.

OUTPUT FORMAT (COMPACT - One note per line):
bar:sub:midi:dur:vel

Example:
${upcomingBars.firstOrNull()?.first ?: 0}:0:60:2:0.85
${upcomingBars.firstOrNull()?.first ?: 0}:4:63:2:0.80

Output ONLY the note lines. No code fences, no markdown, no conversational text.
""".trimIndent()
    }

    suspend fun generateLookaheadNotes(
        provider: LlmProvider,
        apiKey: String,
        musician: MusicianProfile,
        bpm: Int,
        key: String,
        upcomingBars: List<Pair<Int, ChordContext>>, // (barIndex, chordContext)
        customDirective: String = "",
        useFamousPieceMode: Boolean = false,
        timeSignature: String = "4/4",
        beatsPerBar: Int = 4,
        trackTitle: String = "",
        trackStyle: String = "",
        variantLabel: String = "",
        temperature: Float = 0.85f,
        onStreamPartialNotes: ((List<LookaheadNote>) -> Unit)? = null
    ): Result<LlmGenerationResult> = withContext(Dispatchers.IO) {
        if (provider == LlmProvider.LOCAL_THEORY || apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No Cloud LLM configured."))
        }

        val startTime = System.currentTimeMillis()
        val prompt = buildPrompt(
            musician = musician,
            bpm = bpm,
            key = key,
            upcomingBars = upcomingBars,
            customDirective = customDirective,
            useFamousPieceMode = useFamousPieceMode,
            timeSignature = timeSignature,
            beatsPerBar = beatsPerBar,
            trackTitle = trackTitle,
            trackStyle = trackStyle,
            variantLabel = variantLabel
        )

        try {
            var activeModelUsed = provider.defaultModel
            val responseText = when (provider) {
                LlmProvider.GEMINI -> {
                    val cleanKey = apiKey.trim()
                    // 1. Proactively discover models if not yet discovered
                    if (discoveredGeminiModels.isEmpty()) {
                        try {
                            val available = fetchAvailableGeminiModels(cleanKey)
                            if (available.isNotEmpty()) {
                                discoveredGeminiModels = available
                                cachedGeminiModel = selectFastestGeminiModel(available)
                            }
                        } catch (_: Exception) {}
                    }

                    // 2. Direct single call to the cached fastest model
                    val modelToUse = cachedGeminiModel.ifBlank { "gemini-2.5-flash" }
                    val pairResult = try {
                        callGeminiDirect(cleanKey, modelToUse, prompt, temperature)
                    } catch (e: Exception) {
                        // If specific model fails (e.g. 404/not found), fallback once to next discovered model
                        val fallback = discoveredGeminiModels.firstOrNull { it != modelToUse } ?: "gemini-1.5-flash"
                        callGeminiDirect(cleanKey, fallback, prompt, temperature)
                    }
                    cachedGeminiModel = pairResult.first
                    activeModelUsed = pairResult.first
                    pairResult.second
                }
                LlmProvider.OPENAI -> {
                    activeModelUsed = "gpt-4o-mini"
                    callOpenAi(apiKey, prompt)
                }
                LlmProvider.CLAUDE -> {
                    activeModelUsed = "claude-3-5-haiku"
                    callClaude(apiKey, prompt)
                }
                LlmProvider.LOCAL_THEORY -> ""
            }

            val latency = System.currentTimeMillis() - startTime
            val parsedNotes = parseNotes(responseText)

            if (parsedNotes.isEmpty()) {
                Result.failure(Exception("Model returned 0 notes or unrecognized format."))
            } else {
                onStreamPartialNotes?.invoke(parsedNotes)
                Result.success(
                    LlmGenerationResult(
                        provider = provider,
                        providerModel = activeModelUsed,
                        notes = parsedNotes,
                        latencyMs = latency,
                        rawSummary = "Generated ${parsedNotes.size} notes across ${upcomingBars.size} bars in ${latency}ms using $activeModelUsed"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a realistic neural/blues AI lookahead preview phrase when an API key is not yet
     * configured, or as a seamless fallback so the Cloud AI staff is never empty. Features chromatic
     * approaches, syncopations, and blue notes distinct from the local diatonic engine, influenced
     * by any user custom directives.
     */
    fun generateAiPhrasePreview(
        musician: MusicianProfile,
        key: String,
        upcomingBars: List<Pair<Int, ChordContext>>,
        octaveShift: Int = 0,
        customDirective: String = ""
    ): List<LookaheadNote> {
        val notes = mutableListOf<LookaheadNote>()
        val baseOctave = 5 + octaveShift
        val isSustained = customDirective.contains("sustain", ignoreCase = true) || customDirective.contains("ambient", ignoreCase = true)
        val defaultDur = if (isSustained) 4 else 2

        for ((barIndex, chord) in upcomingBars) {
            val chordRoot = chord.rootPitchClass
            val isMinor = chord.isMinor
            // Characteristic blues & jazz AI notes: root, b3/3, 4, #4/b5 (blue note), 5, b7
            val bluesOffsets = if (isMinor) listOf(0, 3, 5, 6, 7, 10, 12) else listOf(0, 2, 3, 4, 7, 9, 12)
            val baseMidi = (baseOctave * 12) + chordRoot

            // Expressive syncopated phrasing with chromatic half-step approach into target tones
            // 1. Downbeat phrase start
            val note1 = baseMidi + (bluesOffsets.random())
            notes.add(LookaheadNote(barIndex, 0, note1.coerceIn(48, 88), defaultDur, 0.92f))

            // 2. Chromatic half-step approach into beat 2
            val targetBeat2 = baseMidi + (if (isMinor) 3 else 4) // chord 3rd
            notes.add(LookaheadNote(barIndex, 3, (targetBeat2 - 1).coerceIn(48, 88), 1, 0.74f))
            notes.add(LookaheadNote(barIndex, 4, targetBeat2.coerceIn(48, 88), defaultDur, 0.88f))

            // 3. Blue note or fifth on beat 3 offbeat (syncopated 16th feel)
            val blueNote = baseMidi + (if (isMinor) 6 else 3)
            notes.add(LookaheadNote(barIndex, 7, blueNote.coerceIn(48, 88), 1, 0.82f))
            notes.add(LookaheadNote(barIndex, 8, (baseMidi + 7).coerceIn(48, 88), defaultDur + 1, 0.90f))

            // 4. Melodic resolution / 7th on beat 4
            val note4 = baseMidi + (bluesOffsets.filter { it >= 7 }.randomOrNull() ?: 10)
            notes.add(LookaheadNote(barIndex, 12, note4.coerceIn(48, 88), defaultDur, 0.85f))
            notes.add(LookaheadNote(barIndex, 14, (baseMidi + 12).coerceIn(48, 88), defaultDur, 0.78f))
        }
        return notes
    }

    private fun callGeminiDirect(
        apiKey: String,
        model: String,
        prompt: String,
        temperature: Float = 0.85f,
        isTest: Boolean = false
    ): Pair<String, String> {
        val cleanKey = apiKey.trim()
        val targetModel = model.ifBlank { "gemini-2.5-flash" }
        val genUrl = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$cleanKey"
        val bodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", temperature.toDouble().coerceIn(0.2, 1.4))
                put("maxOutputTokens", if (isTest) 10 else 800)
                put("thinkingConfig", JSONObject().apply {
                    put("thinkingBudget", 0)
                })
            })
        }
        val req = Request.Builder()
            .url(genUrl)
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val errMsg = parseErrorMessage(body)
                throw Exception("$targetModel ($errMsg)")
            }

            val json = JSONObject(body)
            val text = json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            if (text.isNotBlank()) {
                return Pair(targetModel, text)
            } else {
                throw Exception("$targetModel returned empty response")
            }
        }
    }

    private fun callOpenAi(apiKey: String, prompt: String): String {
        val url = "https://api.openai.com/v1/chat/completions"
        val bodyJson = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an expert jazz and rock lead improviser. Output ONLY valid JSON note arrays.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("response_format", JSONObject().put("type", "json_object"))
            put("temperature", 0.7)
            put("max_tokens", 800)
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${apiKey.trim()}")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            throw Exception("OpenAI error (${response.code}): ${parseErrorMessage(body)}")
        }
        val json = JSONObject(body)
        val choices = json.optJSONArray("choices")
        val msg = choices?.optJSONObject(0)?.optJSONObject("message")
        return msg?.optString("content") ?: ""
    }

    private fun callClaude(apiKey: String, prompt: String): String {
        val url = "https://api.anthropic.com/v1/messages"
        val bodyJson = JSONObject().apply {
            put("model", "claude-3-5-haiku-20241022")
            put("max_tokens", 800)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey.trim())
            .addHeader("anthropic-version", "2023-06-01")
            .post(bodyJson.toString().toRequestBody(jsonMediaType))
            .build()
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            throw Exception("Claude error (${response.code}): ${parseErrorMessage(body)}")
        }
        val json = JSONObject(body)
        val content = json.optJSONArray("content")
        return content?.optJSONObject(0)?.optString("text") ?: ""
    }

    fun parseNotes(rawText: String): List<LookaheadNote> {
        val clean = rawText.trim()
            .removePrefix("```text")
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        // 1. Try compact token format: bar:sub:midi:dur:vel (e.g. 0:0:60:2:0.85)
        val compactPattern = Regex("""(\d+)\s*[:|,]\s*(\d+)\s*[:|,]\s*(\d+)\s*[:|,]\s*(\d+)\s*[:|,]\s*([0-9.]+)""")
        val matches = compactPattern.findAll(clean).toList()
        if (matches.isNotEmpty()) {
            val notes = mutableListOf<LookaheadNote>()
            for (m in matches) {
                try {
                    val bar = m.groupValues[1].toInt()
                    val sub = m.groupValues[2].toInt().coerceIn(0, 31)
                    val midi = m.groupValues[3].toInt().coerceIn(36, 96)
                    val dur = m.groupValues[4].toInt().coerceIn(1, 8)
                    val vel = m.groupValues[5].toFloat().coerceIn(0.2f, 1.0f)
                    notes.add(LookaheadNote(bar, sub, midi, dur, vel))
                } catch (_: Exception) {}
            }
            if (notes.isNotEmpty()) return notes
        }

        // 2. Fallback to JSON array format
        return parseNotesFromJson(clean)
    }

    private fun parseNotesFromJson(rawText: String): List<LookaheadNote> {
        val clean = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val notesList = mutableListOf<LookaheadNote>()

        val jsonArray = try {
            if (clean.startsWith("[")) {
                JSONArray(clean)
            } else if (clean.startsWith("{")) {
                val obj = JSONObject(clean)
                obj.optJSONArray("notes") ?: obj.optJSONArray("melody") ?: JSONArray()
            } else {
                JSONArray()
            }
        } catch (_: Exception) {
            JSONArray()
        }

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue
            val bar = item.optInt("bar", item.optInt("barIndex", 0))
            val sub = item.optInt("sub", item.optInt("subdivision", item.optInt("tick", 0))).coerceIn(0, 31)
            val midi = item.optInt("midi", item.optInt("pitch", item.optInt("note", 60))).coerceIn(36, 96)
            val dur = item.optInt("dur", item.optInt("duration", 2)).coerceIn(1, 8)
            val vel = item.optDouble("vel", item.optDouble("velocity", 0.8)).toFloat().coerceIn(0.2f, 1.0f)

            notesList.add(LookaheadNote(bar, sub, midi, dur, vel))
        }

        return notesList
    }

    private fun parseErrorMessage(jsonBody: String): String {
        return try {
            val obj = JSONObject(jsonBody)
            val errObj = obj.optJSONObject("error")
            errObj?.optString("message") ?: obj.optString("message", jsonBody.take(120))
        } catch (_: Exception) {
            jsonBody.take(120)
        }
    }
}
