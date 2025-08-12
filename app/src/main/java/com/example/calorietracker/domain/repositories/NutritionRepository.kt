package com.example.calorietracker.domain.repositories

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.Meal
import com.example.calorietracker.domain.entities.common.DateRange
import java.time.LocalDate
import java.time.YearMonth

/**
 * Repository interface for nutrition tracking operations
 */
interface NutritionRepository {
    
    /**
     * Get daily nutrition intake for a specific date
     */
    suspend fun getDailyIntake(date: LocalDate): Result<NutritionIntake>
    
    /**
     * Get weekly nutrition intake
     */
    suspend fun getWeeklyIntake(startDate: LocalDate): Result<List<NutritionIntake>>
    
    /**
     * Get monthly nutrition intake
     */
    suspend fun getMonthlyIntake(month: YearMonth): Result<List<NutritionIntake>>
    
    /**
     * Get nutrition intake for a custom date range
     */
    suspend fun getIntakeForDateRange(dateRange: DateRange): Result<List<NutritionIntake>>
    
    /**
     * Save daily nutrition intake
     */
    suspend fun saveDailyIntake(intake: NutritionIntake): Result<Unit>
    
    /**
     * Add meal to daily intake
     */
    suspend fun addMealToDay(date: LocalDate, meal: Meal): Result<Unit>
    
    /**
     * Remove meal from daily intake
     */
    suspend fun removeMealFromDay(date: LocalDate, mealIndex: Int): Result<Unit>
    
    /**
     * Update meal in daily intake
     */
    suspend fun updateMealInDay(date: LocalDate, mealIndex: Int, meal: Meal): Result<Unit>
    
    /**
     * Get nutrition statistics for a date range
     */
    suspend fun getNutritionStatistics(dateRange: DateRange): Result<NutritionStatistics>
    
    /**
     * Get average daily intake for a period
     */
    suspend fun getAverageDailyIntake(dateRange: DateRange): Result<NutritionIntake>
    
    /**
     * Clear nutrition data for a specific date
     */
    suspend fun clearDayData(date: LocalDate): Result<Unit>
    
    /**
     * Invalidate cache for a specific date
     */
    suspend fun invalidateDailyCache(date: LocalDate): Result<Unit>
    
    /**
     * Get nutrition trends (calories, protein, etc. over time)
     */
    suspend fun getNutritionTrends(dateRange: DateRange): Result<NutritionTrends>
    
    /**
     * Export nutrition data for a date range
     */
    suspend fun exportNutritionData(dateRange: DateRange): Result<String>
}

/**
 * Nutrition statistics for analysis
 */
data class NutritionStatistics(
    val averageCalories: Double,
    val averageProtein: Double,
    val averageFat: Double,
    val averageCarbs: Double,
    val totalDays: Int,
    val daysWithData: Int,
    val goalAchievementRate: Double
)

/**
 * Nutrition trends over time
 */
data class NutritionTrends(
    val caloriesTrend: List<Pair<LocalDate, Int>>,
    val proteinTrend: List<Pair<LocalDate, Double>>,
    val fatTrend: List<Pair<LocalDate, Double>>,
    val carbsTrend: List<Pair<LocalDate, Double>>,
    val weightTrend: List<Pair<LocalDate, Double>>? = null
)