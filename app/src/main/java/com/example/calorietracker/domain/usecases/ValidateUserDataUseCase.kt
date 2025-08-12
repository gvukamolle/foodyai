package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.usecases.base.UseCase
import javax.inject.Inject

/**
 * Use case for validating user input data
 */
class ValidateUserDataUseCase @Inject constructor() : UseCase<ValidateUserDataUseCase.Params, User>() {
    
    override suspend fun execute(parameters: Params): Result<User> {
        val user = parameters.user
        val validationErrors = mutableListOf<String>()
        
        // Validate name
        if (user.name.isBlank()) {
            validationErrors.add("Имя не может быть пустым")
        } else if (user.name.length < 2) {
            validationErrors.add("Имя должно содержать минимум 2 символа")
        } else if (user.name.length > 50) {
            validationErrors.add("Имя не может быть длиннее 50 символов")
        }
        
        // Validate birthday
        val age = user.getAge()
        if (user.birthday.isNotBlank()) {
            if (age == null) {
                validationErrors.add("Неверный формат даты рождения")
            } else if (age < 13) {
                validationErrors.add("Возраст должен быть не менее 13 лет")
            } else if (age > 120) {
                validationErrors.add("Возраст не может превышать 120 лет")
            }
        }
        
        // Validate height
        if (user.height > 0) {
            if (user.height < 100) {
                validationErrors.add("Рост должен быть не менее 100 см")
            } else if (user.height > 250) {
                validationErrors.add("Рост не может превышать 250 см")
            }
        }
        
        // Validate weight
        if (user.weight > 0) {
            if (user.weight < 30) {
                validationErrors.add("Вес должен быть не менее 30 кг")
            } else if (user.weight > 300) {
                validationErrors.add("Вес не может превышать 300 кг")
            }
        }
        
        // Validate BMI if both height and weight are provided
        if (user.height > 0 && user.weight > 0) {
            val bmi = user.getBMI()
            if (bmi != null && (bmi < 10 || bmi > 50)) {
                validationErrors.add("Соотношение роста и веса выходит за разумные пределы")
            }
        }
        
        // Validate nutrition targets if provided
        val targets = user.nutritionTargets
        if (targets.dailyCalories > 0) {
            if (targets.dailyCalories < 800) {
                validationErrors.add("Дневная норма калорий не может быть менее 800")
            } else if (targets.dailyCalories > 5000) {
                validationErrors.add("Дневная норма калорий не может превышать 5000")
            }
            
            // Validate macro distribution
            if (!targets.isValidMacroDistribution()) {
                validationErrors.add("Распределение макронутриентов не соответствует общей калорийности")
            }
        }
        
        // Return result
        return if (validationErrors.isEmpty()) {
            Result.success(user)
        } else {
            Result.error(
                DomainException.ValidationException(
                    "Ошибки валидации: ${validationErrors.joinToString("; ")}"
                )
            )
        }
    }
    
    data class Params(
        val user: User
    )
}