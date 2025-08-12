package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.repositories.NutritionRepository
import com.example.calorietracker.domain.usecases.base.UseCase
import java.time.YearMonth
import javax.inject.Inject

/**
 * Use case for retrieving monthly nutrition intake data
 */
class GetMonthlyIntakeUseCase @Inject constructor(
    private val nutritionRepository: NutritionRepository
) : UseCase<GetMonthlyIntakeUseCase.Params, List<NutritionIntake>>() {
    
    override suspend fun execute(parameters: Params): Result<List<NutritionIntake>> {
        return nutritionRepository.getMonthlyIntake(parameters.month)
    }
    
    data class Params(
        val month: YearMonth = YearMonth.now()
    )
}