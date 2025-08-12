package com.example.calorietracker.domain.entities

import com.example.calorietracker.domain.entities.common.NutritionTargets
import java.time.LocalDate

/**
 * Represents daily nutrition intake with meals and totals
 */
data class NutritionIntake(
    val date: LocalDate,
    val meals: List<Meal> = emptyList(),
    val targets: NutritionTargets? = null
) {
    
    /**
     * Calculate total calories for the day
     */
    fun getTotalCalories(): Int = meals.sumOf { it.getTotalCalories() }
    
    /**
     * Calculate total protein for the day
     */
    fun getTotalProtein(): Double = meals.sumOf { it.getTotalProtein() }
    
    /**
     * Calculate total fat for the day
     */
    fun getTotalFat(): Double = meals.sumOf { it.getTotalFat() }
    
    /**
     * Calculate total carbs for the day
     */
    fun getTotalCarbs(): Double = meals.sumOf { it.getTotalCarbs() }
    
    /**
     * Get calorie progress as percentage (0.0 to 1.0+)
     */
    fun getCalorieProgress(): Double? {
        val target = targets?.dailyCalories ?: return null
        if (target <= 0) return null
        return getTotalCalories().toDouble() / target
    }
    
    /**
     * Get protein progress as percentage (0.0 to 1.0+)
     */
    fun getProteinProgress(): Double? {
        val target = targets?.dailyProtein ?: return null
        if (target <= 0) return null
        return getTotalProtein() / target
    }
    
    /**
     * Get fat progress as percentage (0.0 to 1.0+)
     */
    fun getFatProgress(): Double? {
        val target = targets?.dailyFat ?: return null
        if (target <= 0) return null
        return getTotalFat() / target
    }
    
    /**
     * Get carbs progress as percentage (0.0 to 1.0+)
     */
    fun getCarbsProgress(): Double? {
        val target = targets?.dailyCarbs ?: return null
        if (target <= 0) return null
        return getTotalCarbs() / target
    }
    
    /**
     * Get remaining calories for the day
     */
    fun getRemainingCalories(): Int? {
        val target = targets?.dailyCalories ?: return null
        return target - getTotalCalories()
    }
    
    /**
     * Check if daily calorie goal is met
     */
    fun isCalorieGoalMet(): Boolean? {
        val progress = getCalorieProgress() ?: return null
        return progress >= 0.9 && progress <= 1.1 // Within 10% of target
    }
    
    /**
     * Get total number of food items consumed
     */
    fun getTotalFoodItems(): Int = meals.sumOf { it.getFoodCount() }
    
    /**
     * Get meals by type
     */
    fun getMealsByType(type: com.example.calorietracker.domain.entities.common.MealType): List<Meal> {
        return meals.filter { it.type == type }
    }
    
    /**
     * Check if any meals contain AI-analyzed foods
     */
    fun hasAIAnalyzedFoods(): Boolean = meals.any { it.hasAIAnalyzedFoods() }
    
    /**
     * Add a meal to the intake
     */
    fun addMeal(meal: Meal): NutritionIntake {
        return copy(meals = meals + meal)
    }
    
    /**
     * Remove a meal from the intake
     */
    fun removeMeal(mealIndex: Int): NutritionIntake {
        if (mealIndex < 0 || mealIndex >= meals.size) return this
        return copy(meals = meals.toMutableList().apply { removeAt(mealIndex) })
    }
    
    /**
     * Update targets
     */
    fun updateTargets(newTargets: NutritionTargets): NutritionIntake {
        return copy(targets = newTargets)
    }
    
    companion object {
        /**
         * Create empty intake for a date
         */
        fun empty(date: LocalDate, targets: NutritionTargets? = null): NutritionIntake {
            return NutritionIntake(date = date, targets = targets)
        }
        
        /**
         * Create intake for today
         */
        fun today(targets: NutritionTargets? = null): NutritionIntake {
            return empty(LocalDate.now(), targets)
        }
    }
}