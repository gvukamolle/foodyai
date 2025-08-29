package com.example.calorietracker.presentation.viewmodels

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.auth.UserData
import com.example.calorietracker.data.DailyIntake as DataDailyIntake
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.FoodItem
import com.example.calorietracker.data.Meal
import com.example.calorietracker.data.UserProfile
import com.example.calorietracker.data.mappers.FoodMapper
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.common.MealType
import com.example.calorietracker.domain.usecases.*
import com.example.calorietracker.managers.AppMode
import com.example.calorietracker.managers.NetworkManager
import com.example.calorietracker.managers.OfflineManager
import com.example.calorietracker.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDate

/**
 * Refactored ViewModel using Clean Architecture with Use Cases
 */
@HiltViewModel
 class CalorieTrackerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val networkManager: NetworkManager,
    private val dataRepository: DataRepository,
    private val foodMapper: FoodMapper,
    // Domain Use Cases
    private val analyzeFoodPhotoUseCase: AnalyzeFoodPhotoUseCase,
    private val analyzeFoodDescriptionUseCase: AnalyzeFoodDescriptionUseCase,
    private val saveFoodIntakeUseCase: SaveFoodIntakeUseCase,
    private val getDailyIntakeUseCase: GetDailyIntakeUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
    private val calculateNutritionTargetsUseCase: CalculateNutritionTargetsUseCase,
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val validateAIUsageLimitsUseCase: ValidateAIUsageLimitsUseCase
 ) : ViewModel() {
    
    val userId = getOrCreateUserId(context)
    
    // Менеджеры для офлайн-режима
    private val offlineManager = OfflineManager(networkManager, viewModelScope)
    
    // Публичный доступ к состоянию приложения
    val appMode: StateFlow<AppMode> = offlineManager.appMode
    
    // UI State
    private val _uiState = MutableStateFlow(CalorieTrackerUiState())
    val uiState: StateFlow<CalorieTrackerUiState> = _uiState.asStateFlow()
    
    // Current user profile (domain flow)
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfileFlow: StateFlow<User?> = _userProfile.asStateFlow()

    // Legacy-style user profile for UI (data layer model expected by pages)
    val userProfile: UserProfile
        get() = dataRepository.getUserProfile() ?: UserProfile()
    
    // Daily nutrition intake (domain)
    private val _dailyIntake = MutableStateFlow<NutritionIntake?>(null)
    val dailyIntakeFlow: StateFlow<NutritionIntake?> = _dailyIntake.asStateFlow()

    // Legacy-style daily intake for UI components that expect data model
    val dailyIntake: DataDailyIntake
        get() {
            val intake = _dailyIntake.value
            return if (intake == null) {
                DataDailyIntake()
            } else {
                DataDailyIntake(
                    calories = intake.getTotalCalories(),
                    protein = intake.getTotalProtein().toFloat(),
                    carbs = intake.getTotalCarbs().toFloat(),
                    fat = intake.getTotalFat().toFloat(),
                    meals = emptyList() // UI reads meals via viewModel.meals (domain)
                )
            }
        }
    
    // Legacy properties for backward compatibility
    val dailyCalories: Int get() = _dailyIntake.value?.getTotalCalories() ?: 0
    val dailyProtein: Float get() = _dailyIntake.value?.getTotalProtein()?.toFloat() ?: 0f
    val dailyCarbs: Float get() = _dailyIntake.value?.getTotalCarbs()?.toFloat() ?: 0f
    val dailyFat: Float get() = _dailyIntake.value?.getTotalFat()?.toFloat() ?: 0f
    
    val formattedProtein: String get() = NutritionFormatter.formatMacro(dailyProtein)
    val formattedCarbs: String get() = NutritionFormatter.formatMacro(dailyCarbs)
    val formattedFat: String get() = NutritionFormatter.formatMacro(dailyFat)
    
    val meals get() = _dailyIntake.value?.meals ?: emptyList()
    
    // Dialog states
    var showManualInputDialog by mutableStateOf(false)
    var showDescriptionDialog by mutableStateOf(false)
    var showPhotoDialog by mutableStateOf(false)
    var showPhotoConfirmDialog by mutableStateOf(false)
    var showAILimitDialog by mutableStateOf(false)
    var showSubscriptionOffer by mutableStateOf(false)
    
    // Input states
    var inputMessage by mutableStateOf("")
    var pendingFood by mutableStateOf<Food?>(null)
    var prefillFood by mutableStateOf<Food?>(null)
    var selectedMeal by mutableStateOf(MealType.BREAKFAST)
    var isAnalyzing by mutableStateOf(false)
    var attachedPhoto by mutableStateOf<Bitmap?>(null)
    var attachedPhotoPath by mutableStateOf<String?>(null)
    var pendingPhoto by mutableStateOf<Bitmap?>(null)
    
    // Chat and AI states
    var photoCaption by mutableStateOf("")
    var pendingDescription by mutableStateOf("")
    var lastDescriptionMessage by mutableStateOf<String?>(null)
    var lastPhotoPath by mutableStateOf<String?>(null)
    var lastPhotoCaption by mutableStateOf("")
    var showAILoadingScreen by mutableStateOf(false)
    var pendingAIAction: (() -> Unit)? = null
    var aiLoadingInputMethod by mutableStateOf<String?>(null)
    private var aiLoadingJob: kotlinx.coroutines.Job? = null
    
    // Mode states
    var isDailyAnalysisEnabled by mutableStateOf(false)
    var isRecordMode by mutableStateOf(false)
    var isRecipeMode by mutableStateOf(false)
    
    // Current user from auth
    val currentUser: UserData? get() = authManager.currentUser.value
    val isOnline: Boolean get() = networkManager.isOnline.value
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            loadUserProfile()
            loadDailyIntake()
        }
    }
    
    private suspend fun loadUserProfile() {
        when (val result = getUserProfileUseCase()) {
            is Result.Success -> {
                _userProfile.value = result.data
                // Also keep legacy copy in shared prefs for UI
                val u = result.data
                val legacy = UserProfile(
                    name = u.name,
                    birthday = u.birthday,
                    height = u.height,
                    weight = u.weight,
                    gender = u.gender.name.lowercase(),
                    condition = u.activityLevel.name.lowercase(),
                    bodyFeeling = u.bodyFeeling,
                    goal = u.goal.name.lowercase(),
                    dailyCalories = u.nutritionTargets.dailyCalories,
                    dailyProteins = u.nutritionTargets.dailyProtein,
                    dailyFats = u.nutritionTargets.dailyFat,
                    dailyCarbs = u.nutritionTargets.dailyCarbs,
                    isSetupComplete = u.isSetupComplete
                )
                dataRepository.saveUserProfile(legacy)
            }
            is Result.Error -> {
                updateUiState { copy(error = "Failed to load user profile: ${result.exception.message}") }
            }
        }
    }
    
    private suspend fun loadDailyIntake() {
        when (val result = getDailyIntakeUseCase(GetDailyIntakeUseCase.Params())) {
            is Result.Success -> {
                _dailyIntake.value = result.data
            }
            is Result.Error -> {
                updateUiState { copy(error = "Failed to load daily intake: ${result.exception.message}") }
            }
        }
    }
    
    // Methods for managing modes
    fun startLoading() = offlineManager.startLoading()
    fun stopLoading() = offlineManager.stopLoading()
    fun forceOfflineMode() = offlineManager.forceOfflineMode()
    
    // Photo analysis
    suspend fun analyzePhotoWithAI(photoPath: String, caption: String = ""): Result<com.example.calorietracker.domain.entities.Food> {
        updateUiState { copy(isLoading = true) }
        isAnalyzing = true
        
        // Check AI usage limits first
        when (val limitResult = validateAIUsageLimitsUseCase(
            ValidateAIUsageLimitsUseCase.Params(
                com.example.calorietracker.domain.repositories.AIOperationType.PHOTO_ANALYSIS
            )
        )) {
            is Result.Success -> {
                if (!limitResult.data.canProceed) {
                    showAILimitDialog = true
                    isAnalyzing = false
                    updateUiState { copy(isLoading = false) }
                    return Result.error(com.example.calorietracker.domain.exceptions.DomainException.AIAnalysisException("AI limit exceeded"))
                }
            }
            is Result.Error -> {
                updateUiState { 
                    copy(
                        isLoading = false,
                        error = "Failed to check AI limits: ${limitResult.exception.message}"
                    )
                }
                isAnalyzing = false
                return limitResult
            }
        }
        
        // Analyze photo
        val result = analyzeFoodPhotoUseCase(
            AnalyzeFoodPhotoUseCase.Params(photoPath, caption)
        )
        
        when (result) {
            is Result.Success -> {
                updateUiState { copy(isLoading = false) }
            }
            is Result.Error -> {
                updateUiState { 
                    copy(
                        isLoading = false,
                        error = "Failed to analyze photo: ${result.exception.message}"
                    )
                }
            }
        }
        
        isAnalyzing = false
        return result
    }    

    // Description analysis
    fun analyzeDescription() {
        println("DEBUG: analyzeDescription called, inputMessage='$inputMessage'")
        viewModelScope.launch {
            if (inputMessage.isBlank()) {
                println("DEBUG: inputMessage is blank, returning")
                return@launch
            }
            
            updateUiState { copy(isLoading = true) }
            isAnalyzing = true
            
            // Check AI usage limits
            when (val limitResult = validateAIUsageLimitsUseCase(
                ValidateAIUsageLimitsUseCase.Params(
                    com.example.calorietracker.domain.repositories.AIOperationType.TEXT_ANALYSIS
                )
            )) {
                is Result.Success -> {
                    if (!limitResult.data.canProceed) {
                        showAILimitDialog = true
                        isAnalyzing = false
                        updateUiState { copy(isLoading = false) }
                        return@launch
                    }
                }
                is Result.Error -> {
                    updateUiState { 
                        copy(
                            isLoading = false,
                            error = "Failed to check AI limits: ${limitResult.exception.message}"
                        )
                    }
                    isAnalyzing = false
                    return@launch
                }
            }
            
            // Analyze description
            println("DEBUG: Calling analyzeFoodDescriptionUseCase")
            when (val result = analyzeFoodDescriptionUseCase(
                AnalyzeFoodDescriptionUseCase.Params(inputMessage)
            )) {
                is Result.Success -> {
                    println("DEBUG: Analysis successful, clearing inputMessage")
                    pendingFood = result.data
                    inputMessage = ""
                    updateUiState { copy(isLoading = false) }
                }
                is Result.Error -> {
                    println("DEBUG: Analysis failed: ${result.exception.message}")
                    updateUiState { 
                        copy(
                            isLoading = false,
                            error = "Failed to analyze description: ${result.exception.message}"
                        )
                    }
                }
            }
            
            isAnalyzing = false
        }
    }
    
    // Manual food input
    fun handleManualInput(
        name: String,
        calories: String,
        proteins: String,
        fats: String,
        carbs: String,
        weight: String
    ) {
        viewModelScope.launch {
            try {
                val food = Food(
                    name = name,
                    calories = calories.toIntOrNull() ?: 0,
                    protein = proteins.toDoubleOrNull() ?: 0.0,
                    fat = fats.toDoubleOrNull() ?: 0.0,
                    carbs = carbs.toDoubleOrNull() ?: 0.0,
                    weight = weight,
                    source = com.example.calorietracker.domain.entities.common.FoodSource.MANUAL_INPUT
                )
                
                pendingFood = food
            } catch (e: Exception) {
                updateUiState { copy(error = "Invalid input: ${e.message}") }
            }
        }
    }
    
    // Confirm and save food
    fun confirmFood() {
        val food = pendingFood ?: return
        
        viewModelScope.launch {
            updateUiState { copy(isLoading = true) }
            
            when (val result = saveFoodIntakeUseCase(
                SaveFoodIntakeUseCase.Params(food, selectedMeal)
            )) {
                is Result.Success -> {
                    // Удаляем сообщение с карточкой подтверждения
                    _messages.removeAll { it.type == com.example.calorietracker.data.MessageType.FOOD_CONFIRMATION }
                    
                    // Добавляем сообщение об успешном сохранении
                    val successMessage = com.example.calorietracker.data.ChatMessage(
                        id = java.util.UUID.randomUUID().toString(),
                        content = "✅ ${food.name} добавлен в дневник питания",
                        type = com.example.calorietracker.data.MessageType.AI,
                        timestamp = java.time.LocalDateTime.now(),
                        isVisible = true,
                        animate = true
                    )
                    _messages.add(successMessage)

                    // Появление чипа "Что думает Foody?" после подтверждения, если есть мнение AI
                    val confirmedFoodItem = foodMapper.mapDomainToData(food)
                    if (!confirmedFoodItem.aiOpinion.isNullOrBlank()) {
                        val aiChipMessage = com.example.calorietracker.data.ChatMessage(
                            id = java.util.UUID.randomUUID().toString(),
                            content = "",
                            type = com.example.calorietracker.data.MessageType.AI,
                            timestamp = java.time.LocalDateTime.now(),
                            isVisible = true,
                            animate = true,
                            isExpandable = true,
                            foodItem = confirmedFoodItem
                        )
                        _messages.add(aiChipMessage)
                    }
                    
                    pendingFood = null
                    loadDailyIntake() // Refresh daily intake
                    updateUiState { copy(isLoading = false) }
                }
                is Result.Error -> {
                    updateUiState { 
                        copy(
                            isLoading = false,
                            error = "Failed to save food: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }
    
    // Delete meal from history and refresh
    fun deleteMealFromHistory(date: String, mealIndex: Int) {
        dataRepository.deleteMealFromHistory(date, mealIndex)
        viewModelScope.launch { loadDailyIntake() }
    }

    // Удаление приема пищи по объекту FoodItem за сегодняшний день
    fun deleteFoodFromToday(food: FoodItem) {
        val today = com.example.calorietracker.utils.DailyResetUtils.getFoodDate()
        val todayIntake = dataRepository.getDailyIntake()

        fun normalizeWeightToInt(w: String?): Int? {
            if (w.isNullOrBlank()) return null
            val digits = w.filter { it.isDigit() }
            return digits.toIntOrNull()
        }

        val targetName = food.name.trim().lowercase()
        val targetCalories = food.calories
        val targetWeight = normalizeWeightToInt(food.weight)

        var foundIndex = -1
        for (i in todayIntake.meals.indices.reversed()) {
            val meal = todayIntake.meals[i]
            val anyMatch = meal.foods.any { f ->
                val nameOk = f.name.trim().lowercase() == targetName
                val calOk = kotlin.math.abs(f.calories - targetCalories) <= 5
                val weightOk = run {
                    val fw = normalizeWeightToInt(f.weight)
                    targetWeight == null || fw == null || fw == targetWeight
                }
                nameOk && calOk && weightOk
            }
            if (anyMatch) { foundIndex = i; break }
        }

        if (foundIndex != -1) {
            dataRepository.deleteMealFromHistory(today, foundIndex)
            viewModelScope.launch { loadDailyIntake() }
        }
    }

    // Поиск индекса приема пищи за сегодня по FoodItem (как в календаре, но с толерантностью)
    fun findMealIndexForToday(food: FoodItem): Int? {
        val today = com.example.calorietracker.utils.DailyResetUtils.getFoodDate()
        val todayIntake = dataRepository.getDailyIntake(today)

        fun normalizeWeightToInt(w: String?): Int? {
            if (w.isNullOrBlank()) return null
            val digits = w.filter { it.isDigit() }
            return digits.toIntOrNull()
        }

        val targetName = food.name.trim().lowercase()
        val targetCalories = food.calories
        val targetWeight = normalizeWeightToInt(food.weight)

        for (i in todayIntake.meals.indices.reversed()) {
            val meal = todayIntake.meals[i]
            val anyMatch = meal.foods.any { f ->
                val nameOk = f.name.trim().lowercase() == targetName
                val calOk = kotlin.math.abs(f.calories - targetCalories) <= 5
                val weightOk = run {
                    val fw = normalizeWeightToInt(f.weight)
                    targetWeight == null || fw == null || fw == targetWeight
                }
                nameOk && calOk && weightOk
            }
            if (anyMatch) return i
        }
        return null
    }

    // Поиск приема пищи в истории (самая свежая дата сначала)
    fun findMealInHistory(food: FoodItem): Pair<String, Int>? {
        fun normalizeWeightToInt(w: String?): Int? {
            if (w.isNullOrBlank()) return null
            val digits = w.filter { it.isDigit() }
            return digits.toIntOrNull()
        }

        val targetName = food.name.trim().lowercase()
        val targetCalories = food.calories
        val targetWeight = normalizeWeightToInt(food.weight)

        val dates = dataRepository.getAvailableDates()
        for (date in dates) {
            val intake = dataRepository.getIntakeHistory(date) ?: continue
            for (i in intake.meals.indices.reversed()) {
                val meal = intake.meals[i]
                val anyMatch = meal.foods.any { f ->
                    val nameOk = f.name.trim().lowercase() == targetName
                    val calOk = kotlin.math.abs(f.calories - targetCalories) <= 5
                    val fw = normalizeWeightToInt(f.weight)
                    val weightOk = targetWeight == null || fw == null || fw == targetWeight
                    nameOk && calOk && weightOk
                }
                if (anyMatch) return date to i
            }
        }
        return null
    }
    
    // Update date and check for reset
    fun updateDateAndCheckForReset() {
        viewModelScope.launch {
            loadDailyIntake()
        }
    }
    
    // Get progress color
    fun getProgressColor(current: Int, target: Int): Color {
        val progress = if (target > 0) current.toFloat() / target else 0f
        return when {
            progress < 0.5f -> Color(0xFF4CAF50) // Green
            progress < 0.8f -> Color(0xFFFF9800) // Orange
            progress < 1.2f -> Color(0xFF2196F3) // Blue
            else -> Color(0xFFF44336) // Red
        }
    }
    
    // Remove attached photo
    fun removeAttachedPhoto() {
        attachedPhoto = null
        attachedPhotoPath = null
    }

    // Toggle modes used by UI
    fun toggleDailyAnalysis() { isDailyAnalysisEnabled = !isDailyAnalysisEnabled }
    fun toggleRecordMode() { isRecordMode = !isRecordMode }
    fun toggleRecipeMode() { isRecipeMode = !isRecipeMode }

    // Bridge setters from FoodItem to domain Food used by confirm flow
    fun setPrefillFromFoodItem(foodItem: FoodItem?) {
        prefillFood = foodItem?.let { foodMapper.mapDataToDomain(it) }
    }

    fun setPendingFromFoodItem(foodItem: FoodItem?) {
        pendingFood = foodItem?.let { foodMapper.mapDataToDomain(it) }
    }

    // Simple send handler used by chat input
    fun sendMessage() {
        println("DEBUG: sendMessage called, inputMessage='$inputMessage', attachedPhotoPath='$attachedPhotoPath'")
        viewModelScope.launch {
            val messageText = inputMessage
            
            // Проверяем, что есть что отправить
            if (messageText.isBlank() && attachedPhotoPath == null) {
                println("DEBUG: Nothing to send")
                return@launch
            }
            
            // Создаём сообщение пользователя
            val userMessage = com.example.calorietracker.data.ChatMessage(
                id = java.util.UUID.randomUUID().toString(),
                content = messageText,
                type = com.example.calorietracker.data.MessageType.USER,
                timestamp = java.time.LocalDateTime.now(),
                isVisible = true,
                animate = true,
                imagePath = attachedPhotoPath
            )
            
            // Добавляем сообщение пользователя в список
            _messages.add(userMessage)
            
            // Очищаем поле ввода
            inputMessage = ""
            
            // Если есть фото - анализируем его
            if (attachedPhotoPath != null) {
                println("DEBUG: Analyzing photo")
                val photoPath = attachedPhotoPath!!
                removeAttachedPhoto()
                
                // Добавляем цикл загрузочных фраз
                val loadingMessage = com.example.calorietracker.data.ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    content = "",
                    type = com.example.calorietracker.data.MessageType.AI,
                    timestamp = java.time.LocalDateTime.now(),
                    isVisible = true,
                    animate = true,
                    isProcessing = true,
                    inputMethod = "photo"
                )
                _messages.add(loadingMessage)
                startAiLoadingCycle("photo", loadingMessage.id)
                
                // Анализируем фото и обрабатываем результат
                val captionForPhoto = if (isRecipeMode && messageText.isNotBlank()) {
                    "[RECIPE] ${'$'}messageText"
                } else messageText
                when (val result = analyzePhotoWithAI(photoPath, captionForPhoto)) {
                    is Result.Success -> {
                        stopAiLoadingCycle()
                        _messages.removeAll { it.id == loadingMessage.id }
                        
                        // Создаём сообщение с результатом
                        val foodItem = foodMapper.mapDomainToData(result.data)
                        val confirmMessage = com.example.calorietracker.data.ChatMessage(
                            id = java.util.UUID.randomUUID().toString(),
                            content = "",
                            type = com.example.calorietracker.data.MessageType.FOOD_CONFIRMATION,
                            timestamp = java.time.LocalDateTime.now(),
                            isVisible = true,
                            animate = true,
                            foodItem = foodItem
                        )
                        _messages.add(confirmMessage)
                        pendingFood = result.data
                    }
                    is Result.Error -> {
                        stopAiLoadingCycle()
                        _messages.removeAll { it.id == loadingMessage.id }
                        val errorMessage = com.example.calorietracker.data.ChatMessage(
                            id = java.util.UUID.randomUUID().toString(),
                            content = "Не удалось проанализировать фото. Попробуйте ещё раз.",
                            type = com.example.calorietracker.data.MessageType.AI,
                            timestamp = java.time.LocalDateTime.now(),
                            isVisible = true,
                            animate = true,
                            isError = true,
                            retryAction = {
                                viewModelScope.launch {
                                    // Удаляем текущее сообщение об ошибке и запускаем повтор
                                    _messages.removeAll { it.content == "Не удалось проанализировать фото. Попробуйте ещё раз." && it.isError }
                                    startAiLoadingCycle("photo")
                                    when (val retry = analyzePhotoWithAI(photoPath, messageText)) {
                                        is Result.Success -> {
                                            stopAiLoadingCycle()
                                            val foodItem = foodMapper.mapDomainToData(retry.data)
                                            val confirmMessage = com.example.calorietracker.data.ChatMessage(
                                                id = java.util.UUID.randomUUID().toString(),
                                                content = "",
                                                type = com.example.calorietracker.data.MessageType.FOOD_CONFIRMATION,
                                                timestamp = java.time.LocalDateTime.now(),
                                                isVisible = true,
                                                animate = true,
                                                foodItem = foodItem
                                            )
                                            _messages.add(confirmMessage)
                                            pendingFood = retry.data
                                        }
                                        is Result.Error -> {
                                            stopAiLoadingCycle()
                                        }
                                    }
                                }
                            }
                        )
                        _messages.add(errorMessage)
                    }
                }
                return@launch
            }
            
            // Определяем режим работы
            when {
                isRecordMode -> {
                    println("DEBUG: Record mode - analyzing food description")
                    startAiLoadingCycle("text")
                    handleRecordMode(messageText)
                }
                isRecipeMode -> {
                    println("DEBUG: Recipe mode - getting recipe")
                    startAiLoadingCycle("recipe")
                    handleRecipeMode(messageText) 
                }
                isDailyAnalysisEnabled -> {
                    println("DEBUG: Analysis mode - analyzing daily intake")
                    startAiLoadingCycle("analysis")
                    handleAnalysisMode(messageText)
                }
                else -> {
                    println("DEBUG: Chat mode - sending to AI")
                    startAiLoadingCycle("chat")
                    handleChatMode(messageText)
                }
            }
        }
    }

    private fun startAiLoadingCycle(method: String, messageId: String? = null) {
        aiLoadingInputMethod = method
        showAILoadingScreen = true
        aiLoadingJob?.cancel()
        // Добавляем пустое сообщение, если id не передан (для чата/текста и т.д.)
        if (messageId == null) {
            val id = java.util.UUID.randomUUID().toString()
            _messages.add(
                com.example.calorietracker.data.ChatMessage(
                    id = id,
                    content = "",
                    type = com.example.calorietracker.data.MessageType.AI,
                    timestamp = java.time.LocalDateTime.now(),
                    isVisible = true,
                    animate = true,
                    isProcessing = true,
                    inputMethod = method
                )
            )
        }
    }

    private fun stopAiLoadingCycle() {
        aiLoadingJob?.cancel()
        aiLoadingJob = null
        showAILoadingScreen = false
        aiLoadingInputMethod = null
        // Удаляем все активные loading-сообщения
        _messages.removeAll { it.isProcessing }
    }
    
    private suspend fun handleRecordMode(text: String) {
        // Анализируем описание
        when (val result = analyzeFoodDescriptionUseCase(
            AnalyzeFoodDescriptionUseCase.Params(text)
        )) {
            is Result.Success -> {
                stopAiLoadingCycle()
                pendingFood = result.data
                
                // Создаём сообщение с результатом
                val foodItem = foodMapper.mapDomainToData(result.data)
                val confirmMessage = com.example.calorietracker.data.ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    content = "",
                    type = com.example.calorietracker.data.MessageType.FOOD_CONFIRMATION,
                    timestamp = java.time.LocalDateTime.now(),
                    isVisible = true,
                    animate = true,
                    foodItem = foodItem
                )
                _messages.add(confirmMessage)
            }
            is Result.Error -> {
                stopAiLoadingCycle()
                val errorText = "Не удалось проанализировать продукт. Попробуйте ещё раз."
                val errorId = java.util.UUID.randomUUID().toString()
                val errorMessage = com.example.calorietracker.data.ChatMessage(
                    id = errorId,
                    content = errorText,
                    type = com.example.calorietracker.data.MessageType.AI,
                    timestamp = java.time.LocalDateTime.now(),
                    isVisible = true,
                    animate = true,
                    isError = true,
                    retryAction = {
                        viewModelScope.launch {
                            _messages.removeAll { it.id == errorId }
                            startAiLoadingCycle("text")
                            handleRecordMode(text)
                        }
                    }
                )
                _messages.add(errorMessage)
            }
        }
    }
    
    private suspend fun handleRecipeMode(text: String) {
        // Запрос рецепта через чат как текстовый вопрос
        val chatMessage = com.example.calorietracker.domain.entities.ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            content = "Подбери рецепт: $text",
            type = com.example.calorietracker.domain.entities.common.MessageType.USER,
            timestamp = java.time.LocalDateTime.now()
        )
        when (val result = sendChatMessageUseCase(SendChatMessageUseCase.Params(chatMessage))) {
            is Result.Success -> {
                stopAiLoadingCycle()
                val aiResponse = com.example.calorietracker.data.ChatMessage(
                    id = result.data.id,
                    content = result.data.content,
                    type = com.example.calorietracker.data.MessageType.AI,
                    timestamp = java.time.LocalDateTime.now(),
                    isVisible = true,
                    animate = true
                )
                _messages.add(aiResponse)
            }
            is Result.Error -> {
                stopAiLoadingCycle()
                val errorMessage = com.example.calorietracker.data.ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    content = "Не удалось найти рецепт. Попробуйте ещё раз.",
                    type = com.example.calorietracker.data.MessageType.AI,
                    timestamp = java.time.LocalDateTime.now(),
                    isVisible = true,
                    animate = true,
                    isError = true,
                    retryAction = {
                        viewModelScope.launch {
                            _messages.removeAll { it.content == "Не удалось найти рецепт. Попробуйте ещё раз." && it.isError }
                            startAiLoadingCycle("recipe")
                            handleRecipeMode(text)
                        }
                    }
                )
                _messages.add(errorMessage)
            }
        }
    }
    
    private suspend fun handleAnalysisMode(text: String) {
        // Запрашиваем анализ дня через чатовый эндпоинт
        val chatMessage = com.example.calorietracker.domain.entities.ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            content = "Сделай анализ моего рациона за сегодня",
            type = com.example.calorietracker.domain.entities.common.MessageType.USER,
            timestamp = java.time.LocalDateTime.now()
        )
        when (val result = sendChatMessageUseCase(SendChatMessageUseCase.Params(chatMessage))) {
            is Result.Success -> {
                stopAiLoadingCycle()
                val analysisMessage = com.example.calorietracker.data.ChatMessage(
                    id = result.data.id,
                    content = result.data.content,
                    type = com.example.calorietracker.data.MessageType.AI,
                    timestamp = java.time.LocalDateTime.now(),
                    isVisible = true,
                    animate = true
                )
                _messages.add(analysisMessage)
            }
            is Result.Error -> {
                stopAiLoadingCycle()
                val errorMessage = com.example.calorietracker.data.ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    content = "Не удалось выполнить анализ дня. Проверьте интернет и попробуйте снова.",
                    type = com.example.calorietracker.data.MessageType.AI,
                    timestamp = java.time.LocalDateTime.now(),
                    isVisible = true,
                    animate = true,
                    isError = true,
                    retryAction = {
                        viewModelScope.launch {
                            _messages.removeAll { it.content == "Не удалось выполнить анализ дня. Проверьте интернет и попробуйте снова." && it.isError }
                            startAiLoadingCycle("analysis")
                            handleAnalysisMode(text)
                        }
                    }
                )
                _messages.add(errorMessage)
            }
        }
    }
    
    private suspend fun handleChatMode(text: String) {
        // Отправляем через ChatUseCase
        val chatMessage = com.example.calorietracker.domain.entities.ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            content = text,
            type = com.example.calorietracker.domain.entities.common.MessageType.USER,
            timestamp = java.time.LocalDateTime.now()
        )
        
        when (val result = sendChatMessageUseCase(SendChatMessageUseCase.Params(chatMessage))) {
            is Result.Success -> {
                stopAiLoadingCycle()
                val aiResponse = com.example.calorietracker.data.ChatMessage(
                    id = result.data.id,
                    content = result.data.content,
                    type = com.example.calorietracker.data.MessageType.AI,
                    timestamp = java.time.LocalDateTime.now(),
                    isVisible = true,
                    animate = true
                )
                _messages.add(aiResponse)
            }
            is Result.Error -> {
                stopAiLoadingCycle()
                val errorMessage = com.example.calorietracker.data.ChatMessage(
                    id = java.util.UUID.randomUUID().toString(),
                    content = "Не удалось получить ответ. Проверьте подключение к интернету.",
                    type = com.example.calorietracker.data.MessageType.AI,
                    timestamp = java.time.LocalDateTime.now(),
                    isVisible = true,
                    animate = true,
                    isError = true,
                    retryAction = {
                        viewModelScope.launch {
                            _messages.removeAll { it.content == "Не удалось получить ответ. Проверьте подключение к интернету." && it.isError }
                            startAiLoadingCycle("chat")
                            handleChatMode(text)
                        }
                    }
                )
                _messages.add(errorMessage)
            }
        }
    }
    
    // Mark message as animated (legacy compatibility)
    fun markMessageAnimated(message: com.example.calorietracker.data.ChatMessage) {
        // Фикс: после первого проигрывания анимации закрепляем сообщение в чате,
        // чтобы при повторной композиции/скролле оно не анимировалось снова
        val index = _messages.indexOfFirst { it.id == message.id }
        if (index != -1) {
            val current = _messages[index]
            if (current.animate) {
                _messages[index] = current.copy(animate = false)
            }
        }
    }
    
    // Chat messages state
    private val _messages = mutableStateListOf<com.example.calorietracker.data.ChatMessage>()
    val messages: List<com.example.calorietracker.data.ChatMessage> get() = _messages
    
    // Helper function to update UI state
    private fun updateUiState(update: CalorieTrackerUiState.() -> CalorieTrackerUiState) {
        _uiState.value = _uiState.value.update()
    }

    // ------------- Legacy helpers used across pages -------------

    fun updateUserProfile(profile: UserProfile) {
        dataRepository.saveUserProfile(profile)
    }

    fun updateMealInHistory(date: String, index: Int, meal: Meal) {
        dataRepository.updateMeal(date, index, meal)
    }

    // removed duplicate at bottom

    fun getTodayData(): com.example.calorietracker.data.DayData? {
        val intake = dataRepository.getDailyIntake()
        val date = java.time.LocalDate.now()
        return com.example.calorietracker.data.DayData(
            date = date,
            calories = intake.calories.toFloat(),
            proteins = intake.protein,
            fats = intake.fat,
            carbs = intake.carbs,
            mealsCount = intake.meals.size
        )
    }

    fun getDayData(date: LocalDate): com.example.calorietracker.data.DayData? {
        val intake = dataRepository.getIntakeHistory(date.toString()) ?: return null
        return com.example.calorietracker.data.DayData(
            date = date,
            calories = intake.calories.toFloat(),
            proteins = intake.protein,
            fats = intake.fat,
            carbs = intake.carbs,
            mealsCount = intake.meals.size
        )
    }

    fun getAllDaysData(): List<com.example.calorietracker.data.DayData> {
        return dataRepository.getAvailableDates().mapNotNull { str ->
            runCatching { java.time.LocalDate.parse(str) }.getOrNull()
        }.mapNotNull { d -> getDayData(d) }
    }
}

/**
 * UI State for CalorieTracker screen
 */
data class CalorieTrackerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAnalyzing: Boolean = false
)