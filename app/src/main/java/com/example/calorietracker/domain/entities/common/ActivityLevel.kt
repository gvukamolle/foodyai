package com.example.calorietracker.domain.entities.common

/**
 * Represents user activity level for nutrition calculations
 */
enum class ActivityLevel(val displayName: String, val multiplier: Double) {
    SEDENTARY("Малоподвижный", 1.2),
    LIGHTLY_ACTIVE("Легкая активность", 1.375),
    MODERATELY_ACTIVE("Умеренная активность", 1.55),
    VERY_ACTIVE("Высокая активность", 1.725),
    EXTREMELY_ACTIVE("Очень высокая активность", 1.9);
    
    companion object {
        /**
         * Get ActivityLevel from string value (for backward compatibility)
         */
        fun fromString(value: String): ActivityLevel {
            return when (value.lowercase()) {
                "малоподвижный", "sedentary" -> SEDENTARY
                "легкая активность", "lightly_active" -> LIGHTLY_ACTIVE
                "умеренная активность", "moderately_active" -> MODERATELY_ACTIVE
                "высокая активность", "very_active" -> VERY_ACTIVE
                "очень высокая активность", "extremely_active" -> EXTREMELY_ACTIVE
                else -> MODERATELY_ACTIVE
            }
        }
    }
}