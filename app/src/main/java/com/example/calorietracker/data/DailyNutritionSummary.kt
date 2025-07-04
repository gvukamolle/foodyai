package com.example.calorietracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_nutrition_summary")
data class DailyNutritionSummary(
    @PrimaryKey
    val date: LocalDate,
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val mealsCount: Int,
    val waterIntake: Float = 0f,
    val notes: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
