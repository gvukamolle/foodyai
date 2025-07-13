package com.example.calorietracker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.calorietracker.utils.DailyResetUtils
import com.google.gson.Gson
import com.example.calorietracker.ChatMessage
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate

/**
 * Репозиторий для управления данными приложения, такими как профиль пользователя и дневное потребление калорий.
 * Все данные хранятся в SharedPreferences.
 * @param context Контекст приложения, необходимый для доступа к SharedPreferences.
 */
class DataRepository(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("calorie_tracker_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Временное хранение данных календаря в SharedPreferences
    fun saveDailySummary(
        date: LocalDate = DailyResetUtils.getFoodLocalDate(),
        calories: Int,
        protein: Float,
        fat: Float,
        carbs: Float,
        mealsCount: Int
    ) {
        val summary = DailyNutritionSummary(
            date = date,
            totalCalories = calories,
            totalProtein = protein,
            totalFat = fat,
            totalCarbs = carbs,
            mealsCount = mealsCount
        )
        val json = gson.toJson(summary)
        sharedPreferences.edit {
            putString("daily_summary_${date.toEpochDay()}", json)
        }
    }

    // Получение данных для календаря
    fun getCalendarData(months: Int = 3): Flow<List<DailyNutritionSummary>> = flow {
        val endDate = DailyResetUtils.getFoodLocalDate()
        val startDate = endDate.minusMonths(months.toLong())

        val summaries = mutableListOf<DailyNutritionSummary>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            val json = sharedPreferences.getString("daily_summary_${currentDate.toEpochDay()}", null)
            if (json != null) {
                val summary = gson.fromJson(json, DailyNutritionSummary::class.java)
                summaries.add(summary)
            }
            currentDate = currentDate.plusDays(1)
        }

        emit(summaries)
    }

    // Автоматическая очистка старых данных (старше 120 дней)
    fun cleanupOldData() {
        val cutoffDate = LocalDate.now().minusDays(120)
        val cutoffEpochDay = cutoffDate.toEpochDay()

        sharedPreferences.all.keys
            .filter { it.startsWith("daily_summary_") }
            .mapNotNull { key ->
                val epochDay = key.removePrefix("daily_summary_").toLongOrNull()
                if (epochDay != null && epochDay < cutoffEpochDay) key else null
            }
            .forEach { key ->
                sharedPreferences.edit { remove(key) }
            }
    }

    fun saveUserProfile(profile: UserProfile) {
        val json = gson.toJson(profile)
        sharedPreferences.edit { putString("user_profile", json) }
    }

    fun getUserProfile(): UserProfile? {
        val json = sharedPreferences.getString("user_profile", null)
        return if (json != null) gson.fromJson(json, UserProfile::class.java) else null
    }

    // ---------- Chat history methods ----------
    fun saveChatHistory(messages: List<ChatMessage>, date: String = DailyResetUtils.getCurrentDisplayDate()) {
        val json = gson.toJson(messages)
        sharedPreferences.edit {
            putString("chat_history_$date", json)
        }
    }

    fun getChatHistory(date: String = DailyResetUtils.getCurrentDisplayDate()): List<ChatMessage> {
        val json = sharedPreferences.getString("chat_history_$date", null)
        return if (json != null) {
            val type = object : TypeToken<List<ChatMessage>>() {}.type
            gson.fromJson(json, type)
        } else emptyList()
    }

    fun cleanupOldChatHistory(keepDate: String = DailyResetUtils.getCurrentDisplayDate()) {
        sharedPreferences.all.keys
            .filter { it.startsWith("chat_history_") && it.removePrefix("chat_history_") != keepDate }
            .forEach { key ->
                sharedPreferences.edit { remove(key) }
            }
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

    // ---------- Новые методы для редактирования истории ----------

    private data class Totals(
        val calories: Int,
        val protein: Float,
        val fat: Float,
        val carbs: Float
    )

    private fun calculateTotals(meals: List<com.example.calorietracker.Meal>): Totals {
        var calories = 0
        var protein = 0f
        var fat = 0f
        var carbs = 0f
        meals.forEach { meal ->
            meal.foods.forEach { food ->
                calories += food.calories
                protein += food.protein.toFloat()
                fat += food.fat.toFloat()
                carbs += food.carbs.toFloat()
            }
        }
        return Totals(calories, protein, fat, carbs)
    }

    private fun updateSummary(date: String, intake: DailyIntake) {
        val totals = calculateTotals(intake.meals)
        saveDailySummary(
            date = LocalDate.parse(date),
            calories = totals.calories,
            protein = totals.protein,
            fat = totals.fat,
            carbs = totals.carbs,
            mealsCount = intake.meals.size
        )
    }

    fun updateMeal(date: String, index: Int, meal: com.example.calorietracker.Meal) {
        val intake = getIntakeHistory(date) ?: return
        val meals = intake.meals.toMutableList()
        if (index !in meals.indices) return
        meals[index] = meal
        val totals = calculateTotals(meals)
        val updated = DailyIntake(
            calories = totals.calories,
            protein = totals.protein,
            carbs = totals.carbs,
            fat = totals.fat,
            meals = meals
        )
        saveDailyIntake(updated, date)
        updateSummary(date, updated)
    }

    fun deleteMeal(date: String, index: Int) {
        val intake = getIntakeHistory(date) ?: return
        val meals = intake.meals.toMutableList()
        if (index !in meals.indices) return
        meals.removeAt(index)
        val totals = calculateTotals(meals)
        val updated = DailyIntake(
            calories = totals.calories,
            protein = totals.protein,
            carbs = totals.carbs,
            fat = totals.fat,
            meals = meals
        )
        saveDailyIntake(updated, date)
        updateSummary(date, updated)
    }
}