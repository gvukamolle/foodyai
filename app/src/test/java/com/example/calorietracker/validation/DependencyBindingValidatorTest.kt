package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.BindingIssue
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DependencyBindingValidator
 */
class DependencyBindingValidatorTest {
    
    private lateinit var dependencyBindingValidator: DependencyBindingValidator
    
    @Before
    fun setUp() {
        dependencyBindingValidator = DependencyBindingValidator()
    }
    
    @Test
    fun `validateRepositoryBindings should return list of binding issues`() {
        // When
        val result = dependencyBindingValidator.validateRepositoryBindings()
        
        // Then
        assertNotNull("Result should not be null", result)
        assertTrue("Result should be a list", result is List<*>)
    }
    
    @Test
    fun `validateUseCaseBindings should return list of binding issues`() {
        // When
        val result = dependencyBindingValidator.validateUseCaseBindings()
        
        // Then
        assertNotNull("Result should not be null", result)
        assertTrue("Result should be a list", result is List<*>)
    }
    
    @Test
    fun `analyzeDependencyGraphCompleteness should return list of binding issues`() {
        // When
        val result = dependencyBindingValidator.analyzeDependencyGraphCompleteness()
        
        // Then
        assertNotNull("Result should not be null", result)
        assertTrue("Result should be a list", result is List<*>)
    }
    
    @Test
    fun `binding issues should have meaningful error messages`() {
        // When
        val repositoryResult = dependencyBindingValidator.validateRepositoryBindings()
        val useCaseResult = dependencyBindingValidator.validateUseCaseBindings()
        val completenessResult = dependencyBindingValidator.analyzeDependencyGraphCompleteness()
        
        // Then
        val allIssues = repositoryResult + useCaseResult + completenessResult
        
        for (issue in allIssues) {
            assertNotNull("Issue should have an interface name", issue.interfaceName)
            assertNotNull("Issue should have an issue description", issue.issue)
            assertFalse("Issue description should not be empty", issue.issue.isEmpty())
            
            if (issue.fix != null) {
                assertFalse("Fix should not be empty when provided", issue.fix.isEmpty())
            }
        }
    }
}