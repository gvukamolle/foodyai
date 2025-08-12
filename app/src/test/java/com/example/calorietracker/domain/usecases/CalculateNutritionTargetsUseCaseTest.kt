package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.common.*
import com.example.calorietracker.domain.exceptions.DomainException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CalculateNutritionTargetsUseCase
 */
class CalculateNutritionTargetsUseCaseTest {
    
    private lateinit var useCase: CalculateNutritionTargetsUseCase
    
    @Before
    fun setup() {
        useCase = CalculateNutritionTargetsUseCase()
    }
    
    @Test
    fun `calculate targets for valid male user should return success`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            goal = NutritionGoal.MAINTAIN_WEIGHT
        )
        
        // When
        val result = useCase(CalculateNutritionTargetsUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Success)
        val targets = (result as Result.Success).data
        
        assertTrue("Daily calories should be reasonable", targets.dailyCalories in 2000..3000)
        assertTrue("Daily proteins should be reasonable", targets.dailyProteins in 100..200)
        assertTrue("Daily fats should be reasonable", targets.dailyFats in 50..150)
        assertTrue("Daily carbs should be reasonable", targets.dailyCarbs in 200..400)
    }
    
    @Test
    fun `calculate targets for valid female user should return success`() = runTest {
        // Given
        val user = User(
            name = "Jane Doe",
            birthday = "1990-01-01",
            height = 165,
            weight = 60,
            gender = Gender.FEMALE,
            activityLevel = ActivityLevel.LIGHTLY_ACTIVE,
            goal = NutritionGoal.LOSE_WEIGHT
        )
        
        // When
        val result = useCase(CalculateNutritionTargetsUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Success)
        val targets = (result as Result.Success).data
        
        assertTrue("Daily calories should be reasonable for female", targets.dailyCalories in 1200..2200)
        assertTrue("Daily proteins should be reasonable", targets.dailyProteins in 80..150)
        assertTrue("Daily fats should be reasonable", targets.dailyFats in 40..100)
        assertTrue("Daily carbs should be reasonable", targets.dailyCarbs in 150..300)
    }
    
    @Test
    fun `calculate targets for weight loss goal should reduce calories`() = runTest {
        // Given
        val maintainUser = User(
            name = "Test User",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            goal = NutritionGoal.MAINTAIN_WEIGHT
        )
        
        val loseWeightUser = maintainUser.copy(goal = NutritionGoal.LOSE_WEIGHT)
        
        // When
        val maintainResult = useCase(CalculateNutritionTargetsUseCase.Params(maintainUser))
        val loseWeightResult = useCase(CalculateNutritionTargetsUseCase.Params(loseWeightUser))
        
        // Then
        assertTrue(maintainResult is Result.Success)
        assertTrue(loseWeightResult is Result.Success)
        
        val maintainTargets = (maintainResult as Result.Success).data
        val loseWeightTargets = (loseWeightResult as Result.Success).data
        
        assertTrue("Weight loss calories should be less than maintenance", 
            loseWeightTargets.dailyCalories < maintainTargets.dailyCalories)
    }
    
    @Test
    fun `calculate targets for weight gain goal should increase calories`() = runTest {
        // Given
        val maintainUser = User(
            name = "Test User",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            goal = NutritionGoal.MAINTAIN_WEIGHT
        )
        
        val gainWeightUser = maintainUser.copy(goal = NutritionGoal.GAIN_WEIGHT)
        
        // When
        val maintainResult = useCase(CalculateNutritionTargetsUseCase.Params(maintainUser))
        val gainWeightResult = useCase(CalculateNutritionTargetsUseCase.Params(gainWeightUser))
        
        // Then
        assertTrue(maintainResult is Result.Success)
        assertTrue(gainWeightResult is Result.Success)
        
        val maintainTargets = (maintainResult as Result.Success).data
        val gainWeightTargets = (gainWeightResult as Result.Success).data
        
        assertTrue("Weight gain calories should be more than maintenance", 
            gainWeightTargets.dailyCalories > maintainTargets.dailyCalories)
    }
    
    @Test
    fun `calculate targets with invalid user data should return validation error`() = runTest {
        // Given
        val invalidUser = User(
            name = "",
            birthday = "invalid-date",
            height = 0,
            weight = 0
        )
        
        // When
        val result = useCase(CalculateNutritionTargetsUseCase.Params(invalidUser))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("User data is not valid for calculations", result.exception.message)
    }
    
    @Test
    fun `calculate targets with missing gender should return validation error`() = runTest {
        // Given
        val user = User(
            name = "Test User",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = null
        )
        
        // When
        val result = useCase(CalculateNutritionTargetsUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Gender is required for nutrition calculations", result.exception.message)
    }
    
    @Test
    fun `calculate targets with missing activity level should use default`() = runTest {
        // Given
        val user = User(
            name = "Test User",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE,
            activityLevel = null,
            goal = NutritionGoal.MAINTAIN_WEIGHT
        )
        
        // When
        val result = useCase(CalculateNutritionTargetsUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Success)
        val targets = (result as Result.Success).data
        assertTrue("Should calculate with default activity level", targets.dailyCalories > 0)
    }
    
    @Test
    fun `calculate targets should have proper macro distribution`() = runTest {
        // Given
        val user = User(
            name = "Test User",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            goal = NutritionGoal.MAINTAIN_WEIGHT
        )
        
        // When
        val result = useCase(CalculateNutritionTargetsUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Success)
        val targets = (result as Result.Success).data
        
        // Calculate macro calories
        val proteinCalories = targets.dailyProteins * 4
        val fatCalories = targets.dailyFats * 9
        val carbCalories = targets.dailyCarbs * 4
        val totalMacroCalories = proteinCalories + fatCalories + carbCalories
        
        // Should be within reasonable range of total calories (±10%)
        val tolerance = targets.dailyCalories * 0.1
        assertTrue("Macro distribution should match total calories", 
            kotlin.math.abs(totalMacroCalories - targets.dailyCalories) <= tolerance)
    }
}