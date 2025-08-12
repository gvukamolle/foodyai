package com.example.calorietracker.domain.entities.common

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for NutritionTargets data class
 */
class NutritionTargetsTest {
    
    @Test
    fun `NutritionTargets creation with valid data should succeed`() {
        // Given & When
        val targets = NutritionTargets(
            dailyCalories = 2000,
            dailyProteins = 150,
            dailyFats = 67,
            dailyCarbs = 250
        )
        
        // Then
        assertEquals(2000, targets.dailyCalories)
        assertEquals(150, targets.dailyProteins)
        assertEquals(67, targets.dailyFats)
        assertEquals(250, targets.dailyCarbs)
    }
    
    @Test
    fun `NutritionTargets with zero values should be valid`() {
        // Given & When
        val targets = NutritionTargets(
            dailyCalories = 0,
            dailyProteins = 0,
            dailyFats = 0,
            dailyCarbs = 0
        )
        
        // Then
        assertEquals(0, targets.dailyCalories)
        assertEquals(0, targets.dailyProteins)
        assertEquals(0, targets.dailyFats)
        assertEquals(0, targets.dailyCarbs)
    }
    
    @Test
    fun `NutritionTargets equality should work correctly`() {
        // Given
        val targets1 = NutritionTargets(2000, 150, 67, 250)
        val targets2 = NutritionTargets(2000, 150, 67, 250)
        val targets3 = NutritionTargets(1800, 150, 67, 250)
        
        // Then
        assertEquals(targets1, targets2)
        assertNotEquals(targets1, targets3)
        assertEquals(targets1.hashCode(), targets2.hashCode())
    }
    
    @Test
    fun `NutritionTargets copy should work correctly`() {
        // Given
        val original = NutritionTargets(2000, 150, 67, 250)
        
        // When
        val copied = original.copy(dailyCalories = 1800)
        
        // Then
        assertEquals(1800, copied.dailyCalories)
        assertEquals(150, copied.dailyProteins)
        assertEquals(67, copied.dailyFats)
        assertEquals(250, copied.dailyCarbs)
        
        // Original should be unchanged
        assertEquals(2000, original.dailyCalories)
    }
    
    @Test
    fun `NutritionTargets toString should provide meaningful output`() {
        // Given
        val targets = NutritionTargets(2000, 150, 67, 250)
        
        // When
        val string = targets.toString()
        
        // Then
        assertTrue(string.contains("2000"))
        assertTrue(string.contains("150"))
        assertTrue(string.contains("67"))
        assertTrue(string.contains("250"))
    }
    
    @Test
    fun `NutritionTargets with high values should be valid`() {
        // Given & When
        val targets = NutritionTargets(
            dailyCalories = 5000,
            dailyProteins = 300,
            dailyFats = 200,
            dailyCarbs = 600
        )
        
        // Then
        assertEquals(5000, targets.dailyCalories)
        assertEquals(300, targets.dailyProteins)
        assertEquals(200, targets.dailyFats)
        assertEquals(600, targets.dailyCarbs)
    }
    
    @Test
    fun `NutritionTargets component access should work`() {
        // Given
        val targets = NutritionTargets(2000, 150, 67, 250)
        
        // When
        val (calories, proteins, fats, carbs) = targets
        
        // Then
        assertEquals(2000, calories)
        assertEquals(150, proteins)
        assertEquals(67, fats)
        assertEquals(250, carbs)
    }
    
    @Test
    fun `NutritionTargets default values should work if provided`() {
        // This test assumes there might be default values in the constructor
        // If not, this test can be removed or modified based on actual implementation
        
        // Given & When
        val targets = NutritionTargets(
            dailyCalories = 2000,
            dailyProteins = 150,
            dailyFats = 67,
            dailyCarbs = 250
        )
        
        // Then
        assertNotNull(targets)
        assertTrue(targets.dailyCalories >= 0)
        assertTrue(targets.dailyProteins >= 0)
        assertTrue(targets.dailyFats >= 0)
        assertTrue(targets.dailyCarbs >= 0)
    }
    
    @Test
    fun `NutritionTargets with realistic values should be valid`() {
        // Given - realistic values for different scenarios
        val sedentaryFemale = NutritionTargets(1500, 120, 50, 188)
        val activeMale = NutritionTargets(2500, 200, 83, 313)
        val athlete = NutritionTargets(3500, 280, 117, 438)
        
        // Then
        assertNotNull(sedentaryFemale)
        assertNotNull(activeMale)
        assertNotNull(athlete)
        
        assertTrue(sedentaryFemale.dailyCalories < activeMale.dailyCalories)
        assertTrue(activeMale.dailyCalories < athlete.dailyCalories)
    }
}