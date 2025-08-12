package com.example.calorietracker.domain.common

import com.example.calorietracker.domain.exceptions.DomainException
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Result wrapper class
 */
class ResultTest {
    
    @Test
    fun `Result success should contain data`() {
        // Given
        val data = "test data"
        
        // When
        val result = Result.success(data)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(data, (result as Result.Success).data)
    }
    
    @Test
    fun `Result error should contain exception`() {
        // Given
        val exception = DomainException.ValidationException("test error")
        
        // When
        val result = Result.error<String>(exception)
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }
    
    @Test
    fun `Result success should not be error`() {
        // Given
        val result = Result.success("data")
        
        // Then
        assertTrue(result is Result.Success)
        assertFalse(result is Result.Error)
    }
    
    @Test
    fun `Result error should not be success`() {
        // Given
        val result = Result.error<String>(DomainException.ValidationException("error"))
        
        // Then
        assertTrue(result is Result.Error)
        assertFalse(result is Result.Success)
    }
    
    @Test
    fun `Result success with null data should work`() {
        // Given
        val data: String? = null
        
        // When
        val result = Result.success(data)
        
        // Then
        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).data)
    }
    
    @Test
    fun `Result success with Unit should work`() {
        // When
        val result = Result.success(Unit)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(Unit, (result as Result.Success).data)
    }
    
    @Test
    fun `Result error with different exception types should work`() {
        // Given
        val validationEx = DomainException.ValidationException("validation")
        val networkEx = DomainException.NetworkException("network")
        val storageEx = DomainException.StorageException("storage")
        
        // When
        val validationResult = Result.error<String>(validationEx)
        val networkResult = Result.error<String>(networkEx)
        val storageResult = Result.error<String>(storageEx)
        
        // Then
        assertTrue(validationResult is Result.Error)
        assertTrue(networkResult is Result.Error)
        assertTrue(storageResult is Result.Error)
        
        assertEquals(validationEx, (validationResult as Result.Error).exception)
        assertEquals(networkEx, (networkResult as Result.Error).exception)
        assertEquals(storageEx, (storageResult as Result.Error).exception)
    }
    
    @Test
    fun `Result with complex data types should work`() {
        // Given
        data class TestData(val id: Int, val name: String)
        val testData = TestData(1, "test")
        
        // When
        val result = Result.success(testData)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(testData, (result as Result.Success).data)
        assertEquals(1, result.data.id)
        assertEquals("test", result.data.name)
    }
    
    @Test
    fun `Result with list data should work`() {
        // Given
        val listData = listOf("item1", "item2", "item3")
        
        // When
        val result = Result.success(listData)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(listData, (result as Result.Success).data)
        assertEquals(3, result.data.size)
    }
    
    @Test
    fun `Result equality should work correctly`() {
        // Given
        val data1 = "test"
        val data2 = "test"
        val data3 = "different"
        
        val exception1 = DomainException.ValidationException("error")
        val exception2 = DomainException.ValidationException("error")
        val exception3 = DomainException.NetworkException("error")
        
        // When
        val success1 = Result.success(data1)
        val success2 = Result.success(data2)
        val success3 = Result.success(data3)
        
        val error1 = Result.error<String>(exception1)
        val error2 = Result.error<String>(exception2)
        val error3 = Result.error<String>(exception3)
        
        // Then
        assertEquals(success1, success2)
        assertNotEquals(success1, success3)
        
        // Note: Exception equality depends on implementation
        // These tests verify the Result wrapper behavior
        assertTrue(error1 is Result.Error)
        assertTrue(error2 is Result.Error)
        assertTrue(error3 is Result.Error)
    }
    
    @Test
    fun `Result toString should provide meaningful output`() {
        // Given
        val successResult = Result.success("test data")
        val errorResult = Result.error<String>(DomainException.ValidationException("test error"))
        
        // When
        val successString = successResult.toString()
        val errorString = errorResult.toString()
        
        // Then
        assertTrue(successString.contains("Success"))
        assertTrue(successString.contains("test data"))
        
        assertTrue(errorString.contains("Error"))
        assertTrue(errorString.contains("test error"))
    }
    
    @Test
    fun `Result type inference should work`() {
        // When
        val stringResult = Result.success("string")
        val intResult = Result.success(42)
        val booleanResult = Result.success(true)
        
        // Then
        assertTrue(stringResult is Result.Success<String>)
        assertTrue(intResult is Result.Success<Int>)
        assertTrue(booleanResult is Result.Success<Boolean>)
        
        assertEquals("string", (stringResult as Result.Success).data)
        assertEquals(42, (intResult as Result.Success).data)
        assertEquals(true, (booleanResult as Result.Success).data)
    }
}