package com.example.calorietracker.validation.reporting

import com.example.calorietracker.validation.Priority
import com.example.calorietracker.validation.Severity
import com.example.calorietracker.validation.models.*

/**
 * Main report generation engine that aggregates validation results
 * and creates comprehensive reports with actionable recommendations
 */
class ReportGenerator {
    
    /**
     * Generate comprehensive validation report from all validation results
     */
    fun generateReport(
        projectPath: String,
        importResult: ImportValidationResult,
        webhookResult: WebhookValidationResult,
        uiResult: UIValidationResult,
        diResult: DIValidationResult
    ): ValidationReport {
        val timestamp = System.currentTimeMillis()
        
        // Aggregate all issues
        val allIssues = aggregateIssues(importResult, webhookResult, uiResult, diResult)
        
        // Generate summary statistics
        val summary = generateSummary(allIssues)
        
        // Generate actionable recommendations
        val recommendations = generateRecommendations(allIssues, importResult, webhookResult, uiResult, diResult)
        
        return ValidationReport(
            timestamp = timestamp,
            projectPath = projectPath,
            summary = summary,
            importValidation = importResult,
            webhookValidation = webhookResult,
            uiValidation = uiResult,
            diValidation = diResult,
            recommendations = recommendations
        )
    }
    
    /**
     * Aggregate all validation issues into a unified list
     */
    private fun aggregateIssues(
        importResult: ImportValidationResult,
        webhookResult: WebhookValidationResult,
        uiResult: UIValidationResult,
        diResult: DIValidationResult
    ): List<DetailedIssue> {
        val issues = mutableListOf<DetailedIssue>()
        
        // Import validation issues
        issues.addAll(convertImportIssues(importResult))
        
        // Webhook validation issues
        issues.addAll(convertWebhookIssues(webhookResult))
        
        // UI validation issues
        issues.addAll(convertUIIssues(uiResult))
        
        // DI validation issues
        issues.addAll(convertDIIssues(diResult))
        
        return issues
    }
    
    /**
     * Convert import validation results to detailed issues
     */
    private fun convertImportIssues(result: ImportValidationResult): List<DetailedIssue> {
        val issues = mutableListOf<DetailedIssue>()
        
        // Unused imports
        result.unusedImports.forEach { unused ->
            issues.add(
                DetailedIssue(
                    category = "Import Validation",
                    severity = Severity.WARNING,
                    title = "Unused Import",
                    description = "Import '${unused.importStatement}' is not used in the file",
                    filePath = unused.filePath,
                    lineNumber = unused.lineNumber,
                    suggestedFix = "Remove the unused import statement",
                    priority = Priority.LOW
                )
            )
        }
        
        // Missing imports
        result.missingImports.forEach { missing ->
            issues.add(
                DetailedIssue(
                    category = "Import Validation",
                    severity = Severity.CRITICAL,
                    title = "Missing Import",
                    description = "Missing import for '${missing.missingClass}'",
                    filePath = missing.filePath,
                    lineNumber = missing.lineNumber,
                    suggestedFix = missing.suggestedImport?.let { "Add import: $it" },
                    priority = Priority.HIGH
                )
            )
        }
        
        // Architectural violations
        result.architecturalViolations.forEach { violation ->
            issues.add(
                DetailedIssue(
                    category = "Architecture Compliance",
                    severity = Severity.CRITICAL,
                    title = "Architectural Violation",
                    description = violation.description,
                    filePath = violation.filePath,
                    lineNumber = null,
                    suggestedFix = violation.suggestion,
                    priority = Priority.HIGH
                )
            )
        }
        
        // Circular dependencies
        result.circularDependencies.forEach { circular ->
            issues.add(
                DetailedIssue(
                    category = "Import Validation",
                    severity = Severity.CRITICAL,
                    title = "Circular Dependency",
                    description = circular.description,
                    filePath = circular.filePaths.firstOrNull(),
                    lineNumber = null,
                    suggestedFix = "Refactor to break circular dependency",
                    priority = Priority.HIGH
                )
            )
        }
        
        return issues
    }
    
    /**
     * Convert webhook validation results to detailed issues
     */
    private fun convertWebhookIssues(result: WebhookValidationResult): List<DetailedIssue> {
        val issues = mutableListOf<DetailedIssue>()
        
        // Network configuration issues
        result.networkConfig.issues.forEach { issue ->
            issues.add(
                DetailedIssue(
                    category = "Webhook Validation",
                    severity = issue.severity,
                    title = "Network Configuration Issue",
                    description = "Component '${issue.component}': ${issue.issue}",
                    filePath = null,
                    lineNumber = null,
                    suggestedFix = issue.fix,
                    priority = when (issue.severity) {
                        Severity.CRITICAL -> Priority.HIGH
                        Severity.WARNING -> Priority.MEDIUM
                        Severity.INFO -> Priority.LOW
                    }
                )
            )
        }
        
        // API endpoint issues
        result.apiEndpoints.issues.forEach { issue ->
            issues.add(
                DetailedIssue(
                    category = "Webhook Validation",
                    severity = Severity.WARNING,
                    title = "API Endpoint Issue",
                    description = "Endpoint '${issue.endpoint}' (${issue.method}): ${issue.issue}",
                    filePath = null,
                    lineNumber = null,
                    suggestedFix = issue.expectedFormat?.let { "Expected format: $it" },
                    priority = Priority.MEDIUM
                )
            )
        }
        
        // Connectivity issues
        if (!result.connectivity.isConnected) {
            issues.add(
                DetailedIssue(
                    category = "Webhook Validation",
                    severity = Severity.CRITICAL,
                    title = "Connectivity Issue",
                    description = result.connectivity.errorMessage ?: "Unable to connect to webhook endpoint",
                    filePath = null,
                    lineNumber = null,
                    suggestedFix = "Check network configuration and endpoint URL",
                    priority = Priority.HIGH
                )
            )
        }
        
        // Serialization issues
        if (!result.jsonSerialization.isValid) {
            result.jsonSerialization.issues.forEach { issue ->
                issues.add(
                    DetailedIssue(
                        category = "Webhook Validation",
                        severity = Severity.WARNING,
                        title = "JSON Serialization Issue",
                        description = issue,
                        filePath = null,
                        lineNumber = null,
                        suggestedFix = "Review data class annotations and field mappings",
                        priority = Priority.MEDIUM
                    )
                )
            }
        }
        
        return issues
    }
    
    /**
     * Convert UI validation results to detailed issues
     */
    private fun convertUIIssues(result: UIValidationResult): List<DetailedIssue> {
        val issues = mutableListOf<DetailedIssue>()
        
        // StateFlow issues
        result.viewModelValidation.stateFlowIssues.forEach { issue ->
            issues.add(
                DetailedIssue(
                    category = "UI Data Flow",
                    severity = Severity.WARNING,
                    title = "StateFlow Issue",
                    description = "ViewModel '${issue.viewModelClass}', property '${issue.stateProperty}': ${issue.issue}",
                    filePath = null,
                    lineNumber = null,
                    suggestedFix = issue.recommendation,
                    priority = Priority.MEDIUM
                )
            )
        }
        
        // Data binding issues
        result.viewModelValidation.dataBindingIssues.forEach { issue ->
            issues.add(
                DetailedIssue(
                    category = "UI Data Flow",
                    severity = Severity.WARNING,
                    title = "Data Binding Issue",
                    description = "Component '${issue.componentFile}', property '${issue.bindingProperty}': ${issue.issue}",
                    filePath = issue.componentFile,
                    lineNumber = null,
                    suggestedFix = issue.fix,
                    priority = Priority.MEDIUM
                )
            )
        }
        
        // Lifecycle issues
        result.viewModelValidation.lifecycleIssues.forEach { issue ->
            issues.add(
                DetailedIssue(
                    category = "UI Data Flow",
                    severity = Severity.WARNING,
                    title = "Lifecycle Issue",
                    description = issue,
                    filePath = null,
                    lineNumber = null,
                    suggestedFix = "Review ViewModel lifecycle management",
                    priority = Priority.MEDIUM
                )
            )
        }
        
        return issues
    }
    
    /**
     * Convert DI validation results to detailed issues
     */
    private fun convertDIIssues(result: DIValidationResult): List<DetailedIssue> {
        val issues = mutableListOf<DetailedIssue>()
        
        // Module issues
        result.hiltValidation.moduleIssues.forEach { issue ->
            issues.add(
                DetailedIssue(
                    category = "Dependency Injection",
                    severity = issue.severity,
                    title = "Module Issue",
                    description = "Module '${issue.moduleName}': ${issue.issue}",
                    filePath = null,
                    lineNumber = null,
                    suggestedFix = issue.fix,
                    priority = when (issue.severity) {
                        Severity.CRITICAL -> Priority.HIGH
                        Severity.WARNING -> Priority.MEDIUM
                        Severity.INFO -> Priority.LOW
                    }
                )
            )
        }
        
        // Binding issues
        result.hiltValidation.bindingIssues.forEach { issue ->
            issues.add(
                DetailedIssue(
                    category = "Dependency Injection",
                    severity = Severity.WARNING,
                    title = "Binding Issue",
                    description = "Interface '${issue.interfaceName}' to '${issue.implementationName}': ${issue.issue}",
                    filePath = null,
                    lineNumber = null,
                    suggestedFix = issue.fix,
                    priority = Priority.MEDIUM
                )
            )
        }
        
        // Scope issues
        result.hiltValidation.scopeIssues.forEach { issue ->
            issues.add(
                DetailedIssue(
                    category = "Dependency Injection",
                    severity = Severity.WARNING,
                    title = "Scope Issue",
                    description = "Component '${issue.componentName}': ${issue.scopeIssue}",
                    filePath = null,
                    lineNumber = null,
                    suggestedFix = issue.recommendation,
                    priority = Priority.MEDIUM
                )
            )
        }
        
        return issues
    }
    
    /**
     * Generate summary statistics from all issues
     */
    private fun generateSummary(issues: List<DetailedIssue>): ValidationSummary {
        val totalIssues = issues.size
        val criticalIssues = issues.count { it.severity == Severity.CRITICAL }
        val warningIssues = issues.count { it.severity == Severity.WARNING }
        val infoIssues = issues.count { it.severity == Severity.INFO }
        
        // Calculate overall score (0-100)
        val overallScore = calculateOverallScore(totalIssues, criticalIssues, warningIssues, infoIssues)
        
        // Generate category summaries
        val categoryMap = issues.groupBy { it.category }
        val categorySummaries = categoryMap.mapValues { (categoryName, categoryIssues) ->
            val catCritical = categoryIssues.count { it.severity == Severity.CRITICAL }
            val catWarning = categoryIssues.count { it.severity == Severity.WARNING }
            val catInfo = categoryIssues.count { it.severity == Severity.INFO }
            val catScore = calculateOverallScore(categoryIssues.size, catCritical, catWarning, catInfo)
            
            CategorySummary(
                categoryName = categoryName,
                totalIssues = categoryIssues.size,
                criticalIssues = catCritical,
                warningIssues = catWarning,
                infoIssues = catInfo,
                score = catScore
            )
        }
        
        return ValidationSummary(
            totalIssues = totalIssues,
            criticalIssues = criticalIssues,
            warningIssues = warningIssues,
            infoIssues = infoIssues,
            overallScore = overallScore,
            validationCategories = categorySummaries
        )
    }
    
    /**
     * Calculate overall score based on issue counts and severity
     */
    private fun calculateOverallScore(total: Int, critical: Int, warning: Int, info: Int): Int {
        if (total == 0) return 100
        
        // Weight different severity levels
        val weightedScore = (critical * 10) + (warning * 3) + (info * 1)
        val maxPossibleScore = total * 10
        
        // Convert to 0-100 scale (inverted - fewer issues = higher score)
        return maxOf(0, 100 - ((weightedScore * 100) / maxPossibleScore))
    }
    
    /**
     * Generate actionable recommendations based on validation results
     */
    private fun generateRecommendations(
        issues: List<DetailedIssue>,
        importResult: ImportValidationResult,
        webhookResult: WebhookValidationResult,
        uiResult: UIValidationResult,
        diResult: DIValidationResult
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        // Import-related recommendations
        recommendations.addAll(generateImportRecommendations(importResult, issues))
        
        // Webhook-related recommendations
        recommendations.addAll(generateWebhookRecommendations(webhookResult, issues))
        
        // UI-related recommendations
        recommendations.addAll(generateUIRecommendations(uiResult, issues))
        
        // DI-related recommendations
        recommendations.addAll(generateDIRecommendations(diResult, issues))
        
        // General recommendations based on overall issues
        recommendations.addAll(generateGeneralRecommendations(issues))
        
        return recommendations.sortedByDescending { it.priority }
    }
    
    /**
     * Generate import-specific recommendations
     */
    private fun generateImportRecommendations(
        result: ImportValidationResult,
        issues: List<DetailedIssue>
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        if (result.unusedImports.isNotEmpty()) {
            recommendations.add(
                Recommendation(
                    category = "Import Cleanup",
                    priority = Priority.LOW,
                    title = "Remove Unused Imports",
                    description = "Clean up ${result.unusedImports.size} unused import statements to improve code quality",
                    actionItems = listOf(
                        "Use IDE's 'Optimize Imports' feature",
                        "Review and remove unused import statements",
                        "Set up automated import optimization in CI/CD"
                    ),
                    affectedFiles = result.unusedImports.map { it.filePath }.distinct(),
                    estimatedEffort = "15 minutes"
                )
            )
        }
        
        if (result.missingImports.isNotEmpty()) {
            recommendations.add(
                Recommendation(
                    category = "Import Fixes",
                    priority = Priority.HIGH,
                    title = "Fix Missing Imports",
                    description = "Add ${result.missingImports.size} missing import statements to resolve compilation errors",
                    actionItems = listOf(
                        "Add missing import statements",
                        "Verify class availability in dependencies",
                        "Update build.gradle if new dependencies are needed"
                    ),
                    affectedFiles = result.missingImports.map { it.filePath }.distinct(),
                    estimatedEffort = "30 minutes"
                )
            )
        }
        
        if (result.architecturalViolations.isNotEmpty()) {
            recommendations.add(
                Recommendation(
                    category = "Architecture Compliance",
                    priority = Priority.HIGH,
                    title = "Fix Architectural Violations",
                    description = "Resolve ${result.architecturalViolations.size} Clean Architecture violations",
                    actionItems = listOf(
                        "Review dependency directions between layers",
                        "Move classes to appropriate layers",
                        "Use dependency inversion for cross-layer communication"
                    ),
                    affectedFiles = result.architecturalViolations.map { it.filePath }.distinct(),
                    estimatedEffort = "2-4 hours"
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Generate webhook-specific recommendations
     */
    private fun generateWebhookRecommendations(
        result: WebhookValidationResult,
        issues: List<DetailedIssue>
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        if (!result.networkConfig.isValid) {
            recommendations.add(
                Recommendation(
                    category = "Network Configuration",
                    priority = Priority.HIGH,
                    title = "Fix Network Configuration",
                    description = "Resolve network configuration issues to ensure reliable webhook communication",
                    actionItems = listOf(
                        "Review OkHttp and Retrofit configuration",
                        "Verify timeout settings",
                        "Check interceptor setup",
                        "Test network connectivity"
                    ),
                    estimatedEffort = "1-2 hours"
                )
            )
        }
        
        if (!result.connectivity.isConnected) {
            recommendations.add(
                Recommendation(
                    category = "Webhook Connectivity",
                    priority = Priority.HIGH,
                    title = "Fix Webhook Connectivity",
                    description = "Resolve connectivity issues with webhook endpoints",
                    actionItems = listOf(
                        "Verify webhook endpoint URLs",
                        "Check network permissions",
                        "Test with different network conditions",
                        "Implement proper error handling"
                    ),
                    estimatedEffort = "1 hour"
                )
            )
        }
        
        if (!result.jsonSerialization.isValid) {
            recommendations.add(
                Recommendation(
                    category = "Data Serialization",
                    priority = Priority.MEDIUM,
                    title = "Fix JSON Serialization Issues",
                    description = "Resolve JSON serialization problems to ensure proper data transmission",
                    actionItems = listOf(
                        "Review data class annotations",
                        "Check field naming conventions",
                        "Test serialization/deserialization",
                        "Update Gson configuration if needed"
                    ),
                    estimatedEffort = "30-60 minutes"
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Generate UI-specific recommendations
     */
    private fun generateUIRecommendations(
        result: UIValidationResult,
        issues: List<DetailedIssue>
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        if (result.viewModelValidation.stateFlowIssues.isNotEmpty()) {
            recommendations.add(
                Recommendation(
                    category = "UI State Management",
                    priority = Priority.MEDIUM,
                    title = "Improve StateFlow Usage",
                    description = "Optimize StateFlow usage in ViewModels for better UI reactivity",
                    actionItems = listOf(
                        "Review StateFlow initialization",
                        "Ensure proper state updates",
                        "Check for memory leaks",
                        "Implement proper error states"
                    ),
                    estimatedEffort = "1-2 hours"
                )
            )
        }
        
        if (result.dataBinding.issues.isNotEmpty()) {
            recommendations.add(
                Recommendation(
                    category = "Data Binding",
                    priority = Priority.MEDIUM,
                    title = "Fix Data Binding Issues",
                    description = "Resolve data binding problems in UI components",
                    actionItems = listOf(
                        "Review data binding expressions",
                        "Check null safety",
                        "Verify data types",
                        "Test UI updates"
                    ),
                    estimatedEffort = "1 hour"
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Generate DI-specific recommendations
     */
    private fun generateDIRecommendations(
        result: DIValidationResult,
        issues: List<DetailedIssue>
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        if (result.hiltValidation.moduleIssues.isNotEmpty()) {
            recommendations.add(
                Recommendation(
                    category = "Dependency Injection",
                    priority = Priority.HIGH,
                    title = "Fix Hilt Module Issues",
                    description = "Resolve Hilt module configuration problems",
                    actionItems = listOf(
                        "Review module annotations",
                        "Check component installations",
                        "Verify provider methods",
                        "Test dependency injection"
                    ),
                    estimatedEffort = "1-3 hours"
                )
            )
        }
        
        if (!result.dependencyGraph.isValid) {
            recommendations.add(
                Recommendation(
                    category = "Dependency Graph",
                    priority = Priority.HIGH,
                    title = "Fix Dependency Graph Issues",
                    description = "Resolve dependency graph problems to ensure proper injection",
                    actionItems = listOf(
                        "Check for circular dependencies",
                        "Verify all dependencies are provided",
                        "Review scope configurations",
                        "Test application startup"
                    ),
                    estimatedEffort = "2-4 hours"
                )
            )
        }
        
        return recommendations
    }
    
    /**
     * Generate general recommendations based on overall analysis
     */
    private fun generateGeneralRecommendations(issues: List<DetailedIssue>): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        
        val criticalCount = issues.count { it.severity == Severity.CRITICAL }
        if (criticalCount > 0) {
            recommendations.add(
                Recommendation(
                    category = "Critical Issues",
                    priority = Priority.HIGH,
                    title = "Address Critical Issues First",
                    description = "Focus on resolving $criticalCount critical issues that may prevent proper application functionality",
                    actionItems = listOf(
                        "Prioritize critical issues in development backlog",
                        "Test thoroughly after each fix",
                        "Consider code review for critical changes",
                        "Update documentation if needed"
                    ),
                    estimatedEffort = "Varies by issue"
                )
            )
        }
        
        // Add code quality recommendation if there are many issues
        if (issues.size > 20) {
            recommendations.add(
                Recommendation(
                    category = "Code Quality",
                    priority = Priority.MEDIUM,
                    title = "Implement Code Quality Measures",
                    description = "Consider implementing automated code quality checks to prevent future issues",
                    actionItems = listOf(
                        "Set up static code analysis tools",
                        "Implement pre-commit hooks",
                        "Add code quality checks to CI/CD pipeline",
                        "Establish coding standards and guidelines"
                    ),
                    estimatedEffort = "4-8 hours initial setup"
                )
            )
        }
        
        return recommendations
    }
}