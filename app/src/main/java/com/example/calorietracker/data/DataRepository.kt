package com.example.calorietracker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.calorietracker.data.UserProfile
import com.example.calorietracker.utils.DailyResetUtils
import com.google.gson.Gson

class DataRepository(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("calorie_tracker_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveUserProfile(profile: UserProfile) {
        val json = gson.toJson(profile)
        sharedPreferences.edit { putString("user_profile", json) }
    }

    fun getUserProfile(): UserProfile? {
        val json = sharedPreferences.getString("user_profile", null)
        return if (json != null) gson.fromJson(json, UserProfile::class.java) else null
    }

    fun saveDailyIntake(intake: DailyIntake, date: String = DailyResetUtils.getFoodDate()) {
        val json = gson.toJson(intake)
        sharedPreferences.edit {
            putString("daily_intake_$date", json)
            // Сохраняем дату последнего обновления
            putString("last_intake_date", date)
        }
    }

    fun getDailyIntake(date: String = DailyResetUtils.getFoodDate()): DailyIntake {
        // Проверяем, нужно ли обнулить счетчики
        checkAndResetIfNeeded()

        val json = sharedPreferences.getString("daily_intake_$date", null)
        return if (json != null) gson.fromJson(json, DailyIntake::class.java) else DailyIntake()
    }

    /**
     * Проверяет и обнуляет счетчики если нужно
     */
    private fun checkAndResetIfNeeded() {
        val lastDate = sharedPreferences.getString("last_intake_date", null)
        val currentDate = DailyResetUtils.getFoodDate()

        if (DailyResetUtils.shouldResetCounters(lastDate)) {
            // Обнуляем счетчики для новой даты
            saveDailyIntake(DailyIntake(), currentDate)
        }
    }

    /**
     * Получить историю приемов пищи за конкретную дату
     */
    fun getIntakeHistory(date: String): DailyIntake? {
        val json = sharedPreferences.getString("daily_intake_$date", null)
        return if (json != null) gson.fromJson(json, DailyIntake::class.java) else null
    }

    /**
     * Получить список дат, за которые есть данные
     */
    fun getAvailableDates(): List<String> {
        return sharedPreferences.all.keys
            .filter { it.startsWith("daily_intake_") }
            .map { it.removePrefix("daily_intake_") }
            .sorted()
            .reversed()
    }

    /**
     * Очистить старые данные (старше 30 дней)
     */
    fun cleanOldData() {
        val thirtyDaysAgo = java.time.LocalDate.now().minusDays(30).toString()

        sharedPreferences.all.keys
            .filter { it.startsWith("daily_intake_") }
            .map { it.removePrefix("daily_intake_") }
            .filter { it < thirtyDaysAgo }
            .forEach { date ->
                sharedPreferences.edit { remove("daily_intake_$date") }
            }
    }
}