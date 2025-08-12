package com.example.calorietracker.domain.exceptions

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DomainException classes
 */
class DomainExceptionTest {
    
    @Test
    fun `ValidationException should have correct message and cause`() {
        // Given
        val message = "Validation failed"
        val cause = RuntimeException("Root cause")
        
        // When
        val exception = DomainException.ValidationException(message, cause)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is DomainException)
    }
    
    @Test
    fun `ValidationException should work with message only`() {
        // Given
        val message = "Validation failed"
        
        // When
        val exception = DomainException.ValidationException(message)
        
        // Then
        assertEquals(message, exception.message)
        assertNull(exception.cause)
    }
    
    @Test
    fun `NetworkException should have correct message and cause`() {
        // Given
        val message = "Network error occurred"
        val cause = RuntimeException("Connection timeout")
        
        // When
        val exception = DomainException.NetworkException(message, cause)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is DomainException)
    }
    
    @Test
    fun `NetworkException should work with message only`() {
        // Given
        val message = "Network error occurred"
        
        // When
        val exception = DomainException.NetworkException(message)
        
        // Then
        assertEquals(message, exception.message)
        assertNull(exception.cause)
    }
    
    @Test
    fun `StorageException should have correct message and cause`() {
        // Given
        val message = "Storage operation failed"
        val cause = RuntimeException("Database error")
        
        // When
        val exception = DomainException.StorageException(message, cause)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is DomainException)
    }
    
    @Test
    fun `StorageException should work with message only`() {
        // Given
        val message = "Storage operation failed"
        
        // When
        val exception = DomainException.StorageException(message)
        
        // Then
        assertEquals(message, exception.message)
        assertNull(exception.cause)
    }
    
    @Test
    fun `AIAnalysisException should have correct message and cause`() {
        // Given
        val message = "AI analysis failed"
        val cause = RuntimeException("Model error")
        
        // When
        val exception = DomainException.AIAnalysisException(message, cause)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is DomainException)
    }
    
    @Test
    fun `AIAnalysisException should work with message only`() {
        // Given
        val message = "AI analysis failed"
        
        // When
        val exception = DomainException.AIAnalysisException(message)
        
        // Then
        assertEquals(message, exception.message)
        assertNull(exception.cause)
    }
    
    @Test
    fun `UsageLimitException should have correct message and cause`() {
        // Given
        val message = "Usage limit exceeded"
        val cause = RuntimeException("Quota exceeded")
        
        // When
        val exception = DomainException.UsageLimitException(message, cause)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is DomainException)
    }
    
    @Test
    fun `UsageLimitException should work with message only`() {
        // Given
        val message = "Usage limit exceeded"
        
        // When
        val exception = DomainException.UsageLimitException(message)
        
        // Then
        assertEquals(message, exception.message)
        assertNull(exception.cause)
    }
    
    @Test
    fun `UnknownException should have correct message and cause`() {
        // Given
        val message = "Unknown error occurred"
        val cause = RuntimeException("Unexpected error")
        
        // When
        val exception = DomainException.UnknownException(message, cause)
        
        // Then
        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception is DomainException)
    }
    
    @Test
    fun `UnknownException should work with message only`() {
        // Given
        val message = "Unknown error occurred"
        
        // When
        val exception = DomainException.UnknownException(message)
        
        // Then
        assertEquals(message, exception.message)
        assertNull(exception.cause)
    }
    
    @Test
    fun `all exceptions should be distinguishable by type`() {
        // Given
        val validationEx = DomainException.ValidationException("validation")
        val networkEx = DomainException.NetworkException("network")
        val storageEx = DomainException.StorageException("storage")
        val aiEx = DomainException.AIAnalysisException("ai")
        val usageEx = DomainException.UsageLimitException("usage")
        val unknownEx = DomainException.UnknownException("unknown")
        
        // Then
        assertTrue(validationEx is DomainException.ValidationException)
        assertTrue(networkEx is DomainException.NetworkException)
        assertTrue(storageEx is DomainException.StorageException)
        assertTrue(aiEx is DomainException.AIAnalysisException)
        assertTrue(usageEx is DomainException.UsageLimitException)
        assertTrue(unknownEx is DomainException.UnknownException)
        
        // All should be DomainException
        assertTrue(validationEx is DomainException)
        assertTrue(networkEx is DomainException)
        assertTrue(storageEx is DomainException)
        assertTrue(aiEx is DomainException)
        assertTrue(usageEx is DomainException)
        assertTrue(unknownEx is DomainException)
    }
    
    @Test
    fun `exceptions should support inheritance hierarchy`() {
        // Given
        val exception = DomainException.ValidationException("test")
        
        // Then
        assertTrue(exception is DomainException.ValidationException)
        assertTrue(exception is DomainException)
        assertTrue(exception is Exception)
        assertTrue(exception is Throwable)
    }
}