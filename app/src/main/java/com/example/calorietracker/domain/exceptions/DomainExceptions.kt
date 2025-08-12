package com.example.calorietracker.domain.exceptions

/**
 * Base class for all domain-specific exceptions
 */
sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    /**
     * Network-related errors (API calls, connectivity issues)
     */
    class NetworkException(message: String, cause: Throwable? = null) : DomainException(message, cause)
    
    /**
     * Data validation errors (invalid input, business rule violations)
     */
    class ValidationException(message: String, cause: Throwable? = null) : DomainException(message, cause)
    
    /**
     * Data not found errors (missing user profile, food item not found)
     */
    class DataNotFoundException(message: String, cause: Throwable? = null) : DomainException(message, cause)
    
    /**
     * AI analysis specific errors (analysis failed, quota exceeded)
     */
    class AIAnalysisException(message: String, cause: Throwable? = null) : DomainException(message, cause)
    
    /**
     * Authentication and authorization errors
     */
    class AuthenticationException(message: String, cause: Throwable? = null) : DomainException(message, cause)
    
    /**
     * Storage and persistence errors
     */
    class StorageException(message: String, cause: Throwable? = null) : DomainException(message, cause)
    
    /**
     * Business logic errors (insufficient permissions, invalid operations)
     */
    class BusinessLogicException(message: String, cause: Throwable? = null) : DomainException(message, cause)
}