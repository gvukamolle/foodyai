package com.example.calorietracker.domain.repositories

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.common.NutritionTargets

/**
 * Repository interface for user-related operations
 */
interface UserRepository {
    
    /**
     * Get current user profile
     */
    suspend fun getUserProfile(): Result<User>
    
    /**
     * Save user profile
     */
    suspend fun saveUserProfile(user: User): Result<Unit>
    
    /**
     * Calculate nutrition targets based on user profile
     */
    suspend fun calculateNutritionTargets(user: User): Result<NutritionTargets>
    
    /**
     * Update nutrition targets
     */
    suspend fun updateNutritionTargets(targets: NutritionTargets): Result<Unit>
    
    /**
     * Validate user profile data
     */
    suspend fun validateUserProfile(user: User): Result<User>
    
    /**
     * Check if user setup is complete
     */
    suspend fun isSetupComplete(): Result<Boolean>
    
    /**
     * Mark user setup as complete
     */
    suspend fun markSetupComplete(): Result<Unit>
    
    /**
     * Reset user data (for testing or account deletion)
     */
    suspend fun resetUserData(): Result<Unit>
    
    /**
     * Get user preferences
     */
    suspend fun getUserPreferences(): Result<Map<String, Any>>
    
    /**
     * Save user preferences
     */
    suspend fun saveUserPreferences(preferences: Map<String, Any>): Result<Unit>
    
    /**
     * Export user data
     */
    suspend fun exportUserData(): Result<String>
    
    /**
     * Import user data
     */
    suspend fun importUserData(data: String): Result<Unit>
}