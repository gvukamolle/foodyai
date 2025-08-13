package com.example.calorietracker.validation.reporting

import com.example.calorietracker.validation.models.*

/**
 * Aggregates validation results from different validation systems
 * and provides unified access to validation data
 */
class ValidationResultAggregator {
    
    private val importResults = mutableListOf<ImportValidationResult>()
    private val webhookResults = mutableListOf<WebhookValidationResult>()
    private val uiResults = mutableListOf<UIValidationResult>()
    private val diResults = mutableListOf<DIValidationResult>()
    
    /**
     * Add import validation result to aggregation
     */
    fun addImportResult(result: ImportValidationResult) {
        importResults.add(result)
    }
    
    /**
     * Add webhook validation result to aggregation
     */
    fun addWebhookResult(result: WebhookValidationResult) {
        webhookResults.add(result)
    }
    
    /**
     * Add UI validation result to aggregation
     */
    fun addUIResult(result: UIValidationResult) {
        uiResults.add(result)
    }
    
    /**
     * Add DI validation result to aggregation
     */
    fun addDIResult(result: DIValidationResult) {
        diResults.add(result)
    }
    
    /**
     * Get aggregated import validation result
     */
    fun getAggregatedImportResult(): ImportValidationResult {
        return ImportValidationResult(
            unusedImports = importResults.flatMap { it.unusedImports },
            missingImports = importResults.flatMap { it.missingImports },
            architecturalViolations = importResults.flatMap { it.architecturalViolations },
            circularDependencies = importResults.flatMap { it.circularDependencies }
        )
    }
    
    /**
     * Get aggregated webhook validation result
     */
    fun getAggregatedWebhookResult(): WebhookValidationResult {
        val allNetworkIssues = webhookResults.flatMap { it.networkConfig.issues }
        val allApiIssues = webhookResults.flatMap { it.apiEndpoints.issues }
        val allSerializationIssues = webhookResults.flatMap { it.jsonSerialization.issues }
        
        // Take the most recent connectivity result or combine them
        val connectivityResult = webhookResults.lastOrNull()?.connectivity ?: ConnectivityResult(
            isConnected = false,
            responseTime = null,
            errorMessage = "No connectivity tests performed"
        )
        
        return WebhookValidationResult(
            networkConfig = NetworkConfigResult(
                issues = allNetworkIssues,
                isValid = allNetworkIssues.isEmpty()
            ),
            apiEndpoints = ApiEndpointResult(
                issues = allApiIssues,
                validEndpoints = webhookResults.sumOf { it.apiEndpoints.validEndpoints },
                totalEndpoints = webhookResults.sumOf { it.apiEndpoints.totalEndpoints }
            ),
            connectivity = connectivityResult,
            jsonSerialization = SerializationResult(
                issues = allSerializationIssues,
                isValid = allSerializationIssues.isEmpty()
            )
        )
    }
    
    /**
     * Get aggregated UI validation result
     */
    fun getAggregatedUIResult(): UIValidationResult {
        val allStateFlowIssues = uiResults.flatMap { it.viewModelValidation.stateFlowIssues }
        val allDataBindingIssues = uiResults.flatMap { it.viewModelValidation.dataBindingIssues }
        val allLifecycleIssues = uiResults.flatMap { it.viewModelValidation.lifecycleIssues }
        val allUIComponentIssues = uiResults.flatMap { it.uiComponents.issues }
        
        return UIValidationResult(
            viewModelValidation = ViewModelValidationResult(
                stateFlowIssues = allStateFlowIssues,
                dataBindingIssues = allDataBindingIssues,
                lifecycleIssues = allLifecycleIssues
            ),
            dataBinding = DataBindingResult(
                issues = allDataBindingIssues,
                isValid = allDataBindingIssues.isEmpty()
            ),
            stateManagement = StateManagementResult(
                issues = allStateFlowIssues,
                isValid = allStateFlowIssues.isEmpty()
            ),
            uiComponents = UIComponentResult(
                issues = allUIComponentIssues,
                validComponents = uiResults.sumOf { it.uiComponents.validComponents },
                totalComponents = uiResults.sumOf { it.uiComponents.totalComponents }
            )
        )
    }
    
    /**
     * Get aggregated DI validation result
     */
    fun getAggregatedDIResult(): DIValidationResult {
        val allModuleIssues = diResults.flatMap { it.hiltValidation.moduleIssues }
        val allBindingIssues = diResults.flatMap { it.hiltValidation.bindingIssues }
        val allScopeIssues = diResults.flatMap { it.hiltValidation.scopeIssues }
        val allCircularDeps = diResults.flatMap { it.dependencyGraph.circularDependencies }
        val allMissingDeps = diResults.flatMap { it.dependencyGraph.missingDependencies }
        
        return DIValidationResult(
            hiltValidation = HiltValidationResult(
                moduleIssues = allModuleIssues,
                bindingIssues = allBindingIssues,
                scopeIssues = allScopeIssues
            ),
            dependencyGraph = DependencyGraphResult(
                circularDependencies = allCircularDeps,
                missingDependencies = allMissingDeps,
                isValid = allCircularDeps.isEmpty() && allMissingDeps.isEmpty()
            ),
            scopes = ScopeValidationResult(
                issues = allScopeIssues,
                isValid = allScopeIssues.isEmpty()
            )
        )
    }
    
    /**
     * Clear all aggregated results
     */
    fun clear() {
        importResults.clear()
        webhookResults.clear()
        uiResults.clear()
        diResults.clear()
    }
    
    /**
     * Get summary statistics of aggregated results
     */
    fun getSummaryStatistics(): AggregationSummary {
        val importResult = getAggregatedImportResult()
        val webhookResult = getAggregatedWebhookResult()
        val uiResult = getAggregatedUIResult()
        val diResult = getAggregatedDIResult()
        
        return AggregationSummary(
            totalImportIssues = importResult.unusedImports.size + 
                               importResult.missingImports.size + 
                               importResult.architecturalViolations.size + 
                               importResult.circularDependencies.size,
            totalWebhookIssues = webhookResult.networkConfig.issues.size + 
                                webhookResult.apiEndpoints.issues.size + 
                                webhookResult.jsonSerialization.issues.size,
            totalUIIssues = uiResult.viewModelValidation.stateFlowIssues.size + 
                           uiResult.viewModelValidation.dataBindingIssues.size + 
                           uiResult.viewModelValidation.lifecycleIssues.size,
            totalDIIssues = diResult.hiltValidation.moduleIssues.size + 
                           diResult.hiltValidation.bindingIssues.size + 
                           diResult.hiltValidation.scopeIssues.size,
            hasConnectivityIssues = !webhookResult.connectivity.isConnected
        )
    }
}

/**
 * Summary of aggregated validation results
 */
data class AggregationSummary(
    val totalImportIssues: Int,
    val totalWebhookIssues: Int,
    val totalUIIssues: Int,
    val totalDIIssues: Int,
    val hasConnectivityIssues: Boolean
) {
    val totalIssues: Int = totalImportIssues + totalWebhookIssues + totalUIIssues + totalDIIssues
}