package com.example.calorietracker

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.Meal
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val repository: DataRepository,
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager
) : ViewModel() {

    var dailyCalories by mutableStateOf(0)
        private set
    fun setDailyCalories(value: Int) { dailyCalories = value }

    var dailyProtein by mutableStateOf(0f)
        private set
    fun setDailyProtein(value: Float) { dailyProtein = value }

    var dailyCarbs by mutableStateOf(0f)
        private set
    fun setDailyCarbs(value: Float) { dailyCarbs = value }

    var dailyFat by mutableStateOf(0f)
        private set
    fun setDailyFat(value: Float) { dailyFat = value }

    var meals by mutableStateOf<List<Meal>>(emptyList())
        private set
    fun setMeals(value: List<Meal>) { meals = value }
}
