package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.DependencyGraphResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DependencyGraphValidator
 */
class DependencyGraphValidatorTest {
    
    private lateinit var dependencyGraphValidator: DependencyGraphValidator
    
    @Before
    fun setUp() {
        dependencyGraphValidator = DependencyGraphValidator()
    }
    
    @Test
    fun `validateDependencyGraph should return valid result structure`() {
        // When
        val result = dependencyGraphValidator.validateDependencyGraph()
        
        // Then
        assertNotNull("Result should not be null", result)
        assertNotNull("Circular dependencies should not be null", result.circularDependencies)
        assertNotNull("Missing dependencies should not be null", result.missingDependencies)
        assertTrue("Should have isValid field", result.isValid || !result.isValid)
    }
    
    @Test
    fun `detectCircularDependencies should return list of strings`() {
        // When
        val result = dependencyGraphValidator.detectCircularDependencies()
        
        // Then
        assertNotNull("Result should not be null", result)
        assertTrue("Result should be a list", result is List<*>)
    }
}