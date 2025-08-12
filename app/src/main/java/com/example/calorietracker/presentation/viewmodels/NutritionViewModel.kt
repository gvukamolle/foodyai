package com.example.calorietracker.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.common.DateRange
import com.example.calorietracker.domain.repositories.NutritionStatistics
import com.example.calorietracker.domain.repositories.NutritionTrends
import com.example.calorietracker.domain.usecases.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * ViewModel for nutrition tracking and analytics
 */
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val getDailyIntakeUseCase: GetDailyIntakeUseCase,
    private val getWeeklyIntakeUseCase: GetWeeklyIntakeUseCase,
    private val getMonthlyIntakeUseCase: GetMonthlyIntakeUseCase,
    private val calculateNutritionProgressUseCase: CalculateNutritionProgressUseCase
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()
    
    // Daily intake
    private val _dailyIntake = MutableStateFlow<NutritionIntake?>(null)
    val dailyIntake: StateFlow<NutritionIntake?> = _dailyIntake.asStateFlow()
    
    // Weekly data
    private val _weeklyIntakes = MutableStateFlow<List<NutritionIntake>>(emptyList())
    val weeklyIntakes: StateFlow<List<NutritionIntake>> = _weeklyIntakes.asStateFlow()
    
    // Monthly data
    private val _monthlyIntakes = MutableStateFlow<List<NutritionIntake>>(emptyList())
    val monthlyIntakes: StateFlow<List<NutritionIntake>> = _monthlyIntakes.asStateFlow()
    
    // Progress data
    private val _nutritionProgress = MutableStateFlow<NutritionProgress?>(null)
    val nutritionProgress: StateFlow<NutritionProgress?> = _nutritionProgress.asStateFlow()
    
    init {
        loadTodayIntake()
    }
    
    /**
     * Load today's nutrition intake
     */
    fun loadTodayIntake() {
        loadDailyIntake(LocalDate.now())
    }
    
    /**
     * Load nutrition intake for specific date
     */
    fun loadDailyIntake(date: LocalDate) {
        viewModelScope.launch {
            updateUiState { copy(isLoading = true) }
            
            when (val result = getDailyIntakeUseCase(GetDailyIntakeUseCase.Params(date))) {
                is Result.Success -> {
                    _dailyIntake.value = result.data
                    calculateProgress(result.data)
                    updateUiState { copy(isLoading = false) }
                }
                is Result.Error -> {
                    updateUiState { 
                        copy(
                            isLoading = false,
                            error = "Failed to load daily intake: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Load weekly nutrition data
     */
    fun loadWeeklyIntakes(startDate: LocalDate = LocalDate.now().minusDays(6)) {
        viewModelScope.launch {
            updateUiState { copy(isLoading = true) }
            
            when (val result = getWeeklyIntakeUseCase(GetWeeklyIntakeUseCase.Params(startDate))) {
                is Result.Success -> {
                    _weeklyIntakes.value = result.data
                    updateUiState { copy(isLoading = false) }
                }
                is Result.Error -> {
                    updateUiState { 
                        copy(
                            isLoading = false,
                            error = "Failed to load weekly data: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Load monthly nutrition data
     */
    fun loadMonthlyIntakes(month: YearMonth = YearMonth.now()) {
        viewModelScope.launch {
            updateUiState { copy(isLoading = true) }
            
            when (val result = getMonthlyIntakeUseCase(GetMonthlyIntakeUseCase.Params(month))) {
                is Result.Success -> {
                    _monthlyIntakes.value = result.data
                    updateUiState { copy(isLoading = false) }
                }
                is Result.Error -> {
                    updateUiState { 
                        copy(
                            isLoading = false,
                            error = "Failed to load monthly data: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Calculate nutrition progress for current intake
     */
    private fun calculateProgress(intake: NutritionIntake) {
        val targets = intake.targets ?: return
        
        viewModelScope.launch {
            when (val result = calculateNutritionProgressUseCase(
                CalculateNutritionProgressUseCase.Params(intake, targets)
            )) {
                is Result.Success -> {
                    _nutritionProgress.value = result.data
                }
                is Result.Error -> {
                    updateUiState { 
                        copy(error = "Failed to calculate progress: ${result.exception.message}")
                    }
                }
            }
        }
    }
    
    /**
     * Get average daily calories for current week
     */
    fun getWeeklyAverageCalories(): Double {
        val intakes = _weeklyIntakes.value
        return if (intakes.isNotEmpty()) {
            intakes.sumOf { it.getTotalCalories() }.toDouble() / intakes.size
        } else 0.0
    }
    
    /**
     * Get average daily protein for current week
     */
    fun getWeeklyAverageProtein(): Double {
        val intakes = _weeklyIntakes.value
        return if (intakes.isNotEmpty()) {
            intakes.sumOf { it.getTotalProtein() } / intakes.size
        } else 0.0
    }
    
    /**
     * Check if daily goal is met
     */
    fun isDailyGoalMet(): Boolean {
        return _dailyIntake.value?.isCalorieGoalMet() ?: false
    }
    
    /**
     * Get remaining calories for today
     */
    fun getRemainingCalories(): Int? {
        return _dailyIntake.value?.getRemainingCalories()
    }
    
    /**
     * Clear error messages
     */
    fun clearError() {
        updateUiState { copy(error = null) }
    }
    
    // Helper function to update UI state
    private fun updateUiState(update: NutritionUiState.() -> NutritionUiState) {
        _uiState.value = _uiState.value.update()
    }
}

/**
 * UI State for Nutrition screen
 */
data class NutritionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDate: LocalDate = LocalDate.now()
)