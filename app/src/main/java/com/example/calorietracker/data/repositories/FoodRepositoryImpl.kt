package com.example.calorietracker.data.repositories

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.FileOutputStream
import java.net.InetAddress
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
    
    // Overload with messageType for compatibility
    override suspend fun analyzeFoodPhoto(photoPath: String, caption: String, messageType: String): Result<Food> {
        // Simply ignore messageType and use our standard approach
        return analyzeFoodPhoto(photoPath, caption)
    }
    
    override suspend fun analyzeFoodPhoto(photoPath: String, caption: String): Result<Food> {
        return withContext(Dispatchers.IO) {
            try {
                println("DEBUG: analyzeFoodPhoto: Starting photo analysis")
                val originalFile = File(photoPath)
                if (!originalFile.exists()) {
                    throw DomainException.AIAnalysisException("Image file not found: $photoPath")
                }
                println("DEBUG: Original image size: ${originalFile.length()} bytes (${originalFile.length() / 1024}KB)")
                
                // Компрессируем изображение если оно слишком большое
                val imageFile = if (originalFile.length() / 1024 > 500) {
                    compressImage(photoPath, 500) ?: originalFile
                } else {
                    originalFile
                }
                
                val userProfile = getUserProfileForAI()
                
                // Make.com рекомендует multipart/form-data для изображений
                // Отправляем ТОЛЬКО через multipart, без base64
                println("DEBUG: Sending MULTIPART request to Make.com webhook")
                
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val gson = Gson()
                val profileJson = gson.toJson(userProfile)
                val userIdStr = "user_${System.currentTimeMillis()}"
                val captionStr = caption.ifBlank { "" }
                val messageTypeStr = "analysis"
                val firstStr = "false"

                // Используем "photo" вместо "file" для консистентности с MakeWebhookClient
                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    // Файл передаем как бинарные данные
                    .addFormDataPart(
                        name = "photo",  // Изменено с "file" на "photo" для консистентности
                        filename = imageFile.name,
                        body = imageFile.asRequestBody(mediaType)
                    )
                    // Дополнительные поля как обычные form-data
                    .addFormDataPart("filename", imageFile.name)
                    .addFormDataPart("userProfile", profileJson)
                    .addFormDataPart("userId", userIdStr)
                    .addFormDataPart("note", captionStr)  // Make.com часто использует "note" для описаний
                    .addFormDataPart("caption", captionStr)
                    .addFormDataPart("messageType", messageTypeStr)
                    .addFormDataPart("isFirstMessageOfDay", firstStr)
                    .build()

                // Сначала пробуем DNS резолвинг
                val makeComIP = resolveMakeComIP()
                
                val url = if (makeComIP != null && makeComIP.isNotEmpty()) {
                    // Если DNS работает, используем обычный URL
                    MakeService.BASE_URL + MakeService.WEBHOOK_ID
                } else {
                    // DNS заблокирован - пробуем известные IP адреса Make.com
                    println("WARNING: DNS blocked, trying known Make.com IPs")
                    // Эти IP можно получить заранее с VPN и захардкодить
                    val knownIPs = listOf("35.174.94.163", "52.73.40.154") // Примерные IP, нужно проверить актуальные
                    val workingIP = knownIPs.firstOrNull { ip ->
                        try {
                            InetAddress.getByName(ip).isReachable(5000)
                        } catch (e: Exception) {
                            false
                        }
                    }
                    
                    if (workingIP != null) {
                        createUrlWithIP(workingIP)
                    } else {
                        // Если ничего не работает, все равно пробуем обычный URL
                        MakeService.BASE_URL + MakeService.WEBHOOK_ID
                    }
                }
                
                val request = Request.Builder()
                    .url(url)
                    .post(multipartBody)
                    // Изменяем заголовки чтобы выглядеть как обычный браузер
                    .header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Origin", "https://app.calorietracker.com") // Фиктивный origin
                    .header("Referer", "https://app.calorietracker.com/upload")
                    // Если используем IP, добавляем Host заголовок
                    .apply {
                        if (url.contains(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                            header("Host", "hook.us2.make.com")
                        }
                    }
                    .build()

                println("DEBUG: Sending multipart request to: $url")
                println("DEBUG: Request size approximately: ${imageFile.length() / 1024}KB")
                
                val httpResponse = try {
                    val startTime = System.currentTimeMillis()
                    withTimeout(60000) {  // Увеличиваем таймаут до 60 секунд для медленных мобильных сетей
                        val response = okHttpClient.newCall(request).execute()
                        val duration = System.currentTimeMillis() - startTime
                        println("DEBUG: Request completed in ${duration}ms")
                        response
                    }
                } catch (e: TimeoutCancellationException) {
                    println("ERROR: Request timeout - возможно медленная мобильная сеть")
                    throw DomainException.AIAnalysisException("Request timeout after 60s - медленное соединение")
                } catch (e: java.net.SocketTimeoutException) {
                    println("ERROR: Socket timeout - проблема с сетью")
                    throw DomainException.AIAnalysisException("Socket timeout - проверьте интернет соединение")
                } catch (e: java.net.UnknownHostException) {
                    println("ERROR: DNS resolution failed - ${e.message}")
                    throw DomainException.AIAnalysisException("Не удалось подключиться к серверу Make.com")
                } catch (e: java.io.IOException) {
                    println("ERROR: Network IO error - ${e.message}")
                    println("DEBUG: Attempting fallback to base64 method due to multipart failure")
                    
                    // Fallback на base64 метод если multipart не сработал
                    return@withContext analyzeFoodPhotoBase64Fallback(imageFile, caption, userProfile)
                }
                
                val bodyString = httpResponse.body?.string() ?: ""
                println("DEBUG: Response: code=${httpResponse.code}, body length=${bodyString.length}")
                
                if (!httpResponse.isSuccessful) {
                    println("ERROR: Request failed with HTTP ${httpResponse.code}")
                    println("ERROR: Response body: ${bodyString.take(500)}")
                    
                    // Попробуем base64 как fallback для некоторых кодов ошибок
                    if (httpResponse.code in listOf(413, 502, 503, 504)) {
                        println("DEBUG: Attempting base64 fallback due to HTTP ${httpResponse.code}")
                        return@withContext analyzeFoodPhotoBase64Fallback(imageFile, caption, userProfile)
                    }
                    
                    throw DomainException.AIAnalysisException("HTTP ${httpResponse.code}: ${bodyString.take(200)}")
                }
                
                println("DEBUG: Successfully received response from Make.com")
                
                val response = try {
                    val parsed = Gson().fromJson(bodyString, FoodAnalysisResponse::class.java)
                    println("DEBUG: Successfully parsed JSON response")
                    parsed
                } catch (e: Exception) {
                    println("WARNING: Failed to parse as JSON, treating as text response")
                    // Если сервер вернул не JSON, оборачиваем как текстовый ответ
                    FoodAnalysisResponse(status = "ok", answer = bodyString)
                }
                
                val food = parseFoodAnalysisResponse(response, FoodSource.AI_PHOTO_ANALYSIS)
                Result.success(food)
                
            } catch (e: Exception) {
                println("ERROR: analyzeFoodPhoto failed: ${e.message}")
                e.printStackTrace()
                Result.error(DomainException.AIAnalysisException("Analysis failed: ${e.message}", e))
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
    
    /**
     * Fallback метод для отправки фото через base64
     * Используется когда multipart запрос не проходит через мобильную сеть
     */
    private suspend fun analyzeFoodPhotoBase64Fallback(
        imageFile: File, 
        caption: String,
        userProfile: UserProfileData
    ): Result<Food> {
        return try {
            println("DEBUG: Using base64 fallback method for photo analysis")
            
            // Читаем и кодируем изображение в base64
            val imageBytes = imageFile.readBytes()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            println("DEBUG: Base64 encoded size: ${base64Image.length} chars")
            
            // Подготавливаем JSON запрос
            val jsonRequest = JSONObject().apply {
                put("imageBase64", base64Image)
                put("userProfile", JSONObject().apply {
                    put("age", userProfile.age)
                    put("weight", userProfile.weight)
                    put("height", userProfile.height)
                    put("gender", userProfile.gender)
                    put("activityLevel", userProfile.activityLevel)
                    put("goal", userProfile.goal)
                })
                put("caption", caption)
                put("note", caption)
                put("messageType", "analysis")
                put("isFirstMessageOfDay", false)
                put("userId", "user_${System.currentTimeMillis()}")
            }
            
            val url = MakeService.BASE_URL + MakeService.WEBHOOK_ID
            val request = Request.Builder()
                .url(url)
                .post(jsonRequest.toString().toRequestBody("application/json".toMediaType()))
                .header("Accept", "application/json")
                .header("User-Agent", "calorietracker/1.0")
                .header("Content-Type", "application/json")
                .build()
            
            println("DEBUG: Sending base64 JSON request to: $url")
            
            val httpResponse = withTimeout(60000) {
                okHttpClient.newCall(request).execute()
            }
            
            val bodyString = httpResponse.body?.string() ?: ""
            println("DEBUG: Base64 response: code=${httpResponse.code}, body length=${bodyString.length}")
            
            if (!httpResponse.isSuccessful) {
                println("ERROR: Base64 request also failed with HTTP ${httpResponse.code}")
                throw DomainException.AIAnalysisException("Both multipart and base64 methods failed")
            }
            
            val response = try {
                Gson().fromJson(bodyString, FoodAnalysisResponse::class.java)
            } catch (e: Exception) {
                FoodAnalysisResponse(status = "ok", answer = bodyString)
            }
            
            val food = parseFoodAnalysisResponse(response, FoodSource.AI_PHOTO_ANALYSIS)
            
            // Удаляем временный сжатый файл если он был создан
            if (imageFile.name.startsWith("compressed_")) {
                imageFile.delete()
            }
            
            Result.success(food)
            
        } catch (e: Exception) {
            println("ERROR: Base64 fallback failed: ${e.message}")
            Result.error(DomainException.AIAnalysisException("Analysis failed: ${e.message}", e))
        }
    }
    
    /**
     * Проверка DNS и получение IP адреса для Make.com
     * Возвращает IP адрес или null если DNS заблокирован
     */
    private suspend fun resolveMakeComIP(): String? {
        return try {
            withContext(Dispatchers.IO) {
                val host = "hook.us2.make.com"
                println("DEBUG: Resolving DNS for $host")
                
                // Сначала пробуем обычный DNS
                try {
                    val addresses = InetAddress.getAllByName(host)
                    if (addresses.isNotEmpty()) {
                        val ip = addresses[0].hostAddress
                        println("DEBUG: Resolved $host to IP via standard DNS: $ip")
                        return@withContext ip
                    }
                } catch (e: Exception) {
                    println("DEBUG: Standard DNS failed, trying DNS-over-HTTPS")
                }
                
                // Если обычный DNS не работает, пробуем DNS-over-HTTPS
                val dohIP = resolveDNSOverHTTPS(host)
                if (dohIP != null) {
                    println("DEBUG: Resolved $host to IP via DoH: $dohIP")
                    return@withContext dohIP
                }
                
                println("ERROR: No IP addresses found for $host")
                null
            }
        } catch (e: Exception) {
            println("ERROR: DNS resolution failed: ${e.message}")
            println("ERROR: This might indicate DNS blocking by ISP")
            null
        }
    }
    
    /**
     * DNS-over-HTTPS для обхода DNS блокировки
     * Использует Cloudflare DNS
     */
    private suspend fun resolveDNSOverHTTPS(hostname: String): String? {
        return try {
            withContext(Dispatchers.IO) {
                val url = "https://cloudflare-dns.com/dns-query?name=$hostname&type=A"
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/dns-json")
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    val answers = json.optJSONArray("Answer")
                    if (answers != null && answers.length() > 0) {
                        for (i in 0 until answers.length()) {
                            val answer = answers.getJSONObject(i)
                            if (answer.optInt("type") == 1) { // Type A record
                                return@withContext answer.getString("data")
                            }
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            println("ERROR: DNS-over-HTTPS failed: ${e.message}")
            null
        }
    }
    
    /**
     * Создание URL с использованием IP адреса вместо домена
     * Помогает обойти DNS блокировку
     */
    private fun createUrlWithIP(ip: String): String {
        return "https://$ip/${MakeService.WEBHOOK_ID}"
    }
    
    /**
     * Компрессия изображения для уменьшения размера перед отправкой
     * Это может помочь с проблемами передачи данных в мобильных сетях
     */
    private fun compressImage(imagePath: String, maxSizeKB: Int = 500): File? {
        return try {
            val originalFile = File(imagePath)
            val originalSizeKB = originalFile.length() / 1024
            
            // Если файл уже меньше максимального размера, возвращаем оригинал
            if (originalSizeKB <= maxSizeKB) {
                println("DEBUG: Image size ${originalSizeKB}KB is already under limit")
                return originalFile
            }
            
            println("DEBUG: Compressing image from ${originalSizeKB}KB to max ${maxSizeKB}KB")
            
            // Декодируем изображение
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(imagePath, options)
            
            // Вычисляем коэффициент сжатия
            var scale = 1
            while ((options.outWidth / scale / 2) >= 1024 && 
                   (options.outHeight / scale / 2) >= 1024) {
                scale *= 2
            }
            
            // Декодируем с уменьшенным разрешением
            val decodeOptions = BitmapFactory.Options()
            decodeOptions.inSampleSize = scale
            val bitmap = BitmapFactory.decodeFile(imagePath, decodeOptions)
            
            // Создаем временный файл для сжатого изображения
            val compressedFile = File(originalFile.parent, "compressed_${originalFile.name}")
            val outputStream = FileOutputStream(compressedFile)
            
            // Начинаем с качества 90% и уменьшаем пока не достигнем нужного размера
            var quality = 90
            do {
                outputStream.close()
                val os = FileOutputStream(compressedFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, os)
                os.close()
                quality -= 10
            } while (compressedFile.length() / 1024 > maxSizeKB && quality > 30)
            
            bitmap.recycle()
            
            val newSizeKB = compressedFile.length() / 1024
            println("DEBUG: Image compressed to ${newSizeKB}KB (quality: ${quality + 10}%)")
            
            compressedFile
        } catch (e: Exception) {
            println("ERROR: Failed to compress image: ${e.message}")
            null
        }
    }
}