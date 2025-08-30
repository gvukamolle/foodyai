package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.Meal
import com.example.calorietracker.domain.entities.common.MealType
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.FoodRepository
import com.example.calorietracker.domain.repositories.NutritionRepository
import com.example.calorietracker.domain.usecases.base.UseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for saving food intake to daily nutrition tracking
 */
class SaveFoodIntakeUseCase @Inject constructor(
    private val foodRepository: FoodRepository,
    private val nutritionRepository: NutritionRepository
) : UseCase<SaveFoodIntakeUseCase.Params, Unit>() {
    
    override suspend fun execute(parameters: Params): Result<Unit> {
        // Validate input parameters
        try {
            // Validate food data
            if (parameters.food.name.isBlank()) {
                return Result.error(DomainException.ValidationException("Food name cannot be blank"))
            }
            
            if (parameters.food.calories < 0) {
                return Result.error(DomainException.ValidationException("Calories cannot be negative"))
            }
            
            // Validate food data through repository if needed
            val validationResult = foodRepository.validateFoodData(parameters.food)
            if (validationResult is Result.Error) {
                return Result.error(validationResult.exception)
            }
            
            // Create meal with the food item
            val meal = Meal(
                type = parameters.mealType,
                foods = listOf(parameters.food)
            )
            
            // Add meal to daily nutrition tracking (single source of truth for daily history)
            val addMealResult = nutritionRepository.addMealToDay(parameters.date, meal)
            if (addMealResult is Result.Error) {
                return Result.error(addMealResult.exception)
            }
            
            // Invalidate cache to ensure fresh data
            nutritionRepository.invalidateDailyCache(parameters.date)
            
            return Result.success(Unit)
            
        } catch (e: Exception) {
            return Result.error(
                DomainException.StorageException("Failed to save food intake: ${e.message}", e)
            )
        }
    }
    
    data class Params(
        val food: Food,
        val mealType: MealType,
        val date: LocalDate = LocalDate.now()
    )
}
