package com.example.calorietracker.presentation.viewmodels

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.common.FoodSource
import com.example.calorietracker.domain.entities.common.MealType
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.usecases.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CalorieTrackerViewModelTest {
    
    private lateinit var analyzeFoodPhotoUseCase: AnalyzeFoodPhotoUseCase
    private lateinit var analyzeFoodDescriptionUseCase: AnalyzeFoodDescriptionUseCase
    private lateinit var saveFoodIntakeUseCase: SaveFoodIntakeUseCase
    private lateinit var getDailyIntakeUseCase: GetDailyIntakeUseCase
    private lateinit var getFoodHistoryUseCase: GetFoodHistoryUseCase
    private lateinit var viewModel: CalorieTrackerViewModel
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        analyzeFoodPhotoUseCase = mockk()
        analyzeFoodDescriptionUseCase = mockk()
        saveFoodIntakeUseCase = mockk()
        getDailyIntakeUseCase = mockk()
        getFoodHistoryUseCase = mockk()
        
        viewModel = CalorieTrackerViewModel(
            analyzeFoodPhotoUseCase,
            analyzeFoodDescriptionUseCase,
            saveFoodIntakeUseCase,
            getDailyIntakeUseCase,
            getFoodHistoryUseCase
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `analyze food photo successfully updates analyzed food state`() = runTest {
        // Given
        val photoPath = "/path/to/photo.jpg"
        val caption = "Delicious apple"
        val analyzedFood = Food(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = FoodSource.AI_PHOTO_ANALYSIS
        )
        
        coEvery { 
            analyzeFoodPhotoUseCase(any()) 
        } returns Result.success(analyzedFood)
        
        // When
        viewModel.analyzeFoodPhoto(photoPath, caption)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isAnalyzing)
        assertNull(uiState.error)
        
        val currentFood = viewModel.analyzedFood.first()
        assertEquals(analyzedFood, currentFood)
        
        coVerify { analyzeFoodPhotoUseCase(any()) }
    }
    
    @Test
    fun `analyze food photo failure updates error state`() = runTest {
        // Given
        val photoPath = "/path/to/photo.jpg"
        val caption = "caption"
        val error = DomainException.AIAnalysisException("Analysis failed")
        
        coEvery { 
            analyzeFoodPhotoUseCase(any()) 
        } returns Result.error(error)
        
        // When
        viewModel.analyzeFoodPhoto(photoPath, caption)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isAnalyzing)
        assertEquals("Failed to analyze photo: Analysis failed", uiState.error)
        
        val currentFood = viewModel.analyzedFood.first()
        assertNull(currentFood)
    }
    
    @Test
    fun `analyze food description successfully updates analyzed food state`() = runTest {
        // Given
        val description = "One medium banana"
        val analyzedFood = Food(
            name = "Banana",
            calories = 105,
            protein = 1.3,
            fat = 0.4,
            carbs = 27.0,
            weight = "100г",
            source = FoodSource.AI_TEXT_ANALYSIS
        )
        
        coEvery { 
            analyzeFoodDescriptionUseCase(any()) 
        } returns Result.success(analyzedFood)
        
        // When
        viewModel.analyzeFoodDescription(description)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isAnalyzing)
        assertNull(uiState.error)
        
        val currentFood = viewModel.analyzedFood.first()
        assertEquals(analyzedFood, currentFood)
        
        coVerify { analyzeFoodDescriptionUseCase(any()) }
    }
    
    @Test
    fun `save food intake successfully updates success state`() = runTest {
        // Given
        val food = Food(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        val mealType = MealType.BREAKFAST
        val date = LocalDate.now()
        
        coEvery { 
            saveFoodIntakeUseCase(any()) 
        } returns Result.success(Unit)
        
        // When
        viewModel.saveFoodIntake(food, mealType, date)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isSaving)
        assertEquals("Food saved successfully", uiState.successMessage)
        assertNull(uiState.error)
        
        coVerify { saveFoodIntakeUseCase(any()) }
    }
    
    @Test
    fun `save food intake failure updates error state`() = runTest {
        // Given
        val food = Food(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        val mealType = MealType.BREAKFAST
        val date = LocalDate.now()
        val error = DomainException.StorageException("Save failed")
        
        coEvery { 
            saveFoodIntakeUseCase(any()) 
        } returns Result.error(error)
        
        // When
        viewModel.saveFoodIntake(food, mealType, date)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isSaving)
        assertEquals("Failed to save food: Save failed", uiState.error)
        assertNull(uiState.successMessage)
    }
    
    @Test
    fun `load daily intake successfully updates daily intake state`() = runTest {
        // Given
        val date = LocalDate.now()
        val dailyIntake = NutritionIntake(
            date = date,
            meals = emptyList(),
            totalCalories = 1500,
            totalProteins = 120.0,
            totalFats = 50.0,
            totalCarbs = 180.0
        )
        
        coEvery { 
            getDailyIntakeUseCase(any()) 
        } returns Result.success(dailyIntake)
        
        // When
        viewModel.loadDailyIntake(date)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        
        val currentIntake = viewModel.dailyIntake.first()
        assertEquals(dailyIntake, currentIntake)
        
        coVerify { getDailyIntakeUseCase(any()) }
    }
    
    @Test
    fun `load daily intake failure updates error state`() = runTest {
        // Given
        val date = LocalDate.now()
        val error = DomainException.StorageException("Load failed")
        
        coEvery { 
            getDailyIntakeUseCase(any()) 
        } returns Result.error(error)
        
        // When
        viewModel.loadDailyIntake(date)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertEquals("Failed to load daily intake: Load failed", uiState.error)
        
        val currentIntake = viewModel.dailyIntake.first()
        assertNull(currentIntake)
    }
    
    @Test
    fun `load food history successfully updates food history state`() = runTest {
        // Given
        val limit = 10
        val foodHistory = listOf(
            Food("Apple", 95, 0.5, 0.3, 25.0, "100г", FoodSource.MANUAL_INPUT),
            Food("Banana", 105, 1.3, 0.4, 27.0, "100г", FoodSource.AI_PHOTO_ANALYSIS)
        )
        
        coEvery { 
            getFoodHistoryUseCase(any()) 
        } returns Result.success(foodHistory)
        
        // When
        viewModel.loadFoodHistory(limit)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        
        val currentHistory = viewModel.foodHistory.first()
        assertEquals(foodHistory, currentHistory)
        
        coVerify { getFoodHistoryUseCase(any()) }
    }
    
    @Test
    fun `update analyzed food updates state correctly`() = runTest {
        // Given
        val originalFood = Food(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = FoodSource.AI_PHOTO_ANALYSIS
        )
        val updatedFood = originalFood.copy(calories = 100)
        
        // When
        viewModel.updateAnalyzedFood(updatedFood)
        
        // Then
        val currentFood = viewModel.analyzedFood.first()
        assertEquals(updatedFood, currentFood)
        assertEquals(100, currentFood?.calories)
    }
    
    @Test
    fun `clear analyzed food removes food from state`() = runTest {
        // Given - set a food first
        val food = Food(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        viewModel.updateAnalyzedFood(food)
        
        // Verify food is set
        var currentFood = viewModel.analyzedFood.first()
        assertNotNull(currentFood)
        
        // When
        viewModel.clearAnalyzedFood()
        
        // Then
        currentFood = viewModel.analyzedFood.first()
        assertNull(currentFood)
    }
    
    @Test
    fun `clear error removes error from state`() = runTest {
        // Given - set an error first
        val error = DomainException.ValidationException("Test error")
        coEvery { getDailyIntakeUseCase(any()) } returns Result.error(error)
        viewModel.loadDailyIntake(LocalDate.now())
        
        // Verify error is set
        var uiState = viewModel.uiState.first()
        assertNotNull(uiState.error)
        
        // When
        viewModel.clearError()
        
        // Then
        uiState = viewModel.uiState.first()
        assertNull(uiState.error)
    }
    
    @Test
    fun `clear success message removes message from state`() = runTest {
        // Given - set a success message first
        val food = Food("Apple", 95, 0.5, 0.3, 25.0, "100г", FoodSource.MANUAL_INPUT)
        coEvery { saveFoodIntakeUseCase(any()) } returns Result.success(Unit)
        viewModel.saveFoodIntake(food, MealType.BREAKFAST, LocalDate.now())
        
        // Verify success message is set
        var uiState = viewModel.uiState.first()
        assertNotNull(uiState.successMessage)
        
        // When
        viewModel.clearSuccessMessage()
        
        // Then
        uiState = viewModel.uiState.first()
        assertNull(uiState.successMessage)
    }
    
    @Test
    fun `get total calories from daily intake returns correct value`() = runTest {
        // Given
        val dailyIntake = NutritionIntake(
            date = LocalDate.now(),
            meals = emptyList(),
            totalCalories = 1500,
            totalProteins = 120.0,
            totalFats = 50.0,
            totalCarbs = 180.0
        )
        
        coEvery { getDailyIntakeUseCase(any()) } returns Result.success(dailyIntake)
        viewModel.loadDailyIntake(LocalDate.now())
        
        // When
        val totalCalories = viewModel.getTotalCalories()
        
        // Then
        assertEquals(1500, totalCalories)
    }
    
    @Test
    fun `get total calories with no daily intake returns zero`() = runTest {
        // When
        val totalCalories = viewModel.getTotalCalories()
        
        // Then
        assertEquals(0, totalCalories)
    }
}