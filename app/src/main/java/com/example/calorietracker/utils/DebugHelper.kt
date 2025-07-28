package com.example.calorietracker.utils

import android.util.Log
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.FoodItem
import com.example.calorietracker.MealType
import com.example.calorietracker.network.LogFoodRequest
import com.example.calorietracker.network.FoodItemData
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.calorietracker.extensions.toNetworkProfile


/**
 * Утилита для проверки критических функций приложения
 */
object DebugHelper {

    private const val TAG = "CalorieTrackerDebug"

    /**
     * Проверить правильность установки источника данных при разных способах ввода
     */
    fun checkFoodSourceFlow(viewModel: CalorieTrackerViewModel) {
        Log.d(TAG, "=== ПРОВЕРКА ИСТОЧНИКОВ ДАННЫХ ===")
        Log.d(TAG, "Текущий источник: ${viewModel.currentFoodSource}")
        Log.d(TAG, "Есть pendingFood: ${viewModel.pendingFood != null}")
        Log.d(TAG, "Есть prefillFood: ${viewModel.prefillFood != null}")
    }

    /**
     * Проверить формирование запроса на сервер
     */
    fun checkServerRequest(viewModel: CalorieTrackerViewModel, food: FoodItem, mealType: MealType) {
        Log.d(TAG, "=== ПРОВЕРКА ЗАПРОСА НА СЕРВЕР ===")

        val now = LocalDateTime.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val foodItemData = FoodItemData(
            name = food.name,
            calories = food.calories,
            protein = food.protein,
            fat = food.fat,
            carbs = food.carbs,
            weight = food.weight.toIntOrNull() ?: 100
        )

        val request = LogFoodRequest(
            userId = viewModel.userId,
            foodData = foodItemData,
            mealType = mealType.name,
            timestamp = System.currentTimeMillis(),
            date = now.format(dateFormatter),
            time = now.format(timeFormatter),
            source = viewModel.currentFoodSource ?: "manual",
            userProfile = viewModel.userProfile.toNetworkProfile(),
            isFirstMessageOfDay = viewModel.repository.isFirstMessageOfDay()
        )

        Log.d(TAG, "UserId: ${request.userId}")
        Log.d(TAG, "FoodData: ${request.foodData}")
        Log.d(TAG, "MealType: ${request.mealType}")
        Log.d(TAG, "Source: ${request.source}")
        Log.d(TAG, "Date: ${request.date}")
        Log.d(TAG, "Time: ${request.time}")
        Log.d(TAG, "UserProfile: ${request.userProfile}")
    }

    /**
     * Проверить работу офлайн-запросов
     */
    fun testOfflineQueries(viewModel: CalorieTrackerViewModel) {
        Log.d(TAG, "=== ТЕСТ ОФЛАЙН-ЗАПРОСОВ ===")

        // Тестовые запросы
        val testQueries = listOf(
            "что я ел вчера",
            "покажи историю",
            "сколько калорий",
            "статистика за неделю"
        )

        testQueries.forEach { query ->
            Log.d(TAG, "Запрос: $query")
            // Вызов приватного метода через рефлексию для тестирования
            try {
                val method = viewModel.javaClass.getDeclaredMethod("getOfflineResponse", String::class.java)
                method.isAccessible = true
                val response = method.invoke(viewModel, query) as String
                Log.d(TAG, "Ответ: $response")
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при тесте: ${e.message}")
            }
        }
    }

    /**
     * Проверить сохранение истории
     */
    fun checkHistorySaving(repository: com.example.calorietracker.data.DataRepository) {
        Log.d(TAG, "=== ПРОВЕРКА ИСТОРИИ ===")

        val dates = repository.getAvailableDates()
        Log.d(TAG, "Доступные даты: $dates")

        dates.take(3).forEach { date ->
            val intake = repository.getIntakeHistory(date)
            Log.d(TAG, "Дата $date: калории=${intake?.calories}, белки=${intake?.protein}")
        }
    }

    /**
     * Проверить обнуление счетчиков
     */
    fun checkDailyReset() {
        Log.d(TAG, "=== ПРОВЕРКА ОБНУЛЕНИЯ ===")

        val currentFoodDate = DailyResetUtils.getFoodDate()
        val nextResetTime = DailyResetUtils.getNextResetTime()
        val displayDate = DailyResetUtils.getDisplayDate(currentFoodDate)

        Log.d(TAG, "Текущая 'пищевая' дата: $currentFoodDate")
        Log.d(TAG, "Следующее обнуление: $nextResetTime")
        Log.d(TAG, "Отображаемая дата: $displayDate")

        // Тест с разными временными метками
        val testDate = "2025-01-10"
        Log.d(TAG, "Нужно обнулить для $testDate: ${DailyResetUtils.shouldResetCounters(testDate)}")
    }
}
