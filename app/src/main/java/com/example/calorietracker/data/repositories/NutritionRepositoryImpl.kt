package com.example.calorietracker.data.repositories

import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.mappers.FoodMapper
import com.example.calorietracker.data.mappers.NutritionMapper
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Meal
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.common.DateRange
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.NutritionRepository
import com.example.calorietracker.domain.repositories.NutritionStatistics
import com.example.calorietracker.domain.repositories.NutritionTrends
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NutritionRepository that handles nutrition tracking operations
 */
@Singleton
class NutritionRepositoryImpl @Inject constructor(
    private val dataRepository: DataRepository,
    private val nutritionMapper: NutritionMapper,
    private val foodMapper: FoodMapper
) : NutritionRepository {
    
    override suspend fun getDailyIntake(date: LocalDate): Result<NutritionIntake> {
        return withContext(Dispatchers.IO) {
            try {
                // Get meals for the date
                val meals = dataRepository.getMealsForDate(date.toString())
                val domainMeals = foodMapper.mapDataMealsToDomain(meals)
                
                // Get user targets
                val userProfile = dataRepository.getUserProfile()
                val targets = if (userProfile != null) {
                    nutritionMapper.createTargetsFromLegacy(
                        userProfile.dailyCalories,
                        userProfile.dailyProteins,
                        userProfile.dailyFats,
                        userProfile.dailyCarbs
                    )
                } else null
                
                val intake = NutritionIntake(
                    date = date,
                    meals = domainMeals,
                    targets = targets
                )
                
                Result.success(intake)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get daily intake: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun getWeeklyIntake(startDate: LocalDate): Result<List<NutritionIntake>> {
        return withContext(Dispatchers.IO) {
            try {
                val intakes = mutableListOf<NutritionIntake>()
                
                for (i in 0..6) {
                    val date = startDate.plusDays(i.toLong())
                    val dailyResult = getDailyIntake(date)
                    when (dailyResult) {
                        is Result.Success -> intakes.add(dailyResult.data)
                        is Result.Error -> {
                            // Add empty intake for failed days
                            intakes.add(NutritionIntake.empty(date))
                        }
                    }
                }
                
                Result.success(intakes)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get weekly intake: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun getMonthlyIntake(month: YearMonth): Result<List<NutritionIntake>> {
        return withContext(Dispatchers.IO) {
            try {
                val intakes = mutableListOf<NutritionIntake>()
                val startDate = month.atDay(1)
                val endDate = month.atEndOfMonth()
                
                var currentDate = startDate
                while (!currentDate.isAfter(endDate)) {
                    val dailyResult = getDailyIntake(currentDate)
                    when (dailyResult) {
                        is Result.Success -> intakes.add(dailyResult.data)
                        is Result.Error -> {
                            intakes.add(NutritionIntake.empty(currentDate))
                        }
                    }
                    currentDate = currentDate.plusDays(1)
                }
                
                Result.success(intakes)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get monthly intake: ${e.message}",
                        e
                    )
                )
            }
        }
    } 
   
    override suspend fun getIntakeForDateRange(dateRange: DateRange): Result<List<NutritionIntake>> {
        return withContext(Dispatchers.IO) {
            try {
                val intakes = mutableListOf<NutritionIntake>()
                var currentDate = dateRange.startDate
                
                while (!currentDate.isAfter(dateRange.endDate)) {
                    val dailyResult = getDailyIntake(currentDate)
                    when (dailyResult) {
                        is Result.Success -> intakes.add(dailyResult.data)
                        is Result.Error -> {
                            intakes.add(NutritionIntake.empty(currentDate))
                        }
                    }
                    currentDate = currentDate.plusDays(1)
                }
                
                Result.success(intakes)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get intake for date range: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun saveDailyIntake(intake: NutritionIntake): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Save each meal
                for (meal in intake.meals) {
                    val dataMeal = foodMapper.mapDomainMealToData(meal)
                    dataRepository.saveMealToHistory(dataMeal, intake.date.toString())
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to save daily intake: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun addMealToDay(date: LocalDate, meal: Meal): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val dataMeal = foodMapper.mapDomainMealToData(meal)
                dataRepository.saveMealToHistory(dataMeal, date.toString())
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to add meal to day: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun removeMealFromDay(date: LocalDate, mealIndex: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                dataRepository.deleteMealFromHistory(date.toString(), mealIndex)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to remove meal from day: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun updateMealInDay(date: LocalDate, mealIndex: Int, meal: Meal): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Remove old meal and add new one
                removeMealFromDay(date, mealIndex)
                addMealToDay(date, meal)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to update meal in day: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun getNutritionStatistics(dateRange: DateRange): Result<NutritionStatistics> {
        return withContext(Dispatchers.IO) {
            try {
                val intakesResult = getIntakeForDateRange(dateRange)
                when (intakesResult) {
                    is Result.Success -> {
                        val intakes = intakesResult.data
                        val daysWithData = intakes.count { it.meals.isNotEmpty() }
                        val totalDays = dateRange.dayCount().toInt()
                        
                        val avgCalories = if (daysWithData > 0) {
                            intakes.sumOf { it.getTotalCalories() }.toDouble() / daysWithData
                        } else 0.0
                        
                        val avgProtein = if (daysWithData > 0) {
                            intakes.sumOf { it.getTotalProtein() } / daysWithData
                        } else 0.0
                        
                        val avgFat = if (daysWithData > 0) {
                            intakes.sumOf { it.getTotalFat() } / daysWithData
                        } else 0.0
                        
                        val avgCarbs = if (daysWithData > 0) {
                            intakes.sumOf { it.getTotalCarbs() } / daysWithData
                        } else 0.0
                        
                        val goalAchievementRate = if (daysWithData > 0) {
                            intakes.count { it.isCalorieGoalMet() == true }.toDouble() / daysWithData
                        } else 0.0
                        
                        val statistics = NutritionStatistics(
                            averageCalories = avgCalories,
                            averageProtein = avgProtein,
                            averageFat = avgFat,
                            averageCarbs = avgCarbs,
                            totalDays = totalDays,
                            daysWithData = daysWithData,
                            goalAchievementRate = goalAchievementRate
                        )
                        
                        Result.success(statistics)
                    }
                    is Result.Error -> intakesResult
                }
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get nutrition statistics: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun getAverageDailyIntake(dateRange: DateRange): Result<NutritionIntake> {
        return withContext(Dispatchers.IO) {
            try {
                val statisticsResult = getNutritionStatistics(dateRange)
                when (statisticsResult) {
                    is Result.Success -> {
                        val stats = statisticsResult.data
                        
                        // Create average intake (without meals, just totals)
                        val avgIntake = NutritionIntake.empty(dateRange.startDate)
                        
                        Result.success(avgIntake)
                    }
                    is Result.Error -> statisticsResult
                }
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get average daily intake: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun clearDayData(date: LocalDate): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                dataRepository.clearDayData(date.toString())
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to clear day data: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun invalidateDailyCache(date: LocalDate): Result<Unit> {
        // TODO: Implement cache invalidation
        return Result.success(Unit)
    }
    
    override suspend fun getNutritionTrends(dateRange: DateRange): Result<NutritionTrends> {
        return withContext(Dispatchers.IO) {
            try {
                val intakesResult = getIntakeForDateRange(dateRange)
                when (intakesResult) {
                    is Result.Success -> {
                        val intakes = intakesResult.data
                        
                        val caloriesTrend = intakes.map { it.date to it.getTotalCalories() }
                        val proteinTrend = intakes.map { it.date to it.getTotalProtein() }
                        val fatTrend = intakes.map { it.date to it.getTotalFat() }
                        val carbsTrend = intakes.map { it.date to it.getTotalCarbs() }
                        
                        val trends = NutritionTrends(
                            caloriesTrend = caloriesTrend,
                            proteinTrend = proteinTrend,
                            fatTrend = fatTrend,
                            carbsTrend = carbsTrend
                        )
                        
                        Result.success(trends)
                    }
                    is Result.Error -> intakesResult
                }
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get nutrition trends: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun exportNutritionData(dateRange: DateRange): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val intakesResult = getIntakeForDateRange(dateRange)
                when (intakesResult) {
                    is Result.Success -> {
                        val intakes = intakesResult.data
                        val exportData = buildString {
                            appendLine("Nutrition Data Export")
                            appendLine("Date Range: ${dateRange.startDate} to ${dateRange.endDate}")
                            appendLine()
                            
                            for (intake in intakes) {
                                appendLine("Date: ${intake.date}")
                                appendLine("Calories: ${intake.getTotalCalories()}")
                                appendLine("Protein: ${intake.getTotalProtein()}g")
                                appendLine("Fat: ${intake.getTotalFat()}g")
                                appendLine("Carbs: ${intake.getTotalCarbs()}g")
                                appendLine("Meals: ${intake.meals.size}")
                                appendLine()
                            }
                        }
                        
                        Result.success(exportData)
                    }
                    is Result.Error -> intakesResult
                }
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to export nutrition data: ${e.message}",
                        e
                    )
                )
            }
        }
    }
}