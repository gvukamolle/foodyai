package com.example.calorietracker

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.auth.UserData
import com.example.calorietracker.data.DailyIntake
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.example.calorietracker.network.LogFoodRequest
import com.example.calorietracker.network.FoodItemData
import com.example.calorietracker.utils.DailyResetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.calorietracker.data.DailyNutritionSummary
import com.example.calorietracker.utils.NutritionFormatter
import kotlin.math.roundToInt
import com.example.calorietracker.utils.AIUsageManager
import java.util.UUID
import java.time.LocalTime


// Обновленная структура сообщения с датой
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val type: MessageType,
    val content: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val foodItem: FoodItem? = null, // Добавляем информацию о продукте
    val isExpandable: Boolean = false, // Флаг для раскрывающихся сообщений
    val isWelcome: Boolean = false,
    val animate: Boolean = true,
    val isProcessing: Boolean = false, // Новое поле для индикации обработки
    val isVisible: Boolean = true // Для анимации удаления сообщений
)

enum class MessageType {
    USER, AI
}

data class FoodItem(
    val name: String,
    val calories: Int,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val weight: String,
    val source: String = "manual",
    val aiOpinion: String? = null
)

data class Meal(
    val type: MealType,
    val foods: List<FoodItem>,
    val time: Long = System.currentTimeMillis()
)

enum class MealType(val displayName: String) {
    BREAKFAST("Завтрак"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    SUPPER("Перекус")
}

fun getFoodHistoryByDate(date: String): List<FoodHistoryItem> {
    // Этот метод будет вызываться при запросах типа "что я ел вчера"
    // Данные можно получить с сервера или из локальной БД
    return emptyList() // TODO: Implement when server API is ready
}

data class FoodHistoryItem(
    val foodName: String,
    val calories: Int,
    val mealType: String,
    val timestamp: LocalDateTime
)

class CalorieTrackerViewModel(
    internal val repository: DataRepository,
    private val context: Context,
    private val authManager: AuthManager
) : ViewModel() {
    val userId = getOrCreateUserId(context)

    // Поля для отслеживания дневного потребления
    var dailyCalories by mutableStateOf(0)
        private set
    var dailyProtein by mutableStateOf(0f)
        private set
    var dailyCarbs by mutableStateOf(0f)
        private set
    var dailyFat by mutableStateOf(0f)
        private set
    val formattedProtein: String
        get() = NutritionFormatter.formatMacro(dailyProtein)
    val formattedCarbs: String
        get() = NutritionFormatter.formatMacro(dailyCarbs)
    val formattedFat: String
        get() = NutritionFormatter.formatMacro(dailyFat)

    // Список приемов пищи
    var meals by mutableStateOf<List<Meal>>(emptyList())
        private set

    val dailyIntake: DailyIntake
        get() = DailyIntake(
            calories = dailyCalories,
            protein = dailyProtein,
            carbs = dailyCarbs,
            fat = dailyFat,
            meals = meals
        )

    // Состояние для календаря
    private val _calendarData = MutableStateFlow<Map<LocalDate, DailyNutritionSummary>>(emptyMap())
    val calendarData: StateFlow<Map<LocalDate, DailyNutritionSummary>> = _calendarData.asStateFlow()

    var inputMethod by mutableStateOf<String?>(null)

    // UI состояния
    var currentStep by mutableStateOf("setup")
    var showSettings by mutableStateOf(false)
    var userProfile by mutableStateOf(UserProfile())
    var displayDate by mutableStateOf(DailyResetUtils.getFormattedDisplayDate())
    var showAILimitDialog by mutableStateOf(false)
    var showSubscriptionOffer by mutableStateOf(false)
    var pendingAIAction by mutableStateOf<(() -> Unit)?>(null)
        internal set
    var currentFoodSource by mutableStateOf<String?>(null)
    private var _messages by mutableStateOf(
        listOf(
            ChatMessage(
                type = MessageType.AI,
                content = "Привет!\nГотов помочь с анализом питания и дать советы по здоровому образу жизни!",
                timestamp = LocalDateTime.now(),
                isWelcome = true,
                animate = false
            )
        )
    )
    var messages: List<ChatMessage>
        get() = _messages
        set(value) {
            _messages = value
            repository.saveChatHistory(value)
        }
    var inputMessage by mutableStateOf("")
    var pendingFood by mutableStateOf<FoodItem?>(null)
    var prefillFood by mutableStateOf<FoodItem?>(null)
    var selectedMeal by mutableStateOf(MealType.BREAKFAST)
    var isAnalyzing by mutableStateOf(false)
    val currentUser: UserData?
        get() = authManager.currentUser.value

    // AI и сетевые состояния
    var isOnline by mutableStateOf(false)
    var showManualInputDialog by mutableStateOf(false)
    var showDescriptionDialog by mutableStateOf(false)
    var showPhotoDialog by mutableStateOf(false)
    var showPhotoConfirmDialog by mutableStateOf(false)
    var pendingPhoto by mutableStateOf<Bitmap?>(null)
    var showAiOpinionDialog by mutableStateOf(false)
    var aiOpinionText by mutableStateOf<String?>(null)
    var photoCaption by mutableStateOf("")
    var pendingDescription by mutableStateOf("")
    var showAILoadingScreen by mutableStateOf(false)
        private set


    init {
        loadUserData()
        checkInternetConnection()
        // Периодическая проверка обнуления каждые 5 минут
        startPeriodicReset()

        // Загружаем данные календаря
        viewModelScope.launch {
            repository.getCalendarData().collect { summaries ->
                _calendarData.value = summaries.associateBy { it.date }
            }
        }
    }

    private fun loadUserData() {
        repository.getUserProfile()?.let { profile ->
            userProfile = profile
            currentStep = "main"
        }
        // Загружаем данные о потреблении
        val intake = repository.getDailyIntake()
        dailyCalories = intake.calories
        dailyProtein = intake.protein
        dailyCarbs = intake.carbs
        dailyFat = intake.fat
        meals = intake.meals
        // Загружаем историю чата за сегодня
        val history = repository.getChatHistory()
        if (history.isNotEmpty()) {
            messages = history
        }
        repository.cleanupOldChatHistory()
    }

    private fun startPeriodicReset() {
        viewModelScope.launch {
            while (true) {
                delay(5 * 60 * 1000) // 5 минут
                // Перезагружаем данные, что автоматически проверит обнуление
                val intake = repository.getDailyIntake()
                dailyCalories = intake.calories
                dailyProtein = intake.protein
                dailyCarbs = intake.carbs
                dailyFat = intake.fat
                meals = intake.meals
            }
        }
    }

    private fun saveUserData() {
        repository.saveUserProfile(userProfile)
        val currentIntake = DailyIntake(
            calories = dailyCalories,
            protein = dailyProtein,
            carbs = dailyCarbs,
            fat = dailyFat,
            meals = meals
        )
        repository.saveDailyIntake(currentIntake)
    }

    // Сохранение текущего дня при каждом обновлении
    private fun updateAndSaveDailyProgress() {
        viewModelScope.launch {
            repository.saveDailySummary(
                calories = dailyCalories,
                protein = dailyProtein,
                carbs = dailyCarbs,
                fat = dailyFat,
                mealsCount = meals.size
            )
        }
    }

    // Проверка подключения к интернету
    fun checkInternetConnection() {
        viewModelScope.launch {
            val currentOnline = NetworkUtils.isInternetAvailable(context)
            isOnline = currentOnline
        }
    }

    fun updateUserProfile(newProfile: UserProfile) {
        userProfile = newProfile
        handleSetupSubmit() // Эта функция у тебя уже есть, она пересчитывает КБЖУ и сохраняет профиль
    }

    fun canAnalyzeDescription(): Boolean {
        return pendingDescription.isNotBlank() && !isAnalyzing && isOnline
    }

    fun syncWithUserData(userData: UserData) {
        // Тут нужно будет дописать маппинг данных из UserData (Firebase)
        // в твой локальный UserProfile (SharedPreferences)
        // Пока сделаем простую синхронизацию
        userProfile = userProfile.copy(
            isSetupComplete = userData.isSetupComplete || userProfile.isSetupComplete
            // Тут можно добавить и другие поля, если они хранятся в Firebase
        )
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            userProfile = repository.getUserProfile() ?: UserProfile()
            val intake = repository.getDailyIntake()
            dailyCalories = intake.calories
            dailyProtein = intake.protein
            dailyCarbs = intake.carbs
            dailyFat = intake.fat
            meals = intake.meals

            val history = repository.getChatHistory()
            messages = if (history.isNotEmpty()) history else messages
            repository.cleanupOldChatHistory()
        }
    }

    // Подгружает актуальные данные за сегодняшний день
    private fun refreshTodayIntake() {
        viewModelScope.launch {
            val intake = repository.getDailyIntake()
            dailyCalories = intake.calories
            dailyProtein = intake.protein
            dailyCarbs = intake.carbs
            dailyFat = intake.fat
            meals = intake.meals
        }
    }

    // Обновляет данные календаря после изменений в истории
    private fun refreshCalendarData() {
        viewModelScope.launch {
            repository.getCalendarData().collect { summaries ->
                _calendarData.value = summaries.associateBy { it.date }
            }
        }
    }

    // Удаление приема пищи с учётом синхронизации состояния
    fun deleteMealFromHistory(date: String, index: Int) {
        repository.deleteMeal(date, index)
        refreshCalendarData()
        if (date == DailyResetUtils.getFoodDate()) {
            refreshTodayIntake()
        }
    }

    // Обновление приема пищи с учётом синхронизации состояния
    fun updateMealInHistory(date: String, index: Int, meal: Meal) {
        repository.updateMeal(date, index, meal)
        refreshCalendarData()
        if (date == DailyResetUtils.getFoodDate()) {
            refreshTodayIntake()
        }
    }


    fun updateDateAndCheckForReset() {
        // Обновляем строку с датой (например, с "25 Июня" на "26 Июня")
        displayDate = DailyResetUtils.getFormattedDisplayDate()

        // Запускаем проверку, не наступили ли 4 утра нового дня.
        // Используем viewModelScope для выполнения в фоновом потоке.
        viewModelScope.launch(Dispatchers.IO) {
            val wasReset = repository.performResetIfNeeded()
            if (wasReset) {
                // Если данные были сброшены, нужно обновить UI.
                // Загружаем "новые" (пустые) данные за сегодняшний день.
                loadInitialData() // Предполагается, что у тебя есть такая функция для загрузки данных
            }
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

    fun getProgressColor(current: Int, target: Int): Color {
        val percentage = (current.toFloat() / target.toFloat()) * 100

        // Пастельные мягкие цвета для разных зон
        return when {
            percentage < 40 -> androidx.compose.ui.graphics.Color(0xFFFF828B) // Пастельный розовый
            percentage < 80 -> androidx.compose.ui.graphics.Color(0xFFFFDB82) // Пастельный персиковый
            percentage < 100 -> androidx.compose.ui.graphics.Color(0xFF7BFF9C) // Пастельный мятный
            percentage < 110 -> androidx.compose.ui.graphics.Color(0xFF37BE2C) // Пастельный зеленый
            else -> androidx.compose.ui.graphics.Color(0xFFFF5259) // Пастельный лавандовый для переедания
        }
    }

    // Функция для плавного перехода между цветами
    private fun lerpColor(start: Color, end: Color, fraction: Float): Color {
        return androidx.compose.ui.graphics.Color(
            red = lerp(start.red, end.red, fraction),
            green = lerp(start.green, end.green, fraction),
            blue = lerp(start.blue, end.blue, fraction),
            alpha = 1f
        )
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
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

    // Обновленный метод analyzePhotoWithAI
    suspend fun analyzePhotoWithAI(bitmap: Bitmap, caption: String = "") {
        isAnalyzing = true
        currentFoodSource = "ai_photo"

        // Показываем новый экран загрузки вместо временного сообщения
        showAILoadingScreen = true
        inputMethod = "photo" // Добавить это поле в ViewModel

        messages = messages + ChatMessage(
            type = MessageType.USER,
            content = "Фото загружено",
            animate = true
        )

        // Проверяем интернет
        checkInternetConnection()
        if (!isOnline) {
            showAILoadingScreen = false  // Скрываем экран загрузки
            messages = messages + ChatMessage(
                type = MessageType.AI,
                content = "Нет подключения к интернету. Пожалуйста, введите данные о продукте вручную."
            )
            isAnalyzing = false
            showManualInputDialog = true
            return
        }

        val currentUser = authManager.currentUser.value
        if (currentUser != null && !AIUsageManager.canUseAI(currentUser)) {
            showAILoadingScreen = false  // Скрываем экран загрузки
            showAILimitDialog = true
            pendingAIAction = {
                viewModelScope.launch {
                    analyzePhotoWithAI(bitmap, caption)
                }
            }
            return
        }

        // Больше не нужно временное сообщение - у нас есть полноэкранная загрузка
        // val tempMessage = ChatMessage(...) - УДАЛЕНО

        try {
            // 1. Подготавливаем изображение
            val tempFile = File.createTempFile("photo", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { outputStream ->
                val scaledBitmap = scaleBitmap(bitmap, 800)
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }

            // 2. Подготавливаем данные для отправки
            val gson = Gson()
            val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val photoPart = MultipartBody.Part.createFormData("photo", tempFile.name, requestBody)

            val profileJson = gson.toJson(userProfile.toNetworkProfile())
            val profileRequestBody = profileJson.toRequestBody("application/json".toMediaTypeOrNull())
            val userIdRequestBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())
            val captionRequestBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())

            // 3. Отправляем запрос
            val response = safeApiCall {
                NetworkModule.makeService.analyzeFoodPhoto(
                    webhookId = MakeService.WEBHOOK_ID,
                    photo = photoPart,
                    userProfile = profileRequestBody,
                    userId = userIdRequestBody,
                    caption = captionRequestBody
                )
            }

            // Удаляем временный файл
            tempFile.delete()

            // Скрываем экран загрузки после получения ответа
            showAILoadingScreen = false

            if (response.isSuccess && currentUser != null) {
                viewModelScope.launch {
                    val updatedUserData = AIUsageManager.incrementUsage(currentUser)
                    authManager.updateUserData(updatedUserData)
                }
            }

            // 4. Обрабатываем ответ
            if (!response.isSuccess) {
                handleError("Ошибка соединения")
                return
            }

            val result = response.getOrNull()
            if (result?.answer == null) {
                handleError("Сервер не вернул данные")
                return
            }

            // 5. Парсим JSON ответ
            try {
                Log.d("CalorieTracker", "Ответ от сервера: ${result.answer}")

                // Парсим через data class
                val foodData = gson.fromJson(result.answer, FoodDataFromAnswer::class.java)

                when (foodData.food.trim().lowercase()) {
                    "нет", "no" -> {
                        // Еда не найдена
                        messages = messages + ChatMessage(
                            type = MessageType.AI,
                            content = "❌ На фото не обнаружено еды. Попробуйте сделать другое фото или введите данные вручную."
                        )
                        Toast.makeText(context, "На фото не обнаружено еды", Toast.LENGTH_LONG).show()
                        showPhotoDialog = false
                    }

                    "да", "yes" -> {
                        // Еда найдена - заполняем данные
                        messages = messages + ChatMessage(
                            type = MessageType.AI,
                            content = "✅ Распознан продукт: ${foodData.name}"
                        )

                        // Создаем FoodItem из полученных данных С МНЕНИЕМ AI
                        prefillFood = FoodItem(
                            name = foodData.name,
                            calories = foodData.calories,
                            protein = foodData.protein,
                            fat = foodData.fat,
                            carbs = foodData.carbs,
                            weight = foodData.weight,
                            source = currentFoodSource ?: "ai_photo",
                            aiOpinion = foodData.opinion
                        )

                        Log.d("CalorieTracker", "Установлен prefillFood с AI мнением: $prefillFood")
                        showManualInputDialog = true
                    }

                    else -> {
                        // Неизвестный ответ
                        handleError("Не удалось определить тип продукта")
                    }
                }
            } catch (e: Exception) {
                Log.e("CalorieTracker", "Ошибка парсинга ответа", e)
                handleError("Неверный формат ответа от сервера")
            }
        } catch (e: Exception) {
            showAILoadingScreen = false  // Скрываем экран загрузки при ошибке
            handleError("Ошибка анализа изображения: ${e.message}")
        } finally {
            showAILoadingScreen = false  // Обязательно скрываем экран загрузки
            isAnalyzing = false
        }
    }

    // Обновленный метод analyzeDescription
    fun analyzeDescription() {
        if (!canAnalyzeDescription()) return

        val textToAnalyze = pendingDescription
        pendingDescription = ""  // Очищаем после отправки

        viewModelScope.launch {
            isAnalyzing = true
            currentFoodSource = "ai_description"

            // Показываем новый экран загрузки
            showAILoadingScreen = true
            inputMethod = "text" // Указываем метод ввода

            checkInternetConnection()
            if (!isOnline) {
                showAILoadingScreen = false  // Скрываем экран загрузки
                handleError("Нет подключения к интернету")
                isAnalyzing = false
                return@launch
            }

            try {
                val request = FoodAnalysisRequest(
                    weight = 100,
                    userProfile = userProfile.toNetworkProfile(),
                    message = textToAnalyze,
                    userId = userId,
                    messageType = "analysis",
                )

                val response = safeApiCall {
                    NetworkModule.makeService.analyzeFood(
                        webhookId = MakeService.WEBHOOK_ID,
                        request = request
                    )
                }

                // Скрываем экран загрузки после получения ответа
                showAILoadingScreen = false

                if (!response.isSuccess) {
                    handleError("Ошибка соединения")
                    return@launch
                }

                val answer = response.getOrNull()?.answer
                if (answer == null) {
                    handleError("Сервер не вернул данные")
                    return@launch
                }

                val foodData = Gson().fromJson(answer, FoodDataFromAnswer::class.java)

                // Добавляем сообщение о результате
                messages = messages + ChatMessage(
                    type = MessageType.AI,
                    content = "✅ Распознан продукт: ${foodData.name}"
                )

                prefillFood = FoodItem(
                    name = foodData.name,
                    calories = foodData.calories,
                    protein = foodData.protein,
                    fat = foodData.fat,
                    carbs = foodData.carbs,
                    weight = foodData.weight,
                    source = currentFoodSource ?: "ai_description",
                    aiOpinion = foodData.opinion
                )

                showManualInputDialog = true
            } catch (e: Exception) {
                showAILoadingScreen = false  // Скрываем экран загрузки при ошибке
                Log.e("CalorieTracker", "Ошибка анализа описания", e)
                handleError("Не удалось проанализировать")
            } finally {
                showAILoadingScreen = false  // Обязательно скрываем экран загрузки
                isAnalyzing = false
            }
        }
    }

    fun cancelAIAnalysis() {
        showAILoadingScreen = false
        isAnalyzing = false
        // Можно добавить отмену корутин если используете Job
    }

    // Вспомогательная функция для обработки ошибок
    private fun handleError(errorMessage: String) {
        messages = messages + ChatMessage(
            type = MessageType.AI,
            content = "$errorMessage. Введите данные вручную."
        )
        showManualInputDialog = true
    }

    // Data class для парсинга ответа (должен быть в файле с моделями)
    data class FoodDataFromAnswer(
        val food: String,      // "да" или "нет"
        val name: String,
        val calories: Int,
        val protein: Double,
        val fat: Double,
        val carbs: Double,
        val weight: String,
        val opinion: String? = null  // AI мнение о продукте
    )

    // Обработка ручного ввода продукта
    fun handleManualInput(
        name: String,
        calories: String,
        proteins: String,
        fats: String,
        carbs: String,
        weight: String
    ) {
        if (currentFoodSource == null) {
            currentFoodSource = "manual" // Устанавливаем источник если не был установлен
        }

        val aiOpinionToSave = if (currentFoodSource != "manual" && prefillFood?.aiOpinion != null) {
            prefillFood?.aiOpinion
        } else {
            null
        }

        Log.d("CalorieTracker", "handleManualInput - aiOpinion: $aiOpinionToSave")

        pendingFood = FoodItem(
            name = name,
            calories = calories.toFloatOrNull()?.roundToInt() ?: 0,
            protein = proteins.toDoubleOrNull() ?: 0.0,
            fat = fats.toDoubleOrNull() ?: 0.0,
            carbs = carbs.toDoubleOrNull() ?: 0.0,
            weight = (weight.toIntOrNull() ?: 100).toString(),
            source = currentFoodSource ?: "manual",
            aiOpinion = aiOpinionToSave
        )

        selectedMeal = getAutoMealType()

        messages = messages + ChatMessage(
            type = MessageType.USER,
            content = "Добавлен продукт: $name",
            animate = true
        )

        showManualInputDialog = false
    }

    // Обновленный метод confirmFood без удаления несуществующего сообщения
    fun confirmFood() {
        Log.d("CalorieTracker", "confirmFood called, source: $currentFoodSource")
        pendingFood?.let { food ->
            selectedMeal = getAutoMealType()
            // Создаем новый прием пищи
            val meal = Meal(
                type = selectedMeal,
                foods = listOf(food),
                time = System.currentTimeMillis()
            )

            // Обновляем поля
            meals = meals + meal
            dailyCalories += food.calories
            dailyProtein += food.protein.toFloat()
            dailyCarbs += food.carbs.toFloat()
            dailyFat += food.fat.toFloat()

            val aiStatus = if (isOnline) "с помощью AI" else "вручную"

            // Удаляем все временные сообщения анализа
            messages = messages.filterNot {
                it.type == MessageType.AI && (
                        it.content.contains("Анализирую") ||
                                it.content.contains("Обрабатываю") ||
                                it.content.contains("анализирую") ||
                                it.content.contains("обрабатываю")
                        )
            }

            messages = messages + ChatMessage(
                type = MessageType.AI,
                content = "✅ Записал ${food.name} в ${selectedMeal.displayName.lowercase()}",
                foodItem = food,
                isExpandable = food.aiOpinion != null
            )

            // Сохраняем в репозиторий
            val updatedIntake = DailyIntake(
                calories = dailyCalories,
                protein = dailyProtein,
                carbs = dailyCarbs,
                fat = dailyFat,
                meals = meals
            )
            repository.saveDailyIntake(updatedIntake)
            updateAndSaveDailyProgress() // Сохраняем для календаря

            // Отправляем данные на сервер
            if (isOnline) {
                viewModelScope.launch {
                    sendFoodToServer(food, selectedMeal)
                }
            }

            pendingFood = null
            prefillFood = null
            currentFoodSource = null
            saveUserData()
        }
    }

    fun onPhotoSelected(bitmap: Bitmap) {
        pendingPhoto = bitmap
        photoCaption = ""
        showPhotoConfirmDialog = true
    }

    fun confirmPhoto() {
        val bitmap = pendingPhoto ?: return
        showPhotoConfirmDialog = false
        pendingPhoto = null
        viewModelScope.launch { analyzePhotoWithAI(bitmap, photoCaption) }
    }

    private fun getAutoMealType(): MealType {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 4 until 12 -> MealType.BREAKFAST
            in 12 until 18 -> MealType.LUNCH
            in 18 until 24 -> MealType.DINNER
            else -> MealType.SUPPER
        }
    }

    private suspend fun sendFoodToServer(food: FoodItem, mealType: MealType) {
        try {
            val now = LocalDateTime.now()
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            val foodItemData = FoodItemData(
                name = food.name,
                calories = food.calories,
                protein = food.protein,
                fat = food.fat,
                carbs = food.carbs,
                weight = food.weight.toIntOrNull() ?: 100
            )

            val request = LogFoodRequest(
                userId = userId,
                foodData = foodItemData,
                mealType = mealType.name,
                timestamp = System.currentTimeMillis(),
                date = now.format(dateFormatter),
                time = now.format(timeFormatter),
                source = currentFoodSource ?: "manual", // Используем сохраненный источник
                userProfile = userProfile.toNetworkProfile()
            )

            val response = safeApiCall {
                NetworkModule.makeService.logFoodToThread(
                    webhookId = MakeService.WEBHOOK_ID,
                    request = request
                )
            }

            if (response.isSuccess) {
                Log.d("CalorieTracker", "Еда успешно отправлена на сервер (источник: ${currentFoodSource})")
            } else {
                Log.e("CalorieTracker", "Ошибка отправки еды на сервер", response.exceptionOrNull())
            }

            // Сбрасываем источник после отправки
            currentFoodSource = null

        } catch (e: Exception) {
            Log.e("CalorieTracker", "Ошибка при отправке данных о еде", e)
        }
    }

    // Генерация советов по питанию
    private fun generateNutritionalAdvice(food: FoodItem): String {
        val caloriePercent = (food.calories.toFloat() / userProfile.dailyCalories * 100).toInt()
        val remainingCalories = userProfile.dailyCalories - dailyCalories

        return when {
            caloriePercent > 30 -> "Это ${caloriePercent}% от дневной нормы. Планируйте остальные приемы пищи с учетом этого."
            remainingCalories < 200 -> "У вас осталось всего $remainingCalories ккал на день. Выбирайте легкие продукты."
            food.protein > food.carbs -> "Отличный источник белка! Это поможет в достижении ваших целей."
            food.carbs > 50 -> "Много углеводов - отлично для энергии. Не забудьте про физическую активность!"
            else -> "Хороший выбор! Продолжайте в том же духе."
        }
    }

    // 3. Обновить метод sendMessage для использования анимированных точек
    fun sendMessage() {
        if (inputMessage.isNotBlank()) {
            val userMessage = inputMessage
            messages = messages + ChatMessage(
                type = MessageType.USER,
                content = userMessage,
                animate = true
            )
            inputMessage = ""

            viewModelScope.launch {
                if (isOnline) {
                    // Добавляем сообщение с анимированными точками
                    val tempMessage = ChatMessage(
                        type = MessageType.AI,
                        content = "", // Пустое содержимое
                        isProcessing = true // Флаг для отображения точек
                    )
                    messages = messages + tempMessage

                    try {
                        val profileData = userProfile.toNetworkProfile()
                        val response = safeApiCall {
                            NetworkModule.makeService.askAiDietitian(
                                webhookId = MakeService.WEBHOOK_ID,
                                request = AiChatRequest(
                                    message = userMessage,
                                    userProfile = profileData,
                                    userId = userId,
                                    messageType = "chat"
                                )
                            )
                        }

                        // Удаляем временное сообщение с анимацией
                        removeMessageWithAnimation(tempMessage.id)

                        if (response.isSuccess) {
                            val answer = response.getOrNull()?.answer ?: "Ответ от AI не получен."
                            messages = messages + ChatMessage(
                                type = MessageType.AI,
                                content = answer
                            )
                        } else {
                            messages = messages + ChatMessage(
                                type = MessageType.AI,
                                content = "Ошибка сервера: не удалось получить ответ от AI."
                            )
                        }
                    } catch (e: Exception) {
                        // Удаляем временное сообщение в случае ошибки
                        removeMessageWithAnimation(tempMessage.id)
                        messages = messages + ChatMessage(
                            type = MessageType.AI,
                            content = "Произошла ошибка при обращении к AI: ${e.message}"
                        )
                    }
                } else {
                    val offlineResponse = getOfflineResponse(userMessage)
                    messages = messages + ChatMessage(
                        type = MessageType.AI,
                        content = offlineResponse
                    )
                }
            }
        }
    }

    // Генерация офлайн ответов с поддержкой истории
    private fun getOfflineResponse(question: String): String {
        return when {
            question.contains("вчера", ignoreCase = true) -> {
                val yesterday = LocalDate.now().minusDays(1).toString()
                val yesterdayIntake = repository.getIntakeHistory(yesterday)
                if (yesterdayIntake != null && yesterdayIntake.calories > 0) {
                    "Вчера вы употребили: ${yesterdayIntake.calories} ккал, " +
                            "белки: ${NutritionFormatter.formatMacro(yesterdayIntake.protein.toFloat())}г, " +
                            "жиры: ${NutritionFormatter.formatMacro(yesterdayIntake.fat.toFloat())}г, " +
                            "углеводы: ${NutritionFormatter.formatMacro(yesterdayIntake.carbs.toFloat())}г."
                } else {
                    "У меня нет данных о вашем питании за вчера."
                }
            }

            question.contains("история", ignoreCase = true) ||
                    question.contains("статистик", ignoreCase = true) -> {
                val dates = repository.getAvailableDates().take(7)
                if (dates.isNotEmpty()) {
                    val stats = dates.map { date ->
                        val intake = repository.getIntakeHistory(date)
                        val displayDate = DailyResetUtils.getDisplayDate(date)
                        "$displayDate: ${intake?.calories ?: 0} ккал"
                    }.joinToString("\n")
                    "Ваша статистика за последние дни:\n$stats"
                } else {
                    "Пока нет сохраненной истории питания."
                }
            }

            question.contains("калори", ignoreCase = true) ->
                "Ваша дневная норма: ${userProfile.dailyCalories} ккал. Сегодня вы употребили ${dailyCalories} ккал. " +
                        "Осталось: ${userProfile.dailyCalories - dailyCalories} ккал."

            question.contains("белк", ignoreCase = true) ->
                "Норма белка: ${userProfile.dailyProteins}г в день. Сегодня употреблено: ${dailyProtein.toInt()}г. " +
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
                        "Сейчас могу помочь с базовыми вопросами о калориях, белках, воде, истории питания и похудении."
        }
    }

    fun markMessageAnimated(message: ChatMessage) {
        val index = messages.indexOfFirst { it.id == message.id }
        if (index != -1) {
            val list = messages.toMutableList()
            list[index] = list[index].copy(animate = false)
            messages = list
        }
    }

    fun removeMessageWithAnimation(messageId: String) {
        messages = messages.map { msg ->
            if (msg.id == messageId) msg.copy(isVisible = false) else msg
        }
        viewModelScope.launch {
            delay(300)
            messages = messages.filterNot { it.id == messageId }
        }
    }
}