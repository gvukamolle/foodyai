package com.example.calorietracker.presentation.viewmodels

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.auth.UserData
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
import java.time.LocalDate

/**
 * Refactored ViewModel using Clean Architecture with Use Cases
 */
@HiltViewModel
class CalorieTrackerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val networkManager: NetworkManager,
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
    
    // Current user profile
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()
    
    // Daily nutrition intake
    private val _dailyIntake = MutableStateFlow<NutritionIntake?>(null)
    val dailyIntake: StateFlow<NutritionIntake?> = _dailyIntake.asStateFlow()
    
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
    fun analyzePhotoWithAI(photoPath: String, caption: String = "") {
        viewModelScope.launch {
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
            
            // Analyze photo
            when (val result = analyzeFoodPhotoUseCase(
                AnalyzeFoodPhotoUseCase.Params(photoPath, caption)
            )) {
                is Result.Success -> {
                    pendingFood = result.data
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
        }
    }    

    // Description analysis
    fun analyzeDescription() {
        viewModelScope.launch {
            if (inputMessage.isBlank()) return@launch
            
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
            when (val result = analyzeFoodDescriptionUseCase(
                AnalyzeFoodDescriptionUseCase.Params(inputMessage)
            )) {
                is Result.Success -> {
                    pendingFood = result.data
                    inputMessage = ""
                    updateUiState { copy(isLoading = false) }
                }
                is Result.Error -> {
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
    
    // Delete meal from history
    fun deleteMealFromHistory(date: String, mealIndex: Int) {
        viewModelScope.launch {
            // Note: Delete meal functionality would require additional use case
            // For now, just refresh the data
            loadDailyIntake() // Refresh after deletion
        }
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
    
    // Mark message as animated (legacy compatibility)
    fun markMessageAnimated(message: com.example.calorietracker.data.ChatMessage) {
        // Legacy method - no longer needed with new architecture
    }
    
    // Get messages (legacy compatibility)
    val messages: List<com.example.calorietracker.data.ChatMessage> get() = emptyList()
    
    // Helper function to update UI state
    private fun updateUiState(update: CalorieTrackerUiState.() -> CalorieTrackerUiState) {
        _uiState.value = _uiState.value.update()
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