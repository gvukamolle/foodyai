package com.example.calorietracker.data.repositories

import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.mappers.FoodMapper
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.common.Gender
import com.example.calorietracker.domain.entities.common.ActivityLevel
import com.example.calorietracker.domain.entities.common.NutritionGoal
import com.example.calorietracker.domain.entities.common.NutritionTargets
import com.example.calorietracker.domain.repositories.UserRepository
import com.example.calorietracker.network.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for AI integration in FoodRepositoryImpl
 * Tests that Make.com webhook is used instead of DataRepository
 */
class FoodRepositoryImplAITest {

    private lateinit var makeService: MakeService
    private lateinit var dataRepository: DataRepository
    private lateinit var foodMapper: FoodMapper
    private lateinit var userRepository: UserRepository
    private lateinit var foodRepositoryImpl: FoodRepositoryImpl

    private val testUser = User(
        name = "Test User",
        birthday = "1990-01-01",
        height = 175,
        weight = 70,
        gender = Gender.MALE,
        activityLevel = ActivityLevel.MODERATELY_ACTIVE,
        goal = NutritionGoal.MAINTAIN_WEIGHT,
        nutritionTargets = NutritionTargets(2000, 150, 67, 250),
        isSetupComplete = true
    )

    @Before
    fun setup() {
        makeService = mockk()
        dataRepository = mockk()
        foodMapper = mockk()
        userRepository = mockk()
        
        foodRepositoryImpl = FoodRepositoryImpl(
            makeService = makeService,
            dataRepository = dataRepository,
            foodMapper = foodMapper,
            userRepository = userRepository
        )
    }

    @Test
    fun `analyzeFoodPhoto should use MakeService not DataRepository`() = runTest {
        // Given
        val photoPath = "/test/path/image.jpg"
        val caption = "test caption"
        val expectedResponse = FoodAnalysisResponse(
            status = "success",
            answer = """{"name":"Apple","calories":52,"protein":0.3,"fat":0.2,"carbs":14,"weight":"100г"}"""
        )

        every { userRepository.getUserProfile() } returns Result.success(testUser)
        coEvery { makeService.analyzeFoodImage(any(), any()) } returns expectedResponse

        // When
        val result = foodRepositoryImpl.analyzeFoodPhoto(photoPath, caption)

        // Then
        assertTrue("Result should be success", result is Result.Success)
        
        // Verify MakeService was called with correct webhook ID
        coVerify { 
            makeService.analyzeFoodImage(
                MakeService.WEBHOOK_ID,
                any<ImageAnalysisRequest>()
            ) 
        }
        
        // Verify DataRepository was NOT called for AI analysis
        verify(exactly = 0) { dataRepository.analyzePhotoWithAI(any(), any()) }
    }

    @Test
    fun `analyzeFoodDescription should use MakeService not DataRepository`() = runTest {
        // Given
        val description = "яблоко 150г"
        val expectedResponse = FoodAnalysisResponse(
            status = "success",
            answer = """{"name":"Яблоко","calories":78,"protein":0.4,"fat":0.3,"carbs":21,"weight":"150г"}"""
        )

        every { userRepository.getUserProfile() } returns Result.success(testUser)
        coEvery { makeService.analyzeFood(any(), any()) } returns expectedResponse

        // When
        val result = foodRepositoryImpl.analyzeFoodDescription(description)

        // Then
        assertTrue("Result should be success", result is Result.Success)
        
        // Verify MakeService was called with correct webhook ID
        coVerify { 
            makeService.analyzeFood(
                MakeService.WEBHOOK_ID,
                any<FoodAnalysisRequest>()
            ) 
        }
        
        // Verify DataRepository was NOT called for AI analysis
        verify(exactly = 0) { dataRepository.analyzeDescription(any()) }
    }

    @Test
    fun `should use correct webhook ID`() = runTest {
        // Given
        val description = "test food"
        val expectedResponse = FoodAnalysisResponse(status = "success", answer = "{}")

        every { userRepository.getUserProfile() } returns Result.success(testUser)
        coEvery { makeService.analyzeFood(any(), any()) } returns expectedResponse

        // When
        foodRepositoryImpl.analyzeFoodDescription(description)

        // Then
        coVerify { 
            makeService.analyzeFood(
                "653st2c10rmg92nlltf3y0m8sggxaac6", // Correct webhook ID
                any()
            ) 
        }
    }

    @Test
    fun `should handle user profile not found gracefully`() = runTest {
        // Given
        val description = "test food"
        val expectedResponse = FoodAnalysisResponse(status = "success", answer = "{}")

        every { userRepository.getUserProfile() } returns Result.error(Exception("User not found"))
        coEvery { makeService.analyzeFood(any(), any()) } returns expectedResponse

        // When
        val result = foodRepositoryImpl.analyzeFoodDescription(description)

        // Then
        assertTrue("Should still work with default profile", result is Result.Success)
        
        // Should still call MakeService with default profile
        coVerify { makeService.analyzeFood(any(), any()) }
    }
}