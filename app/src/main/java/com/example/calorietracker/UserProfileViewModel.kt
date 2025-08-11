package com.example.calorietracker

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.compose.ui.graphics.Color
import kotlin.math.round
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.UserProfile
import com.example.calorietracker.data.UserData
import com.example.calorietracker.data.DailyIntake
import com.example.calorietracker.utils.calculateAge
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val repository: DataRepository,
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager
) : ViewModel() {

    var userProfile by mutableStateOf(UserProfile())
        private set
    fun setUserProfile(value: UserProfile) { userProfile = value }

    var currentStep by mutableStateOf("setup")
        private set
    fun setCurrentStep(value: String) { currentStep = value }

    var showSettings by mutableStateOf(false)
        private set
    fun setShowSettings(value: Boolean) { showSettings = value }

    fun updateUserProfile(newProfile: UserProfile) {
        userProfile = newProfile
        handleSetupSubmit()
    }

    fun handleSetupSubmit() {
        userProfile = calculateDailyNeeds(userProfile)
        currentStep = "main"
        saveUserData()
    }

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
        return when {
            percentage < 40 -> Color(0xFFFF828B)
            percentage < 80 -> Color(0xFFFFDB82)
            percentage < 100 -> Color(0xFF7BFF9C)
            percentage < 110 -> Color(0xFF37BE2C)
            else -> Color(0xFFFF5259)
        }
    }

    fun syncWithUserData(userData: UserData) {
        userProfile = userProfile.copy(
            isSetupComplete = userData.isSetupComplete || userProfile.isSetupComplete
        )
    }

    fun clearLocalData() {
        userProfile = UserProfile()
        repository.saveUserProfile(userProfile)
        repository.clearChatHistory()
    }

    private fun saveUserData() {
        repository.saveUserProfile(userProfile)
        val currentIntake = DailyIntake(
            calories = 0,
            protein = 0,
            carbs = 0,
            fat = 0,
            meals = emptyList()
        )
        repository.saveDailyIntake(currentIntake)
    }
}
