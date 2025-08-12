package com.example.calorietracker.domain.entities

import com.example.calorietracker.domain.entities.common.ActivityLevel
import com.example.calorietracker.domain.entities.common.Gender
import com.example.calorietracker.domain.entities.common.NutritionGoal
import com.example.calorietracker.domain.entities.common.NutritionTargets
import org.junit.Assert.*
import org.junit.Test

class UserTest {
    
    @Test
    fun `create user with valid data succeeds`() {
        // Given & When
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            goal = NutritionGoal.MAINTAIN_WEIGHT,
            nutritionTargets = NutritionTargets(2000, 150, 65, 250)
        )
        
        // Then
        assertEquals("John Doe", user.name)
        assertEquals("1990-01-01", user.birthday)
        assertEquals(180, user.height)
        assertEquals(75, user.weight)
        assertEquals(Gender.MALE, user.gender)
        assertEquals(ActivityLevel.MODERATELY_ACTIVE, user.activityLevel)
        assertEquals(NutritionGoal.MAINTAIN_WEIGHT, user.goal)
    }
    
    @Test
    fun `calculate age correctly`() {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE
        )
        
        // When
        val age = user.getAge()
        
        // Then
        assertNotNull(age)
        assertTrue(age!! >= 33) // Should be at least 33 in 2023+
    }
    
    @Test
    fun `calculate age with invalid birthday returns null`() {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "invalid-date",
            height = 180,
            weight = 75,
            gender = Gender.MALE
        )
        
        // When
        val age = user.getAge()
        
        // Then
        assertNull(age)
    }
    
    @Test
    fun `calculate BMI correctly`() {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180, // 1.8m
            weight = 72,  // 72kg
            gender = Gender.MALE
        )
        
        // When
        val bmi = user.getBMI()
        
        // Then
        assertNotNull(bmi)
        assertEquals(22.22, bmi!!, 0.01) // 72 / (1.8 * 1.8) = 22.22
    }
    
    @Test
    fun `get BMI category for normal weight`() {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 72, // BMI = 22.22 (normal)
            gender = Gender.MALE
        )
        
        // When
        val category = user.getBMICategory()
        
        // Then
        assertEquals("Нормальный вес", category)
    }
    
    @Test
    fun `get BMI category for underweight`() {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 55, // BMI = 17.0 (underweight)
            gender = Gender.MALE
        )
        
        // When
        val category = user.getBMICategory()
        
        // Then
        assertEquals("Недостаточный вес", category)
    }
    
    @Test
    fun `calculate BMR for male correctly`() {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01", // ~33 years old
            height = 180,
            weight = 75,
            gender = Gender.MALE
        )
        
        // When
        val bmr = user.calculateBMR()
        
        // Then
        assertNotNull(bmr)
        // BMR = (10 * 75) + (6.25 * 180) - (5 * 33) + 5 = 750 + 1125 - 165 + 5 = 1715
        assertTrue(bmr!! > 1700 && bmr < 1750)
    }
    
    @Test
    fun `calculate BMR for female correctly`() {
        // Given
        val user = User(
            name = "Jane Doe",
            birthday = "1990-01-01", // ~33 years old
            height = 165,
            weight = 60,
            gender = Gender.FEMALE
        )
        
        // When
        val bmr = user.calculateBMR()
        
        // Then
        assertNotNull(bmr)
        // BMR = (10 * 60) + (6.25 * 165) - (5 * 33) - 161 = 600 + 1031.25 - 165 - 161 = 1305.25
        assertTrue(bmr!! > 1300 && bmr < 1320)
    }
    
    @Test
    fun `calculate TDEE correctly`() {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE // multiplier = 1.55
        )
        
        // When
        val tdee = user.calculateTDEE()
        
        // Then
        assertNotNull(tdee)
        val bmr = user.calculateBMR()!!
        val expectedTdee = bmr * 1.55
        assertEquals(expectedTdee, tdee!!, 1.0)
    }
    
    @Test
    fun `calculate recommended calories for weight loss`() {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            goal = NutritionGoal.LOSE_WEIGHT // adjustment = 0.8
        )
        
        // When
        val recommendedCalories = user.calculateRecommendedCalories()
        
        // Then
        assertNotNull(recommendedCalories)
        val tdee = user.calculateTDEE()!!
        val expectedCalories = (tdee * 0.8).toInt()
        assertEquals(expectedCalories, recommendedCalories)
    }
    
    @Test
    fun `is valid for calculations returns true for complete profile`() {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE
        )
        
        // When & Then
        assertTrue(user.isValidForCalculations())
    }
    
    @Test
    fun `is valid for calculations returns false for incomplete profile`() {
        // Given
        val user = User(
            name = "",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE
        )
        
        // When & Then
        assertFalse(user.isValidForCalculations())
    }
    
    @Test
    fun `has complete setup returns true when setup is complete and valid`() {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE,
            isSetupComplete = true
        )
        
        // When & Then
        assertTrue(user.hasCompleteSetup())
    }
    
    @Test
    fun `has complete setup returns false when setup is not marked complete`() {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE,
            isSetupComplete = false
        )
        
        // When & Then
        assertFalse(user.hasCompleteSetup())
    }
}