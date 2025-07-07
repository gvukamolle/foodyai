package com.example.calorietracker.data.entities

import androidx.room.*
import com.example.calorietracker.data.UserProfileEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Room
import com.example.calorietracker.data.UserProfileDao

// Enums
enum class RecommendationType {
    NUTRITION, MEAL_PLANNING, HEALTH_TIP, RECIPE, WARNING
}

enum class SyncStatus {
    PENDING, SYNCED, FAILED, CONFLICT
}

// Type Converters
@TypeConverters
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromRecommendationType(type: RecommendationType): String = type.name

    @TypeConverter
    fun toRecommendationType(type: String): RecommendationType =
        RecommendationType.valueOf(type)

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(status: String): SyncStatus = SyncStatus.valueOf(status)

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromListString(list: List<String>): String = gson.toJson(list)

    @TypeConverter
    fun fromMap(value: String): Map<String, Double> {
        val mapType = object : TypeToken<Map<String, Double>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun fromMapToString(map: Map<String, Double>): String = gson.toJson(map)
}

// Extended Food Item Entity
@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    val weight: Int,
    val timestamp: Long,
    val mealType: String,
    val isManualEntry: Boolean,
    val syncStatus: SyncStatus,
    val aiAnalyzed: Boolean = false,
    val micronutrients: Map<String, Double>? = null,
    val healthScore: Int? = null,
    val tags: List<String>? = null,
    val imageUri: String? = null,
    val aiOpinion: String? = null  // Новое поле для AI мнения
)

// AI Recommendation Entity
@Entity(
    tableName = "ai_recommendations",
    indices = [Index(value = ["type", "createdAt"])]
)
data class AIRecommendationEntity(
    @PrimaryKey val id: String,
    val content: String,
    val type: RecommendationType,
    val priority: Int = 0,
    val actionItems: List<String>? = null,
    val createdAt: Long,
    val expiresAt: Long,
    val isRead: Boolean = false,
    val isApplied: Boolean = false
)

// Sync Queue Entity
@Entity(
    tableName = "sync_queue",
    indices = [Index(value = ["endpoint", "createdAt"])]
)
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val data: String, // JSON serialized data
    val endpoint: String,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val createdAt: Long,
    val lastAttempt: Long? = null,
    val error: String? = null
)

// Meal Plan Entity
@Entity(tableName = "meal_plans")
data class MealPlanEntity(
    @PrimaryKey val id: String,
    val date: String, // YYYY-MM-DD
    val mealType: String,
    val foodName: String,
    val calories: Int,
    val recipe: String? = null,
    val ingredients: List<String>? = null,
    val isCompleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

// Analytics Entity
@Entity(
    tableName = "analytics_data",
    indices = [Index(value = ["date"])]
)
data class AnalyticsEntity(
    @PrimaryKey val id: String,
    val date: String, // YYYY-MM-DD
    val totalCalories: Int,
    val totalProteins: Int,
    val totalFats: Int,
    val totalCarbs: Int,
    val mealsCount: Int,
    val waterIntake: Int? = null,
    val exerciseMinutes: Int? = null,
    val mood: String? = null,
    val notes: String? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

// DAOs
@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items ORDER BY timestamp DESC")
    suspend fun getAllFoodItems(): List<FoodItemEntity>

    @Query("SELECT * FROM food_items WHERE date(timestamp/1000, 'unixepoch') = :date ORDER BY timestamp DESC")
    suspend fun getFoodItemsByDate(date: String): List<FoodItemEntity>

    @Query("SELECT * FROM food_items WHERE syncStatus = :status")
    suspend fun getFoodItemsBySyncStatus(status: SyncStatus): List<FoodItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(item: FoodItemEntity)

    @Update
    suspend fun updateFoodItem(item: FoodItemEntity)

    @Delete
    suspend fun deleteFoodItem(item: FoodItemEntity)

    @Query("DELETE FROM food_items WHERE timestamp < :timestamp")
    suspend fun deleteOldFoodItems(timestamp: Long)
}

@Dao
interface AIRecommendationDao {
    @Query("SELECT * FROM ai_recommendations WHERE expiresAt > :currentTime ORDER BY priority DESC, createdAt DESC")
    suspend fun getActiveRecommendations(currentTime: Long): List<AIRecommendationEntity>

    @Query("SELECT * FROM ai_recommendations WHERE type = :type AND expiresAt > :currentTime ORDER BY createdAt DESC")
    suspend fun getRecommendationsByType(type: RecommendationType, currentTime: Long): List<AIRecommendationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendation(recommendation: AIRecommendationEntity)

    @Update
    suspend fun updateRecommendation(recommendation: AIRecommendationEntity)

    @Query("UPDATE ai_recommendations SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM ai_recommendations WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredRecommendations(currentTime: Long)
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE retryCount < maxRetries ORDER BY createdAt ASC")
    suspend fun getPendingSyncItems(): List<SyncQueueEntity>

    @Insert
    suspend fun insertSyncItem(item: SyncQueueEntity)

    @Update
    suspend fun updateSyncItem(item: SyncQueueEntity)

    @Delete
    suspend fun deleteSyncItem(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteSyncItemById(id: String)
}

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plans WHERE date BETWEEN :startDate AND :endDate ORDER BY date, mealType")
    suspend fun getMealPlansBetweenDates(startDate: String, endDate: String): List<MealPlanEntity>

    @Query("SELECT * FROM meal_plans WHERE date = :date ORDER BY mealType")
    suspend fun getMealPlansByDate(date: String): List<MealPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlans(plans: List<MealPlanEntity>)

    @Update
    suspend fun updateMealPlan(plan: MealPlanEntity)

    @Query("UPDATE meal_plans SET isCompleted = :completed WHERE id = :id")
    suspend fun markMealAsCompleted(id: String, completed: Boolean)
}

@Dao
interface AnalyticsDao {
    @Query("SELECT * FROM analytics_data WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getAnalyticsBetweenDates(startDate: String, endDate: String): List<AnalyticsEntity>

    @Query("SELECT * FROM analytics_data WHERE date = :date")
    suspend fun getAnalyticsByDate(date: String): AnalyticsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalytics(analytics: AnalyticsEntity)

    @Query("DELETE FROM analytics_data WHERE date < :date")
    suspend fun deleteOldAnalytics(date: String)
}

// Extended Database
@Database(
    entities = [
        UserProfileEntity::class,
        FoodItemEntity::class,
        AIRecommendationEntity::class,
        SyncQueueEntity::class,
        MealPlanEntity::class,
        AnalyticsEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun aiRecommendationDao(): AIRecommendationDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "foody_ai_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new tables
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS food_items (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        calories REAL NOT NULL,
                        proteins REAL NOT NULL,
                        fats REAL NOT NULL,
                        carbs REAL NOT NULL,
                        weight INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        mealType TEXT NOT NULL,
                        isManualEntry INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL,
                        aiAnalyzed INTEGER NOT NULL DEFAULT 0,
                        micronutrients TEXT,
                        healthScore INTEGER,
                        tags TEXT,
                        imageUri TEXT
                    )
                """.trimIndent())

                // Add other table creation SQLs...
            }
        }
    }
}
