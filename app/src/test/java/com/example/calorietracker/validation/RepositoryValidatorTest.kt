package com.example.calorietracker.validation

import com.example.calorietracker.domain.repositories.FoodRepository
import com.example.calorietracker.domain.repositories.UserRepository
import com.example.calorietracker.domain.repositories.NutritionRepository
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.common.FoodSource
import com.example.calorietracker.domain.entities.common.Gender
import com.example.calorietracker.domain.entities.common.ActivityLevel
import com.example.calorietracker.domain.entities.common.NutritionGoal
import com.example.calorietracker.domain.entities.common.NutritionTargets
import com.example.calorietracker.domain.entities.common.MealType
import com.example.calorietracker.domain.entities.common.DateRange
import com.example.calorietracker.domain.repositories.NutritionStatistics
import com.example.calorietracker.domain.repositories.NutritionTrends
import com.example.calorietracker.validation.models.ValidationResults
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Tests for RepositoryValidator to ensure repository implementations work correctly
 */
class RepositoryValidatorTest {
    
    private lateinit var mockFoodRepository: FoodRepository
    private lateinit var mockUserRepository: UserRepository
    private lateinit var mockNutritionRepository: NutritionRepository
    private lateinit var validator: RepositoryValidator
    
    @Before
    fun setup() {
        mockFoodRepository = mockk()
        mockUserRepository = mockk()
        mockNutritionRepository = mockk()
        validator = RepositoryValidator(mockFoodRepository, mockUserRepository, mockNutritionRepository)
    }
    
    @Test
    fun `validateFoodRepository should test all food operations`() = runTest {
        // Mock successful responses
        coEvery { mockFoodRepository.analyzeFoodDescription(any()) } returns Result.success(createTestFood())
        coEvery { mockFoodRepository.validateFoodData(any()) } returns Result.success(createTestFood())
        coEvery { mockFoodRepository.saveFoodIntake(any(), any()) } returns Result.success(Unit)
        coEvery { mockFoodRepository.getFoodHistory(any()) } returns Result.success(listOf(createTestFood()))
        coEvery { mockFoodRepository.searchFoodByName(any()) } returns Result.success(listOf(createTestFood()))
        coEvery { mockFoodRepository.getRecentFoods(any()) } returns Result.success(listOf(createTestFood()))
        
        val result = validator.validateFoodRepository()
        
        assertNotNull(result)
        assertEquals("FoodRepository", result.validatorName)
        
        // Verify all methods were called
        coVerify { mockFoodRepository.analyzeFoodDescription("one apple") }
        coVerify { mockFoodRepository.validateFoodData(any()) }
        coVerify { mockFoodRepository.saveFoodIntake(any(), MealType.BREAKFAST) }
        coVerify { mockFoodRepository.getFoodHistory(any()) }
        coVerify { mockFoodRepository.searchFoodByName("apple") }
        coVerify { mockFoodRepository.getRecentFoods(5) }
    }
    
    @Test
    fun `validateFoodRepository should detect validation logic errors`() = runTest {
        // Mock validation that incorrectly accepts invalid food
        val invalidFood = Food(
            name = "",
            calories = -100,
            protein = 0.0,
            fat = 0.0,
            carbs = 0.0,
            weight = "100g"
        )
        
        coEvery { mockFoodRepository.analyzeFoodDescription(any()) } returns Result.success(createTestFood())
        coEvery { mockFoodRepository.validateFoodData(match { it.name.isNotBlank() }) } returns Result.success(createTestFood())
        coEvery { mockFoodRepository.validateFoodData(match { it.name.isBlank() }) } returns Result.success(invalidFood)
        coEvery { mockFoodRepository.saveFoodIntake(any(), any()) } returns Result.success(Unit)
        coEvery { mockFoodRepository.getFoodHistory(any()) } returns Result.success(emptyList())
        coEvery { mockFoodRepository.searchFoodByName(any()) } returns Result.success(emptyList())
        coEvery { mockFoodRepository.getRecentFoods(any()) } returns Result.success(emptyList())
        
        val result = validator.validateFoodRepository()
        
        // Should detect validation logic error
        val validationErrors = result.issues.filter { it.type == "VALIDATION_LOGIC_ERROR" }
        assertTrue("Should detect validation logic error", validationErrors.isNotEmpty())
    }
    
    @Test
    fun `validateUserRepository should test all user operations`() = runTest {
        val testUser = createTestUser()
        val testTargets = NutritionTargets(2000, 150, 65, 250)
        
        // Mock successful responses
        coEvery { mockUserRepository.getUserProfile() } returns Result.success(testUser)
        coEvery { mockUserRepository.saveUserProfile(any()) } returns Result.success(Unit)
        coEvery { mockUserRepository.calculateNutritionTargets(any()) } returns Result.success(testTargets)
        coEvery { mockUserRepository.validateUserProfile(any()) } returns Result.success(testUser)
        coEvery { mockUserRepository.isSetupComplete() } returns Result.success(true)
        coEvery { mockUserRepository.markSetupComplete() } returns Result.success(Unit)
        
        val result = validator.validateUserRepository()
        
        assertNotNull(result)
        assertEquals("UserRepository", result.validatorName)
        
        // Verify all methods were called
        coVerify { mockUserRepository.getUserProfile() }
        coVerify { mockUserRepository.saveUserProfile(any()) }
        coVerify { mockUserRepository.calculateNutritionTargets(any()) }
        coVerify { mockUserRepository.validateUserProfile(any()) }
        coVerify { mockUserRepository.isSetupComplete() }
        coVerify { mockUserRepository.markSetupComplete() }
    }
    
    @Test
    fun `validateUserRepository should detect invalid calorie calculations`() = runTest {
        val testUser = createTestUser()
        val invalidTargets = NutritionTargets(0, 0, 0, 0) // Invalid: zero calories
        
        coEvery { mockUserRepository.getUserProfile() } returns Result.success(testUser)
        coEvery { mockUserRepository.saveUserProfile(any()) } returns Result.success(Unit)
        coEvery { mockUserRepository.calculateNutritionTargets(any()) } returns Result.success(invalidTargets)
        coEvery { mockUserRepository.validateUserProfile(any()) } returns Result.success(testUser)
        coEvery { mockUserRepository.isSetupComplete() } returns Result.success(true)
        coEvery { mockUserRepository.markSetupComplete() } returns Result.success(Unit)
        
        val result = validator.validateUserRepository()
        
        // Should detect calculation error
        val calculationErrors = result.issues.filter { it.type == "CALCULATION_ERROR" }
        assertTrue("Should detect invalid calorie calculation", calculationErrors.isNotEmpty())
    }
    
    @Test
    fun `validateNutritionRepository should test all nutrition operations`() = runTest {
        val testIntake = createTestNutritionIntake()
        val testStatistics = NutritionStatistics(
            averageCalories = 2000.0,
            averageProtein = 150.0,
            averageFat = 65.0,
            averageCarbs = 250.0,
            totalDays = 30,
            daysWithData = 25,
            goalAchievementRate = 0.8
        )
        val testTrends = NutritionTrends(
            caloriesTrend = listOf(LocalDate.now() to 2000),
            proteinTrend = listOf(LocalDate.now() to 150.0),
            fatTrend = listOf(LocalDate.now() to 65.0),
            carbsTrend = listOf(LocalDate.now() to 250.0)
        )
        
        // Mock successful responses
        coEvery { mockNutritionRepository.getDailyIntake(any()) } returns Result.success(testIntake)
        coEvery { mockNutritionRepository.getWeeklyIntake(any()) } returns Result.success(listOf(testIntake))
        coEvery { mockNutritionRepository.getMonthlyIntake(any()) } returns Result.success(listOf(testIntake))
        coEvery { mockNutritionRepository.getNutritionStatistics(any()) } returns Result.success(testStatistics)
        coEvery { mockNutritionRepository.getNutritionTrends(any()) } returns Result.success(testTrends)
        coEvery { mockNutritionRepository.saveDailyIntake(any()) } returns Result.success(Unit)
        coEvery { mockNutritionRepository.clearDayData(any()) } returns Result.success(Unit)
        coEvery { mockNutritionRepository.exportNutritionData(any()) } returns Result.success("export data")
        
        val result = validator.validateNutritionRepository()
        
        assertNotNull(result)
        assertEquals("NutritionRepository", result.validatorName)
        
        // Verify key methods were called
        coVerify { mockNutritionRepository.getDailyIntake(any()) }
        coVerify { mockNutritionRepository.getWeeklyIntake(any()) }
        coVerify { mockNutritionRepository.getMonthlyIntake(any()) }
        coVerify { mockNutritionRepository.getNutritionStatistics(any()) }
        coVerify { mockNutritionRepository.getNutritionTrends(any()) }
    }
    
    @Test
    fun `validateAllRepositories should aggregate all repository issues`() = runTest {
        // Mock all repositories to return success
        setupSuccessfulMocks()
        
        val result = validator.validateAllRepositories()
        
        assertNotNull(result)
        assertEquals("RepositoryValidator", result.validatorName)
        assertTrue(result.summary.contains("Comprehensive validation"))
        
        // Should have tested all repositories
        coVerify { mockFoodRepository.analyzeFoodDescription(any()) }
        coVerify { mockUserRepository.getUserProfile() }
        coVerify { mockNutritionRepository.getDailyIntake(any()) }
    }
    
    @Test
    fun `validation should handle repository exceptions gracefully`() = runTest {
        // Mock repositories to throw exceptions
        coEvery { mockFoodRepository.analyzeFoodDescription(any()) } throws RuntimeException("Test exception")
        coEvery { mockUserRepository.getUserProfile() } throws RuntimeException("Test exception")
        coEvery { mockNutritionRepository.getDailyIntake(any()) } throws RuntimeException("Test exception")
        
        val result = validator.validateAllRepositories()
        
        assertNotNull(result)
        
        // Should have exception issues
        val exceptionIssues = result.issues.filter { 
            it.type == "REPOSITORY_EXCEPTION" || it.type == "REPOSITORY_METHOD_EXCEPTION" 
        }
        assertTrue("Should detect repository exceptions", exceptionIssues.isNotEmpty())
    }
    
    @Test
    fun `validation should detect error results from repositories`() = runTest {
        // Mock repositories to return errors
        coEvery { mockFoodRepository.analyzeFoodDescription(any()) } returns Result.error(RuntimeException("Test error"))
        coEvery { mockUserRepository.getUserProfile() } returns Result.error(RuntimeException("Test error"))
        coEvery { mockNutritionRepository.getDailyIntake(any()) } returns Result.error(RuntimeException("Test error"))
        
        // Mock other methods to avoid exceptions
        setupPartialSuccessfulMocks()
        
        val result = validator.validateAllRepositories()
        
        // Should detect error results
        val errorResults = result.issues.filter { it.type == "REPOSITORY_ERROR_RESULT" }
        assertTrue("Should detect error results from repositories", errorResults.isNotEmpty())
    }
    
    private fun setupSuccessfulMocks() {
        val testFood = createTestFood()
        val testUser = createTestUser()
        val testIntake = createTestNutritionIntake()
        val testTargets = NutritionTargets(2000, 150, 65, 250)
        val testStatistics = NutritionStatistics(2000.0, 150.0, 65.0, 250.0, 30, 25, 0.8)
        val testTrends = NutritionTrends(emptyList(), emptyList(), emptyList(), emptyList())
        
        // Food repository mocks
        coEvery { mockFoodRepository.analyzeFoodDescription(any()) } returns Result.success(testFood)
        coEvery { mockFoodRepository.validateFoodData(any()) } returns Result.success(testFood)
        coEvery { mockFoodRepository.saveFoodIntake(any(), any()) } returns Result.success(Unit)
        coEvery { mockFoodRepository.getFoodHistory(any()) } returns Result.success(listOf(testFood))
        coEvery { mockFoodRepository.searchFoodByName(any()) } returns Result.success(listOf(testFood))
        coEvery { mockFoodRepository.getRecentFoods(any()) } returns Result.success(listOf(testFood))
        
        // User repository mocks
        coEvery { mockUserRepository.getUserProfile() } returns Result.success(testUser)
        coEvery { mockUserRepository.saveUserProfile(any()) } returns Result.success(Unit)
        coEvery { mockUserRepository.calculateNutritionTargets(any()) } returns Result.success(testTargets)
        coEvery { mockUserRepository.validateUserProfile(any()) } returns Result.success(testUser)
        coEvery { mockUserRepository.isSetupComplete() } returns Result.success(true)
        coEvery { mockUserRepository.markSetupComplete() } returns Result.success(Unit)
        
        // Nutrition repository mocks
        coEvery { mockNutritionRepository.getDailyIntake(any()) } returns Result.success(testIntake)
        coEvery { mockNutritionRepository.getWeeklyIntake(any()) } returns Result.success(listOf(testIntake))
        coEvery { mockNutritionRepository.getMonthlyIntake(any()) } returns Result.success(listOf(testIntake))
        coEvery { mockNutritionRepository.getNutritionStatistics(any()) } returns Result.success(testStatistics)
        coEvery { mockNutritionRepository.getNutritionTrends(any()) } returns Result.success(testTrends)
        coEvery { mockNutritionRepository.saveDailyIntake(any()) } returns Result.success(Unit)
        coEvery { mockNutritionRepository.clearDayData(any()) } returns Result.success(Unit)
        coEvery { mockNutritionRepository.exportNutritionData(any()) } returns Result.success("export")
    }
    
    private fun setupPartialSuccessfulMocks() {
        val testFood = createTestFood()
        val testUser = createTestUser()
        val testIntake = createTestNutritionIntake()
        
        // Mock some methods to avoid exceptions in other validations
        coEvery { mockFoodRepository.validateFoodData(any()) } returns Result.success(testFood)
        coEvery { mockFoodRepository.saveFoodIntake(any(), any()) } returns Result.success(Unit)
        coEvery { mockFoodRepository.getFoodHistory(any()) } returns Result.success(emptyList())
        coEvery { mockFoodRepository.searchFoodByName(any()) } returns Result.success(emptyList())
        coEvery { mockFoodRepository.getRecentFoods(any()) } returns Result.success(emptyList())
        
        coEvery { mockUserRepository.saveUserProfile(any()) } returns Result.success(Unit)
        coEvery { mockUserRepository.calculateNutritionTargets(any()) } returns Result.success(NutritionTargets(2000, 150, 65, 250))
        coEvery { mockUserRepository.validateUserProfile(any()) } returns Result.success(testUser)
        coEvery { mockUserRepository.isSetupComplete() } returns Result.success(true)
        coEvery { mockUserRepository.markSetupComplete() } returns Result.success(Unit)
        
        coEvery { mockNutritionRepository.getWeeklyIntake(any()) } returns Result.success(listOf(testIntake))
        coEvery { mockNutritionRepository.getMonthlyIntake(any()) } returns Result.success(listOf(testIntake))
        coEvery { mockNutritionRepository.getNutritionStatistics(any()) } returns Result.success(
            NutritionStatistics(2000.0, 150.0, 65.0, 250.0, 30, 25, 0.8)
        )
        coEvery { mockNutritionRepository.getNutritionTrends(any()) } returns Result.success(
            NutritionTrends(emptyList(), emptyList(), emptyList(), emptyList())
        )
        coEvery { mockNutritionRepository.saveDailyIntake(any()) } returns Result.success(Unit)
        coEvery { mockNutritionRepository.clearDayData(any()) } returns Result.success(Unit)
        coEvery { mockNutritionRepository.exportNutritionData(any()) } returns Result.success("export")
    }
    
    private fun createTestFood(): Food {
        return Food(
            name = "Test Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "150g",
            source = FoodSource.MANUAL_INPUT
        )
    }
    
    private fun createTestUser(): User {
        return User(
            name = "Test User",
            birthday = "1990-01-01",
            height = 175,
            weight = 70,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            bodyFeeling = "Good",
            goal = NutritionGoal.MAINTAIN_WEIGHT,
            nutritionTargets = NutritionTargets(2000, 150, 65, 250),
            isSetupComplete = true
        )
    }
    
    private fun createTestNutritionIntake(): NutritionIntake {
        return NutritionIntake(
            date = LocalDate.now(),
            meals = emptyList(),
            targets = NutritionTargets(2000, 150, 65, 250)
        )
    }
}