package com.example.calorietracker.data

data class FoodItem(
    val name: String,
    val calories: Int,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val weight: String,
    val source: String = "manual",
    val aiOpinion: String? = null
)

data class Meal(
    val type: MealType,
    val foods: List<FoodItem>,
    val time: Long = System.currentTimeMillis()
)

enum class MealType(val displayName: String) {
    BREAKFAST("Завтрак"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    SUPPER("Перекус")
}

data class FoodHistoryItem(
    val foodName: String,
    val calories: Int,
    val mealType: String,
    val timestamp: java.time.LocalDateTime
)