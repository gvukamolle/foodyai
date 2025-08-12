package com.example.calorietracker.domain.entities.common

/**
 * Represents user nutrition goal
 */
enum class NutritionGoal(val displayName: String) {
    LOSE_WEIGHT("Похудение"),
    MAINTAIN_WEIGHT("Поддержание веса"),
    GAIN_WEIGHT("Набор веса"),
    BUILD_MUSCLE("Набор мышечной массы"),
    IMPROVE_HEALTH("Улучшение здоровья");
    
    companion object {
        /**
         * Get NutritionGoal from string value (for backward compatibility)
         */
        fun fromString(value: String): NutritionGoal {
            return when (value.lowercase()) {
                "похудение", "lose_weight", "weight_loss" -> LOSE_WEIGHT
                "поддержание веса", "maintain_weight" -> MAINTAIN_WEIGHT
                "набор веса", "gain_weight" -> GAIN_WEIGHT
                "набор мышечной массы", "build_muscle" -> BUILD_MUSCLE
                "улучшение здоровья", "improve_health" -> IMPROVE_HEALTH
                else -> MAINTAIN_WEIGHT
            }
        }
        
        /**
         * Get calorie adjustment factor for goal
         */
        fun getCalorieAdjustment(goal: NutritionGoal): Double {
            return when (goal) {
                LOSE_WEIGHT -> 0.8 // -20%
                MAINTAIN_WEIGHT -> 1.0 // no change
                GAIN_WEIGHT -> 1.2 // +20%
                BUILD_MUSCLE -> 1.15 // +15%
                IMPROVE_HEALTH -> 1.0 // no change
            }
        }
    }
}