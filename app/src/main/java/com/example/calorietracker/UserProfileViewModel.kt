package com.example.calorietracker

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.UserProfile
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
}
