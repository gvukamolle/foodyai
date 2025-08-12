package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.common.DateRange
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.FoodRepository
import com.example.calorietracker.domain.usecases.base.UseCase
import javax.inject.Inject

/**
 * Use case for retrieving food history for a date range
 */
class GetFoodHistoryUseCase @Inject constructor(
    private val foodRepository: FoodRepository
) : UseCase<GetFoodHistoryUseCase.Params, List<Food>>() {
    
    override suspend fun execute(parameters: Params): Result<List<Food>> {
        // Validate date range
        if (parameters.dateRange.dayCount() > 365) {
            return Result.error(
                DomainException.ValidationException("Date range cannot exceed 365 days")
            )
        }
        
        // Get food history from repository
        return when (val result = foodRepository.getFoodHistory(parameters.dateRange)) {
            is Result.Success -> {
                // Sort foods by most recent first and apply limit if specified
                val sortedFoods = result.data.sortedByDescending { food ->
                    // Assuming we have some way to get timestamp - this would need to be added to Food entity
                    // For now, sort by name as placeholder
                    food.name
                }
                
                val limitedFoods = if (parameters.limit > 0) {
                    sortedFoods.take(parameters.limit)
                } else {
                    sortedFoods
                }
                
                Result.success(limitedFoods)
            }
            is Result.Error -> result
        }
    }
    
    data class Params(
        val dateRange: DateRange,
        val limit: Int = 0 // 0 means no limit
    )
}