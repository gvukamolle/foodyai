package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.FoodRepository
import com.example.calorietracker.domain.usecases.base.UseCase
import javax.inject.Inject

/**
 * Use case for analyzing food from photo using AI
 */
class AnalyzeFoodPhotoUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) : UseCase<AnalyzeFoodPhotoUseCase.Params, Food>() {
    
    override suspend fun execute(parameters: Params): Result<Food> {
        // Validate input parameters
        if (parameters.photoPath.isBlank()) {
            return Result.error(DomainException.ValidationException("Photo path cannot be blank"))
        }
        
        // Validate photo file exists (basic validation)
        if (!isValidPhotoPath(parameters.photoPath)) {
            return Result.error(DomainException.ValidationException("Invalid photo path"))
        }
        
        // Decide messageType explicitly if provided, else default to photo
        val messageType = parameters.messageType?.ifBlank { null } ?: "photo"

        // Call repository to analyze photo with explicit messageType
        return when (val result = foodRepository.analyzeFoodPhoto(parameters.photoPath, parameters.caption, messageType)) {
            is Result.Success -> {
                // Validate the returned food data
                validateFoodResult(result.data)
            }
            is Result.Error -> result
        }
    }
    
    private fun isValidPhotoPath(path: String): Boolean {
        // Basic validation - check if path looks like a valid file path
        return path.contains("/") && (path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png"))
    }
    
    private fun validateFoodResult(food: Food): Result<Food> {
        return try {
            // Validate that the food has reasonable nutrition values
            if (!food.hasReasonableNutrition()) {
                return Result.error(
                    DomainException.AIAnalysisException(
                        "AI analysis returned unreasonable nutrition values for ${food.name}"
                    )
                )
            }
            
            Result.success(food)
        } catch (e: Exception) {
            Result.error(DomainException.ValidationException("Food validation failed: ${e.message}"))
        }
    }
    
    data class Params(
        val photoPath: String,
        val caption: String = "",
        val messageType: String? = null
    )
}
