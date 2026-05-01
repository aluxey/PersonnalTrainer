package com.personaltrainer.exporter

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId

object ExporterPermissions {
    val basePermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class)
    )

    fun backgroundReadAvailable(client: HealthConnectClient): Boolean {
        return client.features.getFeatureStatus(
            HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND
        ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    }

    fun requestedPermissions(client: HealthConnectClient): Set<String> {
        return if (backgroundReadAvailable(client)) {
            basePermissions + PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
        } else {
            basePermissions
        }
    }
}

class HealthConnectExporter(private val client: HealthConnectClient) {
    suspend fun hasRequiredPermissions(includeBackground: Boolean = false): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        val required = if (includeBackground && ExporterPermissions.backgroundReadAvailable(client)) {
            ExporterPermissions.basePermissions + PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
        } else {
            ExporterPermissions.basePermissions
        }
        return granted.containsAll(required)
    }

    suspend fun buildYesterdayPayload(): DailyHealthPayload {
        return buildPayloadForDate(LocalDate.now().minusDays(1))
    }

    suspend fun buildPayloadForDate(date: LocalDate): DailyHealthPayload {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant()

        val aggregate = client.aggregate(
            AggregateRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    SleepSessionRecord.SLEEP_DURATION_TOTAL,
                    ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                    WeightRecord.WEIGHT_AVG,
                    RestingHeartRateRecord.BPM_AVG,
                    ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
                    NutritionRecord.ENERGY_TOTAL,
                    NutritionRecord.PROTEIN_TOTAL,
                    NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL,
                    NutritionRecord.TOTAL_FAT_TOTAL,
                    NutritionRecord.DIETARY_FIBER_TOTAL,
                    HydrationRecord.VOLUME_TOTAL
                ),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )

        val metrics = mutableListOf<MetricEntry>()

        aggregate[StepsRecord.COUNT_TOTAL]?.let {
            metrics += MetricEntry("steps", it.toDouble(), "count")
        }
        aggregate[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.let {
            metrics += MetricEntry("sleep_duration_min", it.toMinutes().toDouble(), "min")
        }
        aggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.let {
            metrics += MetricEntry("active_energy_kcal", it.inKilocalories, "kcal")
        }
        aggregate[WeightRecord.WEIGHT_AVG]?.let {
            metrics += MetricEntry("weight_kg", it.inKilograms, "kg")
        }
        aggregate[RestingHeartRateRecord.BPM_AVG]?.let {
            metrics += MetricEntry("heart_rate_resting_bpm", it.toDouble(), "bpm")
        }
        aggregate[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.let {
            metrics += MetricEntry("workout_duration_min", it.toMinutes().toDouble(), "min")
        }
        aggregate[NutritionRecord.ENERGY_TOTAL]?.let {
            metrics += MetricEntry("calories_intake_kcal", it.inKilocalories, "kcal")
        }
        aggregate[NutritionRecord.PROTEIN_TOTAL]?.let {
            metrics += MetricEntry("protein_g", it.inGrams, "g")
        }
        aggregate[NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL]?.let {
            metrics += MetricEntry("carbs_g", it.inGrams, "g")
        }
        aggregate[NutritionRecord.TOTAL_FAT_TOTAL]?.let {
            metrics += MetricEntry("fat_g", it.inGrams, "g")
        }
        aggregate[NutritionRecord.DIETARY_FIBER_TOTAL]?.let {
            metrics += MetricEntry("fiber_g", it.inGrams, "g")
        }
        aggregate[HydrationRecord.VOLUME_TOTAL]?.let {
            metrics += MetricEntry("water_ml", it.inMilliliters, "ml")
        }

        val exerciseSessions = client.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        if (exerciseSessions.records.isNotEmpty()) {
            metrics += MetricEntry("workout_count", exerciseSessions.records.size.toDouble(), "count")
        }

        return DailyHealthPayload(
            date = date.toString(),
            metrics = metrics
        )
    }
}

class SyncUseCase(context: Context) {
    private val appContext = context.applicationContext
    private val configStore = ConfigStore(appContext)
    private val backendClient = BackendClient()

    suspend fun syncYesterday(): SyncResult {
        val status = HealthConnectClient.getSdkStatus(appContext)
        if (status != HealthConnectClient.SDK_AVAILABLE) {
            error("Health Connect indisponible sur cet appareil. Status=$status")
        }

        val client = HealthConnectClient.getOrCreate(appContext)
        val exporter = HealthConnectExporter(client)
        if (!exporter.hasRequiredPermissions(includeBackground = false)) {
            error("Permissions Health Connect manquantes.")
        }

        val payload = exporter.buildYesterdayPayload()
        if (payload.metrics.isEmpty()) {
            error("Aucune donnee Health Connect trouvee pour hier.")
        }

        val response = backendClient.send(configStore.read(), payload)
        return SyncResult(
            date = payload.date,
            metricCount = payload.metrics.size,
            response = response
        )
    }
}

data class SyncResult(
    val date: String,
    val metricCount: Int,
    val response: String
)
