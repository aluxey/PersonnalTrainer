package com.personaltrainer.exporter

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackendClient {
    suspend fun send(config: AppConfig, payload: DailyHealthPayload): String = withContext(Dispatchers.IO) {
        require(config.isComplete) { "Backend URL ou API key manquante." }

        val endpoint = URL("${config.backendUrl.trimEnd('/')}/api/ingest/health-connect")
        val connection = endpoint.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.doOutput = true
        connection.setRequestProperty("content-type", "application/json")
        connection.setRequestProperty("x-api-key", config.apiKey)

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toJson())
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()
        connection.disconnect()

        if (responseCode !in 200..299) {
            error("Backend HTTP $responseCode: $responseBody")
        }

        responseBody.ifBlank { "OK" }
    }
}
