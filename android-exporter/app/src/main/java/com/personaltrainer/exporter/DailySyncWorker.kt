package com.personaltrainer.exporter

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailySyncWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        return try {
            SyncUseCase(applicationContext).syncYesterday()
            Result.success()
        } catch (error: Throwable) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
