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
import com.google.gson.Gson
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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
    private val userRepository: UserRepository,
    private val okHttpClient: OkHttpClient
) : FoodRepository {

    /**
     * Быстрый аплоад изображения на временный хост и возврат прямой ссылки.
     * 1) 0x0.st (multipart)
     * 2) fallback: transfer.sh (PUT)
     */
    private fun uploadImageToTemporaryHost(imageFile: File): String {
        // 1) 0x0.st
        try {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    imageFile.name,
                    imageFile.asRequestBody("image/jpeg".toMediaType())
                )
                .build()
            val req = Request.Builder()
                .url("https://0x0.st")
                .post(body)
                .header("Accept", "text/plain")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                val url = resp.body?.string()?.trim().orEmpty()
                if (resp.isSuccessful && url.startsWith("http")) {
                    return url
                } else {
                    throw IllegalStateException("0x0.st failed: code=${resp.code} body=${url.take(120)}")
                }
            }
        } catch (e: Exception) {
            println("WARN: 0x0.st upload failed: ${e.message}")
        }

        // 2) transfer.sh
        val putReq = Request.Builder()
            .url("https://transfer.sh/${imageFile.name}")
            .put(imageFile.asRequestBody("application/octet-stream".toMediaType()))
            .header("Accept", "text/plain")
            .build()
        okHttpClient.newCall(putReq).execute().use { resp ->
            val url = resp.body?.string()?.trim().orEmpty()
            if (resp.isSuccessful && url.startsWith("http")) {
                return url
            } else {
                throw DomainException.AIAnalysisException("transfer.sh failed: code=${resp.code} body=${url.take(120)}")
            }
        }
    }
    
    override suspend fun analyzeFoodPhoto(photoPath: String, caption: String): Result<Food> {
        return withContext(Dispatchers.IO) {
            try {
                val userProfile = getUserProfileForAI()
                val imageFile = File(photoPath)
                if (!imageFile.exists()) {
                    throw DomainException.AIAnalysisException("Image file not found: $photoPath")
                }

                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val photoPart = MultipartBody.Part.createFormData(
                    name = "photo",
                    filename = imageFile.name,
                    body = imageFile.asRequestBody(mediaType)
                )

                val profileJson = Gson().toJson(userProfile)
                val userProfileBody = profileJson.toRequestBody("application/json".toMediaType())
                val userIdBody = java.util.UUID.randomUUID().toString().toRequestBody("text/plain".toMediaType())
                val captionBody = caption.ifBlank { "" }.toRequestBody("text/plain".toMediaType())
                val inferredMessageType = if (caption.startsWith("[RECIPE]")) "recipe_photo" else "photo"
                val messageTypeBody = inferredMessageType.toRequestBody("text/plain".toMediaType())
                val firstBody = "false".toRequestBody("text/plain".toMediaType())
                // Совместимость с роутером: value содержит JSON со служебными полями
                val valueJson = "{" +
                        "\"messageType\":\"${inferredMessageType}\"," +
                        "\"isFirstMessageOfDay\":false" +
                        "}"
                val valueBody = valueJson.toRequestBody("application/json".toMediaType())

                val chatResp = withTimeout(60000) {
                    makeService.askAiDietitianWithPhoto(
                        MakeService.WEBHOOK_ID,
                        photoPart,
                        userProfileBody,
                        userIdBody,
                        captionBody,
                        messageTypeBody,
                        firstBody,
                        valueBody
                    )
                }
                val response = FoodAnalysisResponse(status = chatResp.status, answer = chatResp.answer)
                val food = parseFoodAnalysisResponse(response, FoodSource.AI_PHOTO_ANALYSIS)
                Result.success(food)
            } catch (e: Exception) {
                println("DEBUG: Exception in analyzeFoodPhoto: ${e.message}")
                e.printStackTrace()
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
                val userProfile = getUserProfileForAI()
                val request = FoodAnalysisRequest(
                    weight = 100,
                    userProfile = userProfile,
                    message = description,
                    userId = "user_${System.currentTimeMillis()}",
                    messageType = "analysis",
                    includeOpinion = true
                )

                val response = withTimeout(30000) {
                    makeService.analyzeFood(MakeService.WEBHOOK_ID, request)
                }

                val food = parseFoodAnalysisResponse(response, FoodSource.AI_TEXT_ANALYSIS)
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
     * Convert image file to Base64 string with compression
     */
    private fun convertImageToBase64(imagePath: String): String {
        return try {
            val imageFile = File(imagePath)
            
            // Декодируем изображение
            var bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
            
            // Ресайз если изображение слишком большое (макс 1024x1024)
            val maxDimension = 1024
            if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val scale = minOf(
                    maxDimension.toFloat() / bitmap.width,
                    maxDimension.toFloat() / bitmap.height
                )
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                println("DEBUG: Image resized to ${newWidth}x${newHeight}")
            }
            
            val stream = java.io.ByteArrayOutputStream()
            
            // Сжимаем до 85% качества в JPEG
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
            val imageBytes = stream.toByteArray()
            
            println("DEBUG: Image compressed from ${imageFile.length()} to ${imageBytes.size} bytes")
            
            // Проверяем размер base64 (он будет ~33% больше)
            val base64Size = (imageBytes.size * 4 / 3)
            if (base64Size > 5_000_000) { // 5MB лимит для base64
                throw DomainException.AIAnalysisException("Image too large even after compression")
            }
            
            // Важно: NO_WRAP, чтобы убрать переносы строк в base64
            Base64.encodeToString(imageBytes, Base64.NO_WRAP)
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