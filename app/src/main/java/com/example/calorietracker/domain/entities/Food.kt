package com.example.calorietracker.domain.entities

import com.example.calorietracker.domain.entities.common.FoodSource

/**
 * Represents a food item with its nutritional information
 */
data class Food(
    val name: String,
    val calories: Int,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val weight: String,
    val source: FoodSource = FoodSource.MANUAL_INPUT,
    val aiOpinion: String? = null
) {
    init {
        require(name.isNotBlank()) { "Food name cannot be blank" }
        require(calories >= 0) { "Calories cannot be negative" }
        require(protein >= 0) { "Protein cannot be negative" }
        require(fat >= 0) { "Fat cannot be negative" }
        require(carbs >= 0) { "Carbs cannot be negative" }
        require(weight.isNotBlank()) { "Weight cannot be blank" }
    }
    
    /**
     * Calculate total macronutrient calories
     */
    fun calculateMacroCalories(): Int {
        return ((protein * 4) + (fat * 9) + (carbs * 4)).toInt()
    }
    
    /**
     * Get weight as numeric value (assumes weight is in grams)
     */
    fun getWeightInGrams(): Double? {
        return weight.replace("г", "").replace("g", "").trim().toDoubleOrNull()
    }
    
    /**
     * Calculate nutrition per 100g
     */
    fun getNutritionPer100g(): Food? {
        val weightInGrams = getWeightInGrams() ?: return null
        if (weightInGrams <= 0) return null
        
        val factor = 100.0 / weightInGrams
        return copy(
            calories = (calories * factor).toInt(),
            protein = protein * factor,
            fat = fat * factor,
            carbs = carbs * factor,
            weight = "100г"
        )
    }
    
    /**
     * Check if this food has AI analysis
     */
    fun hasAIAnalysis(): Boolean = aiOpinion != null
    
    /**
     * Check if nutrition values are reasonable
     */
    fun hasReasonableNutrition(): Boolean {
        val macroCalories = calculateMacroCalories()
        val difference = kotlin.math.abs(macroCalories - calories)
        return difference <= (calories * 0.2) // Allow 20% difference
    }
}