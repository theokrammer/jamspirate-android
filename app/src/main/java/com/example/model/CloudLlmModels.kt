package com.example.model

enum class LlmProvider(
    val id: String,
    val displayName: String,
    val company: String,
    val defaultModel: String,
    val keyHint: String,
    val docsUrl: String
) {
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        company = "Google DeepMind",
        defaultModel = "gemini-2.5-flash",
        keyHint = "AIzaSy...",
        docsUrl = "https://aistudio.google.com/app/apikey"
    ),
    OPENAI(
        id = "openai",
        displayName = "OpenAI ChatGPT",
        company = "OpenAI",
        defaultModel = "gpt-4o-mini",
        keyHint = "sk-proj-...",
        docsUrl = "https://platform.openai.com/api-keys"
    ),
    CLAUDE(
        id = "claude",
        displayName = "Anthropic Claude",
        company = "Anthropic",
        defaultModel = "claude-3-5-haiku-20241022",
        keyHint = "sk-ant-...",
        docsUrl = "https://console.anthropic.com/settings/keys"
    )
}

data class CloudNoteEvent(
    val barOffset: Int = 0,     // 0 = first requested bar, 1 = second bar
    val subInBar: Int = 0,      // 0 to 15 (16th note subdivision within the bar)
    val midiNote: Int = 60,     // MIDI pitch (e.g. 60 = C4, 69 = A4)
    val noteName: String = "C4",
    val durationBeats: Float = 0.5f, // 0.25 = 16th, 0.5 = 8th, 1.0 = quarter note
    val velocity: Float = 0.85f,     // 0.1 to 1.0 dynamic volume
    val isBend: Boolean = false
)

data class CloudLookaheadStatus(
    val isEnabled: Boolean = false,
    val isRequesting: Boolean = false,
    val currentProvider: LlmProvider = LlmProvider.GEMINI,
    val activeModel: String = "gemini-2.5-flash",
    val lastLatencyMs: Long = 0L,
    val bufferedBarsSummary: String = "Idle",
    val lastNoteCount: Int = 0,
    val fallbackCount: Int = 0,
    val lastError: String? = null,
    val lastPromptSent: String = "",
    val lastRawResponse: String = ""
)

data class CloudLlmConfig(
    val isEnabled: Boolean = false,
    val provider: LlmProvider = LlmProvider.GEMINI,
    val geminiApiKey: String = "",
    val openaiApiKey: String = "",
    val claudeApiKey: String = "",
    val lookaheadBars: Int = 2,
    val customModel: String = ""
) {
    fun getApiKeyForCurrentProvider(): String {
        return when (provider) {
            LlmProvider.GEMINI -> geminiApiKey
            LlmProvider.OPENAI -> openaiApiKey
            LlmProvider.CLAUDE -> claudeApiKey
        }
    }

    fun getEffectiveModel(): String {
        if (customModel.isNotBlank()) return customModel.trim()
        return provider.defaultModel
    }
}
