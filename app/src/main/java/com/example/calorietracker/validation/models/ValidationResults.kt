package com.example.calorietracker.validation.models

/**
 * Import validation result container
 */
data class ImportValidationResult(
    val unusedImports: List<UnusedImport>,
    val missingImports: List<MissingImport>,
    val architecturalViolations: List<ArchitecturalViolation>,
    val circularDependencies: List<CircularDependency>
)

/**
 * Webhook validation result container
 */
data class WebhookValidationResult(
    val networkConfig: NetworkConfigResult,
    val apiEndpoints: ApiEndpointResult,
    val connectivity: ConnectivityResult,
    val jsonSerialization: SerializationResult
)

data class NetworkConfigResult(
    val issues: List<NetworkConfigIssue>,
    val isValid: Boolean
)

data class ApiEndpointResult(
    val issues: List<ApiEndpointIssue>,
    val validEndpoints: Int,
    val totalEndpoints: Int
)

data class ConnectivityResult(
    val isConnected: Boolean,
    val responseTime: Long?,
    val errorMessage: String?
) {
    val isSuccessful: Boolean get() = isConnected
}

data class SerializationResult(
    val issues: List<String>,
    val isValid: Boolean
)

/**
 * UI validation result container
 */
data class UIValidationResult(
    val viewModelValidation: ViewModelValidationResult,
    val dataBinding: DataBindingResult,
    val stateManagement: StateManagementResult,
    val uiComponents: UIComponentResult
)

data class ViewModelValidationResult(
    val stateFlowIssues: List<StateFlowIssue>,
    val dataBindingIssues: List<DataBindingIssue>,
    val lifecycleIssues: List<String>
)

data class DataBindingResult(
    val issues: List<DataBindingIssue>,
    val isValid: Boolean
)

data class StateManagementResult(
    val issues: List<StateFlowIssue>,
    val isValid: Boolean
)

data class UIComponentResult(
    val issues: List<String>,
    val validComponents: Int,
    val totalComponents: Int
)

/**
 * DI validation result container
 */
data class DIValidationResult(
    val hiltValidation: HiltValidationResult,
    val dependencyGraph: DependencyGraphResult,
    val scopes: ScopeValidationResult
)

data class HiltValidationResult(
    val moduleIssues: List<ModuleIssue>,
    val bindingIssues: List<BindingIssue>,
    val scopeIssues: List<ScopeIssue>
)

data class DependencyGraphResult(
    val circularDependencies: List<String>,
    val missingDependencies: List<String>,
    val isValid: Boolean
)

data class ScopeValidationResult(
    val issues: List<ScopeIssue>,
    val isValid: Boolean
)