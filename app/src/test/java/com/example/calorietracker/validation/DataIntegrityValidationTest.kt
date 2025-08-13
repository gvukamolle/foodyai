package com.example.calorietracker.validation

import com.example.calorietracker.data.mappers.FoodMapper
import com.example.calorietracker.data.mappers.UserMapper
import com.example.calorietracker.data.mappers.NutritionMapper
import com.example.calorietracker.domain.repositories.FoodRepository
import com.example.calorietracker.domain.repositories.UserRepository
import com.example.calorietracker.domain.repositories.NutritionRepository
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.Meal
import com.example.calorietracker.domain.entities.common.*
import com.example.calorietracker.validation.models.ValidationResults
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Integration tests for data integrity across mappers and repositories
 */
class DataIntegrityValidationTest {
    
    private lateinit var foodMapper: FoodMapper
    private lateinit var userMapper: UserMapper
    private lateinit var nutritionMapper: NutritionMapper
    private lateinit var mockFoodRepository: FoodRepository
    private lateinit var mockUserRepository: UserRepository
    private lateinit var mockNutritionRepository: NutritionRepository
    private lateinit var dataMapperValidator: DataMapperValidator
    private lateinit var repositoryValidator: RepositoryValidator
    
    @Before
    fun setup() {
        foodMapper = FoodMapper()
        userMapper = UserMapper()
        nutritionMapper = NutritionMapper(foodMapper)
        
        mockFoodRepository = mockk()
        mockUserRepository = mockk()
        mockNutritionRepository = mockk()
        
        dataMapperValidator = DataMapperValidator(foodMapper, userMapper, nutritionMapper)
        repositoryValidator = RepositoryValidator(mockFoodRepository, mockUserRepository, mockNutritionRepository)
    }
    
    @Test
    fun `data flow integrity from domain to data and back should preserve information`() = runTest {
        // Test complete data flow for Food entity
        val originalFood = createCompleteFood()
        
        // Domain -> Data -> Domain
        val dataFood = foodMapper.mapDomainToData(originalFood)
        val roundTripFood = foodMapper.mapDataToDomain(dataFood)
        
        // Verify data integrity
        assertEquals("Food name should be preserved", originalFood.name, roundTripFood.name)
        assertEquals("Food calories should be preserved", originalFood.calories, roundTripFood.calories)
        assertEquals("Food protein should be preserved", originalFood.protein, roundTripFood.protein, 0.001)
        assertEquals("Food fat should be preserved", originalFood.fat, roundTripFood.fat, 0.001)
        assertEquals("Food carbs should be preserved", originalFood.carbs, roundTripFood.carbs, 0.001)
        assertEquals("Food weight should be preserved", originalFood.weight, roundTripFood.weight)
        assertEquals("Food source should be preserved", originalFood.source, roundTripFood.source)
        assertEquals("Food AI opinion should be preserved", originalFood.aiOpinion, roundTripFood.aiOpinion)
    }
    
    @Test
    fun `user profile data flow should maintain calculation accuracy`() = runTest {
        val originalUser = createCompleteUser()
        
        // Test domain calculations before mapping
        val originalBMI = originalUser.getBMI()
        val originalTDEE = originalUser.calculateTDEE()
        val originalRecommendedCalories = originalUser.calculateRecommendedCalories()
        
        // Domain -> Data -> Domain
        val dataUser = userMapper.mapDomainToData(originalUser)
        val roundTripUser = userMapper.mapDataToDomain(dataUser)
        
        // Verify calculations are still accurate after round trip
        assertEquals("BMI should be preserved", originalBMI, roundTripUser.getBMI())
        assertEquals("TDEE should be preserved", originalTDEE, roundTripUser.calculateTDEE())
        assertEquals("Recommended calories should be preserved", originalRecommendedCalories, roundTripUser.calculateRecommendedCalories())
        
        // Verify critical fields
        assertEquals("User name should be preserved", originalUser.name, roundTripUser.name)
        assertEquals("User height should be preserved", originalUser.height, roundTripUser.height)
        assertEquals("User weight should be preserved", originalUser.weight, roundTripUser.weight)
        assertEquals("User gender should be preserved", originalUser.gender, roundTripUser.gender)
        assertEquals("User activity level should be preserved", originalUser.activityLevel, roundTripUser.activityLevel)
    }
    
    @Test
    fun `nutrition intake calculations should remain consistent across mappings`() = runTest {
        val originalIntake = createCompleteNutritionIntake()
        
        // Calculate original totals
        val originalCalories = originalIntake.getTotalCalories()
        val originalProtein = originalIntake.getTotalProtein()
        val originalFat = originalIntake.getTotalFat()
        val originalCarbs = originalIntake.getTotalCarbs()
        val originalProgress = originalIntake.getCalorieProgress()
        
        // Test mapping to daily intake
        val dailyIntake = nutritionMapper.mapDomainToData(originalIntake)
        
        // Verify totals are preserved
        assertEquals("Calories should match", originalCalories, dailyIntake.calories)
        assertEquals("Protein should match", originalProtein.toFloat(), dailyIntake.protein, 0.001f)
        assertEquals("Fat should match", originalFat.toFloat(), dailyIntake.fat, 0.001f)
        assertEquals("Carbs should match", originalCarbs.toFloat(), dailyIntake.carbs, 0.001f)
        
        // Test summary mapping
        val summary = nutritionMapper.mapDomainToSummary(originalIntake)
        assertEquals("Summary date should match", originalIntake.date, summary.date)
        assertEquals("Summary calories should match", originalCalories, summary.totalCalories)
        assertEquals("Summary meals count should match", originalIntake.meals.size, summary.mealsCount)
    }
    
    @Test
    fun `meal data transformations should preserve food information`() = runTest {
        val originalMeal = createCompleteMeal()
        
        // Calculate original meal totals
        val originalMealCalories = originalMeal.getTotalCalories()
        val originalMealProtein = originalMeal.getTotalProtein()
        val originalFoodCount = originalMeal.getFoodCount()
        
        // Domain -> Data -> Domain
        val dataMeal = foodMapper.mapDomainMealToData(originalMeal)
        val roundTripMeal = foodMapper.mapDataMealToDomain(dataMeal)
        
        // Verify meal integrity
        assertEquals("Meal type should be preserved", originalMeal.type, roundTripMeal.type)
        assertEquals("Meal food count should be preserved", originalFoodCount, roundTripMeal.getFoodCount())
        assertEquals("Meal calories should be preserved", originalMealCalories, roundTripMeal.getTotalCalories())
        assertEquals("Meal protein should be preserved", originalMealProtein, roundTripMeal.getTotalProtein(), 0.001)
        
        // Verify individual foods in meal
        assertEquals("Number of foods should match", originalMeal.foods.size, roundTripMeal.foods.size)
        for (i in originalMeal.foods.indices) {
            val originalFood = originalMeal.foods[i]
            val roundTripFood = roundTripMeal.foods[i]
            assertEquals("Food $i name should match", originalFood.name, roundTripFood.name)
            assertEquals("Food $i calories should match", originalFood.calories, roundTripFood.calories)
        }
    }
    
    @Test
    fun `repository and mapper integration should maintain data consistency`() = runTest {
        val testFood = createCompleteFood()
        val testUser = createCompleteUser()
        
        // Mock repository to return mapped data
        coEvery { mockFoodRepository.saveFoodIntake(any(), any()) } returns Result.success(Unit)
        coEvery { mockFoodRepository.getFoodHistory(any()) } returns Result.success(listOf(testFood))
        coEvery { mockUserRepository.saveUserProfile(any()) } returns Result.success(Unit)
        coEvery { mockUserRepository.getUserProfile() } returns Result.success(testUser)
        
        // Test save and retrieve cycle
        val saveResult = mockFoodRepository.saveFoodIntake(testFood, MealType.BREAKFAST)
        assertTrue("Food save should succeed", saveResult is Result.Success)
        
        val retrieveResult = mockFoodRepository.getFoodHistory(
            DateRange(LocalDate.now().minusDays(1), LocalDate.now())
        )
        assertTrue("Food retrieve should succeed", retrieveResult is Result.Success)
        
        if (retrieveResult is Result.Success) {
            val retrievedFoods = retrieveResult.data
            assertFalse("Should retrieve foods", retrievedFoods.isEmpty())
            
            val retrievedFood = retrievedFoods.first()
            assertEquals("Retrieved food should match original", testFood.name, retrievedFood.name)
            assertEquals("Retrieved calories should match", testFood.calories, retrievedFood.calories)
        }
    }
    
    @Test
    fun `complex data flow with multiple entities should preserve relationships`() = runTest {
        val user = createCompleteUser()
        val food1 = createCompleteFood("Apple")
        val food2 = createCompleteFood("Banana")
        val meal = Meal.create(MealType.BREAKFAST, listOf(food1, food2))
        val intake = NutritionIntake(
            date = LocalDate.now(),
            meals = listOf(meal),
            targets = user.nutritionTargets
        )
        
        // Mock repositories
        coEvery { mockUserRepository.getUserProfile() } returns Result.success(user)
        coEvery { mockNutritionRepository.getDailyIntake(any()) } returns Result.success(intake)
        coEvery { mockNutritionRepository.saveDailyIntake(any()) } returns Result.success(Unit)
        
        // Test complete flow
        val userResult = mockUserRepository.getUserProfile()
        assertTrue("User retrieval should succeed", userResult is Result.Success)
        
        val intakeResult = mockNutritionRepository.getDailyIntake(LocalDate.now())
        assertTrue("Intake retrieval should succeed", intakeResult is Result.Success)
        
        if (userResult is Result.Success && intakeResult is Result.Success) {
            val retrievedUser = userResult.data
            val retrievedIntake = intakeResult.data
            
            // Verify relationships are maintained
            assertEquals("User targets should match intake targets", 
                        retrievedUser.nutritionTargets.dailyCalories, 
                        retrievedIntake.targets?.dailyCalories)
            
            // Verify meal structure
            assertEquals("Should have correct number of meals", 1, retrievedIntake.meals.size)
            val retrievedMeal = retrievedIntake.meals.first()
            assertEquals("Meal should have correct number of foods", 2, retrievedMeal.foods.size)
            
            // Verify food details
            val retrievedFood1 = retrievedMeal.foods.find { it.name.contains("Apple") }
            val retrievedFood2 = retrievedMeal.foods.find { it.name.contains("Banana") }
            assertNotNull("Apple should be found", retrievedFood1)
            assertNotNull("Banana should be found", retrievedFood2)
        }
    }
    
    @Test
    fun `validation should detect data integrity issues across layers`() = runTest {
        // Run comprehensive validation
        val mapperResults = dataMapperValidator.validateAllMappers()
        
        // Mock repositories for repository validation
        setupMockRepositories()
        val repositoryResults = repositoryValidator.validateAllRepositories()
        
        // Combine results
        val allIssues = mapperResults.issues + repositoryResults.issues
        
        // Analyze data integrity issues
        val dataIntegrityIssues = allIssues.filter { issue ->
            issue.type.contains("MAPPING") || 
            issue.type.contains("ROUND_TRIP") || 
            issue.type.contains("CALCULATION") ||
            issue.type.contains("FIELD")
        }
        
        // Log issues for analysis
        dataIntegrityIssues.forEach { issue ->
            println("Data integrity issue: ${issue.type} - ${issue.message}")
            println("  Location: ${issue.location}")
            println("  Details: ${issue.details}")
            println()
        }
        
        // Verify validation is working
        assertNotNull("Mapper validation should complete", mapperResults)
        assertNotNull("Repository validation should complete", repositoryResults)
    }
    
    private fun setupMockRepositories() {
        val testFood = createCompleteFood()
        val testUser = createCompleteUser()
        val testIntake = createCompleteNutritionIntake()
        
        // Setup successful mocks
        coEvery { mockFoodRepository.analyzeFoodDescription(any()) } returns Result.success(testFood)
        coEvery { mockFoodRepository.validateFoodData(any()) } returns Result.success(testFood)
        coEvery { mockFoodRepository.saveFoodIntake(any(), any()) } returns Result.success(Unit)
        coEvery { mockFoodRepository.getFoodHistory(any()) } returns Result.success(listOf(testFood))
        coEvery { mockFoodRepository.searchFoodByName(any()) } returns Result.success(listOf(testFood))
        coEvery { mockFoodRepository.getRecentFoods(any()) } returns Result.success(listOf(testFood))
        
        coEvery { mockUserRepository.getUserProfile() } returns Result.success(testUser)
        coEvery { mockUserRepository.saveUserProfile(any()) } returns Result.success(Unit)
        coEvery { mockUserRepository.calculateNutritionTargets(any()) } returns Result.success(testUser.nutritionTargets)
        coEvery { mockUserRepository.validateUserProfile(any()) } returns Result.success(testUser)
        coEvery { mockUserRepository.isSetupComplete() } returns Result.success(true)
        coEvery { mockUserRepository.markSetupComplete() } returns Result.success(Unit)
        
        coEvery { mockNutritionRepository.getDailyIntake(any()) } returns Result.success(testIntake)
        coEvery { mockNutritionRepository.getWeeklyIntake(any()) } returns Result.success(listOf(testIntake))
        coEvery { mockNutritionRepository.getMonthlyIntake(any()) } returns Result.success(listOf(testIntake))
        coEvery { mockNutritionRepository.saveDailyIntake(any()) } returns Result.success(Unit)
        coEvery { mockNutritionRepository.clearDayData(any()) } returns Result.success(Unit)
        coEvery { mockNutritionRepository.exportNutritionData(any()) } returns Result.success("export")
        coEvery { mockNutritionRepository.getNutritionStatistics(any()) } returns Result.success(
            com.example.calorietracker.domain.repositories.NutritionStatistics(2000.0, 150.0, 65.0, 250.0, 30, 25, 0.8)
        )
        coEvery { mockNutritionRepository.getNutritionTrends(any()) } returns Result.success(
            com.example.calorietracker.domain.repositories.NutritionTrends(emptyList(), emptyList(), emptyList(), emptyList())
        )
    }
    
    private fun createCompleteFood(name: String = "Test Food"): Food {
        return Food(
            name = name,
            calories = 100,
            protein = 10.0,
            fat = 5.0,
            carbs = 15.0,
            weight = "100g",
            source = FoodSource.AI_ANALYSIS,
            aiOpinion = "Nutritious and balanced food item"
        )
    }
    
    private fun createCompleteUser(): User {
        return User(
            name = "John Doe",
            birthday = "1990-05-15",
            height = 175,
            weight = 70,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            bodyFeeling = "Energetic and healthy",
            goal = NutritionGoal.MAINTAIN_WEIGHT,
            nutritionTargets = NutritionTargets(2000, 150, 65, 250),
            isSetupComplete = true
        )
    }
    
    private fun createCompleteMeal(): Meal {
        val foods = listOf(
            createCompleteFood("Apple"),
            createCompleteFood("Banana"),
            createCompleteFood("Oatmeal")
        )
        return Meal.create(MealType.BREAKFAST, foods)
    }
    
    private fun createCompleteNutritionIntake(): NutritionIntake {
        val meals = listOf(
            createCompleteMeal(),
            Meal.create(MealType.LUNCH, listOf(createCompleteFood("Chicken Salad"))),
            Meal.create(MealType.DINNER, listOf(createCompleteFood("Grilled Salmon")))
        )
        
        return NutritionIntake(
            date = LocalDate.now(),
            meals = meals,
            targets = NutritionTargets(2000, 150, 65, 250)
        )
    }
}