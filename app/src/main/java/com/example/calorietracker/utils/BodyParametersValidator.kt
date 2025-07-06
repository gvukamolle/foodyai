// Создайте новый файл: app/src/main/java/com/example/calorietracker/utils/BodyParametersValidator.kt

package com.example.calorietracker.utils

import java.time.LocalDate
import java.time.Period

/**
 * Валидатор для параметров тела с понятными сообщениями об ошибках
 */
object BodyParametersValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null

    )

    /**
     * Валидация роста
     */
    fun validateHeight(heightStr: String): ValidationResult {
        val height = heightStr.toIntOrNull()

        return when {
            height == null -> ValidationResult(false, "Введите корректное число")
            height < 50 -> ValidationResult(false, "Рост не может быть меньше 50 см")
            height > 300 -> ValidationResult(false, "Рост не может быть больше 300 см")
            else -> ValidationResult(true)
        }
    }

    /**
     * Валидация веса
     */
    fun validateWeight(weightStr: String): ValidationResult {
        val weight = weightStr.toIntOrNull()

        return when {
            weight == null -> ValidationResult(false, "Введите корректное число")
            weight < 20 -> ValidationResult(false, "Вес не может быть меньше 20 кг")
            weight > 500 -> ValidationResult(false, "Вес не может быть больше 500 кг")
            else -> ValidationResult(true)
        }
    }

    /**
     * Валидация даты рождения
     */
    fun validateBirthday(year: String, month: String, day: String): ValidationResult {
        val yearInt = year.toIntOrNull()
        val monthInt = month.toIntOrNull()
        val dayInt = day.toIntOrNull()

        if (yearInt == null || monthInt == null || dayInt == null) {
            return ValidationResult(false, "Заполните все поля даты")
        }

        val currentYear = LocalDate.now().year

        // Проверка года
        if (yearInt < 1900) {
            return ValidationResult(false, "Год не может быть меньше 1900")
        }
        if (yearInt > currentYear) {
            return ValidationResult(false, "Год не может быть больше текущего")
        }

        // Проверка месяца
        if (monthInt !in 1..12) {
            return ValidationResult(false, "Месяц должен быть от 1 до 12")
        }

        // Проверка дня
        val daysInMonth = try {
            LocalDate.of(yearInt, monthInt, 1).lengthOfMonth()
        } catch (e: Exception) {
            31 // По умолчанию
        }

        if (dayInt !in 1..daysInMonth) {
            return ValidationResult(false, "День должен быть от 1 до $daysInMonth")
        }

        // Проверка возраста
        try {
            val birthday = LocalDate.of(yearInt, monthInt, dayInt)
            val age = Period.between(birthday, LocalDate.now()).years

            return when {
                age < 5 -> ValidationResult(false, "Возраст должен быть не менее 5 лет")
                age > 120 -> ValidationResult(false, "Проверьте правильность даты")
                else -> ValidationResult(true)
            }
        } catch (e: Exception) {
            return ValidationResult(false, "Некорректная дата")
        }
    }

    /**
     * Рассчитать возраст
     */
    fun calculateAge(birthday: String): Int? {
        return try {
            val parts = birthday.split("-").mapNotNull { it.toIntOrNull() }
            if (parts.size == 3) {
                val date = LocalDate.of(parts[0], parts[1], parts[2])
                Period.between(date, LocalDate.now()).years
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Валидация всех параметров
     */
    fun validateAll(
        height: String,
        weight: String,
        year: String,
        month: String,
        day: String,
        gender: String,
        condition: String,
        goal: String
    ): ValidationResult {
        // Базовые параметры
        val heightResult = validateHeight(height)
        if (!heightResult.isValid) return heightResult

        val weightResult = validateWeight(weight)
        if (!weightResult.isValid) return weightResult

        val birthdayResult = validateBirthday(year, month, day)
        if (!birthdayResult.isValid) return birthdayResult

        // Дополнительные параметры
        if (gender.isEmpty()) {
            return ValidationResult(false, "Выберите пол")
        }

        if (condition.isEmpty()) {
            return ValidationResult(false, "Выберите уровень активности")
        }

        if (goal.isEmpty()) {
            return ValidationResult(false, "Выберите цель")
        }

        return ValidationResult(true)
    }

    /**
     * Получить рекомендации по параметрам
     */
    fun getRecommendations(height: Int, weight: Int, gender: String): String {
        val bmi = weight.toFloat() / ((height.toFloat() / 100) * (height.toFloat() / 100))

        val bmiCategory = when {
            bmi < 18.5 -> "недостаточный вес"
            bmi < 25 -> "нормальный вес"
            bmi < 30 -> "избыточный вес"
            else -> "ожирение"
        }

        val idealWeight = when (gender) {
            "male" -> (height - 100) * 0.9
            "female" -> (height - 100) * 0.85
            else -> (height - 100) * 0.9
        }

        return buildString {
            append("ИМТ: %.1f (${bmiCategory})\n".format(bmi))
            append("Идеальный вес: %.0f кг".format(idealWeight))
        }
    }
}

