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
import com.example.calorietracker.network.safeApiCall
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
import kotlinx.coroutines.withContext
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
import kotlinx.coroutines.delay

// Обновленная структура сообщения с датой
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val type: MessageType,
    val content: String,
    val imagePath: String? = null,
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

// Модель для сохранения анализа дня
data class DailyAnalysis(
    val date: String,
    val result: String,
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
    var currentDate by mutableStateOf(java.time.LocalDate.now())
    var pendingAIAction by mutableStateOf<(() -> Unit)?>(null)
        internal set
    var currentFoodSource by mutableStateOf<String?>(null)
    private var _messages by mutableStateOf(
        listOf(
            ChatMessage(
                type = MessageType.AI,
                content = "Привет!\nГотов помочь с питанием!",
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
        }

    // Методы для работы с анализами дня
    fun getDailyAnalysis(date: String): DailyAnalysis? {
        // Проверяем, не устарел ли анализ (если это не сегодня)
        val today = LocalDate.now().toString()
        if (date != today) {
            dailyAnalysisCache.remove(date)
            return null
        }
        return dailyAnalysisCache[date]
    }

    private fun saveDailyAnalysis(date: String, result: String) {
        dailyAnalysisCache[date] = DailyAnalysis(
            date = date,
            result = result,
            timestamp = LocalDateTime.now()
        )
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
    var lastDescriptionMessage by mutableStateOf<String?>(null)
    var lastPhotoPath by mutableStateOf<String?>(null)
    var lastPhotoCaption by mutableStateOf("")
    var showAILoadingScreen by mutableStateOf(false)
        private set

    var isDailyAnalysisEnabled by mutableStateOf(false)
        private set

    // Переключение режима анализа
    fun toggleDailyAnalysis() {
        isDailyAnalysisEnabled = !isDailyAnalysisEnabled
    }

    private fun startWatchMyFood(userQuery: String = "") {
        viewModelScope.launch {
            try {
                if (userQuery.isNotBlank()) {
                    messages = messages + ChatMessage(
                        type = MessageType.USER,
                        content = userQuery,
                        animate = true
                    )
                    messages = messages + ChatMessage(
                        type = MessageType.AI,
                        content = "",
                        isProcessing = true,
                        animate = true
                    )
                }
                val mealsData = meals.flatMap { meal ->
                    meal.foods.map { food ->
                        FoodItemData(
                            name = food.name,
                            calories = food.calories,
                            protein = food.protein,
                            fat = food.fat,
                            carbs = food.carbs,
                            weight = food.weight.toIntOrNull() ?: 100
                        )
                    }
                }

                val request = WatchMyFoodRequest(
                    userId = userId,
                    date = LocalDate.now().toString(),
                    userProfile = userProfile.toNetworkProfile(),
                    targetNutrients = TargetNutrients(
                        calories = userProfile.dailyCalories,
                        proteins = userProfile.dailyProteins.toFloat(),
                        fats = userProfile.dailyFats.toFloat(),
                        carbs = userProfile.dailyCarbs.toFloat()
                    ),
                    meals = mealsData,
                    message = userQuery,
                    messageType = "watch_myfood"
                )

                val answer = sendWatchMyFoodRequest(request)
                if (userQuery.isNotBlank()) {
                    messages = messages.filter { !it.isProcessing }
                    messages = messages + ChatMessage(
                        type = MessageType.AI,
                        content = answer ?: "Ошибка сервера: не удалось получить ответ",
                        animate = true
                    )
                }
            } catch (e: Exception) {
                Log.e("CalorieTracker", "Ошибка формирования watch_myfood", e)
            }
        }
    }

    // Хранилище анализов дня
    private val dailyAnalysisCache = mutableMapOf<String, DailyAnalysis>()


    init {
        loadUserData()
        viewModelScope.launch { checkInternetConnection() }
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

                // Очищаем старые анализы
                clearOldAnalysis()
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
    suspend fun checkInternetConnection(): Boolean {
        val currentOnline = withContext(Dispatchers.IO) {
            NetworkUtils.isInternetAvailable(context)
        }
        isOnline = currentOnline
        return currentOnline
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

    fun loadDataForDate(date: java.time.LocalDate) {
        currentDate = date
        viewModelScope.launch {
            val intake = repository.getDailyIntake(date.toString())
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
    fun UserProfile.toNetworkProfile(): UserProfileData {
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

        // Сохраняем фото для отображения в чате
        val chatFile = File.createTempFile("photo_chat", ".jpg", context.cacheDir)
        FileOutputStream(chatFile).use { outputStream ->
            val scaledBitmap = scaleBitmap(bitmap, 800)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        }
        lastPhotoPath = chatFile.absolutePath
        lastPhotoCaption = caption
        // Используем тот же файл для отправки
        val tempFile = chatFile

        // Проверяем интернет
        if (!checkInternetConnection()) {
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
            // 1. Подготавливаем данные для отправки
            val gson = Gson()
            val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val photoPart = MultipartBody.Part.createFormData("photo", tempFile.name, requestBody)

            val profileJson = gson.toJson(userProfile.toNetworkProfile())
            val profileRequestBody =
                profileJson.toRequestBody("application/json".toMediaTypeOrNull())
            val userIdRequestBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())
            val captionRequestBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())
            val messageTypeRequestBody = "photo".toRequestBody("text/plain".toMediaTypeOrNull())

            // 2. Отправляем запрос
            val response = safeApiCall {
                NetworkModule.makeService.analyzeFoodPhoto(
                    webhookId = MakeService.WEBHOOK_ID,
                    photo = photoPart,
                    userProfile = profileRequestBody,
                    userId = userIdRequestBody,
                    caption = captionRequestBody,
                    messageType = messageTypeRequestBody
                )
            }

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
                        Toast.makeText(context, "На фото не обнаружено еды", Toast.LENGTH_LONG)
                            .show()
                        showPhotoDialog = false
                    }

                    "да", "yes" -> {

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
        lastDescriptionMessage = textToAnalyze

        viewModelScope.launch {
            isAnalyzing = true
            currentFoodSource = "ai_description"

            // Показываем новый экран загрузки
            showAILoadingScreen = true
            inputMethod = "text" // Указываем метод ввода

            if (!checkInternetConnection()) {
                showAILoadingScreen = false  // Скрываем экран загрузки
                handleError("Нет подключения к интернету")
                isAnalyzing = false
                return@launch
            }

            val currentUser = authManager.currentUser.value
            if (currentUser != null && !AIUsageManager.canUseAI(currentUser)) {
                showAILoadingScreen = false
                showAILimitDialog = true
                pendingAIAction = {
                    analyzeDescription()
                }
                isAnalyzing = false
                return@launch
            }

            try {
                val request = FoodAnalysisRequest(
                    weight = 100,
                    userProfile = userProfile.toNetworkProfile(),
                    message = textToAnalyze,
                    userId = userId,
                    messageType = if (isDailyAnalysisEnabled) "dayfood_analysis" else "analysis",
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
                if (currentUser != null) {
                    val updatedUserData = AIUsageManager.incrementUsage(currentUser)
                    authManager.updateUserData(updatedUserData)
                }
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

        when (inputMethod) {
            "text" -> {
                messages = messages + ChatMessage(
                    type = MessageType.USER,
                    content = lastDescriptionMessage ?: "Добавлен продукт: $name",
                    animate = true
                )
                lastDescriptionMessage = null
            }

            "photo" -> {
                messages = messages + ChatMessage(
                    type = MessageType.USER,
                    content = lastPhotoCaption,
                    imagePath = lastPhotoPath,
                    animate = true
                )
                lastPhotoPath = null
                lastPhotoCaption = ""
            }

            else -> {
                messages = messages + ChatMessage(
                    type = MessageType.USER,
                    content = "Добавлен продукт: $name",
                    animate = true
                )
            }
        }

        showManualInputDialog = false
    }

// В CalorieTrackerViewModel.kt обновите метод confirmFood():

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

            // Добавляем сообщение о записи еды
            messages = messages + ChatMessage(
                type = MessageType.AI,
                content = "Записал! Сегодня на ${selectedMeal.displayName.lowercase()} у нас **${food.name}**",
                foodItem = null,
                isExpandable = false
            )

            // Если есть мнение AI, добавляем отдельное сообщение с кнопкой с небольшой задержкой
            if (food.aiOpinion != null) {
                viewModelScope.launch {
                    delay(500) // Небольшая задержка для эффекта
                    messages = messages + ChatMessage(
                        type = MessageType.AI,
                        content = "",  // Пустой контент, так как будет только кнопка
                        foodItem = food,
                        isExpandable = true,
                        animate = true
                    )
                }
            }

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
            inputMethod = null
            lastDescriptionMessage = null
            lastPhotoPath = null
            lastPhotoCaption = ""
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
                Log.d(
                    "CalorieTracker",
                    "Еда успешно отправлена на сервер (источник: ${currentFoodSource})"
                )
            } else {
                Log.e("CalorieTracker", "Ошибка отправки еды на сервер", response.exceptionOrNull())
            }

            // Сбрасываем источник после отправки
            currentFoodSource = null

        } catch (e: Exception) {
            Log.e("CalorieTracker", "Ошибка при отправке данных о еде", e)
        }
    }

    // 3. Обновить метод sendMessage для использования анимированных точек
    fun sendMessage() {
        val message = inputMessage.trim()

        if (message.isBlank()) return

        // Проверяем, является ли это запросом анализа
        if (message.startsWith("[АНАЛИЗ]")) {
            val query = message.removePrefix("[АНАЛИЗ]").trim()
            startWatchMyFood(query)
            inputMessage = ""
            return
        }

        if (inputMessage.isNotBlank()) {
            val userMessage = inputMessage
            val isFirstOfDay = messages.none { it.type == MessageType.USER }
            messages = messages + ChatMessage(
                type = MessageType.USER,
                content = userMessage,
                animate = true
            )
            inputMessage = ""

            viewModelScope.launch {
                if (isOnline) {
                    val currentUser = authManager.currentUser.value
                    if (currentUser != null && !AIUsageManager.canUseAI(currentUser)) {
                        showAILimitDialog = true
                        pendingAIAction = {
                            inputMessage = userMessage
                            sendMessage()
                        }
                        return@launch
                    }

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
                                    isFirstMessageOfDay = isFirstOfDay,
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
                            if (currentUser != null) {
                                val updatedUserData = AIUsageManager.incrementUsage(currentUser)
                                authManager.updateUserData(updatedUserData)
                            }
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

    // Методы для экрана аналитики
    fun getTodayData(): com.example.calorietracker.data.DayData? {
        val today = LocalDate.now()
        return if (dailyCalories > 0 || dailyProtein > 0 || dailyCarbs > 0 || dailyFat > 0) {
            com.example.calorietracker.data.DayData(
                date = today,
                calories = dailyCalories.toFloat(),
                proteins = dailyProtein,
                fats = dailyFat,
                carbs = dailyCarbs,
                mealsCount = meals.size
            )
        } else {
            null
        }
    }

    fun getDayData(date: LocalDate): com.example.calorietracker.data.DayData? {
        val dateString = date.toString()
        val intake = repository.getIntakeHistory(dateString)
        return if (intake != null && intake.calories > 0) {
            com.example.calorietracker.data.DayData(
                date = date,
                calories = intake.calories.toFloat(),
                proteins = intake.protein,
                fats = intake.fat,
                carbs = intake.carbs,
                mealsCount = intake.meals.size
            )
        } else {
            null
        }
    }

    fun getAllDaysData(): List<com.example.calorietracker.data.DayData> {
        val allDates = repository.getAvailableDates()
        return allDates.mapNotNull { dateString ->
            try {
                val date = LocalDate.parse(dateString)
                getDayData(date)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun sendAnalysisRequest(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            try {
                // Добавляем сообщение пользователя с меткой анализа
                val userMessage = ChatMessage(
                    type = MessageType.USER,
                    content = "📊 Анализ: $query",
                    animate = true
                )
                messages = messages + userMessage

                // Очищаем поле ввода
                inputMessage = ""

                // Показываем индикатор обработки
                val processingMessage = ChatMessage(
                    type = MessageType.AI,
                    content = "",
                    isProcessing = true,
                    animate = true
                )
                messages = messages + processingMessage

                // Собираем данные за сегодня для анализа
                val todayMeals = meals.toList()
                val mealsData = todayMeals.flatMap { meal ->
                    meal.foods.map { food ->
                        FoodItemData(
                            name = food.name,
                            calories = food.calories,
                            protein = food.protein,
                            fat = food.fat,
                            carbs = food.carbs,
                            weight = food.weight.toIntOrNull() ?: 0
                        )
                    }
                }

                // Считаем общие показатели
                val totalCalories = todayMeals.sumOf { meal ->
                    meal.foods.sumOf { it.calories }
                }
                val totalProtein = todayMeals.sumOf { meal ->
                    meal.foods.sumOf { it.protein }
                }
                val totalFat = todayMeals.sumOf { meal ->
                    meal.foods.sumOf { it.fat }
                }
                val totalCarbs = todayMeals.sumOf { meal ->
                    meal.foods.sumOf { it.carbs }
                }

                // Получаем цели пользователя
                val profile = repository.getUserProfile()
                val dailyCalorieGoal = profile?.dailyCalories ?: 2000
                val proteinGoal = profile?.dailyProteins ?: 50
                val fatGoal = profile?.dailyFats ?: 65
                val carbsGoal = profile?.dailyCarbs ?: 250

                // Формируем контекст для анализа
                val analysisContext = buildString {
                    appendLine("Запрос пользователя: $query")
                    appendLine()
                    appendLine("Данные о питании за сегодня:")
                    appendLine("- Калории: $totalCalories из $dailyCalorieGoal ккал")
                    appendLine("- Белки: ${totalProtein.roundToInt()}г из ${proteinGoal}г")
                    appendLine("- Жиры: ${totalFat.roundToInt()}г из ${fatGoal}г")
                    appendLine("- Углеводы: ${totalCarbs.roundToInt()}г из ${carbsGoal}г")
                    appendLine()
                    appendLine("Приемы пищи:")
                    todayMeals.forEach { meal ->
                        appendLine()
                        appendLine("${meal.type.displayName} (${meal.time}):")
                        meal.foods.forEach { food ->
                            appendLine("- ${food.name}: ${food.calories} ккал, Б:${food.protein}г, Ж:${food.fat}г, У:${food.carbs}г")
                        }
                    }
                }

                // Создаем запрос к API для анализа
                val analysisRequest = DailyAnalysisRequest(
                    userId = userId,
                    date = LocalDate.now().toString(),
                    userProfile = profile?.toNetworkProfile() ?: UserProfile().toNetworkProfile(),
                    targetNutrients = TargetNutrients(
                        calories = dailyCalorieGoal,
                        proteins = proteinGoal.toFloat(),
                        fats = fatGoal.toFloat(),
                        carbs = carbsGoal.toFloat()
                    ),
                    meals = mealsData
                )

                // Отправляем запрос
                val response = safeApiCall {
                    NetworkModule.makeService.analyzeDailyIntake(
                        webhookId = MakeService.WEBHOOK_ID,
                        request = analysisRequest
                    )
                }

                // Удаляем сообщение обработки
                messages = messages.filter { !it.isProcessing }

                // Добавляем ответ AI
                response.onSuccess { analysisResponse ->
                    val aiMessage = ChatMessage(
                        type = MessageType.AI,
                        content = analysisResponse.answer ?: "Ответ не получен",
                        animate = true
                    )
                    messages = messages + aiMessage
                }.onFailure { error ->
                    val errorMessage = ChatMessage(
                        type = MessageType.AI,
                        content = "Извините, не удалось выполнить анализ. Попробуйте позже.",
                        animate = true
                    )
                    messages = messages + errorMessage
                }

            } catch (e: Exception) {
                // Удаляем сообщение обработки в случае ошибки
                messages = messages.filter { !it.isProcessing }

                val errorMessage = ChatMessage(
                    type = MessageType.AI,
                    content = "Произошла ошибка при анализе. Попробуйте еще раз.",
                    animate = true
                )
                messages = messages + errorMessage
            }
        }
    }
    
    // Метод для отправки запроса на анализ дня
    suspend fun sendDailyAnalysisRequest(request: DailyAnalysisRequest): String? {
        return try {
            if (!checkInternetConnection()) {
                return null
            }
            
            // Проверяем лимиты AI
            val currentUser = authManager.currentUser.value
            if (currentUser != null && !AIUsageManager.canUseAI(currentUser)) {
                showAILimitDialog = true
                return null
            }
            
            val response = safeApiCall {
                NetworkModule.makeService.analyzeDailyIntake(
                    webhookId = MakeService.WEBHOOK_ID,
                    request = request
                )
            }
            
            if (response.isSuccess) {
                // Увеличиваем счетчик использования AI
                if (currentUser != null) {
                    val updatedUserData = AIUsageManager.incrementUsage(currentUser)
                    authManager.updateUserData(updatedUserData)
                }
                
                val answer = response.getOrNull()?.answer
                // Сохраняем анализ в кэш
                if (answer != null) {
                    saveDailyAnalysis(request.date, answer)
                }
                answer
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CalorieTracker", "Ошибка при анализе дня", e)
            null
        }
    }

    // Метод для отправки запроса "watch my food" на сервер
    suspend fun sendWatchMyFoodRequest(request: WatchMyFoodRequest): String? {
        val updatedRequest = request.copy(messageType = "watch_myfood")
        return try {
            if (!checkInternetConnection()) {
                return null
            }

            val currentUser = authManager.currentUser.value
            if (currentUser != null && !AIUsageManager.canUseAI(currentUser)) {
                showAILimitDialog = true
                return null
            }

            val response = safeApiCall {
                NetworkModule.makeService.watchMyFood(
                    webhookId = MakeService.WEBHOOK_ID,
                    request = updatedRequest
                )
            }

            if (response.isSuccess) {
                if (currentUser != null) {
                    val updatedUserData = AIUsageManager.incrementUsage(currentUser)
                    authManager.updateUserData(updatedUserData)
                }

                val answer = response.getOrNull()?.answer
                if (answer != null) {
                    saveDailyAnalysis(updatedRequest.date, answer)
                }
                answer
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("CalorieTracker", "Ошибка при отправке watch_myfood", e)
            null
        }
    }

    // Очищаем анализ при смене дня
    private fun clearOldAnalysis() {
        val today = LocalDate.now().toString()
        dailyAnalysisCache.keys.removeAll { it != today }
    }
}