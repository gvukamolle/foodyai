package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.common.Gender
import com.example.calorietracker.domain.exceptions.DomainException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ValidateUserDataUseCase
 */
class ValidateUserDataUseCaseTest {
    
    private lateinit var useCase: ValidateUserDataUseCase
    
    @Before
    fun setup() {
        useCase = ValidateUserDataUseCase()
    }
    
    @Test
    fun `validate valid user data should return success`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE
        )
        
        // When
        val result = useCase(ValidateUserDataUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(user, (result as Result.Success).data)
    }
    
    @Test
    fun `validate user with blank name should return validation error`() = runTest {
        // Given
        val user = User(
            name = "",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE
        )
        
        // When
        val result = useCase(ValidateUserDataUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Name cannot be blank", result.exception.message)
    }
    
    @Test
    fun `validate user with whitespace-only name should return validation error`() = runTest {
        // Given
        val user = User(
            name = "   ",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE
        )
        
        // When
        val result = useCase(ValidateUserDataUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Name cannot be blank", result.exception.message)
    }
    
    @Test
    fun `validate user with invalid birthday format should return validation error`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "invalid-date",
            height = 180,
            weight = 75,
            gender = Gender.MALE
        )
        
        // When
        val result = useCase(ValidateUserDataUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Invalid birthday format", result.exception.message)
    }
    
    @Test
    fun `validate user with future birthday should return validation error`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "2030-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE
        )
        
        // When
        val result = useCase(ValidateUserDataUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Birthday cannot be in the future", result.exception.message)
    }
    
    @Test
    fun `validate user with zero height should return validation error`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 0,
            weight = 75,
            gender = Gender.MALE
        )
        
        // When
        val result = useCase(ValidateUserDataUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Height must be greater than 0", result.exception.message)
    }
    
    @Test
    fun `validate user with negative height should return validation error`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = -10,
            weight = 75,
            gender = Gender.MALE
        )
        
        // When
        val result = useCase(ValidateUserDataUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Height must be greater than 0", result.exception.message)
    }
    
    @Test
    fun `validate user with unrealistic height should return validation error`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 300, // 3 meters
            weight = 75,
            gender = Gender.MALE
        )
        
        // When
        val result = useCase(ValidateUserDataUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Height must be between 50 and 250 cm", result.exception.message)
    }
    
    @Test
    fun `validate user with zero weight should return validation error`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 0,
            gender = Gender.MALE
        )
        
        // When
        val result = useCase(ValidateUserDataUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Weight must be greater than 0", result.exception.message)
    }
    
    @Test
    fun `validate user with negative weight should return validation error`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = -10,
            gender = Gender.MALE
        )
        
        // When
        val result = useCase(ValidateUserDataUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Weight must be greater than 0", result.exception.message)
    }
    
    @Test
    fun `validate user with unrealistic weight should return validation error`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 500, // 500 kg
            gender = Gender.MALE
        )
        
        // When
        val result = useCase(ValidateUserDataUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Weight must be between 20 and 300 kg", result.exception.message)
    }
    
    @Test
    fun `validate user with valid edge case values should return success`() = runTest {
        // Given
        val user = User(
            name = "A", // Single character name
            birthday = "1900-01-01", // Very old birthday
            height = 50, // Minimum height
            weight = 20, // Minimum weight
            gender = Gender.FEMALE
        )
        
        // When
        val result = useCase(ValidateUserDataUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(user, (result as Result.Success).data)
    }
    
    @Test
    fun `validate user with maximum valid values should return success`() = runTest {
        // Given
        val user = User(
            name = "Very Long Name That Is Still Valid",
            birthday = "2000-12-31",
            height = 250, // Maximum height
            weight = 300, // Maximum weight
            gender = Gender.MALE
        )
        
        // When
        val result = useCase(ValidateUserDataUseCase.Params(user))
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(user, (result as Result.Success).data)
    }
}