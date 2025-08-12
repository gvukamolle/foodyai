package com.example.calorietracker.domain.entities

import com.example.calorietracker.domain.entities.common.MealType
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Represents a meal containing one or more food items
 */
data class Meal(
    val type: MealType,
    val foods: List<Food>,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(foods.isNotEmpty()) { "Meal must contain at least one food item" }
    }
    
    /**
     * Calculate total calories for this meal
     */
    fun getTotalCalories(): Int = foods.sumOf { it.calories }
    
    /**
     * Calculate total protein for this meal
     */
    fun getTotalProtein(): Double = foods.sumOf { it.protein }
    
    /**
     * Calculate total fat for this meal
     */
    fun getTotalFat(): Double = foods.sumOf { it.fat }
    
    /**
     * Calculate total carbs for this meal
     */
    fun getTotalCarbs(): Double = foods.sumOf { it.carbs }
    
    /**
     * Get timestamp as epoch milliseconds (for backward compatibility)
     */
    fun getTimestampMillis(): Long = timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    
    /**
     * Check if meal contains any AI-analyzed foods
     */
    fun hasAIAnalyzedFoods(): Boolean = foods.any { it.hasAIAnalysis() }
    
    /**
     * Get count of food items in this meal
     */
    fun getFoodCount(): Int = foods.size
    
    companion object {
        /**
         * Create a meal from legacy data (timestamp as Long)
         */
        fun fromLegacyData(type: MealType, foods: List<Food>, timestampMillis: Long): Meal {
            val timestamp = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestampMillis),
                ZoneId.systemDefault()
            )
            return Meal(type, foods, timestamp)
        }
    }
}