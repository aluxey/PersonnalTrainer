package com.personaltrainer.exporter

import android.content.Context

data class AppConfig(
    val backendUrl: String,
    val apiKey: String
) {
    val isComplete: Boolean
        get() = backendUrl.startsWith("http") && apiKey.length >= 16
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
        preferences.edit()
            .putString(KEY_BACKEND_URL, config.backendUrl.trim().trimEnd('/'))
            .putString(KEY_API_KEY, config.apiKey.trim())
            .apply()
    }

    companion object {
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_API_KEY = "api_key"
    }
}
