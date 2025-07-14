// Создать новый файл: app/src/main/java/com/example/calorietracker/utils/NutritionFormatter.kt

package com.example.calorietracker.utils

import kotlin.math.round

/**
 * Утилита для форматирования значений КБЖУ
 * Ограничивает количество знаков после запятой до 1 с округлением вверх
 */
object NutritionFormatter {

    /**
     * Форматирует значение БЖУ (белки, жиры, углеводы)
     * @param value значение для форматирования
     * @return строка с одним знаком после запятой, округление вверх
     */
    fun formatMacro(value: Float): String {
        // Округляем до одного знака после запятой
        val rounded = round(value * 10) / 10f
        return "%.1f".format(rounded)
    }

    /**
     * Форматирует значение БЖУ без знаков после запятой
     * округляя вверх до целого числа
     */
    fun formatMacroInt(value: Float): String {
        return kotlin.math.ceil(value.toDouble()).toInt().toString()
    }


    /**
     * Форматирует значение БЖУ с единицей измерения
     */
    fun formatMacroWithUnit(value: Float): String {
        return "${formatMacro(value)} г"
    }

    /**
     * Форматирует калории (целое число)
     */
    fun formatCalories(value: Int): String {
        return "$value ккал"
    }

    /**
     * Форматирует вес продукта
     */
    fun formatWeight(value: Float): String {
        return "${value.toInt()} г"
    }

    /**
     * Extension функции для удобства
     */
    fun Float.formatAsMacro(): String = formatMacro(this)
    fun Float.formatAsMacroInt(): String = formatMacroInt(this)
    fun Int.formatAsCalories(): String = formatCalories(this)
}