package com.example.calorietracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyNutritionDao {
    @Query("SELECT * FROM daily_nutrition_summary WHERE date = :date")
    suspend fun getSummaryForDate(date: LocalDate): DailyNutritionSummary?

    @Query("SELECT * FROM daily_nutrition_summary WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getSummariesBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyNutritionSummary>>

    @Query("SELECT * FROM daily_nutrition_summary ORDER BY date DESC LIMIT :limit")
    fun getRecentSummaries(limit: Int = 90): Flow<List<DailyNutritionSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSummary(summary: DailyNutritionSummary)

    @Query("DELETE FROM daily_nutrition_summary WHERE date < :cutoffDate")
    suspend fun deleteOldSummaries(cutoffDate: LocalDate)
}

// Converters для Room
class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? {
        return epochDay?.let { LocalDate.ofEpochDay(it) }
    }
}
