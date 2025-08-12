package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.common.FoodSource
import com.example.calorietracker.domain.exceptions.DomainException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ProcessAIResponseUseCase
 */
class ProcessAIResponseUseCaseTest {
    
    private lateinit var useCase: ProcessAIResponseUseCase
    
    @Before
    fun setup() {
        useCase = ProcessAIResponseUseCase()
    }
    
    @Test
    fun `process valid AI response should return success`() = runTest {
        // Given
        val aiResponse = """
            {
                "name": "Apple",
                "calories": 95,
                "protein": 0.5,
                "fat": 0.3,
                "carbs": 25.0,
                "weight": "100г",
                "analysis": "Fresh red apple, medium size"
            }
        """.trimIndent()
        
        // When
        val result = useCase(ProcessAIResponseUseCase.Params(aiResponse, FoodSource.AI_PHOTO_ANALYSIS))
        
        // Then
        assertTrue(result is Result.Success)
        val food = (result as Result.Success).data
        assertEquals("Apple", food.name)
        assertEquals(95, food.calories)
        assertEquals(0.5, food.protein, 0.01)
        assertEquals(FoodSource.AI_PHOTO_ANALYSIS, food.source)
    }
    
    @Test
    fun `process AI response with missing required fields should return validation error`() = runTest {
        // Given
        val aiResponse = """
            {
                "calories": 95,
                "protein": 0.5,
                "fat": 0.3,
                "carbs": 25.0
            }
        """.trimIndent()
        
        // When
        val result = useCase(ProcessAIResponseUseCase.Params(aiResponse, FoodSource.AI_PHOTO_ANALYSIS))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.AIAnalysisException)
        assertTrue(result.exception.message!!.contains("Missing required field: name"))
    }
    
    @Test
    fun `process invalid JSON should return parsing error`() = runTest {
        // Given
        val aiResponse = "invalid json content"
        
        // When
        val result = useCase(ProcessAIResponseUseCase.Params(aiResponse, FoodSource.AI_PHOTO_ANALYSIS))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.AIAnalysisException)
        assertTrue(result.exception.message!!.contains("Failed to parse AI response"))
    }
    
    @Test
    fun `process empty AI response should return validation error`() = runTest {
        // Given
        val aiResponse = ""
        
        // When
        val result = useCase(ProcessAIResponseUseCase.Params(aiResponse, FoodSource.AI_PHOTO_ANALYSIS))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("AI response cannot be blank", result.exception.message)
    }
    
    @Test
    fun `process AI response with negative calories should return validation error`() = runTest {
        // Given
        val aiResponse = """
            {
                "name": "Apple",
                "calories": -10,
                "protein": 0.5,
                "fat": 0.3,
                "carbs": 25.0,
                "weight": "100г"
            }
        """.trimIndent()
        
        // When
        val result = useCase(ProcessAIResponseUseCase.Params(aiResponse, FoodSource.AI_PHOTO_ANALYSIS))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertTrue(result.exception.message!!.contains("Calories cannot be negative"))
    }
    
    @Test
    fun `process AI response with unreasonable nutrition values should return validation error`() = runTest {
        // Given
        val aiResponse = """
            {
                "name": "Apple",
                "calories": 100,
                "protein": 50.0,
                "fat": 30.0,
                "carbs": 40.0,
                "weight": "100г"
            }
        """.trimIndent()
        
        // When
        val result = useCase(ProcessAIResponseUseCase.Params(aiResponse, FoodSource.AI_PHOTO_ANALYSIS))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.AIAnalysisException)
        assertTrue(result.exception.message!!.contains("unreasonable nutrition values"))
    }
    
    @Test
    fun `process AI response with text analysis source should set correct source`() = runTest {
        // Given
        val aiResponse = """
            {
                "name": "Banana",
                "calories": 105,
                "protein": 1.3,
                "fat": 0.4,
                "carbs": 27.0,
                "weight": "100г"
            }
        """.trimIndent()
        
        // When
        val result = useCase(ProcessAIResponseUseCase.Params(aiResponse, FoodSource.AI_TEXT_ANALYSIS))
        
        // Then
        assertTrue(result is Result.Success)
        val food = (result as Result.Success).data
        assertEquals(FoodSource.AI_TEXT_ANALYSIS, food.source)
    }
    
    @Test
    fun `process AI response with optional fields should include them`() = runTest {
        // Given
        val aiResponse = """
            {
                "name": "Grilled Chicken Breast",
                "calories": 165,
                "protein": 31.0,
                "fat": 3.6,
                "carbs": 0.0,
                "weight": "100г",
                "analysis": "Lean protein source, well-cooked",
                "confidence": 0.95,
                "tags": ["protein", "lean", "cooked"]
            }
        """.trimIndent()
        
        // When
        val result = useCase(ProcessAIResponseUseCase.Params(aiResponse, FoodSource.AI_PHOTO_ANALYSIS))
        
        // Then
        assertTrue(result is Result.Success)
        val food = (result as Result.Success).data
        assertEquals("Grilled Chicken Breast", food.name)
        assertEquals("Lean protein source, well-cooked", food.aiOpinion)
        assertTrue(food.hasAIAnalysis())
    }
    
    @Test
    fun `process AI response with zero carbs should be valid`() = runTest {
        // Given
        val aiResponse = """
            {
                "name": "Egg White",
                "calories": 52,
                "protein": 11.0,
                "fat": 0.2,
                "carbs": 0.0,
                "weight": "100г"
            }
        """.trimIndent()
        
        // When
        val result = useCase(ProcessAIResponseUseCase.Params(aiResponse, FoodSource.AI_PHOTO_ANALYSIS))
        
        // Then
        assertTrue(result is Result.Success)
        val food = (result as Result.Success).data
        assertEquals(0.0, food.carbs, 0.01)
    }
    
    @Test
    fun `process AI response with decimal values should handle precision correctly`() = runTest {
        // Given
        val aiResponse = """
            {
                "name": "Almonds",
                "calories": 579,
                "protein": 21.15,
                "fat": 49.93,
                "carbs": 21.55,
                "weight": "100г"
            }
        """.trimIndent()
        
        // When
        val result = useCase(ProcessAIResponseUseCase.Params(aiResponse, FoodSource.AI_PHOTO_ANALYSIS))
        
        // Then
        assertTrue(result is Result.Success)
        val food = (result as Result.Success).data
        assertEquals(21.15, food.protein, 0.01)
        assertEquals(49.93, food.fat, 0.01)
        assertEquals(21.55, food.carbs, 0.01)
    }
    
    @Test
    fun `process AI response with reasonable nutrition should pass validation`() = runTest {
        // Given
        val aiResponse = """
            {
                "name": "Mixed Salad",
                "calories": 100,
                "protein": 5.0,
                "fat": 3.0,
                "carbs": 13.0,
                "weight": "100г"
            }
        """.trimIndent()
        
        // When
        val result = useCase(ProcessAIResponseUseCase.Params(aiResponse, FoodSource.AI_PHOTO_ANALYSIS))
        
        // Then
        assertTrue(result is Result.Success)
        val food = (result as Result.Success).data
        assertTrue("Should have reasonable nutrition", food.hasReasonableNutrition())
    }
}