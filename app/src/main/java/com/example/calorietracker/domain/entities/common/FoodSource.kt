package com.example.calorietracker.domain.entities.common

/**
 * Represents the source of food data entry
 */
enum class FoodSource(val displayName: String) {
    MANUAL_INPUT("Ручной ввод"),
    AI_PHOTO_ANALYSIS("Анализ фото"),
    AI_TEXT_ANALYSIS("Анализ текста"),
    BARCODE_SCAN("Сканирование штрих-кода"),
    RECIPE_ANALYSIS("Анализ рецепта");
    
    companion object {
        /**
         * Get FoodSource from string value (for backward compatibility)
         */
        fun fromString(value: String): FoodSource {
            return when (value.lowercase()) {
                "manual" -> MANUAL_INPUT
                "photo" -> AI_PHOTO_ANALYSIS
                "text", "description" -> AI_TEXT_ANALYSIS
                "barcode" -> BARCODE_SCAN
                "recipe" -> RECIPE_ANALYSIS
                else -> MANUAL_INPUT
            }
        }
    }
}