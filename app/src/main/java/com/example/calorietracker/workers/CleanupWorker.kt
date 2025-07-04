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
            // ИСПРАВЛЕНО: Конструктор DataRepository принимает только один аргумент - Context.
            // Переменной 'database' здесь не существует, и она не нужна.
            val repository = DataRepository(applicationContext)

            // Эта строка теперь будет работать, т.к. 'repository' создан правильно.
            // Очищаем старые данные
            repository.cleanupOldData()

            Result.success()
        } catch (e: Exception) {
            // (Рекомендация) Хорошей практикой будет логировать ошибку для отладки.
            // Например: Log.e("CleanupWorker", "Ошибка при очистке старых данных", e)
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
