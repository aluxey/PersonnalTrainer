package com.personaltrainer.exporter

import android.content.Context

data class AppConfig(
    val backendUrl: String,
    val apiKey: String
) {
    val isComplete: Boolean
        get() = backendUrl.trim().startsWith("http") && apiKey.trim().length >= 16

    fun normalized(): AppConfig {
        return AppConfig(
            backendUrl = backendUrl.trim().trimEnd('/'),
            apiKey = apiKey.trim()
        )
    }
}

class ConfigStore(context: Context) {
    private val preferences = context.getSharedPreferences("personal_trainer_exporter", Context.MODE_PRIVATE)

    fun read(): AppConfig {
        return AppConfig(
            backendUrl = preferences.getString(KEY_BACKEND_URL, "") ?: "",
            apiKey = preferences.getString(KEY_API_KEY, "") ?: ""
        )
    }

    fun save(config: AppConfig) {
        val normalized = config.normalized()
        preferences.edit()
            .putString(KEY_BACKEND_URL, normalized.backendUrl)
            .putString(KEY_API_KEY, normalized.apiKey)
            .apply()
    }

    companion object {
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_API_KEY = "api_key"
    }
}
