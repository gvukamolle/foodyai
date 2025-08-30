package com.example.calorietracker.presentation.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.common.NutritionTargets
import com.example.calorietracker.domain.usecases.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Refactored UserProfileViewModel using Clean Architecture with Use Cases
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val saveUserProfileUseCase: SaveUserProfileUseCase,
    private val calculateNutritionTargetsUseCase: CalculateNutritionTargetsUseCase,
    private val validateUserDataUseCase: ValidateUserDataUseCase
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()
    
    // Current user profile
    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()
    
    // Calculated nutrition targets
    private val _nutritionTargets = MutableStateFlow<NutritionTargets?>(null)
    val nutritionTargets: StateFlow<NutritionTargets?> = _nutritionTargets.asStateFlow()
    
    init {
        loadUserProfile()
    }
    
    /**
     * Load user profile from repository
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            updateUiState { copy(isLoading = true) }
            
            when (val result = getUserProfileUseCase()) {
                is Result.Success -> {
                    _userProfile.value = result.data
                    updateUiState { copy(isLoading = false) }
                    
                    // Calculate nutrition targets if profile is complete
                    if (result.data.isValidForCalculations()) {
                        calculateNutritionTargets(result.data)
                    }
                }
                is Result.Error -> {
                    updateUiState { 
                        copy(
                            isLoading = false,
                            error = "Failed to load profile: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Save user profile with validation
     */
    fun saveUserProfile(user: User) {
        viewModelScope.launch {
            updateUiState { copy(isLoading = true) }
            
            // First validate the user data
            when (val validationResult = validateUserDataUseCase(
                ValidateUserDataUseCase.Params(user)
            )) {
                is Result.Success -> {
                    val validatedUser = validationResult.data
                    
                    // Save the validated user
                    when (val saveResult = saveUserProfileUseCase(
                        SaveUserProfileUseCase.Params(validatedUser)
                    )) {
                        is Result.Success -> {
                            _userProfile.value = validatedUser
                            updateUiState { 
                                copy(
                                    isLoading = false,
                                    successMessage = "Profile saved successfully"
                                )
                            }
                            
                            // Calculate new nutrition targets
                            calculateNutritionTargets(validatedUser)
                        }
                        is Result.Error -> {
                            updateUiState { 
                                copy(
                                    isLoading = false,
                                    error = "Failed to save profile: ${saveResult.exception.message}"
                                )
                            }
                        }
                    }
                }
                is Result.Error -> {
                    updateUiState { 
                        copy(
                            isLoading = false,
                            error = "Validation failed: ${validationResult.exception.message}"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Calculate nutrition targets for user
     */
    fun calculateNutritionTargets(user: User) {
        viewModelScope.launch {
            when (val result = calculateNutritionTargetsUseCase(
                CalculateNutritionTargetsUseCase.Params(user)
            )) {
                is Result.Success -> {
                    _nutritionTargets.value = result.data
                    
                    // Update user with new targets
                    val updatedUser = user.copy(nutritionTargets = result.data)
                    _userProfile.value = updatedUser

                    // Persist updated user so targets are saved in both domain and legacy
                    when (val saveResult = saveUserProfileUseCase(
                        SaveUserProfileUseCase.Params(updatedUser)
                    )) {
                        is Result.Error -> {
                            // Keep UI responsive; just log via println for diagnostics
                            println("DEBUG_PROFILE_VM: failed to persist updated targets: ${saveResult.exception.message}")
                        }
                        else -> {}
                    }
                }
                is Result.Error -> {
                    updateUiState { 
                        copy(
                            error = "Failed to calculate targets: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Update user profile field
     */
    fun updateUserField(update: (User) -> User) {
        val currentUser = _userProfile.value ?: return
        val updatedUser = update(currentUser)
        _userProfile.value = updatedUser
        
        // Recalculate targets if user data changed
        if (updatedUser.isValidForCalculations()) {
            calculateNutritionTargets(updatedUser)
        }
    }
    
    /**
     * Clear any error messages
     */
    fun clearError() {
        updateUiState { copy(error = null) }
    }
    
    /**
     * Clear success messages
     */
    fun clearSuccessMessage() {
        updateUiState { copy(successMessage = null) }
    }
    
    /**
     * Check if setup is complete
     */
    fun isSetupComplete(): Boolean {
        return _userProfile.value?.hasCompleteSetup() ?: false
    }
    
    /**
     * Get BMI for current user
     */
    fun getCurrentBMI(): Double? {
        return _userProfile.value?.getBMI()
    }
    
    /**
     * Get BMI category for current user
     */
    fun getCurrentBMICategory(): String? {
        return _userProfile.value?.getBMICategory()
    }
    
    /**
     * Get recommended calories for current user
     */
    fun getRecommendedCalories(): Int? {
        return _userProfile.value?.calculateRecommendedCalories()
    }
    
    // Helper function to update UI state
    private fun updateUiState(update: UserProfileUiState.() -> UserProfileUiState) {
        _uiState.value = _uiState.value.update()
    }
}

/**
 * UI State for UserProfile screen
 */
data class UserProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isCalculatingTargets: Boolean = false
)