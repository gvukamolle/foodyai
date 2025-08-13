package com.example.calorietracker.validation.models

import com.example.calorietracker.validation.Priority
import com.example.calorietracker.validation.Severity
import com.example.calorietracker.validation.ViolationType

/**
 * Import validation models
 */
data class UnusedImport(
    val filePath: String,
    val importStatement: String,
    val lineNumber: Int
)

data class MissingImport(
    val filePath: String,
    val missingClass: String,
    val suggestedImport: String?,
    val lineNumber: Int
)

data class ArchitecturalViolation(
    val filePath: String,
    val violationType: ViolationType,
    val description: String,
    val suggestion: String
)

data class CircularDependency(
    val filePaths: List<String>,
    val description: String
)

/**
 * Webhook validation models
 */
data class NetworkConfigIssue(
    val component: String,
    val issue: String,
    val severity: Severity,
    val fix: String?
)

data class ApiEndpointIssue(
    val endpoint: String,
    val method: String,
    val issue: String,
    val expectedFormat: String?
)

/**
 * UI validation models
 */
data class StateFlowIssue(
    val viewModelClass: String,
    val stateProperty: String,
    val issue: String,
    val recommendation: String
)

data class DataBindingIssue(
    val componentFile: String,
    val bindingProperty: String,
    val issue: String,
    val fix: String?
)

/**
 * DI validation models
 */
data class ModuleIssue(
    val moduleName: String,
    val issue: String,
    val severity: Severity,
    val fix: String?
)

data class BindingIssue(
    val interfaceName: String,
    val implementationName: String?,
    val issue: String,
    val fix: String?
)

data class ScopeIssue(
    val componentName: String,
    val scopeIssue: String,
    val recommendation: String
)

/**
 * Validation error model
 */
data class ValidationError(
    val type: com.example.calorietracker.validation.ErrorType,
    val message: String,
    val details: String,
    val suggestedFix: String?
)