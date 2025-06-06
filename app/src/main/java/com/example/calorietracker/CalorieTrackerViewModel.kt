package com.example.calorietracker

import android.content.Context
import android.graphics.Bitmap
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
import kotlinx.coroutines.launch
import kotlin.math.round
import android.util.Log

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
    val weight: Int
)

enum class MealType(val displayName: String) {
    BREAKFAST("Завтрак"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    SNACK("Полдник"),
    LATE_BREAKFAST("Ланч"),
    SUPPER("Перекус")
}

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
    var messages by mutableStateOf(listOf(
        ChatMessage(MessageType.AI, "Привет! Я ваш персональный AI-диетолог. Готов помочь с анализом питания и дать советы по здоровому образу жизни.")
    ))
    var inputMessage by mutableStateOf("")
    var pendingFood by mutableStateOf<FoodItem?>(null)
    var selectedMeal by mutableStateOf(MealType.BREAKFAST)
    var isAnalyzing by mutableStateOf(false)

    // AI и сетевые состояния
    private val foodAnalyzer = FoodAnalyzer()
    var isOnline by mutableStateOf(false)
    var showManualInputDialog by mutableStateOf(false)

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
    private var prevOnlineStatus: Boolean? = null

    fun checkInternetConnection() {
        viewModelScope.launch {
            val currentOnline = NetworkUtils.isInternetAvailable(context)
            prevOnlineStatus = currentOnline
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
                        val userId = getOrCreateUserId(context)  // <-- твоя функция из utils
                        Log.d("CalorieTracker", "userId: $userId, Профиль для AI: $profileData")

                        val response = safeApiCall {
                            NetworkModule.makeService.askAiDietitian(
                                webhookId = "653st2c10rmg92nlltf3y0m8sggxaac6",
                                request = AiChatRequest(
                                    message = userMessage,
                                    userProfile = profileData,
                                    userId = userId // добавь поле в AiChatRequest (если его ещё нет)
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
    // ----------

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

    // Анализ фото (оставлено без изменений — если используешь свой AI)
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
        // Вызов анализатора (оставь свою реализацию)
        // ...
        isAnalyzing = false
    }

    // Старый метод для совместимости
    suspend fun analyzePhoto(bitmap: Bitmap) {
        analyzePhotoWithAI(bitmap)
    }

    // Остальное без изменений (ручной ввод еды, подтверждение, советы и т.д.)
    // ...

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
            weight = weight.toIntOrNull() ?: 100
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