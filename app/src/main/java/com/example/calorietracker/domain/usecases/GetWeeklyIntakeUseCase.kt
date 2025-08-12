package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.repositories.NutritionRepository
import com.example.calorietracker.domain.usecases.base.UseCase
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for retrieving weekly nutrition intake data
 */
class GetWeeklyIntakeUseCase @Inject constructor(
    private val nutritionRepository: NutritionRepository
) : UseCase<GetWeeklyIntakeUseCase.Params, List<NutritionIntake>>() {
    
    override suspend fun execute(parameters: Params): Result<List<NutritionIntake>> {
        return nutritionRepository.getWeeklyIntake(parameters.startDate)
    }
    
    data class Params(
        val startDate: LocalDate = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value - 1L)
    )
}