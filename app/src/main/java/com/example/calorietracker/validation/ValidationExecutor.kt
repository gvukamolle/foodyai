package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.*
import com.example.calorietracker.validation.reporting.ComprehensiveReportSystem
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Main executor for comprehensive code validation
 * Executes all validation tasks and generates reports
 */
class ValidationExecutor {
    
    private val comprehensiveReportSystem = ComprehensiveReportSystem()
    
    /**
     * Executes comprehensive validation for the entire project
     */
    fun executeComprehensiveValidation(projectPath: String = "."): ValidationExecutionResult {
        return runBlocking {
            try {
                println("🚀 Starting Comprehensive Code Validation...")
                println("Project Path: $projectPath")
                println("=" * 60)
                
                // Create validation system with real implementations
                val validationSystem = createValidationSystem()
                
                // Execute all validation tasks
                val results = ValidationExecutionResult()
                
                // Task 8.1: Run complete import validation
                println("\n📋 Task 8.1: Running Import Validation...")
                val importResult = executeImportValidation(validationSystem, projectPath)
                results.importValidation = importResult
                printTaskResult("Import Validation", importResult.isSuccessful)
                
                // Task 8.2: Execute webhook service validation
                println("\n🌐 Task 8.2: Running Webhook Service Validation...")
                val webhookResult = executeWebhookValidation(validationSystem, projectPath)
                results.webhookValidation = webhookResult
                printTaskResult("Webhook Validation", webhookResult.isSuccessful)
                
                // Task 8.3: Execute UI data flow validation
                println("\n🎨 Task 8.3: Running UI Data Flow Validation...")
                val uiResult = executeUIValidation(validationSystem, projectPath)
                results.uiValidation = uiResult
                printTaskResult("UI Data Flow Validation", uiResult.isSuccessful)
                
                // Task 8.4: Execute DI configuration validation
                println("\n🔧 Task 8.4: Running DI Configuration Validation...")
                val diResult = executeDIValidation(validationSystem, projectPath)
                results.diValidation = diResult
                printTaskResult("DI Configuration Validation", diResult.isSuccessful)
                
                // Task 8.5: Generate final comprehensive report
                println("\n📊 Task 8.5: Generating Comprehensive Report...")
                val report = generateComprehensiveReport(validationSystem, projectPath, results)
                results.finalReport = report
                printTaskResult("Report Generation", report != null)
                
                // Save reports to files
                saveReports(report, projectPath)
                
                println("\n" + "=" * 60)
                println("✅ Comprehensive Code Validation Completed!")
                printExecutionSummary(results)
                
                results
                
            } catch (e: Exception) {
                println("❌ Validation execution failed: ${e.message}")
                e.printStackTrace()
                ValidationExecutionResult().apply {
                    executionError = e.message
                }
            }
        }
    }
    
    /**
     * Task 8.1: Execute import validation across all Kotlin files
     */
    private suspend fun executeImportValidation(
        validationSystem: ValidationSystem,
        projectPath: String
    ): ImportValidationTaskResult {
        return try {
            val result = validationSystem.executeValidationByCategory(
                projectPath, 
                ValidationCategory.IMPORTS
            )
            
            // Get detailed import validation results
            val importValidator = createImportValidator()
            val detailedResult = importValidator.validateImports(projectPath)
            
            ImportValidationTaskResult(
                isSuccessful = result is ValidationResult.Success,
                unusedImportsCount = detailedResult.unusedImports.size,
                missingImportsCount = detailedResult.missingImports.size,
                architecturalViolationsCount = detailedResult.architecturalViolations.size,
                circularDependenciesCount = detailedResult.circularDependencies.size,
                detailedResult = detailedResult,
                message = when (result) {
                    is ValidationResult.Success -> result.message
                    is ValidationResult.Warning -> result.message
                    is ValidationResult.Error -> result.message
                }
            )
        } catch (e: Exception) {
            ImportValidationTaskResult(
                isSuccessful = false,
                message = "Import validation failed: ${e.message}"
            )
        }
    }
    
    /**
     * Task 8.2: Execute webhook service validation
     */
    private suspend fun executeWebhookValidation(
        validationSystem: ValidationSystem,
        projectPath: String
    ): WebhookValidationTaskResult {
        return try {
            val result = validationSystem.executeValidationByCategory(
                projectPath,
                ValidationCategory.WEBHOOKS
            )
            
            // Get detailed webhook validation results
            val webhookValidator = createWebhookValidator()
            val detailedResult = webhookValidator.validateMakeService()
            
            WebhookValidationTaskResult(
                isSuccessful = result is ValidationResult.Success,
                networkConfigValid = detailedResult.networkConfig.isValid,
                apiEndpointsValid = detailedResult.apiEndpoints.validEndpoints == detailedResult.apiEndpoints.totalEndpoints,
                connectivitySuccessful = detailedResult.connectivity.isSuccessful,
                jsonSerializationValid = detailedResult.jsonSerialization.isValid,
                detailedResult = detailedResult,
                message = when (result) {
                    is ValidationResult.Success -> result.message
                    is ValidationResult.Warning -> result.message
                    is ValidationResult.Error -> result.message
                }
            )
        } catch (e: Exception) {
            WebhookValidationTaskResult(
                isSuccessful = false,
                message = "Webhook validation failed: ${e.message}"
            )
        }
    }
    
    /**
     * Task 8.3: Execute UI data flow validation
     */
    private suspend fun executeUIValidation(
        validationSystem: ValidationSystem,
        projectPath: String
    ): UIValidationTaskResult {
        return try {
            val result = validationSystem.executeValidationByCategory(
                projectPath,
                ValidationCategory.UI_DATA_FLOW
            )
            
            // Get detailed UI validation results
            val uiValidator = createUIDataFlowValidator()
            val viewModelResult = uiValidator.validateViewModels()
            val dataBindingResult = uiValidator.validateDataBinding()
            val stateManagementResult = uiValidator.validateStateManagement()
            val uiComponentsResult = uiValidator.validateUIComponents()
            
            UIValidationTaskResult(
                isSuccessful = result is ValidationResult.Success,
                viewModelIssuesCount = viewModelResult.stateFlowIssues.size + 
                                     viewModelResult.dataBindingIssues.size + 
                                     viewModelResult.lifecycleIssues.size,
                dataBindingValid = dataBindingResult.isValid,
                stateManagementValid = stateManagementResult.isValid,
                uiComponentsValid = uiComponentsResult.validComponents == uiComponentsResult.totalComponents,
                message = when (result) {
                    is ValidationResult.Success -> result.message
                    is ValidationResult.Warning -> result.message
                    is ValidationResult.Error -> result.message
                }
            )
        } catch (e: Exception) {
            UIValidationTaskResult(
                isSuccessful = false,
                message = "UI validation failed: ${e.message}"
            )
        }
    }
    
    /**
     * Task 8.4: Execute DI configuration validation
     */
    private suspend fun executeDIValidation(
        validationSystem: ValidationSystem,
        projectPath: String
    ): DIValidationTaskResult {
        return try {
            val result = validationSystem.executeValidationByCategory(
                projectPath,
                ValidationCategory.DEPENDENCY_INJECTION
            )
            
            // Get detailed DI validation results
            val diValidator = createDIValidator()
            val hiltResult = diValidator.validateHiltModules()
            val dependencyGraphResult = diValidator.validateDependencyGraph()
            val scopesResult = diValidator.validateScopes()
            
            DIValidationTaskResult(
                isSuccessful = result is ValidationResult.Success,
                hiltModulesValid = hiltResult.moduleIssues.isEmpty() && 
                                 hiltResult.bindingIssues.isEmpty() && 
                                 hiltResult.scopeIssues.isEmpty(),
                dependencyGraphValid = dependencyGraphResult.isValid,
                scopesValid = scopesResult.isValid,
                moduleIssuesCount = hiltResult.moduleIssues.size,
                bindingIssuesCount = hiltResult.bindingIssues.size,
                message = when (result) {
                    is ValidationResult.Success -> result.message
                    is ValidationResult.Warning -> result.message
                    is ValidationResult.Error -> result.message
                }
            )
        } catch (e: Exception) {
            DIValidationTaskResult(
                isSuccessful = false,
                message = "DI validation failed: ${e.message}"
            )
        }
    }
    
    /**
     * Task 8.5: Generate final comprehensive report
     */
    private suspend fun generateComprehensiveReport(
        validationSystem: ValidationSystem,
        projectPath: String,
        results: ValidationExecutionResult
    ): ValidationReport? {
        return try {
            val report = validationSystem.executeComprehensiveValidation(projectPath)
            
            // Generate comprehensive report using the report system
            comprehensiveReportSystem.generateCompleteReport(
                projectPath = projectPath,
                importResult = results.importValidation?.detailedResult ?: ImportValidationResult(
                    unusedImports = emptyList(),
                    missingImports = emptyList(),
                    architecturalViolations = emptyList(),
                    circularDependencies = emptyList()
                ),
                webhookResult = results.webhookValidation?.detailedResult ?: WebhookValidationResult(
                    networkConfig = NetworkConfigResult(emptyList(), false),
                    apiEndpoints = ApiEndpointResult(emptyList(), 0, 0),
                    connectivity = ConnectivityResult(false, null, "Not tested"),
                    jsonSerialization = SerializationResult(emptyList(), false)
                ),
                uiResult = UIValidationResult(
                    viewModelValidation = ViewModelValidationResult(emptyList(), emptyList(), emptyList()),
                    dataBinding = DataBindingResult(emptyList(), true),
                    stateManagement = StateManagementResult(emptyList(), true),
                    uiComponents = UIComponentResult(emptyList(), 0, 0)
                ),
                diResult = DIValidationResult(
                    hiltValidation = HiltValidationResult(emptyList(), emptyList(), emptyList()),
                    dependencyGraph = DependencyGraphResult(emptyList(), emptyList(), true),
                    scopes = ScopeValidationResult(emptyList(), true)
                )
            )
            
            report
        } catch (e: Exception) {
            println("Failed to generate comprehensive report: ${e.message}")
            null
        }
    }
    
    /**
     * Save reports to files
     */
    private fun saveReports(report: ValidationReport?, projectPath: String) {
        if (report == null) return
        
        try {
            val reportsDir = File("$projectPath/validation-reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            
            // Save markdown report
            val validationSystem = createValidationSystem()
            val markdownReport = validationSystem.formatReport(report, ReportFormat.MARKDOWN)
            File(reportsDir, "validation-report-$timestamp.md").writeText(markdownReport)
            
            // Save console report
            val consoleReport = validationSystem.formatReport(report, ReportFormat.CONSOLE)
            File(reportsDir, "validation-report-$timestamp.txt").writeText(consoleReport)
            
            // Save JSON report
            val jsonReport = validationSystem.formatReport(report, ReportFormat.JSON)
            File(reportsDir, "validation-report-$timestamp.json").writeText(jsonReport)
            
            println("📁 Reports saved to: ${reportsDir.absolutePath}")
            
        } catch (e: Exception) {
            println("⚠️  Failed to save reports: ${e.message}")
        }
    }
    
    private fun createValidationSystem(): ValidationSystem {
        return ValidationSystemFactory.create(
            importValidator = createImportValidator(),
            webhookValidator = createWebhookValidator(),
            uiDataFlowValidator = createUIDataFlowValidator(),
            diValidator = createDIValidator()
        )
    }
    
    private fun createImportValidator() = ImportValidatorImpl()
    private fun createWebhookValidator() = WebhookValidatorImpl()
    private fun createUIDataFlowValidator() = UIDataFlowValidatorImpl(
        viewModelValidator = ViewModelStateValidator(),
        composeValidator = ComposeDataBindingValidator()
    )
    private fun createDIValidator() = DIValidatorImpl()
    
    private fun printTaskResult(taskName: String, success: Boolean) {
        val status = if (success) "✅ PASSED" else "❌ FAILED"
        println("   $status - $taskName")
    }
    
    private fun printExecutionSummary(results: ValidationExecutionResult) {
        println("📈 Execution Summary:")
        println("   Import Validation: ${if (results.importValidation?.isSuccessful == true) "✅" else "❌"}")
        println("   Webhook Validation: ${if (results.webhookValidation?.isSuccessful == true) "✅" else "❌"}")
        println("   UI Data Flow Validation: ${if (results.uiValidation?.isSuccessful == true) "✅" else "❌"}")
        println("   DI Configuration Validation: ${if (results.diValidation?.isSuccessful == true) "✅" else "❌"}")
        println("   Report Generation: ${if (results.finalReport != null) "✅" else "❌"}")
        
        if (results.executionError != null) {
            println("   ⚠️  Execution Error: ${results.executionError}")
        }
    }
}

/**
 * Result of the entire validation execution
 */
data class ValidationExecutionResult(
    var importValidation: ImportValidationTaskResult? = null,
    var webhookValidation: WebhookValidationTaskResult? = null,
    var uiValidation: UIValidationTaskResult? = null,
    var diValidation: DIValidationTaskResult? = null,
    var finalReport: ValidationReport? = null,
    var executionError: String? = null
)

/**
 * Result of import validation task
 */
data class ImportValidationTaskResult(
    val isSuccessful: Boolean,
    val unusedImportsCount: Int = 0,
    val missingImportsCount: Int = 0,
    val architecturalViolationsCount: Int = 0,
    val circularDependenciesCount: Int = 0,
    val detailedResult: ImportValidationResult? = null,
    val message: String = ""
)

/**
 * Result of webhook validation task
 */
data class WebhookValidationTaskResult(
    val isSuccessful: Boolean,
    val networkConfigValid: Boolean = false,
    val apiEndpointsValid: Boolean = false,
    val connectivitySuccessful: Boolean = false,
    val jsonSerializationValid: Boolean = false,
    val detailedResult: WebhookValidationResult? = null,
    val message: String = ""
)

/**
 * Result of UI validation task
 */
data class UIValidationTaskResult(
    val isSuccessful: Boolean,
    val viewModelIssuesCount: Int = 0,
    val dataBindingValid: Boolean = false,
    val stateManagementValid: Boolean = false,
    val uiComponentsValid: Boolean = false,
    val message: String = ""
)

/**
 * Result of DI validation task
 */
data class DIValidationTaskResult(
    val isSuccessful: Boolean,
    val hiltModulesValid: Boolean = false,
    val dependencyGraphValid: Boolean = false,
    val scopesValid: Boolean = false,
    val moduleIssuesCount: Int = 0,
    val bindingIssuesCount: Int = 0,
    val message: String = ""
)

/**
 * Extension function for string repetition
 */
private operator fun String.times(n: Int): String = this.repeat(n)