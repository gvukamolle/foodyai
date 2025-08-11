package com.example.calorietracker

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.data.DailyNutritionSummary
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.Meal
import com.example.calorietracker.utils.DailyResetUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: DataRepository,
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager
) : ViewModel() {

    private val _calendarData = MutableStateFlow<Map<LocalDate, DailyNutritionSummary>>(emptyMap())
    val calendarData: StateFlow<Map<LocalDate, DailyNutritionSummary>> = _calendarData.asStateFlow()
    fun setCalendarData(value: Map<LocalDate, DailyNutritionSummary>) { _calendarData.value = value }

    var currentDate by mutableStateOf(LocalDate.now())
        private set
    fun setCurrentDate(date: LocalDate) { currentDate = date }

    var displayDate by mutableStateOf(DailyResetUtils.getFormattedDisplayDate())
        private set
    fun setDisplayDate(value: String) { displayDate = value }

    fun loadDataForDate(date: LocalDate) {
        currentDate = date
        viewModelScope.launch {
            val intake = repository.getDailyIntake(date.toString())
            // These fields belong to NutritionViewModel in the new architecture;
            // for now we simply expose calendar data.
        }
    }

    private fun refreshCalendarData() {
        viewModelScope.launch {
            repository.getCalendarData().collect { summaries ->
                _calendarData.value = summaries.associateBy { it.date }
            }
        }
    }

    fun deleteMealFromHistory(date: String, index: Int) {
        repository.deleteMeal(date, index)
        refreshCalendarData()
        if (date == DailyResetUtils.getFoodDate()) {
            // TODO: notify NutritionViewModel to refresh today's intake
        }
    }

    fun updateMealInHistory(date: String, index: Int, meal: Meal) {
        repository.updateMeal(date, index, meal)
        refreshCalendarData()
        if (date == DailyResetUtils.getFoodDate()) {
            // TODO: notify NutritionViewModel to refresh today's intake
        }
    }
}