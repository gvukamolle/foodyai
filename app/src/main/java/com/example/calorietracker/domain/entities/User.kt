package com.example.calorietracker.domain.entities

import com.example.calorietracker.domain.entities.common.Gender
import com.example.calorietracker.domain.entities.common.ActivityLevel
import com.example.calorietracker.domain.entities.common.NutritionGoal
import com.example.calorietracker.domain.entities.common.NutritionTargets
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Represents a user with their profile information
 */
data class User(
    val name: String = "",
    val birthday: String = "", // Format: "YYYY-MM-DD"
    val height: Int = 0, // in cm
    val weight: Int = 0, // in kg
    val gender: Gender = Gender.OTHER,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATELY_ACTIVE,
    val bodyFeeling: String = "",
    val goal: NutritionGoal = NutritionGoal.MAINTAIN_WEIGHT,
    val nutritionTargets: NutritionTargets = NutritionTargets(0, 0, 0, 0),
    val isSetupComplete: Boolean = false
) {
    
    /**
     * Calculate user's age based on birthday
     */
    fun getAge(): Int? {
        if (birthday.isBlank()) return null
        return try {
            val birthDate = LocalDate.parse(birthday, DateTimeFormatter.ISO_LOCAL_DATE)
            val today = LocalDate.now()
            var age = today.year - birthDate.year
            if (today.dayOfYear < birthDate.dayOfYear) {
                age--
            }
            age
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculate BMI (Body Mass Index)
     */
    fun getBMI(): Double? {
        if (height <= 0 || weight <= 0) return null
        val heightInMeters = height / 100.0
        return weight / (heightInMeters * heightInMeters)
    }
    
    /**
     * Get BMI category
     */
    fun getBMICategory(): String? {
        val bmi = getBMI() ?: return null
        return when {
            bmi < 18.5 -> "Недостаточный вес"
            bmi < 25.0 -> "Нормальный вес"
            bmi < 30.0 -> "Избыточный вес"
            else -> "Ожирение"
        }
    }
    
    /**
     * Calculate Basal Metabolic Rate using Mifflin-St Jeor equation
     */
    fun calculateBMR(): Double? {
        val age = getAge() ?: return null
        if (height <= 0 || weight <= 0) return null
        
        return when (gender) {
            Gender.MALE -> (10 * weight) + (6.25 * height) - (5 * age) + 5
            Gender.FEMALE -> (10 * weight) + (6.25 * height) - (5 * age) - 161
            Gender.OTHER -> {
                // Use average of male and female calculations
                val maleBMR = (10 * weight) + (6.25 * height) - (5 * age) + 5
                val femaleBMR = (10 * weight) + (6.25 * height) - (5 * age) - 161
                (maleBMR + femaleBMR) / 2
            }
        }
    }
    
    /**
     * Calculate Total Daily Energy Expenditure (TDEE)
     */
    fun calculateTDEE(): Double? {
        val bmr = calculateBMR() ?: return null
        return bmr * activityLevel.multiplier
    }
    
    /**
     * Calculate recommended daily calories based on goal
     */
    fun calculateRecommendedCalories(): Int? {
        val tdee = calculateTDEE() ?: return null
        val adjustment = NutritionGoal.getCalorieAdjustment(goal)
        return (tdee * adjustment).toInt()
    }
    
    /**
     * Check if user profile is valid for nutrition calculations
     */
    fun isValidForCalculations(): Boolean {
        return name.isNotBlank() && 
               birthday.isNotBlank() && 
               height > 0 && 
               weight > 0 && 
               getAge() != null
    }
    
    /**
     * Check if setup is complete
     */
    fun hasCompleteSetup(): Boolean {
        return isSetupComplete && isValidForCalculations()
    }
    
    companion object {
        /**
         * Create User from legacy UserProfile data
         */
        fun fromLegacyProfile(
            name: String,
            birthday: String,
            height: Int,
            weight: Int,
            gender: String,
            condition: String, // maps to activityLevel
            bodyFeeling: String,
            goal: String,
            dailyCalories: Int,
            dailyProteins: Int,
            dailyFats: Int,
            dailyCarbs: Int,
            isSetupComplete: Boolean
        ): User {
            return User(
                name = name,
                birthday = birthday,
                height = height,
                weight = weight,
                gender = Gender.fromString(gender),
                activityLevel = ActivityLevel.fromString(condition),
                bodyFeeling = bodyFeeling,
                goal = NutritionGoal.fromString(goal),
                nutritionTargets = NutritionTargets(dailyCalories, dailyProteins, dailyFats, dailyCarbs),
                isSetupComplete = isSetupComplete
            )
        }
    }
}