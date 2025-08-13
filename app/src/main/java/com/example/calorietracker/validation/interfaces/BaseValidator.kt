package com.example.calorietracker.validation.interfaces

import com.example.calorietracker.validation.ValidationResult

/**
 * Base interface for all validators
 */
interface BaseValidator {
    /**
     * Validates the specific component and returns validation results
     */
    suspend fun validate(): List<ValidationResult>
    
    /**
     * Gets the validator name for reporting purposes
     */
    fun getValidatorName(): String
    
    /**
     * Gets the validation category
     */
    fun getCategory(): String
}