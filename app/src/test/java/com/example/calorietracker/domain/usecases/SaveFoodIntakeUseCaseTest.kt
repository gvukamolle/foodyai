package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.common.FoodSource
import com.example.calorietracker.domain.entities.common.MealType
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.FoodRepository
import com.example.calorietracker.domain.repositories.NutritionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class SaveFoodIntakeUseCaseTest {
    
    private lateinit var foodRepository: FoodRepository
    private lateinit var nutritionRepository: NutritionRepository
    private lateinit var useCase: SaveFoodIntakeUseCase
    
    @Before
    fun setup() {
        foodRepository = mockk()
        nutritionRepository = mockk()
        useCase = SaveFoodIntakeUseCase(foodRepository, nutritionRepository)
    }
    
    @Test
    fun `save food intake successfully`() = runTest {
        // Given
        val food = Food(
            name = "Apple",
            calories = 80,
            protein = 0.3,
            fat = 0.2,
            carbs = 21.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        val mealType = MealType.BREAKFAST
        val date = LocalDate.now()
        
        coEvery { foodRepository.validateFoodData(food) } returns Result.success(food)
        coEvery { foodRepository.saveFoodIntake(food, mealType) } returns Result.success(Unit)
        coEvery { nutritionRepository.addMealToDay(any(), any()) } returns Result.success(Unit)
        coEvery { nutritionRepository.invalidateDailyCache(date) } returns Result.success(Unit)
        
        // When
        val result = useCase(SaveFoodIntakeUseCase.Params(food, mealType, date))
        
        // Then
        assertTrue(result is Result.Success)
        coVerify { foodRepository.validateFoodData(food) }
        coVerify { foodRepository.saveFoodIntake(food, mealType) }
        coVerify { nutritionRepository.addMealToDay(date, any()) }
        coVerify { nutritionRepository.invalidateDailyCache(date) }
    }
    
    @Test
    fun `save food with blank name returns validation error`() = runTest {
        // Given
        val food = Food(
            name = "",
            calories = 80,
            protein = 0.3,
            fat = 0.2,
            carbs = 21.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        val mealType = MealType.BREAKFAST
        
        // When
        val result = useCase(SaveFoodIntakeUseCase.Params(food, mealType))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Food name cannot be blank", result.exception.message)
    }
    
    @Test
    fun `save food with negative calories returns validation error`() = runTest {
        // Given
        val food = Food(
            name = "Apple",
            calories = -10,
            protein = 0.3,
            fat = 0.2,
            carbs = 21.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        val mealType = MealType.BREAKFAST
        
        // When
        val result = useCase(SaveFoodIntakeUseCase.Params(food, mealType))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Calories cannot be negative", result.exception.message)
    }
    
    @Test
    fun `save food validation failure returns error`() = runTest {
        // Given
        val food = Food(
            name = "Apple",
            calories = 80,
            protein = 0.3,
            fat = 0.2,
            carbs = 21.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        val mealType = MealType.BREAKFAST
        val validationError = DomainException.ValidationException("Invalid food data")
        
        coEvery { foodRepository.validateFoodData(food) } returns Result.error(validationError)
        
        // When
        val result = useCase(SaveFoodIntakeUseCase.Params(food, mealType))
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(validationError, (result as Result.Error).exception)
    }
    
    @Test
    fun `save food repository failure returns error`() = runTest {
        // Given
        val food = Food(
            name = "Apple",
            calories = 80,
            protein = 0.3,
            fat = 0.2,
            carbs = 21.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        val mealType = MealType.BREAKFAST
        val repositoryError = DomainException.StorageException("Storage error")
        
        coEvery { foodRepository.validateFoodData(food) } returns Result.success(food)
        coEvery { foodRepository.saveFoodIntake(food, mealType) } returns Result.error(repositoryError)
        
        // When
        val result = useCase(SaveFoodIntakeUseCase.Params(food, mealType))
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(repositoryError, (result as Result.Error).exception)
    }
}