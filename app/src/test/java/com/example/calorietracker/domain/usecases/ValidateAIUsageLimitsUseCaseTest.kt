package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.exceptions.DomainException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for ValidateAIUsageLimitsUseCase
 */
class ValidateAIUsageLimitsUseCaseTest {
    
    private lateinit var useCase: ValidateAIUsageLimitsUseCase
    
    @Before
    fun setup() {
        useCase = ValidateAIUsageLimitsUseCase()
    }
    
    @Test
    fun `validate usage within daily limit should return success`() = runTest {
        // Given
        val currentUsage = 5
        val dailyLimit = 10
        val date = LocalDate.now()
        
        // When
        val result = useCase(ValidateAIUsageLimitsUseCase.Params(currentUsage, dailyLimit, date))
        
        // Then
        assertTrue(result is Result.Success)
        val validationResult = (result as Result.Success).data
        assertTrue(validationResult.isAllowed)
        assertEquals(5, validationResult.remainingUsage)
        assertNull(validationResult.resetTime)
    }
    
    @Test
    fun `validate usage at daily limit should return limit exceeded error`() = runTest {
        // Given
        val currentUsage = 10
        val dailyLimit = 10
        val date = LocalDate.now()
        
        // When
        val result = useCase(ValidateAIUsageLimitsUseCase.Params(currentUsage, dailyLimit, date))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.UsageLimitException)
        assertEquals("Daily AI usage limit exceeded", result.exception.message)
    }
    
    @Test
    fun `validate usage over daily limit should return limit exceeded error`() = runTest {
        // Given
        val currentUsage = 15
        val dailyLimit = 10
        val date = LocalDate.now()
        
        // When
        val result = useCase(ValidateAIUsageLimitsUseCase.Params(currentUsage, dailyLimit, date))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.UsageLimitException)
        assertEquals("Daily AI usage limit exceeded", result.exception.message)
    }
    
    @Test
    fun `validate usage with zero limit should return limit exceeded error`() = runTest {
        // Given
        val currentUsage = 1
        val dailyLimit = 0
        val date = LocalDate.now()
        
        // When
        val result = useCase(ValidateAIUsageLimitsUseCase.Params(currentUsage, dailyLimit, date))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.UsageLimitException)
        assertEquals("Daily AI usage limit exceeded", result.exception.message)
    }
    
    @Test
    fun `validate usage with negative current usage should return validation error`() = runTest {
        // Given
        val currentUsage = -1
        val dailyLimit = 10
        val date = LocalDate.now()
        
        // When
        val result = useCase(ValidateAIUsageLimitsUseCase.Params(currentUsage, dailyLimit, date))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Current usage cannot be negative", result.exception.message)
    }
    
    @Test
    fun `validate usage with negative daily limit should return validation error`() = runTest {
        // Given
        val currentUsage = 5
        val dailyLimit = -1
        val date = LocalDate.now()
        
        // When
        val result = useCase(ValidateAIUsageLimitsUseCase.Params(currentUsage, dailyLimit, date))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Daily limit cannot be negative", result.exception.message)
    }
    
    @Test
    fun `validate usage with future date should return validation error`() = runTest {
        // Given
        val currentUsage = 5
        val dailyLimit = 10
        val futureDate = LocalDate.now().plusDays(1)
        
        // When
        val result = useCase(ValidateAIUsageLimitsUseCase.Params(currentUsage, dailyLimit, futureDate))
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DomainException.ValidationException)
        assertEquals("Date cannot be in the future", result.exception.message)
    }
    
    @Test
    fun `validate usage one request before limit should return success with remaining 1`() = runTest {
        // Given
        val currentUsage = 9
        val dailyLimit = 10
        val date = LocalDate.now()
        
        // When
        val result = useCase(ValidateAIUsageLimitsUseCase.Params(currentUsage, dailyLimit, date))
        
        // Then
        assertTrue(result is Result.Success)
        val validationResult = (result as Result.Success).data
        assertTrue(validationResult.isAllowed)
        assertEquals(1, validationResult.remainingUsage)
    }
    
    @Test
    fun `validate usage with zero current usage should return full limit remaining`() = runTest {
        // Given
        val currentUsage = 0
        val dailyLimit = 10
        val date = LocalDate.now()
        
        // When
        val result = useCase(ValidateAIUsageLimitsUseCase.Params(currentUsage, dailyLimit, date))
        
        // Then
        assertTrue(result is Result.Success)
        val validationResult = (result as Result.Success).data
        assertTrue(validationResult.isAllowed)
        assertEquals(10, validationResult.remainingUsage)
    }
    
    @Test
    fun `validate usage with high daily limit should return success`() = runTest {
        // Given
        val currentUsage = 50
        val dailyLimit = 100
        val date = LocalDate.now()
        
        // When
        val result = useCase(ValidateAIUsageLimitsUseCase.Params(currentUsage, dailyLimit, date))
        
        // Then
        assertTrue(result is Result.Success)
        val validationResult = (result as Result.Success).data
        assertTrue(validationResult.isAllowed)
        assertEquals(50, validationResult.remainingUsage)
    }
    
    @Test
    fun `validate usage with past date should return success`() = runTest {
        // Given
        val currentUsage = 5
        val dailyLimit = 10
        val pastDate = LocalDate.now().minusDays(1)
        
        // When
        val result = useCase(ValidateAIUsageLimitsUseCase.Params(currentUsage, dailyLimit, pastDate))
        
        // Then
        assertTrue(result is Result.Success)
        val validationResult = (result as Result.Success).data
        assertTrue(validationResult.isAllowed)
        assertEquals(5, validationResult.remainingUsage)
    }
}