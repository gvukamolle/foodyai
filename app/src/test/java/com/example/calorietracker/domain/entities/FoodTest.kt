package com.example.calorietracker.domain.entities

import com.example.calorietracker.domain.entities.common.FoodSource
import org.junit.Assert.*
import org.junit.Test

class FoodTest {
    
    @Test
    fun `create food with valid data succeeds`() {
        // Given & When
        val food = Food(
            name = "Apple",
            calories = 80,
            protein = 0.3,
            fat = 0.2,
            carbs = 21.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        
        // Then
        assertEquals("Apple", food.name)
        assertEquals(80, food.calories)
        assertEquals(0.3, food.protein, 0.01)
        assertEquals(0.2, food.fat, 0.01)
        assertEquals(21.0, food.carbs, 0.01)
        assertEquals("100г", food.weight)
        assertEquals(FoodSource.MANUAL_INPUT, food.source)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `create food with blank name throws exception`() {
        Food(
            name = "",
            calories = 80,
            protein = 0.3,
            fat = 0.2,
            carbs = 21.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `create food with negative calories throws exception`() {
        Food(
            name = "Apple",
            calories = -10,
            protein = 0.3,
            fat = 0.2,
            carbs = 21.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `create food with negative protein throws exception`() {
        Food(
            name = "Apple",
            calories = 80,
            protein = -0.3,
            fat = 0.2,
            carbs = 21.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
    }
    
    @Test
    fun `calculate macro calories correctly`() {
        // Given
        val food = Food(
            name = "Test Food",
            calories = 100,
            protein = 10.0, // 40 calories
            fat = 5.0,      // 45 calories
            carbs = 15.0,   // 60 calories
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        
        // When
        val macroCalories = food.calculateMacroCalories()
        
        // Then
        assertEquals(145, macroCalories) // 40 + 45 + 60
    }
    
    @Test
    fun `get weight in grams correctly`() {
        // Given
        val food = Food(
            name = "Test Food",
            calories = 100,
            protein = 10.0,
            fat = 5.0,
            carbs = 15.0,
            weight = "150г",
            source = FoodSource.MANUAL_INPUT
        )
        
        // When
        val weightInGrams = food.getWeightInGrams()
        
        // Then
        assertEquals(150.0, weightInGrams!!, 0.01)
    }
    
    @Test
    fun `get weight in grams with g suffix`() {
        // Given
        val food = Food(
            name = "Test Food",
            calories = 100,
            protein = 10.0,
            fat = 5.0,
            carbs = 15.0,
            weight = "200g",
            source = FoodSource.MANUAL_INPUT
        )
        
        // When
        val weightInGrams = food.getWeightInGrams()
        
        // Then
        assertEquals(200.0, weightInGrams!!, 0.01)
    }
    
    @Test
    fun `get nutrition per 100g correctly`() {
        // Given
        val food = Food(
            name = "Test Food",
            calories = 200,
            protein = 20.0,
            fat = 10.0,
            carbs = 30.0,
            weight = "200г",
            source = FoodSource.MANUAL_INPUT
        )
        
        // When
        val per100g = food.getNutritionPer100g()
        
        // Then
        assertNotNull(per100g)
        assertEquals(100, per100g!!.calories)
        assertEquals(10.0, per100g.protein, 0.01)
        assertEquals(5.0, per100g.fat, 0.01)
        assertEquals(15.0, per100g.carbs, 0.01)
        assertEquals("100г", per100g.weight)
    }
    
    @Test
    fun `has AI analysis returns true when opinion exists`() {
        // Given
        val food = Food(
            name = "Test Food",
            calories = 100,
            protein = 10.0,
            fat = 5.0,
            carbs = 15.0,
            weight = "100г",
            source = FoodSource.AI_PHOTO_ANALYSIS,
            aiOpinion = "This looks like a healthy snack"
        )
        
        // When & Then
        assertTrue(food.hasAIAnalysis())
    }
    
    @Test
    fun `has AI analysis returns false when opinion is null`() {
        // Given
        val food = Food(
            name = "Test Food",
            calories = 100,
            protein = 10.0,
            fat = 5.0,
            carbs = 15.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        
        // When & Then
        assertFalse(food.hasAIAnalysis())
    }
    
    @Test
    fun `has reasonable nutrition returns true for reasonable values`() {
        // Given
        val food = Food(
            name = "Test Food",
            calories = 100,
            protein = 5.0,  // 20 calories
            fat = 3.0,      // 27 calories
            carbs = 13.0,   // 52 calories
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        // Total macro calories: 99, actual calories: 100 (1% difference)
        
        // When & Then
        assertTrue(food.hasReasonableNutrition())
    }
    
    @Test
    fun `has reasonable nutrition returns false for unreasonable values`() {
        // Given
        val food = Food(
            name = "Test Food",
            calories = 100,
            protein = 20.0, // 80 calories
            fat = 10.0,     // 90 calories
            carbs = 20.0,   // 80 calories
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        // Total macro calories: 250, actual calories: 100 (150% difference)
        
        // When & Then
        assertFalse(food.hasReasonableNutrition())
    }
}