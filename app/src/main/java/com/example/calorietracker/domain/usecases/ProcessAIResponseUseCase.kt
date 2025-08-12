package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.ChatRepository
import com.example.calorietracker.domain.usecases.base.UseCase
import javax.inject.Inject

/**
 * Use case for processing AI responses and extracting food information
 */
class ProcessAIResponseUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) : UseCase<ProcessAIResponseUseCase.Params, ProcessedAIResponse>() {
    
    override suspend fun execute(parameters: Params): Result<ProcessedAIResponse> {
        val response = parameters.response.trim()
        
        if (response.isBlank()) {
            return Result.error(
                DomainException.AIAnalysisException("AI response is empty")
            )
        }
        
        // Try to extract food information from response
        val foodResult = chatRepository.processAIFoodAnalysis(response)
        
        return when (foodResult) {
            is Result.Success -> {
                Result.success(
                    ProcessedAIResponse(
                        originalResponse = response,
                        extractedFood = foodResult.data,
                        hasFood = true,
                        confidence = calculateConfidence(response, foodResult.data)
                    )
                )
            }
            is Result.Error -> {
                // If food extraction fails, still return the response without food
                Result.success(
                    ProcessedAIResponse(
                        originalResponse = response,
                        extractedFood = null,
                        hasFood = false,
                        confidence = 0.0,
                        error = foodResult.exception.message
                    )
                )
            }
        }
    }
    
    private fun calculateConfidence(response: String, food: Food): Double {
        // Simple confidence calculation based on response content
        var confidence = 0.5 // Base confidence
        
        // Increase confidence if response contains specific nutrition terms
        val nutritionTerms = listOf("калории", "белки", "жиры", "углеводы", "ккал", "грамм")
        val foundTerms = nutritionTerms.count { response.lowercase().contains(it) }
        confidence += (foundTerms * 0.1)
        
        // Increase confidence if food has reasonable nutrition values
        if (food.hasReasonableNutrition()) {
            confidence += 0.2
        }
        
        // Decrease confidence if food name is too generic
        if (food.name.length < 3 || food.name.lowercase() in listOf("еда", "продукт", "блюдо")) {
            confidence -= 0.3
        }
        
        return confidence.coerceIn(0.0, 1.0)
    }
    
    data class Params(
        val response: String
    )
}

/**
 * Represents processed AI response with extracted information
 */
data class ProcessedAIResponse(
    val originalResponse: String,
    val extractedFood: Food?,
    val hasFood: Boolean,
    val confidence: Double,
    val error: String? = null
) {
    /**
     * Check if the response is reliable enough to use
     */
    fun isReliable(): Boolean = confidence >= 0.7
    
    /**
     * Check if food extraction was successful
     */
    fun hasFoodData(): Boolean = hasFood && extractedFood != null
}