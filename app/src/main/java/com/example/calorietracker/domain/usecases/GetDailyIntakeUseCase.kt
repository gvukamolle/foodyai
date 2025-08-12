package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.repositories.NutritionRepository
import com.example.calorietracker.domain.repositories.UserRepository
import com.example.calorietracker.domain.usecases.base.UseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for retrieving daily nutrition intake with user targets
 */
class GetDailyIntakeUseCase @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val userRepository: UserRepository
) : UseCase<GetDailyIntakeUseCase.Params, NutritionIntake>() {
    
    override suspend fun execute(parameters: Params): Result<NutritionIntake> {
        // Get daily intake from repository
        val intakeResult = nutritionRepository.getDailyIntake(parameters.date)
        
        return when (intakeResult) {
            is Result.Success -> {
                var intake = intakeResult.data
                
                // If intake doesn't have targets, try to get them from user profile
                if (intake.targets == null) {
                    val userResult = userRepository.getUserProfile()
                    if (userResult is Result.Success) {
                        val user = userResult.data
                        intake = intake.updateTargets(user.nutritionTargets)
                    }
                }
                
                Result.success(intake)
            }
            is Result.Error -> intakeResult
        }
    }
    
    data class Params(
        val date: LocalDate = LocalDate.now()
    )
}