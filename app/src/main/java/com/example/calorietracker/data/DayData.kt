package com.example.calorietracker.data

import java.time.LocalDate

/**
 * Data class для хранения информации о конкретном дне.
 * Используется на экране аналитики.
 */
data class DayData(
    val date: LocalDate,
    val calories: Float,
    val proteins: Float,
    val fats: Float,
    val carbs: Float,
    val mealsCount: Int = 0
)