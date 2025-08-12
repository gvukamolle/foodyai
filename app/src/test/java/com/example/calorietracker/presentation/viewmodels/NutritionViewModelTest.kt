package com.example.calorietracker.presentation.viewmodels

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.common.DateRange
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
class NutritionViewModelTest {
    
    private lateinit var getDailyIntakeUseCase: GetDailyIntakeUseCase
    private lateinit var getWeeklyIntakeUseCase: GetWeeklyIntakeUseCase
    private lateinit var getMonthlyIntakeUseCase: GetMonthlyIntakeUseCase
    private lateinit var calculateNutritionProgressUseCase: CalculateNutritionProgressUseCase
    private lateinit var viewModel: NutritionViewModel
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        getDailyIntakeUseCase = mockk()
        getWeeklyIntakeUseCase = mockk()
        getMonthlyIntakeUseCase = mockk()
        calculateNutritionProgressUseCase = mockk()
        
        viewModel = NutritionViewModel(
            getDailyIntakeUseCase,
            getWeeklyIntakeUseCase,
            getMonthlyIntakeUseCase,
            calculateNutritionProgressUseCase
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `load daily nutrition successfully updates daily intake state`() = runTest {
        // Given
        val date = LocalDate.now()
        val dailyIntake = NutritionIntake(
            date = date,
            meals = emptyList(),
            totalCalories = 1800,
            totalProteins = 140.0,
            totalFats = 60.0,
            totalCarbs = 220.0
        )
        
        coEvery { 
            getDailyIntakeUseCase(any()) 
        } returns Result.success(dailyIntake)
        
        // When
        viewModel.loadDailyNutrition(date)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        
        val currentIntake = viewModel.dailyIntake.first()
        assertEquals(dailyIntake, currentIntake)
        
        coVerify { getDailyIntakeUseCase(any()) }
    }
    
    @Test
    fun `load daily nutrition failure updates error state`() = runTest {
        // Given
        val date = LocalDate.now()
        val error = DomainException.StorageException("Database error")
        
        coEvery { 
            getDailyIntakeUseCase(any()) 
        } returns Result.error(error)
        
        // When
        viewModel.loadDailyNutrition(date)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertEquals("Failed to load daily nutrition: Database error", uiState.error)
        
        val currentIntake = viewModel.dailyIntake.first()
        assertNull(currentIntake)
    }
    
    @Test
    fun `load weekly nutrition successfully updates weekly intake state`() = runTest {
        // Given
        val startDate = LocalDate.now().minusDays(6)
        val endDate = LocalDate.now()
        val dateRange = DateRange(startDate, endDate)
        
        val weeklyIntakes = listOf(
            NutritionIntake(startDate, emptyList(), 1500, 120.0, 50.0, 180.0),
            NutritionIntake(startDate.plusDays(1), emptyList(), 1600, 130.0, 55.0, 190.0),
            NutritionIntake(endDate, emptyList(), 1700, 140.0, 60.0, 200.0)
        )
        
        coEvery { 
            getWeeklyIntakeUseCase(any()) 
        } returns Result.success(weeklyIntakes)
        
        // When
        viewModel.loadWeeklyNutrition(dateRange)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        
        val currentWeekly = viewModel.weeklyIntake.first()
        assertEquals(weeklyIntakes, currentWeekly)
        
        coVerify { getWeeklyIntakeUseCase(any()) }
    }
    
    @Test
    fun `load monthly nutrition successfully updates monthly intake state`() = runTest {
        // Given
        val startDate = LocalDate.now().minusDays(29)
        val endDate = LocalDate.now()
        val dateRange = DateRange(startDate, endDate)
        
        val monthlyIntakes = listOf(
            NutritionIntake(startDate, emptyList(), 1800, 150.0, 65.0, 220.0),
            NutritionIntake(startDate.plusDays(15), emptyList(), 1900, 160.0, 70.0, 230.0),
            NutritionIntake(endDate, emptyList(), 2000, 170.0, 75.0, 240.0)
        )
        
        coEvery { 
            getMonthlyIntakeUseCase(any()) 
        } returns Result.success(monthlyIntakes)
        
        // When
        viewModel.loadMonthlyNutrition(dateRange)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        
        val currentMonthly = viewModel.monthlyIntake.first()
        assertEquals(monthlyIntakes, currentMonthly)
        
        coVerify { getMonthlyIntakeUseCase(any()) }
    }
    
    @Test
    fun `calculate nutrition progress successfully updates progress state`() = runTest {
        // Given
        val dailyIntake = NutritionIntake(
            date = LocalDate.now(),
            meals = emptyList(),
            totalCalories = 1800,
            totalProteins = 140.0,
            totalFats = 60.0,
            totalCarbs = 220.0
        )
        val targetCalories = 2000
        val targetProteins = 150.0
        val targetFats = 65.0
        val targetCarbs = 250.0
        
        val expectedProgress = mapOf(
            "calories" to 0.9, // 1800/2000
            "proteins" to 0.93, // 140/150
            "fats" to 0.92, // 60/65
            "carbs" to 0.88 // 220/250
        )
        
        coEvery { 
            calculateNutritionProgressUseCase(any()) 
        } returns Result.success(expectedProgress)
        
        // When
        viewModel.calculateNutritionProgress(
            dailyIntake, targetCalories, targetProteins, targetFats, targetCarbs
        )
        
        // Then
        val progress = viewModel.nutritionProgress.first()
        assertEquals(expectedProgress, progress)
        
        coVerify { calculateNutritionProgressUseCase(any()) }
    }
    
    @Test
    fun `get average daily calories from weekly data returns correct value`() = runTest {
        // Given
        val weeklyIntakes = listOf(
            NutritionIntake(LocalDate.now().minusDays(2), emptyList(), 1500, 120.0, 50.0, 180.0),
            NutritionIntake(LocalDate.now().minusDays(1), emptyList(), 1700, 130.0, 55.0, 190.0),
            NutritionIntake(LocalDate.now(), emptyList(), 1800, 140.0, 60.0, 200.0)
        )
        
        coEvery { getWeeklyIntakeUseCase(any()) } returns Result.success(weeklyIntakes)
        viewModel.loadWeeklyNutrition(DateRange(LocalDate.now().minusDays(6), LocalDate.now()))
        
        // When
        val averageCalories = viewModel.getAverageDailyCalories()
        
        // Then
        assertEquals(1667, averageCalories) // (1500 + 1700 + 1800) / 3
    }
    
    @Test
    fun `get average daily calories with no data returns zero`() = runTest {
        // When
        val averageCalories = viewModel.getAverageDailyCalories()
        
        // Then
        assertEquals(0, averageCalories)
    }
    
    @Test
    fun `get total weekly calories returns correct sum`() = runTest {
        // Given
        val weeklyIntakes = listOf(
            NutritionIntake(LocalDate.now().minusDays(2), emptyList(), 1500, 120.0, 50.0, 180.0),
            NutritionIntake(LocalDate.now().minusDays(1), emptyList(), 1700, 130.0, 55.0, 190.0),
            NutritionIntake(LocalDate.now(), emptyList(), 1800, 140.0, 60.0, 200.0)
        )
        
        coEvery { getWeeklyIntakeUseCase(any()) } returns Result.success(weeklyIntakes)
        viewModel.loadWeeklyNutrition(DateRange(LocalDate.now().minusDays(6), LocalDate.now()))
        
        // When
        val totalCalories = viewModel.getTotalWeeklyCalories()
        
        // Then
        assertEquals(5000, totalCalories) // 1500 + 1700 + 1800
    }
    
    @Test
    fun `get nutrition trend shows increasing trend`() = runTest {
        // Given
        val weeklyIntakes = listOf(
            NutritionIntake(LocalDate.now().minusDays(6), emptyList(), 1400, 110.0, 45.0, 170.0),
            NutritionIntake(LocalDate.now().minusDays(5), emptyList(), 1500, 120.0, 50.0, 180.0),
            NutritionIntake(LocalDate.now().minusDays(4), emptyList(), 1600, 130.0, 55.0, 190.0),
            NutritionIntake(LocalDate.now().minusDays(3), emptyList(), 1700, 140.0, 60.0, 200.0),
            NutritionIntake(LocalDate.now().minusDays(2), emptyList(), 1800, 150.0, 65.0, 210.0),
            NutritionIntake(LocalDate.now().minusDays(1), emptyList(), 1900, 160.0, 70.0, 220.0),
            NutritionIntake(LocalDate.now(), emptyList(), 2000, 170.0, 75.0, 230.0)
        )
        
        coEvery { getWeeklyIntakeUseCase(any()) } returns Result.success(weeklyIntakes)
        viewModel.loadWeeklyNutrition(DateRange(LocalDate.now().minusDays(6), LocalDate.now()))
        
        // When
        val trend = viewModel.getNutritionTrend()
        
        // Then
        assertEquals("increasing", trend) // Calories are consistently increasing
    }
    
    @Test
    fun `clear error removes error from state`() = runTest {
        // Given - set an error first
        val error = DomainException.StorageException("Test error")
        coEvery { getDailyIntakeUseCase(any()) } returns Result.error(error)
        viewModel.loadDailyNutrition(LocalDate.now())
        
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
    fun `refresh data reloads current date nutrition`() = runTest {
        // Given
        val today = LocalDate.now()
        val dailyIntake = NutritionIntake(
            date = today,
            meals = emptyList(),
            totalCalories = 1500,
            totalProteins = 120.0,
            totalFats = 50.0,
            totalCarbs = 180.0
        )
        
        coEvery { getDailyIntakeUseCase(any()) } returns Result.success(dailyIntake)
        
        // When
        viewModel.refreshData()
        
        // Then
        val currentIntake = viewModel.dailyIntake.first()
        assertEquals(dailyIntake, currentIntake)
        
        coVerify { getDailyIntakeUseCase(GetDailyIntakeUseCase.Params(today)) }
    }
    
    @Test
    fun `get calories progress percentage returns correct value`() = runTest {
        // Given
        val progress = mapOf(
            "calories" to 0.75, // 75%
            "proteins" to 0.85,
            "fats" to 0.90,
            "carbs" to 0.80
        )
        
        coEvery { calculateNutritionProgressUseCase(any()) } returns Result.success(progress)
        
        val dailyIntake = NutritionIntake(
            LocalDate.now(), emptyList(), 1500, 120.0, 50.0, 180.0
        )
        viewModel.calculateNutritionProgress(dailyIntake, 2000, 150.0, 65.0, 250.0)
        
        // When
        val caloriesProgress = viewModel.getCaloriesProgressPercentage()
        
        // Then
        assertEquals(75, caloriesProgress) // 0.75 * 100
    }
    
    @Test
    fun `is nutrition goal met returns correct boolean`() = runTest {
        // Given
        val progress = mapOf(
            "calories" to 0.95, // 95% - close to goal
            "proteins" to 1.05, // 105% - over goal
            "fats" to 0.90,     // 90% - under goal
            "carbs" to 0.98     // 98% - close to goal
        )
        
        coEvery { calculateNutritionProgressUseCase(any()) } returns Result.success(progress)
        
        val dailyIntake = NutritionIntake(
            LocalDate.now(), emptyList(), 1900, 157.0, 58.0, 245.0
        )
        viewModel.calculateNutritionProgress(dailyIntake, 2000, 150.0, 65.0, 250.0)
        
        // When
        val isGoalMet = viewModel.isNutritionGoalMet(0.90) // 90% threshold
        
        // Then
        assertTrue(isGoalMet) // All macros are above 90%
    }
}