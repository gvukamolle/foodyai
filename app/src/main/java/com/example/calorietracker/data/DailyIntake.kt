package com.example.calorietracker.data

data class DailyIntake(
    val calories: Int = 0,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val meals: List<Meal> = emptyList()
)