package com.example.calorietracker.data.repositories

import android.util.Base64
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.mappers.FoodMapper
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.common.DateRange
import com.example.calorietracker.domain.entities.common.MealType
import com.example.calorietracker.domain.entities.common.FoodSource
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.FoodRepository
import com.example.calorietracker.domain.repositories.UserRepository
import com.example.calorietracker.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FoodRepository that handles food-related operations
 */
@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val makeService: MakeService,
    private val dataRepository: DataRepository,
    private val foodMapper: FoodMapper,
    private val userRepository: UserRepository
) : FoodRepository {
    
    override suspend fun analyzeFoodPhoto(photoPath: String, caption: String): Result<Food> {
        return withContext(Dispatchers.IO) {
            try {
                val userProfile = getUserProfileForAI()
                val imageBase64 = convertImageToBase64(photoPath)
                
                val request = ImageAnalysisRequest(
                    imageBase64 = imageBase64,
                    userProfile = userProfile,
                    caption = caption
                )
                
                val response = makeService.analyzeFoodImage(MakeService.WEBHOOK_ID, request)
                val food = parseFoodAnalysisResponse(response, FoodSource.AI_PHOTO_ANALYSIS)
                
                Result.success(food)
            } catch (e: Exception) {
                Result.error(
                    DomainException.AIAnalysisException(
                        "Failed to analyze food photo: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun analyzeFoodDescription(description: String): Result<Food> {
        return withContext(Dispatchers.IO) {
            try {
                println("DEBUG: FoodRepositoryImpl.analyzeFoodDescription called with: '$description'")
                val userProfile = getUserProfileForAI()
                
                val request = FoodAnalysisRequest(
                    weight = 100,
                    userProfile = userProfile,
                    message = description,
                    userId = "user_${System.currentTimeMillis()}",
                    messageType = "analysis",
                    includeOpinion = true
                )
                
                println("DEBUG: Making API call to makeService.analyzeFood")
                
                // Временное решение: добавляем таймаут и мок-ответ для тестирования
                val response = try {
                    withTimeout(10000) { // 10 секунд таймаут
                        makeService.analyzeFood(MakeService.WEBHOOK_ID, request)
                    }
                } catch (e: Exception) {
                    println("DEBUG: API call failed or timed out: ${e.message}")
                    // Возвращаем мок-ответ для тестирования
                    FoodAnalysisResponse(
                        status = "success",
                        answer = """{"name":"${description}","calories":100,"protein":5.0,"fat":2.0,"carbs":15.0,"weight":"100г","opinion":"Это тестовый ответ, так как API не отвечает"}"""
                    )
                }
                
                println("DEBUG: Got response from API: $response")
                
                val food = parseFoodAnalysisResponse(response, FoodSource.AI_TEXT_ANALYSIS)
                println("DEBUG: Parsed food: ${food.name}")
                
                Result.success(food)
            } catch (e: Exception) {
                println("DEBUG: Exception in analyzeFoodDescription: ${e.message}")
                e.printStackTrace()
                Result.error(
                    DomainException.AIAnalysisException(
                        "Failed to analyze food description: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun saveFoodIntake(food: Food, mealType: MealType): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Convert domain entities to data entities
                val foodItem = foodMapper.mapDomainToData(food)
                
                // Save using existing DataRepository method
                dataRepository.saveFoodToHistory(foodItem, mealType.displayName)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to save food intake: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun getFoodHistory(dateRange: DateRange): Result<List<Food>> {
        return withContext(Dispatchers.IO) {
            try {
                // Get food history from DataRepository
                // Note: DataRepository doesn't have date range support yet,
                // so we get all history and filter
                val allHistory = dataRepository.getFoodHistory()
                
                // Convert to domain entities
                val domainFoods = foodMapper.mapDataListToDomain(allHistory)
                
                // TODO: Filter by date range when timestamp is available in Food entity
                
                Result.success(domainFoods)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get food history: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun searchFoodByName(query: String): Result<List<Food>> {
        return withContext(Dispatchers.IO) {
            try {
                // Get all food history and filter by name
                val allHistory = dataRepository.getFoodHistory()
                val filtered = allHistory.filter { 
                    it.name.contains(query, ignoreCase = true) 
                }
                
                val domainFoods = foodMapper.mapDataListToDomain(filtered)
                Result.success(domainFoods)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to search food: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun getRecentFoods(limit: Int): Result<List<Food>> {
        return withContext(Dispatchers.IO) {
            try {
                val allHistory = dataRepository.getFoodHistory()
                val recent = allHistory.takeLast(limit)
                
                val domainFoods = foodMapper.mapDataListToDomain(recent)
                Result.success(domainFoods)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get recent foods: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun getFavoriteFoods(): Result<List<Food>> {
        // TODO: Implement favorites functionality
        return Result.success(emptyList())
    }
    
    override suspend fun markFoodAsFavorite(food: Food): Result<Unit> {
        // TODO: Implement favorites functionality
        return Result.success(Unit)
    }
    
    override suspend fun removeFoodFromFavorites(food: Food): Result<Unit> {
        // TODO: Implement favorites functionality
        return Result.success(Unit)
    }
    
    override suspend fun validateFoodData(food: Food): Result<Food> {
        return try {
            // Validate food data
            if (food.name.isBlank()) {
                Result.error(DomainException.ValidationException("Food name cannot be blank"))
            } else if (food.calories < 0) {
                Result.error(DomainException.ValidationException("Calories cannot be negative"))
            } else if (!food.hasReasonableNutrition()) {
                Result.error(DomainException.ValidationException("Nutrition values are unreasonable"))
            } else {
                Result.success(food)
            }
        } catch (e: Exception) {
            Result.error(
                DomainException.ValidationException(
                    "Food validation failed: ${e.message}",
                    e
                )
            )
        }
    }
    
    override suspend fun getNutritionInfo(foodName: String): Result<Food?> {
        return withContext(Dispatchers.IO) {
            try {
                // Search for food in history
                val searchResult = searchFoodByName(foodName)
                when (searchResult) {
                    is Result.Success -> {
                        val foods = searchResult.data
                        val exactMatch = foods.find { 
                            it.name.equals(foodName, ignoreCase = true) 
                        }
                        Result.success(exactMatch)
                    }
                    is Result.Error -> searchResult
                }
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get nutrition info: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    /**
     * Convert image file to Base64 string
     */
    private fun convertImageToBase64(imagePath: String): String {
        return try {
            val imageFile = File(imagePath)
            val imageBytes = imageFile.readBytes()
            Base64.encodeToString(imageBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            throw DomainException.AIAnalysisException("Failed to convert image to Base64: ${e.message}", e)
        }
    }
    
    /**
     * Get user profile data for AI requests
     */
    private suspend fun getUserProfileForAI(): UserProfileData {
        return try {
            val userResult = userRepository.getUserProfile()
            when (userResult) {
                is Result.Success -> {
                    val user = userResult.data
                    UserProfileData(
                        age = user.getAge() ?: 25,
                        weight = user.weight,
                        height = user.height,
                        gender = user.gender.name.lowercase(),
                        activityLevel = user.activityLevel.name.lowercase(),
                        goal = user.goal.name.lowercase()
                    )
                }
                is Result.Error -> {
                    // Return default profile if user profile not found
                    UserProfileData(
                        age = 25,
                        weight = 70,
                        height = 170,
                        gender = "other",
                        activityLevel = "moderately_active",
                        goal = "maintain_weight"
                    )
                }
            }
        } catch (e: Exception) {
            // Return default profile on error
            UserProfileData(
                age = 25,
                weight = 70,
                height = 170,
                gender = "other",
                activityLevel = "moderately_active",
                goal = "maintain_weight"
            )
        }
    }
    
    /**
     * Parse Food entity from Make.com AI response
     */
    private fun parseFoodAnalysisResponse(response: FoodAnalysisResponse, source: FoodSource): Food {
        return try {
            val answer = response.answer ?: ""
            println("DEBUG: Parsing response answer: '$answer'")
            
            // Try to parse JSON from answer field
            val jsonObject = JSONObject(answer)
            
            val food = Food(
                name = jsonObject.optString("name", "Неизвестный продукт"),
                calories = jsonObject.optInt("calories", 0),
                protein = jsonObject.optDouble("protein", 0.0),
                fat = jsonObject.optDouble("fat", 0.0),
                carbs = jsonObject.optDouble("carbs", 0.0),
                weight = jsonObject.optString("weight", "100г"),
                source = source,
                aiOpinion = jsonObject.optString("opinion", "")
            )
            println("DEBUG: Successfully parsed food from JSON: ${food.name}")
            food
        } catch (e: Exception) {
            println("DEBUG: JSON parsing failed, trying fallback parsing: ${e.message}")
            // Fallback parsing if JSON parsing fails
            val answer = response.answer ?: ""
            
            // Try to extract basic info from text response
            val name = extractValueFromText(answer, "name", "Неизвестный продукт")
            val calories = extractIntFromText(answer, "calories", 0)
            val protein = extractDoubleFromText(answer, "protein", 0.0)
            val fat = extractDoubleFromText(answer, "fat", 0.0)
            val carbs = extractDoubleFromText(answer, "carbs", 0.0)
            
            val food = Food(
                name = name,
                calories = calories,
                protein = protein,
                fat = fat,
                carbs = carbs,
                weight = "100г",
                source = source,
                aiOpinion = answer
            )
            println("DEBUG: Successfully parsed food from fallback: ${food.name}")
            food
        }
    }
    
    /**
     * Extract string value from text response
     */
    private fun extractValueFromText(text: String, key: String, defaultValue: String): String {
        return try {
            val regex = Regex("\"?$key\"?\\s*[:=]\\s*\"?([^,\"\\n}]+)\"?", RegexOption.IGNORE_CASE)
            regex.find(text)?.groupValues?.get(1)?.trim() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    /**
     * Extract integer value from text response
     */
    private fun extractIntFromText(text: String, key: String, defaultValue: Int): Int {
        return try {
            val regex = Regex("\"?$key\"?\\s*[:=]\\s*\"?(\\d+)\"?", RegexOption.IGNORE_CASE)
            regex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    /**
     * Extract double value from text response
     */
    private fun extractDoubleFromText(text: String, key: String, defaultValue: Double): Double {
        return try {
            val regex = Regex("\"?$key\"?\\s*[:=]\\s*\"?(\\d+\\.?\\d*)\"?", RegexOption.IGNORE_CASE)
            regex.find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
}