package com.example.calorietracker.validation.interfaces

import com.example.calorietracker.validation.models.*

/**
 * Interface for UI data flow validation functionality
 */
interface UIDataFlowValidator : BaseValidator {
    /**
     * Validates ViewModels for proper state management
     */
    suspend fun validateViewModels(): ViewModelValidationResult
    
    /**
     * Validates data binding in UI components
     */
    suspend fun validateDataBinding(): DataBindingResult
    
    /**
     * Validates state management patterns
     */
    suspend fun validateStateManagement(): StateManagementResult
    
    /**
     * Validates UI components for proper data flow
     */
    suspend fun validateUIComponents(): UIComponentResult
    
    /**
     * Validates data mappers for correct transformations
     */
    suspend fun validateDataMappers(): List<String>
}