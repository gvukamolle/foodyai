package com.example.calorietracker.data.repositories

import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.mappers.UserMapper
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.common.NutritionTargets
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UserRepository that handles user-related operations
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val dataRepository: DataRepository,
    private val userMapper: UserMapper
) : UserRepository {
    
    override suspend fun getUserProfile(): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val userProfile = dataRepository.getUserProfile()
                if (userProfile != null) {
                    val domainUser = userMapper.mapDataToDomain(userProfile)
                    Result.success(domainUser)
                } else {
                    Result.error(
                        DomainException.DataNotFoundException("User profile not found")
                    )
                }
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get user profile: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun saveUserProfile(user: User): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userProfile = userMapper.mapDomainToData(user)
                dataRepository.saveUserProfile(userProfile)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to save user profile: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun calculateNutritionTargets(user: User): Result<NutritionTargets> {
        return withContext(Dispatchers.IO) {
            try {
                val recommendedCalories = user.calculateRecommendedCalories()
                    ?: return@withContext Result.error(
                        DomainException.BusinessLogicException("Cannot calculate recommended calories")
                    )
                
                // Calculate macro distribution
                val proteinCalories = (recommendedCalories * 0.25).toInt()
                val fatCalories = (recommendedCalories * 0.30).toInt()
                val carbsCalories = recommendedCalories - proteinCalories - fatCalories
                
                val targets = NutritionTargets(
                    dailyCalories = recommendedCalories,
                    dailyProtein = proteinCalories / 4,
                    dailyFat = fatCalories / 9,
                    dailyCarbs = carbsCalories / 4
                )
                
                Result.success(targets)
            } catch (e: Exception) {
                Result.error(
                    DomainException.BusinessLogicException(
                        "Failed to calculate nutrition targets: ${e.message}",
                        e
                    )
                )
            }
        }
    }    
   
 override suspend fun updateNutritionTargets(targets: NutritionTargets): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = getUserProfile()
                when (currentUser) {
                    is Result.Success -> {
                        val updatedUser = userMapper.updateUserTargets(currentUser.data, targets)
                        saveUserProfile(updatedUser)
                    }
                    is Result.Error -> currentUser
                }
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to update nutrition targets: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun validateUserProfile(user: User): Result<User> {
        return try {
            if (!user.isValidForCalculations()) {
                Result.error(DomainException.ValidationException("User profile is incomplete"))
            } else {
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.error(
                DomainException.ValidationException(
                    "User validation failed: ${e.message}",
                    e
                )
            )
        }
    }
    
    override suspend fun isSetupComplete(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val userResult = getUserProfile()
                when (userResult) {
                    is Result.Success -> Result.success(userResult.data.hasCompleteSetup())
                    is Result.Error -> Result.success(false)
                }
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to check setup status: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun markSetupComplete(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userResult = getUserProfile()
                when (userResult) {
                    is Result.Success -> {
                        val updatedUser = userResult.data.copy(isSetupComplete = true)
                        saveUserProfile(updatedUser)
                    }
                    is Result.Error -> userResult
                }
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to mark setup complete: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun resetUserData(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                dataRepository.clearUserData()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to reset user data: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun getUserPreferences(): Result<Map<String, Any>> {
        // User preferences functionality not implemented yet
        return Result.success(emptyMap())
    }
    
    override suspend fun saveUserPreferences(preferences: Map<String, Any>): Result<Unit> {
        // User preferences functionality not implemented yet
        return Result.success(Unit)
    }
    
    override suspend fun exportUserData(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val userResult = getUserProfile()
                when (userResult) {
                    is Result.Success -> {
                        // TODO: Implement proper data export
                        val exportData = "User data export: ${userResult.data}"
                        Result.success(exportData)
                    }
                    is Result.Error -> userResult
                }
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to export user data: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun importUserData(data: String): Result<Unit> {
        // TODO: Implement data import
        return Result.success(Unit)
    }
}