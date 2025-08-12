package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.common.NutritionTargets
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.UserRepository
import com.example.calorietracker.domain.usecases.base.UseCase
import javax.inject.Inject

/**
 * Use case for calculating nutrition targets based on user profile
 */
class CalculateNutritionTargetsUseCase @Inject constructor(
    private val userRepository: UserRepository
) : UseCase<CalculateNutritionTargetsUseCase.Params, NutritionTargets>() {
    
    override suspend fun execute(parameters: Params): Result<NutritionTargets> {
        val user = parameters.user
        
        // Validate user data for calculations
        if (!user.isValidForCalculations()) {
            return Result.error(
                DomainException.ValidationException("User profile is incomplete for nutrition calculations")
            )
        }
        
        // Calculate recommended calories
        val recommendedCalories = user.calculateRecommendedCalories()
            ?: return Result.error(
                DomainException.BusinessLogicException("Cannot calculate recommended calories")
            )
        
        // Use repository calculation (which might have more complex logic)
        return when (val repositoryResult = userRepository.calculateNutritionTargets(user)) {
            is Result.Success -> {
                // Validate the calculated targets
                val targets = repositoryResult.data
                if (!targets.isValidMacroDistribution()) {
                    // If repository calculation is invalid, use our fallback calculation
                    val fallbackTargets = calculateFallbackTargets(recommendedCalories)
                    Result.success(fallbackTargets)
                } else {
                    Result.success(targets)
                }
            }
            is Result.Error -> {
                // If repository fails, use fallback calculation
                val fallbackTargets = calculateFallbackTargets(recommendedCalories)
                Result.success(fallbackTargets)
            }
        }
    }
    
    private fun calculateFallbackTargets(calories: Int): NutritionTargets {
        // Standard macro distribution:
        // Protein: 25-30% of calories (4 cal/g)
        // Fat: 20-35% of calories (9 cal/g)  
        // Carbs: 45-65% of calories (4 cal/g)
        
        val proteinCalories = (calories * 0.25).toInt()
        val fatCalories = (calories * 0.30).toInt()
        val carbsCalories = calories - proteinCalories - fatCalories
        
        return NutritionTargets(
            dailyCalories = calories,
            dailyProtein = proteinCalories / 4, // 4 calories per gram of protein
            dailyFat = fatCalories / 9, // 9 calories per gram of fat
            dailyCarbs = carbsCalories / 4 // 4 calories per gram of carbs
        )
    }
    
    data class Params(
        val user: User
    )
}