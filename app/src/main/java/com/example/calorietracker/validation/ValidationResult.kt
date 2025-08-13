package com.example.calorietracker.validation

/**
 * Base sealed class for all validation results
 */
sealed class ValidationResult {
    data class Success(val message: String) : ValidationResult()
    data class Warning(val message: String, val details: String) : ValidationResult()
    data class Error(val message: String, val details: String, val fix: String?) : ValidationResult()
}

/**
 * Severity levels for validation issues
 */
enum class Severity {
    CRITICAL,
    WARNING,
    INFO
}

/**
 * Priority levels for recommendations
 */
enum class Priority {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Types of validation errors
 */
enum class ErrorType {
    COMPILATION_ERROR,
    RUNTIME_RISK,
    CODE_QUALITY,
    ARCHITECTURAL_VIOLATION,
    NETWORK_CONFIGURATION,
    DATA_BINDING,
    DEPENDENCY_INJECTION
}

/**
 * Violation types for architectural issues
 */
enum class ViolationType {
    LAYER_DEPENDENCY_VIOLATION,
    CIRCULAR_DEPENDENCY,
    IMPROPER_IMPORT,
    SCOPE_VIOLATION
}

/**
 * Container for validation results with detailed issues
 */
data class ValidationResults(
    val validatorName: String,
    val issues: List<ValidationIssue>,
    val summary: String
) {
    /**
     * Individual validation issue
     */
    data class ValidationIssue(
        val type: String,
        val severity: Severity,
        val message: String,
        val details: String,
        val location: String
    )
    
    /**
     * Severity levels for validation issues
     */
    enum class Severity {
        ERROR,
        WARNING,
        INFO
    }
}

