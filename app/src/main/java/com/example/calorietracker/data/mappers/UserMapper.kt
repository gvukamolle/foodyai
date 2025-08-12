package com.example.calorietracker.data.mappers

import com.example.calorietracker.data.UserProfile
import com.example.calorietracker.data.UserProfileEntity
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.common.ActivityLevel
import com.example.calorietracker.domain.entities.common.Gender
import com.example.calorietracker.domain.entities.common.NutritionGoal
import com.example.calorietracker.domain.entities.common.NutritionTargets
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for transforming User entities between domain and data layers
 */
@Singleton
class UserMapper @Inject constructor() {
    
    /**
     * Convert domain User to data UserProfile
     */
    fun mapDomainToData(user: User): UserProfile {
        return UserProfile(
            name = user.name,
            birthday = user.birthday,
            height = user.height,
            weight = user.weight,
            gender = user.gender.name.lowercase(),
            condition = user.activityLevel.name.lowercase(),
            bodyFeeling = user.bodyFeeling,
            goal = user.goal.name.lowercase(),
            dailyCalories = user.nutritionTargets.dailyCalories,
            dailyProteins = user.nutritionTargets.dailyProtein,
            dailyFats = user.nutritionTargets.dailyFat,
            dailyCarbs = user.nutritionTargets.dailyCarbs,
            isSetupComplete = user.isSetupComplete
        )
    }
    
    /**
     * Convert data UserProfile to domain User
     */
    fun mapDataToDomain(userProfile: UserProfile): User {
        return User(
            name = userProfile.name,
            birthday = userProfile.birthday,
            height = userProfile.height,
            weight = userProfile.weight,
            gender = Gender.fromString(userProfile.gender),
            activityLevel = ActivityLevel.fromString(userProfile.condition),
            bodyFeeling = userProfile.bodyFeeling,
            goal = NutritionGoal.fromString(userProfile.goal),
            nutritionTargets = NutritionTargets(
                dailyCalories = userProfile.dailyCalories,
                dailyProtein = userProfile.dailyProteins,
                dailyFat = userProfile.dailyFats,
                dailyCarbs = userProfile.dailyCarbs
            ),
            isSetupComplete = userProfile.isSetupComplete
        )
    }
    
    /**
     * Convert domain User to data UserProfileEntity (for Room database)
     */
    fun mapDomainToEntity(user: User): UserProfileEntity {
        return UserProfileEntity(
            name = user.name,
            birthday = user.birthday,
            height = user.height,
            weight = user.weight,
            gender = user.gender.name.lowercase(),
            condition = user.activityLevel.name.lowercase(),
            bodyFeeling = user.bodyFeeling,
            goal = user.goal.name.lowercase(),
            dailyCalories = user.nutritionTargets.dailyCalories,
            dailyProteins = user.nutritionTargets.dailyProtein,
            dailyFats = user.nutritionTargets.dailyFat,
            dailyCarbs = user.nutritionTargets.dailyCarbs
        )
    }
    
    /**
     * Convert data UserProfileEntity to domain User
     */
    fun mapEntityToDomain(entity: UserProfileEntity): User {
        return User(
            name = entity.name,
            birthday = entity.birthday,
            height = entity.height,
            weight = entity.weight,
            gender = Gender.fromString(entity.gender),
            activityLevel = ActivityLevel.fromString(entity.condition),
            bodyFeeling = entity.bodyFeeling,
            goal = NutritionGoal.fromString(entity.goal),
            nutritionTargets = NutritionTargets(
                dailyCalories = entity.dailyCalories,
                dailyProtein = entity.dailyProteins,
                dailyFat = entity.dailyFats,
                dailyCarbs = entity.dailyCarbs
            ),
            isSetupComplete = true // Entity exists, so setup is complete
        )
    }
    
    /**
     * Convert data UserProfile to UserProfileEntity
     */
    fun mapDataToEntity(userProfile: UserProfile): UserProfileEntity {
        return UserProfileEntity(
            name = userProfile.name,
            birthday = userProfile.birthday,
            height = userProfile.height,
            weight = userProfile.weight,
            gender = userProfile.gender,
            condition = userProfile.condition,
            bodyFeeling = userProfile.bodyFeeling,
            goal = userProfile.goal,
            dailyCalories = userProfile.dailyCalories,
            dailyProteins = userProfile.dailyProteins,
            dailyFats = userProfile.dailyFats,
            dailyCarbs = userProfile.dailyCarbs
        )
    }
    
    /**
     * Convert UserProfileEntity to data UserProfile
     */
    fun mapEntityToData(entity: UserProfileEntity): UserProfile {
        return UserProfile(
            name = entity.name,
            birthday = entity.birthday,
            height = entity.height,
            weight = entity.weight,
            gender = entity.gender,
            condition = entity.condition,
            bodyFeeling = entity.bodyFeeling,
            goal = entity.goal,
            dailyCalories = entity.dailyCalories,
            dailyProteins = entity.dailyProteins,
            dailyFats = entity.dailyFats,
            dailyCarbs = entity.dailyCarbs,
            isSetupComplete = true
        )
    }
    
    /**
     * Extract NutritionTargets from domain User
     */
    fun extractNutritionTargets(user: User): NutritionTargets {
        return user.nutritionTargets
    }
    
    /**
     * Update User with new NutritionTargets
     */
    fun updateUserTargets(user: User, targets: NutritionTargets): User {
        return user.copy(nutritionTargets = targets)
    }
}