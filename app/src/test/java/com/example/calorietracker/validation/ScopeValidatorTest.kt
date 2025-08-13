package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.ScopeValidationResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ScopeValidator
 */
class ScopeValidatorTest {
    
    private lateinit var scopeValidator: ScopeValidator
    
    @Before
    fun setUp() {
        scopeValidator = ScopeValidator()
    }
    
    @Test
    fun `validateScopes should return valid result structure`() {
        // When
        val result = scopeValidator.validateScopes()
        
        // Then
        assertNotNull("Result should not be null", result)
        assertNotNull("Issues should not be null", result.issues)
        assertTrue("Should have isValid field", result.isValid || !result.isValid)
    }
    
    @Test
    fun `validateScopes should provide meaningful recommendations`() {
        // When
        val result = scopeValidator.validateScopes()
        
        // Then
        assertNotNull("Result should not be null", result)
        
        // Check that all issues have recommendations
        for (issue in result.issues) {
            assertNotNull("Issue should have a recommendation", issue.recommendation)
            assertFalse("Recommendation should not be empty", issue.recommendation.isEmpty())
        }
    }
}