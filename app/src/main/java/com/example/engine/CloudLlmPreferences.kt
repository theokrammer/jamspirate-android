package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import com.example.model.CloudLlmConfig
import com.example.model.LlmProvider

class CloudLlmPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jamspirate_cloud_llm", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENABLED = "cloud_lookahead_enabled"
        private const val KEY_PROVIDER = "selected_provider"
        private const val KEY_GEMINI_KEY = "api_key_gemini"
        private const val KEY_OPENAI_KEY = "api_key_openai"
        private const val KEY_CLAUDE_KEY = "api_key_claude"
        private const val KEY_LOOKAHEAD_BARS = "lookahead_bars"
        private const val KEY_CUSTOM_MODEL = "custom_model"
    }

    fun loadConfig(): CloudLlmConfig {
        val providerName = prefs.getString(KEY_PROVIDER, LlmProvider.GEMINI.name) ?: LlmProvider.GEMINI.name
        val provider = try {
            LlmProvider.valueOf(providerName)
        } catch (_: Exception) {
            LlmProvider.GEMINI
        }

        return CloudLlmConfig(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            provider = provider,
            geminiApiKey = prefs.getString(KEY_GEMINI_KEY, "").orEmpty(),
            openaiApiKey = prefs.getString(KEY_OPENAI_KEY, "").orEmpty(),
            claudeApiKey = prefs.getString(KEY_CLAUDE_KEY, "").orEmpty(),
            lookaheadBars = prefs.getInt(KEY_LOOKAHEAD_BARS, 2).coerceIn(1, 4),
            customModel = prefs.getString(KEY_CUSTOM_MODEL, "").orEmpty()
        )
    }

    fun saveConfig(config: CloudLlmConfig) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.isEnabled)
            .putString(KEY_PROVIDER, config.provider.name)
            .putString(KEY_GEMINI_KEY, config.geminiApiKey)
            .putString(KEY_OPENAI_KEY, config.openaiApiKey)
            .putString(KEY_CLAUDE_KEY, config.claudeApiKey)
            .putInt(KEY_LOOKAHEAD_BARS, config.lookaheadBars)
            .putString(KEY_CUSTOM_MODEL, config.customModel)
            .apply()
    }

    fun saveApiKey(provider: LlmProvider, key: String) {
        val keyName = when (provider) {
            LlmProvider.GEMINI -> KEY_GEMINI_KEY
            LlmProvider.OPENAI -> KEY_OPENAI_KEY
            LlmProvider.CLAUDE -> KEY_CLAUDE_KEY
        }
        prefs.edit().putString(keyName, key.trim()).apply()
    }
}
