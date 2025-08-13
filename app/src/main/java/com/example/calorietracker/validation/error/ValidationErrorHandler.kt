package com.example.calorietracker.validation.error

import com.example.calorietracker.validation.ErrorType
import com.example.calorietracker.validation.ValidationResult
import com.example.calorietracker.validation.models.ValidationError

/**
 * Handles validation errors and converts them to appropriate ValidationResult types
 */
class ValidationErrorHandler {
    
    /**
     * Handles a validation error and returns appropriate ValidationResult
     */
    fun handleValidationError(error: ValidationError): ValidationResult {
        return when (error.type) {
            ErrorType.COMPILATION_ERROR -> ValidationResult.Error(
                message = error.message,
                details = error.details,
                fix = error.suggestedFix
            )
            ErrorType.RUNTIME_RISK -> ValidationResult.Warning(
                message = error.message,
                details = error.details
            )
            ErrorType.CODE_QUALITY -> ValidationResult.Success(
                message = "Suggestion: ${error.message}"
            )
            ErrorType.ARCHITECTURAL_VIOLATION -> ValidationResult.Error(
                message = "Architectural Violation: ${error.message}",
                details = error.details,
                fix = error.suggestedFix
            )
            ErrorType.NETWORK_CONFIGURATION -> ValidationResult.Error(
                message = "Network Configuration Issue: ${error.message}",
                details = error.details,
                fix = error.suggestedFix
            )
            ErrorType.DATA_BINDING -> ValidationResult.Warning(
                message = "Data Binding Issue: ${error.message}",
                details = error.details
            )
            ErrorType.DEPENDENCY_INJECTION -> ValidationResult.Error(
                message = "DI Configuration Issue: ${error.message}",
                details = error.details,
                fix = error.suggestedFix
            )
        }
    }
    
    /**
     * Handles multiple validation errors
     */
    fun handleValidationErrors(errors: List<ValidationError>): List<ValidationResult> {
        return errors.map { handleValidationError(it) }
    }
    
    /**
     * Creates a validation error from an exception
     */
    fun createValidationError(
        exception: Exception,
        type: ErrorType,
        context: String
    ): ValidationError {
        return ValidationError(
            type = type,
            message = "Error in $context: ${exception.message}",
            details = exception.stackTraceToString(),
            suggestedFix = getSuggestedFix(exception, type)
        )
    }
    
    private fun getSuggestedFix(exception: Exception, type: ErrorType): String? {
        return when (type) {
            ErrorType.COMPILATION_ERROR -> "Check for missing imports or syntax errors"
            ErrorType.RUNTIME_RISK -> "Review the code for potential runtime issues"
            ErrorType.ARCHITECTURAL_VIOLATION -> "Ensure proper layer separation according to Clean Architecture"
            ErrorType.NETWORK_CONFIGURATION -> "Check network module configuration and dependencies"
            ErrorType.DATA_BINDING -> "Verify data binding setup and state management"
            ErrorType.DEPENDENCY_INJECTION -> "Check Hilt module configuration and bindings"
            ErrorType.CODE_QUALITY -> "Consider refactoring for better code quality"
        }
    }
}