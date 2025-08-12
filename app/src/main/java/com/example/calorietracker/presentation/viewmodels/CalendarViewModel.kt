package com.example.calorietracker.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.common.DateRange
import com.example.calorietracker.domain.usecases.GetDailyIntakeUseCase
import com.example.calorietracker.domain.usecases.GetMonthlyIntakeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * ViewModel for calendar and historical data
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getDailyIntakeUseCase: GetDailyIntakeUseCase,
    private val getMonthlyIntakeUseCase: GetMonthlyIntakeUseCase
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    
    // Calendar data
    private val _calendarData = MutableStateFlow<Map<LocalDate, NutritionIntake>>(emptyMap())
    val calendarData: StateFlow<Map<LocalDate, NutritionIntake>> = _calendarData.asStateFlow()
    
    // Selected date
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    
    // Selected date intake
    private val _selectedDateIntake = MutableStateFlow<NutritionIntake?>(null)
    val selectedDateIntake: StateFlow<NutritionIntake?> = _selectedDateIntake.asStateFlow()
    
    init {
        loadCurrentMonth()
    }
    
    /**
     * Load calendar data for current month
     */
    fun loadCurrentMonth() {
        loadMonth(YearMonth.now())
    }
    
    /**
     * Load calendar data for specific month
     */
    fun loadMonth(month: YearMonth) {
        viewModelScope.launch {
            updateUiState { copy(isLoading = true, currentMonth = month) }
            
            when (val result = getMonthlyIntakeUseCase(GetMonthlyIntakeUseCase.Params(month))) {
                is Result.Success -> {
                    val dataMap = result.data.associateBy { it.date }
                    _calendarData.value = dataMap
                    updateUiState { copy(isLoading = false) }
                }
                is Result.Error -> {
                    updateUiState { 
                        copy(
                            isLoading = false,
                            error = "Failed to load calendar data: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Select a specific date
     */
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        loadDateIntake(date)
    }
    
    /**
     * Load intake data for specific date
     */
    private fun loadDateIntake(date: LocalDate) {
        viewModelScope.launch {
            when (val result = getDailyIntakeUseCase(GetDailyIntakeUseCase.Params(date))) {
                is Result.Success -> {
                    _selectedDateIntake.value = result.data
                }
                is Result.Error -> {
                    updateUiState { 
                        copy(error = "Failed to load date data: ${result.exception.message}")
                    }
                }
            }
        }
    }
    
    /**
     * Navigate to previous month
     */
    fun previousMonth() {
        val currentMonth = _uiState.value.currentMonth
        loadMonth(currentMonth.minusMonths(1))
    }
    
    /**
     * Navigate to next month
     */
    fun nextMonth() {
        val currentMonth = _uiState.value.currentMonth
        loadMonth(currentMonth.plusMonths(1))
    }
    
    /**
     * Go to today
     */
    fun goToToday() {
        val today = LocalDate.now()
        loadMonth(YearMonth.from(today))
        selectDate(today)
    }
    
    /**
     * Get intake for specific date
     */
    fun getIntakeForDate(date: LocalDate): NutritionIntake? {
        return _calendarData.value[date]
    }
    
    /**
     * Check if date has data
     */
    fun hasDataForDate(date: LocalDate): Boolean {
        val intake = getIntakeForDate(date)
        return intake != null && intake.meals.isNotEmpty()
    }
    
    /**
     * Get calories for date
     */
    fun getCaloriesForDate(date: LocalDate): Int {
        return getIntakeForDate(date)?.getTotalCalories() ?: 0
    }
    
    /**
     * Get goal achievement for date
     */
    fun isGoalMetForDate(date: LocalDate): Boolean? {
        return getIntakeForDate(date)?.isCalorieGoalMet()
    }
    
    /**
     * Clear error messages
     */
    fun clearError() {
        updateUiState { copy(error = null) }
    }
    
    // Helper function to update UI state
    private fun updateUiState(update: CalendarUiState.() -> CalendarUiState) {
        _uiState.value = _uiState.value.update()
    }
}

/**
 * UI State for Calendar screen
 */
data class CalendarUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentMonth: YearMonth = YearMonth.now()
)