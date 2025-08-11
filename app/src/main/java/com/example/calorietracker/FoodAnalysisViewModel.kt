package com.example.calorietracker

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.data.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class FoodAnalysisViewModel @Inject constructor(
    private val repository: DataRepository,
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager
) : ViewModel() {

    var isAnalyzing by mutableStateOf(false)
        private set
    fun setIsAnalyzing(value: Boolean) { isAnalyzing = value }

    var attachedPhoto by mutableStateOf<Bitmap?>(null)
        private set
    fun setAttachedPhoto(value: Bitmap?) { attachedPhoto = value }

    var pendingPhoto by mutableStateOf<Bitmap?>(null)
        private set
    fun setPendingPhoto(value: Bitmap?) { pendingPhoto = value }

    var currentFoodSource by mutableStateOf<String?>(null)
        private set
    fun setCurrentFoodSource(value: String?) { currentFoodSource = value }

    var inputMethod by mutableStateOf<String?>(null)
        private set
    fun setInputMethod(value: String?) { inputMethod = value }
}
