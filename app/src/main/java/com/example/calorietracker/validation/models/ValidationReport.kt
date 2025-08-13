package com.example.calorietracker.validation.models

import com.example.calorietracker.validation.Priority

/**
 * Comprehensive validation report containing all validation results
 */
data class ValidationReport(
    val timestamp: Long,
    val projectPath: String,
    val summary: ValidationSummary,
    val importValidation: ImportValidationResult,
    val webhookValidation: WebhookValidationResult,
    val uiValidation: UIValidationResult,
    val diValidation: DIValidationResult,
    val recommendations: List<Recommendation>
)

/**
 * Summary of validation results with statistics
 */
data class ValidationSummary(
    val totalIssues: Int,
    val criticalIssues: Int,
    val warningIssues: Int,
    val infoIssues: Int,
    val overallScore: Int, // 0-100
    val validationCategories: Map<String, CategorySummary>
)

/**
 * Summary for each validation category
 */
data class CategorySummary(
    val categoryName: String,
    val totalIssues: Int,
    val criticalIssues: Int,
    val warningIssues: Int,
    val infoIssues: Int,
    val score: Int // 0-100
)

/**
 * Actionable recommendation for fixing issues
 */
data class Recommendation(
    val category: String,
    val priority: Priority,
    val title: String,
    val description: String,
    val actionItems: List<String>,
    val affectedFiles: List<String> = emptyList(),
    val estimatedEffort: String? = null
)

/**
 * Detailed issue information for reporting
 */
data class DetailedIssue(
    val category: String,
    val severity: com.example.calorietracker.validation.Severity,
    val title: String,
    val description: String,
    val filePath: String?,
    val lineNumber: Int?,
    val suggestedFix: String?,
    val priority: Priority
)