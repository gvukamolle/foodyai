package com.example.calorietracker.data.repositories

import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.FoodItem
import com.example.calorietracker.data.mappers.FoodMapper
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.common.FoodSource
import com.example.calorietracker.domain.entities.common.MealType
import com.example.calorietracker.domain.exceptions.DomainException
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FoodRepositoryImpl
 */
class FoodRepositoryImplTest {
    
    private lateinit var dataRepository: DataRepository
    private lateinit var foodMapper: FoodMapper
    private lateinit var repository: FoodRepositoryImpl
    
    @Before
    fun setup() {
        dataRepository = mockk()
        foodMapper = mockk()
        repository = FoodRepositoryImpl(dataRepository, foodMapper)
    }
    
    @Test
    fun `analyze food photo successfully returns mapped food`() = runTest {
        // Given
        val photoPath = "/path/to/photo.jpg"
        val caption = "Delicious apple"
        val dataFoodItem = FoodItem(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = "photo"
        )
        val domainFood = Food(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = FoodSource.AI_PHOTO_ANALYSIS
        )
        
        coEvery { dataRepository.analyzeFoodPhoto(photoPath, caption) } returns dataFoodItem
        every { foodMapper.mapDataToDomain(dataFoodItem) } returns domainFood
        
        // When
        val result = repository.analyzeFoodPhoto(photoPath, caption)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(domainFood, (result as Result.Success).data)
        coVerify { dataRepository.analyzeFoodPhoto(photoPath, caption) }
        verify { foodMapper.mapDataToDomain(dataFoodItem) }
    }
    
    @Test
    fun `analyze food photo with repository exception returns error`() = runTest {
        // Given
        val photoPath = "/path/to/photo.jpg"
        val caption = "caption"
        val exception = RuntimeException("Network error")
        
        coEvery { dataRepository.analyzeFoodPhoto(photoPath, caption) } throws exception
        
        // When
        val result = repository.analyzeFoodPhoto(photoPath, caption)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.NetworkException)
        assertEquals("Failed to analyze food photo", result.exception.message)
        assertEquals(exception, result.exception.cause)
    }
    
    @Test
    fun `analyze food description successfully returns mapped food`() = runTest {
        // Given
        val description = "One medium banana"
        val dataFoodItem = FoodItem(
            name = "Banana",
            calories = 105,
            protein = 1.3,
            fat = 0.4,
            carbs = 27.0,
            weight = "100г",
            source = "text"
        )
        val domainFood = Food(
            name = "Banana",
            calories = 105,
            protein = 1.3,
            fat = 0.4,
            carbs = 27.0,
            weight = "100г",
            source = FoodSource.AI_TEXT_ANALYSIS
        )
        
        coEvery { dataRepository.analyzeFoodDescription(description) } returns dataFoodItem
        every { foodMapper.mapDataToDomain(dataFoodItem) } returns domainFood
        
        // When
        val result = repository.analyzeFoodDescription(description)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(domainFood, (result as Result.Success).data)
        coVerify { dataRepository.analyzeFoodDescription(description) }
        verify { foodMapper.mapDataToDomain(dataFoodItem) }
    }
    
    @Test
    fun `save food intake successfully returns success`() = runTest {
        // Given
        val domainFood = Food(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        val mealType = MealType.BREAKFAST
        val dataFoodItem = FoodItem(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = "manual_input"
        )
        
        every { foodMapper.mapDomainToData(domainFood) } returns dataFoodItem
        coEvery { dataRepository.saveFoodIntake(dataFoodItem, any()) } returns Unit
        
        // When
        val result = repository.saveFoodIntake(domainFood, mealType)
        
        // Then
        assertTrue(result is Result.Success)
        verify { foodMapper.mapDomainToData(domainFood) }
        coVerify { dataRepository.saveFoodIntake(dataFoodItem, any()) }
    }
    
    @Test
    fun `save food intake with repository exception returns error`() = runTest {
        // Given
        val domainFood = Food(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        val mealType = MealType.BREAKFAST
        val dataFoodItem = FoodItem(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = "manual_input"
        )
        val exception = RuntimeException("Storage error")
        
        every { foodMapper.mapDomainToData(domainFood) } returns dataFoodItem
        coEvery { dataRepository.saveFoodIntake(dataFoodItem, any()) } throws exception
        
        // When
        val result = repository.saveFoodIntake(domainFood, mealType)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.StorageException)
        assertEquals("Failed to save food intake", result.exception.message)
        assertEquals(exception, result.exception.cause)
    }
    
    @Test
    fun `validate food data with valid food returns success`() = runTest {
        // Given
        val validFood = Food(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        
        // When
        val result = repository.validateFoodData(validFood)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(validFood, (result as Result.Success).data)
    }
    
    @Test
    fun `validate food data with blank name returns validation error`() = runTest {
        // Given
        val invalidFood = Food(
            name = "",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        
        // When
        val result = repository.validateFoodData(invalidFood)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Food name cannot be blank", result.exception.message)
    }
    
    @Test
    fun `validate food data with negative calories returns validation error`() = runTest {
        // Given
        val invalidFood = Food(
            name = "Apple",
            calories = -10,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        
        // When
        val result = repository.validateFoodData(invalidFood)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Calories cannot be negative", result.exception.message)
    }
    
    @Test
    fun `validate food data with negative protein returns validation error`() = runTest {
        // Given
        val invalidFood = Food(
            name = "Apple",
            calories = 95,
            protein = -1.0,
            fat = 0.3,
            carbs = 25.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        
        // When
        val result = repository.validateFoodData(invalidFood)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Protein cannot be negative", result.exception.message)
    }
    
    @Test
    fun `validate food data with negative fat returns validation error`() = runTest {
        // Given
        val invalidFood = Food(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = -1.0,
            carbs = 25.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        
        // When
        val result = repository.validateFoodData(invalidFood)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Fat cannot be negative", result.exception.message)
    }
    
    @Test
    fun `validate food data with negative carbs returns validation error`() = runTest {
        // Given
        val invalidFood = Food(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = -1.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        
        // When
        val result = repository.validateFoodData(invalidFood)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Carbs cannot be negative", result.exception.message)
    }
    
    @Test
    fun `validate food data with blank weight returns validation error`() = runTest {
        // Given
        val invalidFood = Food(
            name = "Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "",
            source = FoodSource.MANUAL_INPUT
        )
        
        // When
        val result = repository.validateFoodData(invalidFood)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Weight cannot be blank", result.exception.message)
    }
    
    @Test
    fun `validate food data with zero values should be valid`() = runTest {
        // Given
        val validFood = Food(
            name = "Water",
            calories = 0,
            protein = 0.0,
            fat = 0.0,
            carbs = 0.0,
            weight = "100мл",
            source = FoodSource.MANUAL_INPUT
        )
        
        // When
        val result = repository.validateFoodData(validFood)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(validFood, (result as Result.Success).data)
    }
    
    @Test
    fun `get food history successfully returns mapped foods`() = runTest {
        // Given
        val dataFoodItems = listOf(
            FoodItem("Apple", 95, 0.5, 0.3, 25.0, "100г", "manual"),
            FoodItem("Banana", 105, 1.3, 0.4, 27.0, "100г", "photo")
        )
        val domainFoods = listOf(
            Food("Apple", 95, 0.5, 0.3, 25.0, "100г", FoodSource.MANUAL_INPUT),
            Food("Banana", 105, 1.3, 0.4, 27.0, "100г", FoodSource.AI_PHOTO_ANALYSIS)
        )
        
        coEvery { dataRepository.getFoodHistory(10) } returns dataFoodItems
        every { foodMapper.mapDataListToDomain(dataFoodItems) } returns domainFoods
        
        // When
        val result = repository.getFoodHistory(10)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(domainFoods, (result as Result.Success).data)
        coVerify { dataRepository.getFoodHistory(10) }
        verify { foodMapper.mapDataListToDomain(dataFoodItems) }
    }
    
    @Test
    fun `get food history with repository exception returns error`() = runTest {
        // Given
        val exception = RuntimeException("Database error")
        
        coEvery { dataRepository.getFoodHistory(10) } throws exception
        
        // When
        val result = repository.getFoodHistory(10)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.StorageException)
        assertEquals("Failed to get food history", result.exception.message)
        assertEquals(exception, result.exception.cause)
    }
}