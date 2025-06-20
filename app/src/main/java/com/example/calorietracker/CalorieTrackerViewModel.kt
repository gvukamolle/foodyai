package com.example.calorietracker

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.analyzer.FoodAnalyzer
import com.example.calorietracker.data.DailyIntake
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.UserProfile
import com.example.calorietracker.network.*
import com.example.calorietracker.utils.NetworkUtils
import com.example.calorietracker.utils.calculateAge
import com.example.calorietracker.utils.getOrCreateUserId
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import kotlin.math.round
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.calorietracker.network.FoodDataFromAnswer
import android.widget.Toast

data class ChatMessage(
    val type: MessageType,
    val content: String
)

enum class MessageType {
    USER, AI
}

data class FoodItem(
    val name: String,
    val calories: Int,
    val proteins: Int,
    val fats: Int,
    val carbs: Int,
    val weight: String
)

enum class MealType(val displayName: String) {
    BREAKFAST("Завтрак"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    SNACK("Полдник"),
    LATE_BREAKFAST("Ланч"),
    SUPPER("Перекус")
}

// Исправленная структура CalorieTrackerViewModel.kt

class CalorieTrackerViewModel(
    private val repository: DataRepository,
    private val context: Context
) : ViewModel() {
    val userId = getOrCreateUserId(context)

    // UI состояния
    var currentStep by mutableStateOf("setup")
    var showSettings by mutableStateOf(false)
    var userProfile by mutableStateOf(UserProfile())
    var dailyIntake by mutableStateOf(DailyIntake())
    var messages by mutableStateOf(
        listOf(
            ChatMessage(
                MessageType.AI,
                "Привет! Я ваш персональный AI-диетолог. Готов помочь с анализом питания и дать советы по здоровому образу жизни."
            )
        )
    )
    var inputMessage by mutableStateOf("")
    var pendingFood by mutableStateOf<FoodItem?>(null)
    var prefillFood by mutableStateOf<FoodItem?>(null)
    var selectedMeal by mutableStateOf(MealType.BREAKFAST)
    var isAnalyzing by mutableStateOf(false)

    // AI и сетевые состояния
    private val foodAnalyzer = FoodAnalyzer()
    var isOnline by mutableStateOf(false)
    var showManualInputDialog by mutableStateOf(false)
    var showPhotoDialog by mutableStateOf(false)

    init {
        loadUserData()
        checkInternetConnection()
    }

    private fun loadUserData() {
        repository.getUserProfile()?.let { profile ->
            userProfile = profile
            currentStep = "main"
        }
        dailyIntake = repository.getDailyIntake()
    }

    private fun saveUserData() {
        repository.saveUserProfile(userProfile)
        repository.saveDailyIntake(dailyIntake)
    }

    // Проверка подключения к интернету
    fun checkInternetConnection() {
        viewModelScope.launch {
            val currentOnline = NetworkUtils.isInternetAvailable(context)
            isOnline = currentOnline
        }
    }

    // Расчет дневных потребностей
    private fun calculateDailyNeeds(profile: UserProfile): UserProfile {
        val age = calculateAge(profile.birthday)
        val bmr = if (profile.gender == "male") {
            88.362 + (13.397 * profile.weight) + (4.799 * profile.height) - (5.677 * age)
        } else {
            447.593 + (9.247 * profile.weight) + (3.098 * profile.height) - (4.330 * age)
        }

        val activityFactor = when (profile.condition) {
            "sedentary" -> 1.2
            "active" -> 1.5
            "very-active" -> 1.75
            else -> 1.2
        }

        var calories = bmr * activityFactor

        when (profile.goal) {
            "lose" -> calories *= 0.8
            "gain" -> calories *= 1.2
        }

        return profile.copy(
            dailyCalories = round(calories).toInt(),
            dailyProteins = round(profile.weight * 1.6).toInt(),
            dailyFats = round(calories * 0.25 / 9).toInt(),
            dailyCarbs = round((calories * 0.45) / 4).toInt()
        )
    }

    fun getProgressColor(current: Int, target: Int): androidx.compose.ui.graphics.Color {
        val percentage = (current.toFloat() / target.toFloat()) * 100
        return when {
            percentage < 80 -> androidx.compose.ui.graphics.Color(0xFFFF9292)
            percentage < 95 -> androidx.compose.ui.graphics.Color(0xFFFFE08A)
            percentage <= 110 -> androidx.compose.ui.graphics.Color(0xFF82FFAE)
            else -> androidx.compose.ui.graphics.Color(0xFFFF9292)
        }
    }

    fun handleSetupSubmit() {
        userProfile = calculateDailyNeeds(userProfile)
        currentStep = "main"
        saveUserData()
    }

    // Преобразование UserProfile в UserProfileData для сети
    private fun UserProfile.toNetworkProfile(): UserProfileData {
        val age = calculateAge(birthday)
        return UserProfileData(
            age = age,
            weight = weight,
            height = height,
            gender = gender,
            activityLevel = condition,
            goal = goal
        )
    }

    // Вспомогательная функция для масштабирования изображения
    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap

        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val newHeight = (maxWidth * aspectRatio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }

    // Анализ фото с AI
    suspend fun analyzePhotoWithAI(bitmap: Bitmap) {
        isAnalyzing = true
        messages = messages + ChatMessage(MessageType.USER, "Фото загружено")

        checkInternetConnection()
        if (!isOnline) {
            messages = messages + ChatMessage(
                MessageType.AI,
                "Нет подключения к интернету. Пожалуйста, введите данные о продукте вручную."
            )
            isAnalyzing = false
            showManualInputDialog = true
            return
        }

        messages = messages + ChatMessage(
            MessageType.AI,
            "Анализирую фото..."
        )

        try {
            // Создаем временный файл
            val tempFile = File.createTempFile("photo", ".jpg", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)

            // Сжимаем и сохраняем
            val scaledBitmap = scaleBitmap(bitmap, 800)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.close()

            // Подготавливаем данные
            val profileData = userProfile.toNetworkProfile()

            // Создаем multipart parts
            val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val photoPart = MultipartBody.Part.createFormData("photo", tempFile.name, requestBody)

            // Создаем JSON для userProfile
            val gson = Gson()
            val profileJson = gson.toJson(profileData)
            val profileRequestBody = profileJson.toRequestBody("application/json".toMediaTypeOrNull())

            // userId как текст
            val userIdRequestBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())

            // Отправляем запрос
            val response = safeApiCall {
                NetworkModule.makeService.analyzeFoodPhoto(
                    webhookId = "653st2c10rmg92nlltf3y0m8sggxaac6",
                    photo = photoPart,
                    userProfile = profileRequestBody,
                    userId = userIdRequestBody
                )
            }

            // Удаляем временный файл
            tempFile.delete()

            if (response.isSuccess) {
                val result = response.getOrNull()

                if (result?.answer != null) {
                    try {
                        Log.i("FoodParseDebug", "answer: " + result.answer)
                        val foodData = gson.fromJson(result.answer, FoodDataFromAnswer::class.java)
                        val flag = foodData.food.trim().lowercase()

                        // Проверяем, обнаружена ли еда
                        if (flag == "нет" || flag == "no") {
                            messages = messages + ChatMessage(
                                MessageType.AI,
                                "❌ На фото не обнаружено еды. Попробуйте сделать другое фото или введите данные вручную."
                            )
                            Toast.makeText(
                                context,
                                "На фото не обнаружено еды",
                                Toast.LENGTH_LONG
                            ).show()
                            // Предлагаем переснять
                            showPhotoDialog = true
                        } else if (flag == "да" || flag == "yes") {
                            Log.d("PolCheck", "AI нашёл еду: ${foodData.name}, открываю диалог")
                            // Если еда обнаружена, заполняем данные
                            messages = messages + ChatMessage(
                                MessageType.AI,
                                "✅ Распознан продукт: ${foodData.name}"
                            )
                            // ВАЖНО: Устанавливаем prefillFood перед открытием диалога
                            prefillFood = FoodItem(
                                name = foodData.name,
                                calories = foodData.calories.toIntOrNull() ?: 0,
                                proteins = foodData.proteins.toIntOrNull() ?: 0,
                                fats = foodData.fats.toIntOrNull() ?: 0,
                                carbs = foodData.carbs.toIntOrNull() ?: 0,
                                weight = foodData.weight
                            )
                            showManualInputDialog = true
                        } else {
                            // Если ответ непонятный
                            messages = messages + ChatMessage(
                                MessageType.AI,
                                "Не удалось определить тип продукта. Введите данные вручную."
                            )
                            showManualInputDialog = true
                        }
                    } catch (e: Exception) {
                        Log.e("CalorieTracker", "Ошибка парсинга JSON", e)
                        messages = messages + ChatMessage(
                            MessageType.AI,
                            "Не удалось обработать ответ. Введите данные вручную."
                        )
                        showManualInputDialog = true
                    }
                } else {
                    messages = messages + ChatMessage(
                        MessageType.AI,
                        "Сервер не вернул данные. Попробуйте еще раз."
                    )
                }
            } else {
                Log.e("CalorieTracker", "Ошибка API", response.exceptionOrNull())
                messages = messages + ChatMessage(
                    MessageType.AI,
                    "Ошибка соединения. Проверьте интернет и попробуйте снова."
                )
                showManualInputDialog = true
            }
        } catch (e: Exception) {
            Log.e("CalorieTracker", "Общая ошибка", e)
            messages = messages + ChatMessage(
                MessageType.AI,
                "Произошла ошибка. Введите данные вручную."
            )
            showManualInputDialog = true
        } finally {
            isAnalyzing = false
        }
    }

    // Обработка ручного ввода продукта
    fun handleManualInput(
        name: String,
        calories: String,
        proteins: String,
        fats: String,
        carbs: String,
        weight: String
    ) {
        pendingFood = FoodItem(
            name = name,
            calories = calories.toIntOrNull() ?: 0,
            proteins = proteins.toIntOrNull() ?: 0,
            fats = fats.toIntOrNull() ?: 0,
            carbs = carbs.toIntOrNull() ?: 0,
            weight = (weight.toIntOrNull() ?: 100).toString()
        )

        messages = messages + ChatMessage(
            MessageType.USER,
            "Добавлен продукт: $name"
        )

        messages = messages + ChatMessage(
            MessageType.AI,
            "Записал данные о продукте. Выберите прием пищи и подтвердите."
        )

        showManualInputDialog = false
    }

    // Подтверждение добавления продукта
    fun confirmFood() {
        pendingFood?.let { food ->
            dailyIntake = dailyIntake.copy(
                calories = dailyIntake.calories + food.calories,
                proteins = dailyIntake.proteins + food.proteins,
                fats = dailyIntake.fats + food.fats,
                carbs = dailyIntake.carbs + food.carbs
            )

            val aiStatus = if (isOnline) "с помощью AI" else "вручную"
            messages = messages + ChatMessage(
                MessageType.AI,
                "Отлично! Записал ${food.name} в ${selectedMeal.displayName.lowercase()} ($aiStatus). " +
                        generateNutritionalAdvice(food)
            )

            pendingFood = null
            saveUserData()
        }
    }

    // Генерация советов по питанию
    private fun generateNutritionalAdvice(food: FoodItem): String {
        val caloriePercent = (food.calories.toFloat() / userProfile.dailyCalories * 100).toInt()
        val remainingCalories = userProfile.dailyCalories - dailyIntake.calories

        return when {
            caloriePercent > 30 -> "Это ${caloriePercent}% от дневной нормы. Планируйте остальные приемы пищи с учетом этого."
            remainingCalories < 200 -> "У вас осталось всего $remainingCalories ккал на день. Выбирайте легкие продукты."
            food.proteins > food.carbs -> "Отличный источник белка! Это поможет в достижении ваших целей."
            food.carbs > 50 -> "Много углеводов - отлично для энергии. Не забудьте про физическую активность!"
            else -> "Хороший выбор! Продолжайте в том же духе."
        }
    }

    // Отправка сообщения в чат
    fun sendMessage() {
        if (inputMessage.isNotBlank()) {
            val userMessage = inputMessage
            Log.d("CalorieTracker", "sendMessage вызван. Сообщение: $userMessage")
            messages = messages + ChatMessage(MessageType.USER, userMessage)
            inputMessage = ""

            viewModelScope.launch {
                if (isOnline) {
                    messages = messages + ChatMessage(
                        MessageType.AI,
                        "Обрабатываю ваш вопрос..."
                    )
                    try {
                        val profileData = userProfile.toNetworkProfile()
                        val userId = getOrCreateUserId(context)
                        Log.d("CalorieTracker", "userId: $userId, Профиль для AI: $profileData")

                        val response = safeApiCall {
                            NetworkModule.makeService.askAiDietitian(
                                webhookId = "653st2c10rmg92nlltf3y0m8sggxaac6",
                                request = AiChatRequest(
                                    message = userMessage,
                                    userProfile = profileData,
                                    userId = userId
                                )
                            )
                        }
                        Log.d("CalorieTracker", "Ответ от Make: $response")
                        if (response.isSuccess) {
                            val answer = response.getOrNull()?.answer ?: "Ответ от AI не получен."
                            messages = messages + ChatMessage(MessageType.AI, answer)
                        } else {
                            messages = messages + ChatMessage(
                                MessageType.AI,
                                "Ошибка сервера: не удалось получить ответ от AI."
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("CalorieTracker", "Ошибка при обращении к Make", e)
                        messages = messages + ChatMessage(
                            MessageType.AI,
                            "Произошла ошибка при обращении к AI: ${e.message}"
                        )
                    }
                } else {
                    // Офлайн ответы
                    val offlineResponse = getOfflineResponse(userMessage)
                    messages = messages + ChatMessage(MessageType.AI, offlineResponse)
                }
            }
        }
    }

    // Генерация офлайн ответов
    private fun getOfflineResponse(question: String): String {
        return when {
            question.contains("калори", ignoreCase = true) ->
                "Ваша дневная норма: ${userProfile.dailyCalories} ккал. Сегодня вы употребили ${dailyIntake.calories} ккал. " +
                        "Осталось: ${userProfile.dailyCalories - dailyIntake.calories} ккал."

            question.contains("белк", ignoreCase = true) ->
                "Норма белка: ${userProfile.dailyProteins}г в день. Сегодня употреблено: ${dailyIntake.proteins}г. " +
                        "Хорошие источники белка: куриная грудка (31г/100г), творог (18г/100г), яйца (13г/100г)."

            question.contains("вод", ignoreCase = true) ->
                "Рекомендуется выпивать ${userProfile.weight * 30}мл воды в день. " +
                        "Это примерно ${userProfile.weight * 30 / 250} стаканов."

            question.contains("похуде", ignoreCase = true) ->
                "Для похудения важен дефицит калорий. Ваша норма для похудения: ${userProfile.dailyCalories} ккал. " +
                        "Добавьте физическую активность и следите за размером порций."

            question.contains("завтрак", ignoreCase = true) ->
                "Идеальный завтрак должен содержать 25-30% дневной нормы калорий (${userProfile.dailyCalories * 0.25}-${userProfile.dailyCalories * 0.3} ккал). " +
                        "Включите белки, сложные углеводы и полезные жиры."

            else ->
                "Для полноценных консультаций AI необходимо подключение к интернету. " +
                        "Сейчас могу помочь с базовыми вопросами о калориях, белках, воде и похудении."
        }
    }
}