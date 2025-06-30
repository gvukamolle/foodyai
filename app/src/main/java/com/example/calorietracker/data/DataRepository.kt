
package com.example.calorietracker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.calorietracker.utils.DailyResetUtils
import com.google.gson.Gson
import java.time.LocalDate
import com.example.calorietracker.ui.theme.ThemeMode


/**
 * Репозиторий для управления данными приложения, такими как профиль пользователя и дневное потребление калорий.
 * Все данные хранятся в SharedPreferences.
 * @param context Контекст приложения, необходимый для доступа к SharedPreferences.
 */
class DataRepository(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("calorie_tracker_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    //region Theme mode
    fun saveThemeMode(mode: ThemeMode) {
        sharedPreferences.edit { putString("theme_mode", mode.name) }
    }

    fun getThemeMode(): ThemeMode {
        val stored = sharedPreferences.getString("theme_mode", ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(stored ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }
    //endregion

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
            // Сохраняем "пищевую" дату последнего обновления
            putString("last_intake_date", date)
        }
    }

    /**
     * Получает данные о потреблении за указанную дату.
     * Перед этим запускает проверку, не наступил ли новый "пищевой" день для сброса счетчиков.
     * @param date Дата в формате "YYYY-MM-DD". По умолчанию - текущая "пищевая" дата.
     * @return Объект [DailyIntake]. Если данных нет, вернется пустой объект.
     */
    fun getDailyIntake(date: String = DailyResetUtils.getFoodDate()): DailyIntake {
        // Проверяем, нужно ли обнулить счетчики
        performResetIfNeeded()
        val json = sharedPreferences.getString("daily_intake_$date", null)
        return if (json != null) gson.fromJson(json, DailyIntake::class.java) else DailyIntake()
    }

    /**
     * Проверяет, наступил ли новый "пищевой" день, и если да, сбрасывает счетчики.
     * @return `true` если сброс был выполнен, иначе `false`.
     */
    fun performResetIfNeeded(): Boolean {
        val lastDate = sharedPreferences.getString("last_intake_date", null)
        if (DailyResetUtils.shouldResetCounters(lastDate)) {
            // Обнуляем счетчики для новой "пищевой" даты
            val currentDate = DailyResetUtils.getFoodDate()
            saveDailyIntake(DailyIntake(), currentDate)
            return true // Сообщаем, что сброс произошел
        }
        return false // Сброс не требовался
    }

    /**
     * Получить историю приемов пищи за конкретную дату без проверки на сброс.
     */
    fun getIntakeHistory(date: String): DailyIntake? {
        val json = sharedPreferences.getString("daily_intake_$date", null)
        return if (json != null) gson.fromJson(json, DailyIntake::class.java) else null
    }

    /**
     * Получить список дат (в формате "YYYY-MM-DD"), за которые есть данные,
     * отсортированный от новой к старой.
     */
    fun getAvailableDates(): List<String> {
        return sharedPreferences.all.keys
            .filter { it.startsWith("daily_intake_") }
            .map { it.removePrefix("daily_intake_") }
            .sorted()
            .reversed()
    }

    /**
     * Очищает старые данные о приемах пищи (старше 30 дней).
     */
    fun cleanOldData() {
        // Получаем дату, которая была 30 дней назад
        val thirtyDaysAgo = LocalDate.now().minusDays(30)

        sharedPreferences.all.keys
            .filter { it.startsWith("daily_intake_") }
            .map { it.removePrefix("daily_intake_") }
            .filter { dateString ->
                try {
                    // Превращаем строку "YYYY-MM-DD" в полноценный объект даты
                    val entryDate = LocalDate.parse(dateString)
                    // Корректно сравниваем, была ли дата записи раньше, чем 30 дней назад
                    entryDate.isBefore(thirtyDaysAgo)
                } catch (e: Exception) {
                    // Если вдруг ключ имеет неверный формат, игнорируем его
                    false
                }
            }
            .forEach { date ->
                sharedPreferences.edit { remove("daily_intake_$date") }
            }
    }
}