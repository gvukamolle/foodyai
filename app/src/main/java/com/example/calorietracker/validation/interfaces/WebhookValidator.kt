package com.example.calorietracker.validation.interfaces

import com.example.calorietracker.validation.models.*

/**
 * Interface for webhook service validation functionality
 */
interface WebhookValidator : BaseValidator {
    /**
     * Validates the MakeService configuration and implementation
     */
    suspend fun validateMakeService(): WebhookValidationResult
    
    /**
     * Validates network configuration (OkHttp, Retrofit setup)
     */
    suspend fun validateNetworkConfiguration(): NetworkConfigResult
    
    /**
     * Validates API endpoint definitions
     */
    suspend fun validateApiEndpoints(): ApiEndpointResult
    
    /**
     * Tests webhook connectivity
     */
    suspend fun testWebhookConnectivity(): ConnectivityResult
    
    /**
     * Validates JSON serialization for request/response data classes
     */
    suspend fun validateJsonSerialization(): SerializationResult
    
    /**
     * Validates error handling implementation
     */
    suspend fun validateErrorHandling(): List<String>
}