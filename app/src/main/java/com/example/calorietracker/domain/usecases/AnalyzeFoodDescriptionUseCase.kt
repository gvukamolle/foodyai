package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.FoodRepository
import com.example.calorietracker.domain.usecases.base.UseCase
import javax.inject.Inject

/**
 * Use case for analyzing food from text description using AI
 */
class AnalyzeFoodDescriptionUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) : UseCase<AnalyzeFoodDescriptionUseCase.Params, Food>() {
    
    override suspend fun execute(parameters: Params): Result<Food> {
        // Validate input parameters
        val cleanDescription = parameters.description.trim()
        if (cleanDescription.isBlank()) {
            return Result.error(DomainException.ValidationException("Description cannot be blank"))
        }
        
        if (cleanDescription.length < 3) {
            return Result.error(DomainException.ValidationException("Description too short"))
        }
        
        if (cleanDescription.length > 1000) {
            return Result.error(DomainException.ValidationException("Description too long (max 1000 characters)"))
        }
        
        // Check for potentially harmful content (basic filtering)
        if (containsInappropriateContent(cleanDescription)) {
            return Result.error(DomainException.ValidationException("Description contains inappropriate content"))
        }
        
        // Call repository to analyze description
        return when (val result = foodRepository.analyzeFoodDescription(cleanDescription)) {
            is Result.Success -> {
                // Validate the returned food data
                validateFoodResult(result.data)
            }
            is Result.Error -> result
        }
    }
    
    private fun containsInappropriateContent(description: String): Boolean {
        // Basic content filtering - can be expanded
        val inappropriateWords = listOf("script", "<", ">", "javascript", "eval")
        val lowerDescription = description.lowercase()
        return inappropriateWords.any { lowerDescription.contains(it) }
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
            
            // Check if food name is reasonable
            if (food.name.isBlank() || food.name.length < 2) {
                return Result.error(
                    DomainException.AIAnalysisException("AI analysis returned invalid food name")
                )
            }
            
            Result.success(food)
        } catch (e: Exception) {
            Result.error(DomainException.ValidationException("Food validation failed: ${e.message}"))
        }
    }
    
    data class Params(
        val description: String
    )
}