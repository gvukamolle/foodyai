package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DIValidatorImpl
 */
class DIValidatorImplTest {
    
    private lateinit var diValidator: DIValidatorImpl
    
    @Before
    fun setUp() {
        diValidator = DIValidatorImpl()
    }
    
    @Test
    fun `getValidatorName should return correct name`() {
        // When
        val name = diValidator.getValidatorName()
        
        // Then
        assertEquals("DI Configuration Validator", name)
    }
    
    @Test
    fun `getCategory should return correct category`() {
        // When
        val category = diValidator.getCategory()
        
        // Then
        assertEquals("Dependency Injection", category)
    }
}