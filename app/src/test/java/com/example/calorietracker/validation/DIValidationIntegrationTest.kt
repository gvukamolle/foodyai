package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for DI validation system
 */
class DIValidationIntegrationTest {
    
    private lateinit var diValidator: DIValidatorImpl
    
    @Before
    fun setUp() {
        diValidator = DIValidatorImpl()
    }
    
    @Test
    fun `validator metadata should be correct`() {
        // When
        val name = diValidator.getValidatorName()
        val category = diValidator.getCategory()
        
        // Then
        assertEquals("Validator name should be correct", "DI Configuration Validator", name)
        assertEquals("Category should be correct", "Dependency Injection", category)
    }
}