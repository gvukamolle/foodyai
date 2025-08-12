package com.example.calorietracker.domain.entities.common

/**
 * Represents the type of meal
 */
enum class MealType(val displayName: String) {
    BREAKFAST("Завтрак"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    SNACK("Перекус");
    
    companion object {
        /**
         * Get MealType from display name
         */
        fun fromDisplayName(displayName: String): MealType? {
            return values().find { it.displayName == displayName }
        }
        
        /**
         * Get default meal type based on current time
         */
        fun getDefaultForCurrentTime(): MealType {
            val hour = java.time.LocalTime.now().hour
            return when (hour) {
                in 6..10 -> BREAKFAST
                in 11..15 -> LUNCH
                in 16..21 -> DINNER
                else -> SNACK
            }
        }
    }
}