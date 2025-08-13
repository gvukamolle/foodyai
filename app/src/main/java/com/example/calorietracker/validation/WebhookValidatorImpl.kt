package com.example.calorietracker.validation

import com.example.calorietracker.validation.interfaces.WebhookValidator
import com.example.calorietracker.validation.models.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация валидатора вебхук сервисов
 */
@Singleton
class WebhookValidatorImpl @Inject constructor() : WebhookValidator {
    
    private val networkConfigValidator = NetworkConfigValidator()
    private val makeServiceValidator = MakeServiceValidator()
    private val jsonSerializationValidator = JsonSerializationValidator()
    private val errorHandlingValidator = ErrorHandlingValidator()
    private val connectivityValidator = WebhookConnectivityValidator()
    
    override suspend fun validate(): List<ValidationResult> {
        val projectPath = System.getProperty("user.dir") ?: "."
        val issues = mutableListOf<String>()
        
        try {
            // Выполняем все проверки
            val webhookResult = validateMakeService()
            val networkResult = validateNetworkConfiguration()
            val endpointsResult = validateApiEndpoints()
            val connectivityResult = testWebhookConnectivity()
            val serializationResult = validateJsonSerialization()
            val errorHandlingIssues = validateErrorHandling()
            
            // Собираем все проблемы
            issues.addAll(webhookResult.networkConfig.issues.map { "${it.component}: ${it.issue}" })
            issues.addAll(webhookResult.apiEndpoints.issues.map { "${it.endpoint}: ${it.issue}" })
            issues.addAll(webhookResult.jsonSerialization.issues)
            issues.addAll(errorHandlingIssues)
            
            if (!webhookResult.connectivity.isConnected) {
                webhookResult.connectivity.errorMessage?.let { issues.add("Connectivity: $it") }
            }
            
            return if (issues.isEmpty()) {
                listOf(ValidationResult.Success("Webhook validation completed successfully"))
            } else {
                listOf(ValidationResult.Warning(
                    "Webhook validation completed with issues",
                    issues.joinToString("\n")
                ))
            }
            
        } catch (e: Exception) {
            return listOf(ValidationResult.Error(
                "Webhook validation failed",
                e.message ?: "Unknown error",
                "Check webhook configuration and network connectivity"
            ))
        }
    }
    
    override suspend fun validateMakeService(): WebhookValidationResult {
        val projectPath = System.getProperty("user.dir")
        
        val networkConfig = validateNetworkConfiguration()
        val apiEndpoints = validateApiEndpoints()
        val connectivity = testWebhookConnectivity()
        val jsonSerialization = validateJsonSerialization()
        
        return WebhookValidationResult(
            networkConfig = networkConfig,
            apiEndpoints = apiEndpoints,
            connectivity = connectivity,
            jsonSerialization = jsonSerialization
        )
    }
    
    override suspend fun validateNetworkConfiguration(): NetworkConfigResult {
        val projectPath = System.getProperty("user.dir")
        return networkConfigValidator.validateNetworkModule(projectPath)
    }
    
    override suspend fun validateApiEndpoints(): ApiEndpointResult {
        val projectPath = System.getProperty("user.dir")
        return makeServiceValidator.validateMakeService(projectPath)
    }
    
    override suspend fun testWebhookConnectivity(): ConnectivityResult {
        return try {
            // Получаем конфигурацию из MakeService
            val baseUrl = "https://hook.us2.make.com/"
            val webhookId = "653st2c10rmg92nlltf3y0m8sggxaac6"
            
            connectivityValidator.testWebhookConnectivity(baseUrl, webhookId)
        } catch (e: Exception) {
            ConnectivityResult(
                isConnected = false,
                responseTime = null,
                errorMessage = e.message
            )
        }
    }
    
    override suspend fun validateJsonSerialization(): SerializationResult {
        val projectPath = System.getProperty("user.dir")
        return jsonSerializationValidator.validateJsonSerialization(projectPath)
    }
    
    override suspend fun validateErrorHandling(): List<String> {
        val projectPath = System.getProperty("user.dir")
        return errorHandlingValidator.validateErrorHandling(projectPath)
    }
    
    /**
     * Выполняет комплексную проверку всех аспектов webhook сервиса
     */
    suspend fun performComprehensiveValidation(): WebhookValidationReport {
        val projectPath = System.getProperty("user.dir")
        
        // Сбор всех результатов валидации
        val networkConfig = validateNetworkConfiguration()
        val apiEndpoints = validateApiEndpoints()
        val jsonSerialization = validateJsonSerialization()
        val errorHandling = validateErrorHandling()
        val connectivity = testWebhookConnectivity()
        
        // Дополнительные проверки
        val connectivityIssues = performDetailedConnectivityTests()
        val responseValidationIssues = validateWebhookResponses()
        
        return WebhookValidationReport(
            networkConfiguration = networkConfig,
            apiEndpoints = apiEndpoints,
            jsonSerialization = jsonSerialization,
            errorHandling = errorHandling,
            connectivity = connectivity,
            connectivityIssues = connectivityIssues,
            responseValidation = responseValidationIssues,
            overallScore = calculateOverallScore(
                networkConfig, apiEndpoints, jsonSerialization, 
                errorHandling, connectivity, connectivityIssues, responseValidationIssues
            )
        )
    }
    
    private suspend fun performDetailedConnectivityTests(): List<String> {
        return try {
            val baseUrl = "https://hook.us2.make.com/"
            val webhookId = "653st2c10rmg92nlltf3y0m8sggxaac6"
            
            connectivityValidator.performComprehensiveConnectivityTest(baseUrl, webhookId)
        } catch (e: Exception) {
            listOf("Ошибка при выполнении детальных тестов подключения: ${e.message}")
        }
    }
    
    private suspend fun validateWebhookResponses(): List<String> {
        return try {
            val baseUrl = "https://hook.us2.make.com/"
            val webhookId = "653st2c10rmg92nlltf3y0m8sggxaac6"
            
            connectivityValidator.validateWebhookResponses(baseUrl, webhookId)
        } catch (e: Exception) {
            listOf("Ошибка при валидации ответов webhook: ${e.message}")
        }
    }
    
    private fun calculateOverallScore(
        networkConfig: NetworkConfigResult,
        apiEndpoints: ApiEndpointResult,
        jsonSerialization: SerializationResult,
        errorHandling: List<String>,
        connectivity: ConnectivityResult,
        connectivityIssues: List<String>,
        responseValidation: List<String>
    ): Int {
        var score = 100
        
        // Снижаем балл за каждую проблему
        score -= networkConfig.issues.size * 5
        score -= apiEndpoints.issues.size * 3
        score -= jsonSerialization.issues.size * 4
        score -= errorHandling.size * 3
        score -= connectivityIssues.size * 2
        score -= responseValidation.size * 2
        
        // Критические проблемы
        if (!networkConfig.isValid) score -= 20
        if (!connectivity.isConnected) score -= 30
        if (!jsonSerialization.isValid) score -= 15
        
        return maxOf(0, score)
    }
    
    override fun getValidatorName(): String = "Webhook Validator"
    
    override fun getCategory(): String = "Webhook Services"
    
    /**
     * Освобождает ресурсы
     */
    fun cleanup() {
        connectivityValidator.cleanup()
    }
}

/**
 * Расширенный отчет о валидации webhook сервиса
 */
data class WebhookValidationReport(
    val networkConfiguration: NetworkConfigResult,
    val apiEndpoints: ApiEndpointResult,
    val jsonSerialization: SerializationResult,
    val errorHandling: List<String>,
    val connectivity: ConnectivityResult,
    val connectivityIssues: List<String>,
    val responseValidation: List<String>,
    val overallScore: Int
) {
    fun getSummary(): String {
        val totalIssues = networkConfiguration.issues.size + 
                         apiEndpoints.issues.size + 
                         jsonSerialization.issues.size + 
                         errorHandling.size + 
                         connectivityIssues.size + 
                         responseValidation.size
        
        return """
            Webhook Validation Summary:
            - Overall Score: $overallScore/100
            - Total Issues: $totalIssues
            - Network Config: ${if (networkConfiguration.isValid) "✓" else "✗"} (${networkConfiguration.issues.size} issues)
            - API Endpoints: ${apiEndpoints.validEndpoints}/${apiEndpoints.totalEndpoints} valid (${apiEndpoints.issues.size} issues)
            - JSON Serialization: ${if (jsonSerialization.isValid) "✓" else "✗"} (${jsonSerialization.issues.size} issues)
            - Error Handling: ${errorHandling.size} issues
            - Connectivity: ${if (connectivity.isConnected) "✓" else "✗"} (${connectivity.responseTime ?: "N/A"}ms)
            - Connectivity Tests: ${connectivityIssues.size} issues
            - Response Validation: ${responseValidation.size} issues
        """.trimIndent()
    }
    
    fun getDetailedReport(): String {
        val sb = StringBuilder()
        sb.appendLine(getSummary())
        sb.appendLine()
        
        if (networkConfiguration.issues.isNotEmpty()) {
            sb.appendLine("Network Configuration Issues:")
            networkConfiguration.issues.forEach { issue ->
                sb.appendLine("  - [${issue.severity}] ${issue.component}: ${issue.issue}")
                issue.fix?.let { sb.appendLine("    Fix: $it") }
            }
            sb.appendLine()
        }
        
        if (apiEndpoints.issues.isNotEmpty()) {
            sb.appendLine("API Endpoints Issues:")
            apiEndpoints.issues.forEach { issue ->
                sb.appendLine("  - ${issue.endpoint} (${issue.method}): ${issue.issue}")
                issue.expectedFormat?.let { sb.appendLine("    Expected: $it") }
            }
            sb.appendLine()
        }
        
        if (jsonSerialization.issues.isNotEmpty()) {
            sb.appendLine("JSON Serialization Issues:")
            jsonSerialization.issues.forEach { issue ->
                sb.appendLine("  - $issue")
            }
            sb.appendLine()
        }
        
        if (errorHandling.isNotEmpty()) {
            sb.appendLine("Error Handling Issues:")
            errorHandling.forEach { issue ->
                sb.appendLine("  - $issue")
            }
            sb.appendLine()
        }
        
        if (connectivityIssues.isNotEmpty()) {
            sb.appendLine("Connectivity Issues:")
            connectivityIssues.forEach { issue ->
                sb.appendLine("  - $issue")
            }
            sb.appendLine()
        }
        
        if (responseValidation.isNotEmpty()) {
            sb.appendLine("Response Validation Issues:")
            responseValidation.forEach { issue ->
                sb.appendLine("  - $issue")
            }
        }
        
        return sb.toString()
    }
}