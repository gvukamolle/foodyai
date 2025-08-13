package com.example.calorietracker.validation

import com.example.calorietracker.validation.error.ValidationErrorHandler
import com.example.calorietracker.validation.interfaces.*
import com.example.calorietracker.validation.models.*
import com.example.calorietracker.validation.reporting.ReportFormatter
import com.example.calorietracker.validation.reporting.ReportGenerator
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Main entry point for the validation system
 * Coordinates all validation activities and generates comprehensive reports
 */
class ValidationSystem(
    private val importValidator: ImportValidator,
    private val webhookValidator: WebhookValidator,
    private val uiDataFlowValidator: UIDataFlowValidator,
    private val diValidator: DIValidator,
    private val errorHandler: ValidationErrorHandler = ValidationErrorHandler(),
    private val reportGenerator: ReportGenerator = ReportGenerator(),
    private val reportFormatter: ReportFormatter = ReportFormatter()
) {
    
    /**
     * Executes comprehensive validation of the entire project
     */
    suspend fun executeComprehensiveValidation(projectPath: String): ValidationReport {
        return coroutineScope {
            try {
                // Run all validations in parallel for better performance
                val importValidationDeferred = async { 
                    runSafeValidation { importValidator.validateImports(projectPath) }
                }
                val webhookValidationDeferred = async { 
                    runSafeValidation { webhookValidator.validateMakeService() }
                }
                val uiValidationDeferred = async { 
                    runSafeValidation { createUIValidationResult() }
                }
                val diValidationDeferred = async { 
                    runSafeValidation { createDIValidationResult() }
                }
                
                // Wait for all validations to complete
                val importValidation = importValidationDeferred.await()
                val webhookValidation = webhookValidationDeferred.await()
                val uiValidation = uiValidationDeferred.await()
                val diValidation = diValidationDeferred.await()
                
                // Generate comprehensive report
                // TODO: Fix parameter names
                /*
                reportGenerator.generateReport(
                    projectPath = projectPath,
                    importValidation = importValidation,
                    webhookValidation = webhookValidation,
                    uiValidation = uiValidation,
                    diValidation = diValidation
                )
                */
                
            } catch (e: Exception) {
                // Handle system-level errors
                val error = errorHandler.createValidationError(e, ErrorType.COMPILATION_ERROR, "ValidationSystem")
                createEmptyReport(projectPath, error)
            }
        }
    }
    
    /**
     * Executes validation for a specific category
     */
    suspend fun executeValidationByCategory(
        projectPath: String,
        category: ValidationCategory
    ): ValidationResult {
        return try {
            when (category) {
                ValidationCategory.IMPORTS -> {
                    val result = importValidator.validateImports(projectPath)
                    ValidationResult.Success("Import validation completed: ${result.unusedImports.size} unused imports found")
                }
                ValidationCategory.WEBHOOKS -> {
                    val result = webhookValidator.validateMakeService()
                    ValidationResult.Success("Webhook validation completed: Network config valid = ${result.networkConfig.isValid}")
                }
                ValidationCategory.UI_DATA_FLOW -> {
                    val result = createUIValidationResult()
                    ValidationResult.Success("UI validation completed: ${result.viewModelValidation.stateFlowIssues.size} StateFlow issues found")
                }
                ValidationCategory.DEPENDENCY_INJECTION -> {
                    val result = createDIValidationResult()
                    ValidationResult.Success("DI validation completed: ${result.hiltValidation.moduleIssues.size} module issues found")
                }
            }
        } catch (e: Exception) {
            val error = errorHandler.createValidationError(e, ErrorType.COMPILATION_ERROR, category.name)
            errorHandler.handleValidationError(error)
        }
    }
    
    /**
     * Formats validation report for different output types
     */
    fun formatReport(report: ValidationReport, format: ReportFormat): String {
        return when (format) {
            ReportFormat.MARKDOWN -> reportFormatter.formatAsMarkdown(report)
            ReportFormat.CONSOLE -> reportFormatter.formatForConsole(report)
            ReportFormat.JSON -> reportFormatter.formatAsJson(report)
        }
    }
    
    /**
     * Gets validation system status
     */
    fun getSystemStatus(): ValidationSystemStatus {
        return ValidationSystemStatus(
            isInitialized = true,
            availableValidators = listOf(
                importValidator.getValidatorName(),
                webhookValidator.getValidatorName(),
                uiDataFlowValidator.getValidatorName(),
                diValidator.getValidatorName()
            ),
            version = "1.0.0"
        )
    }
    
    private suspend fun <T> runSafeValidation(validation: suspend () -> T): T {
        return try {
            validation()
        } catch (e: Exception) {
            throw ValidationSystemException("Validation failed: ${e.message}", e)
        }
    }
    
    private suspend fun createUIValidationResult(): UIValidationResult {
        return UIValidationResult(
            viewModelValidation = uiDataFlowValidator.validateViewModels(),
            dataBinding = uiDataFlowValidator.validateDataBinding(),
            stateManagement = uiDataFlowValidator.validateStateManagement(),
            uiComponents = uiDataFlowValidator.validateUIComponents()
        )
    }
    
    private suspend fun createDIValidationResult(): DIValidationResult {
        return DIValidationResult(
            hiltValidation = diValidator.validateHiltModules(),
            dependencyGraph = diValidator.validateDependencyGraph(),
            scopes = diValidator.validateScopes()
        )
    }
    
    private fun createEmptyReport(projectPath: String, error: ValidationError): ValidationReport {
        return ValidationReport(
            timestamp = System.currentTimeMillis(),
            projectPath = projectPath,
            summary = ValidationSummary(
                totalIssues = 1,
                criticalIssues = 1,
                warningIssues = 0,
                infoIssues = 0,
                overallScore = 0
            ),
            importValidation = ImportValidationResult(
                unusedImports = emptyList(),
                missingImports = emptyList(),
                architecturalViolations = emptyList(),
                circularDependencies = emptyList()
            ),
            webhookValidation = WebhookValidationResult(
                networkConfig = NetworkConfigResult(emptyList(), false),
                apiEndpoints = ApiEndpointResult(emptyList(), 0, 0),
                connectivity = ConnectivityResult(false, null, error.message),
                jsonSerialization = SerializationResult(emptyList(), false)
            ),
            uiValidation = UIValidationResult(
                viewModelValidation = ViewModelValidationResult(emptyList(), emptyList(), emptyList()),
                dataBinding = DataBindingResult(emptyList(), false),
                stateManagement = StateManagementResult(emptyList(), false),
                uiComponents = UIComponentResult(emptyList(), 0, 0)
            ),
            diValidation = DIValidationResult(
                hiltValidation = HiltValidationResult(emptyList(), emptyList(), emptyList()),
                dependencyGraph = DependencyGraphResult(emptyList(), emptyList(), false),
                scopes = ScopeValidationResult(emptyList(), false)
            ),
            recommendations = emptyList()
        )
    }
}

/**
 * Validation categories
 */
enum class ValidationCategory {
    IMPORTS,
    WEBHOOKS,
    UI_DATA_FLOW,
    DEPENDENCY_INJECTION
}

/**
 * Report output formats
 */
enum class ReportFormat {
    MARKDOWN,
    CONSOLE,
    JSON
}

/**
 * Validation system status
 */
data class ValidationSystemStatus(
    val isInitialized: Boolean,
    val availableValidators: List<String>,
    val version: String
)

/**
 * Custom exception for validation system errors
 */
class ValidationSystemException(message: String, cause: Throwable? = null) : Exception(message, cause)