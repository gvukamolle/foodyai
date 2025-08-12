package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.common.FoodSource
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.FoodRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AnalyzeFoodPhotoUseCaseTest {
    
    private lateinit var foodRepository: FoodRepository
    private lateinit var useCase: AnalyzeFoodPhotoUseCase
    
    @Before
    fun setup() {
        foodRepository = mockk()
        useCase = AnalyzeFoodPhotoUseCase(foodRepository)
    }
    
    @Test
    fun `analyze photo successfully returns food`() = runTest {
        // Given
        val photoPath = "/path/to/photo.jpg"
        val caption = "Delicious pizza"
        val expectedFood = Food(
            name = "Pizza",
            calories = 300,
            protein = 12.0,
            fat = 10.0,
            carbs = 35.0,
            weight = "100г",
            source = FoodSource.AI_PHOTO_ANALYSIS
        )
        
        coEvery { 
            foodRepository.analyzeFoodPhoto(photoPath, caption) 
        } returns Result.success(expectedFood)
        
        // When
        val result = useCase(AnalyzeFoodPhotoUseCase.Params(photoPath, caption))
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedFood, (result as Result.Success).data)
        coVerify { foodRepository.analyzeFoodPhoto(photoPath, caption) }
    }
    
    @Test
    fun `analyze photo with blank path returns validation error`() = runTest {
        // Given
        val photoPath = ""
        val caption = "Pizza"
        
        // When
        val result = useCase(AnalyzeFoodPhotoUseCase.Params(photoPath, caption))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Photo path cannot be blank", result.exception.message)
    }
    
    @Test
    fun `analyze photo with invalid path returns validation error`() = runTest {
        // Given
        val photoPath = "invalid-path"
        val caption = "Pizza"
        
        // When
        val result = useCase(AnalyzeFoodPhotoUseCase.Params(photoPath, caption))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Invalid photo path", result.exception.message)
    }
    
    @Test
    fun `analyze photo repository error returns error`() = runTest {
        // Given
        val photoPath = "/path/to/photo.jpg"
        val caption = "Pizza"
        val repositoryError = DomainException.NetworkException("Network error")
        
        coEvery { 
            foodRepository.analyzeFoodPhoto(photoPath, caption) 
        } returns Result.error(repositoryError)
        
        // When
        val result = useCase(AnalyzeFoodPhotoUseCase.Params(photoPath, caption))
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(repositoryError, (result as Result.Error).exception)
    }
    
    @Test
    fun `analyze photo with unreasonable nutrition returns validation error`() = runTest {
        // Given
        val photoPath = "/path/to/photo.jpg"
        val caption = "Pizza"
        val invalidFood = Food(
            name = "Pizza",
            calories = 100,
            protein = 50.0, // Unreasonable - too much protein for calories
            fat = 10.0,
            carbs = 35.0,
            weight = "100г",
            source = FoodSource.AI_PHOTO_ANALYSIS
        )
        
        coEvery { 
            foodRepository.analyzeFoodPhoto(photoPath, caption) 
        } returns Result.success(invalidFood)
        
        // When
        val result = useCase(AnalyzeFoodPhotoUseCase.Params(photoPath, caption))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.AIAnalysisException)
    }
}