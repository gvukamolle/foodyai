package com.example.calorietracker.validation.interfaces

import com.example.calorietracker.validation.models.*

/**
 * Interface for Dependency Injection validation functionality
 */
interface DIValidator : BaseValidator {
    /**
     * Validates Hilt modules configuration
     */
    suspend fun validateHiltModules(): HiltValidationResult
    
    /**
     * Validates dependency graph for completeness and correctness
     */
    suspend fun validateDependencyGraph(): DependencyGraphResult
    
    /**
     * Validates scope configurations
     */
    suspend fun validateScopes(): ScopeValidationResult
    
    /**
     * Validates interface to implementation bindings
     */
    suspend fun validateBindings(): List<BindingIssue>
    
    /**
     * Detects circular dependencies in DI graph
     */
    suspend fun detectCircularDependencies(): List<String>
}