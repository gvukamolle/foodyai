package com.example.calorietracker.domain.entities.common

/**
 * Represents daily nutrition targets for a user
 */
data class NutritionTargets(
    val dailyCalories: Int,
    val dailyProtein: Int,
    val dailyFat: Int,
    val dailyCarbs: Int
) {
    init {
        require(dailyCalories > 0) { "Daily calories must be positive" }
        require(dailyProtein >= 0) { "Daily protein cannot be negative" }
        require(dailyFat >= 0) { "Daily fat cannot be negative" }
        require(dailyCarbs >= 0) { "Daily carbs cannot be negative" }
    }
    
    /**
     * Calculate total macronutrient calories
     */
    fun totalMacroCalories(): Int {
        return (dailyProtein * 4) + (dailyFat * 9) + (dailyCarbs * 4)
    }
    
    /**
     * Check if macro distribution is reasonable (within 10% of total calories)
     */
    fun isValidMacroDistribution(): Boolean {
        val macroCalories = totalMacroCalories()
        val difference = kotlin.math.abs(macroCalories - dailyCalories)
        return difference <= (dailyCalories * 0.1)
    }
}