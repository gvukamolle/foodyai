package com.example.calorietracker.domain.entities.common

/**
 * Represents user gender for nutrition calculations
 */
enum class Gender(val displayName: String) {
    MALE("Мужской"),
    FEMALE("Женский"),
    OTHER("Другой");
    
    companion object {
        /**
         * Get Gender from string value (for backward compatibility)
         */
        fun fromString(value: String): Gender {
            return when (value.lowercase()) {
                "мужской", "male", "m" -> MALE
                "женский", "female", "f" -> FEMALE
                else -> OTHER
            }
        }
    }
}