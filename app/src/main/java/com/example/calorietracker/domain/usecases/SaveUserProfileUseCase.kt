package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.UserRepository
import com.example.calorietracker.domain.usecases.base.UseCase
import javax.inject.Inject

/**
 * Use case for saving user profile with validation
 */
class SaveUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) : UseCase<SaveUserProfileUseCase.Params, Unit>() {
    
    override suspend fun execute(parameters: Params): Result<Unit> {
        // Validate user data
        val validationResult = validateUser(parameters.user)
        if (validationResult is Result.Error) {
            return validationResult
        }
        
        // Validate through repository
        val repositoryValidation = userRepository.validateUserProfile(parameters.user)
        if (repositoryValidation is Result.Error) {
            return Result.error(repositoryValidation.exception)
        }
        
        // Save user profile
        return userRepository.saveUserProfile(parameters.user)
    }
    
    private fun validateUser(user: User): Result<Unit> {
        // Validate name
        if (user.name.isBlank()) {
            return Result.error(DomainException.ValidationException("Name cannot be blank"))
        }
        
        if (user.name.length < 2) {
            return Result.error(DomainException.ValidationException("Name must be at least 2 characters"))
        }
        
        // Validate age
        val age = user.getAge()
        if (age == null) {
            return Result.error(DomainException.ValidationException("Invalid birthday format"))
        }
        
        if (age < 13 || age > 120) {
            return Result.error(DomainException.ValidationException("Age must be between 13 and 120"))
        }
        
        // Validate height
        if (user.height <= 0) {
            return Result.error(DomainException.ValidationException("Height must be positive"))
        }
        
        if (user.height < 100 || user.height > 250) {
            return Result.error(DomainException.ValidationException("Height must be between 100 and 250 cm"))
        }
        
        // Validate weight
        if (user.weight <= 0) {
            return Result.error(DomainException.ValidationException("Weight must be positive"))
        }
        
        if (user.weight < 30 || user.weight > 300) {
            return Result.error(DomainException.ValidationException("Weight must be between 30 and 300 kg"))
        }
        
        // Validate BMI is reasonable
        val bmi = user.getBMI()
        if (bmi != null && (bmi < 10 || bmi > 50)) {
            return Result.error(DomainException.ValidationException("BMI is outside reasonable range"))
        }
        
        return Result.success(Unit)
    }
    
    data class Params(
        val user: User
    )
}