package com.example.calorietracker.workers

import android.content.Context
import androidx.work.*
import com.example.calorietracker.data.DataRepository
import java.util.concurrent.TimeUnit

class CleanupWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val repository = DataRepository(applicationContext)

            // Очищаем старые данные
            repository.cleanOldData()

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "cleanup_worker"

        /**
         * Запланировать периодическую очистку
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
                1, TimeUnit.DAYS // Раз в день
            )
                .setConstraints(constraints)
                .setInitialDelay(4, TimeUnit.HOURS) // Начать через 4 часа
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
        }
    }
}
