package com.personaltrainer.exporter

import org.json.JSONArray
import org.json.JSONObject

data class MetricEntry(
    val metric: String,
    val value: Double,
    val unit: String,
    val source: String = "health_connect"
)

data class DailyHealthPayload(
    val date: String,
    val source: String = "health_connect",
    val metrics: List<MetricEntry>
) {
    fun toJson(): String {
        val metricsJson = JSONArray()
        metrics.forEach { entry ->
            metricsJson.put(
                JSONObject()
                    .put("metric", entry.metric)
                    .put("value", entry.value)
                    .put("unit", entry.unit)
                    .put("source", entry.source)
            )
        }

        return JSONObject()
            .put("date", date)
            .put("source", source)
            .put("metrics", metricsJson)
            .toString()
    }
}
