package com.example.calorietracker.validation.reporting

import com.example.calorietracker.validation.models.*

/**
 * Comprehensive validation report system that orchestrates the entire reporting process
 */
class ComprehensiveReportSystem {
    
    private val reportGenerator = ReportGenerator()
    private val detailedAnalysisReporter = DetailedAnalysisReporter()
    private val outputManager = ReportOutputManager()
    private val resultAggregator = ValidationResultAggregator()
    
    /**
     * Generate complete validation report with all analysis and output formats
     */
    fun generateCompleteReport(
        projectPath: String,
        importResult: ImportValidationResult,
        webhookResult: WebhookValidationResult,
        uiResult: UIValidationResult,
        diResult: DIValidationResult,
        outputDirectory: String = "validation_reports"
    ): CompleteReportResult {
        
        // Step 1: Generate base validation report
        val validationReport = reportGenerator.generateReport(
            projectPath = projectPath,
            importResult = importResult,
            webhookResult = webhookResult,
            uiResult = uiResult,
            diResult = diResult
        )
        
        // Step 2: Generate detailed analysis
        val detailedAnalysis = detailedAnalysisReporter.generateDetailedAnalysis(validationReport)
        
        // Step 3: Generate multiple output formats
        val reportOutputs = outputManager.generateMultipleFormats(
            report = validationReport,
            detailedAnalysis = detailedAnalysis,
            outputDirectory = outputDirectory
        )
        
        // Step 4: Generate executive summary
        val executiveSummary = outputManager.generateExecutiveSummary(
            report = validationReport,
            detailedAnalysis = detailedAnalysis,
            outputDirectory = outputDirectory
        )
        
        // Step 5: Generate action plan
        val actionPlan = outputManager.generateActionPlanReport(
            detailedAnalysis = detailedAnalysis,
            outputDirectory = outputDirectory
        )
        
        return CompleteReportResult(
            validationReport = validationReport,
            detailedAnalysis = detailedAnalysis,
            reportOutputs = reportOutputs,
            executiveSummary = executiveSummary,
            actionPlan = actionPlan,
            summary = generateReportSummary(validationReport, detailedAnalysis)
        )
    }
    
    /**
     * Generate quick validation summary for immediate feedback
     */
    fun generateQuickSummary(
        projectPath: String,
        importResult: ImportValidationResult,
        webhookResult: WebhookValidationResult,
        uiResult: UIValidationResult,
        diResult: DIValidationResult
    ): QuickSummaryResult {
        
        val validationReport = reportGenerator.generateReport(
            projectPath = projectPath,
            importResult = importResult,
            webhookResult = webhookResult,
            uiResult = uiResult,
            diResult = diResult
        )
        
        val consoleOutput = outputManager.generateReport(
            report = validationReport,
            config = ReportOutputManager.OutputConfig(
                format = ReportOutputManager.OutputFormat.CONSOLE,
                destination = ReportOutputManager.OutputDestination.CONSOLE,
                includeDetailedAnalysis = false
            )
        )
        
        return QuickSummaryResult(
            overallScore = validationReport.summary.overallScore,
            totalIssues = validationReport.summary.totalIssues,
            criticalIssues = validationReport.summary.criticalIssues,
            status = determineOverallStatus(validationReport.summary),
            topIssues = extractTopIssues(validationReport),
            consoleOutput = consoleOutput.content
        )
    }
    
    /**
     * Generate progressive report (can be called multiple times as validation progresses)
     */
    fun generateProgressiveReport(
        projectPath: String,
        outputDirectory: String = "validation_reports"
    ): ProgressiveReportBuilder {
        return ProgressiveReportBuilder(
            projectPath = projectPath,
            outputDirectory = outputDirectory,
            reportSystem = this
        )
    }
    
    /**
     * Generate comparison report between two validation runs
     */
    fun generateComparisonReport(
        previousReport: ValidationReport,
        currentReport: ValidationReport,
        outputDirectory: String = "validation_reports"
    ): ComparisonReportResult {
        
        val comparison = ComparisonAnalysis(
            previousScore = previousReport.summary.overallScore,
            currentScore = currentReport.summary.overallScore,
            scoreChange = currentReport.summary.overallScore - previousReport.summary.overallScore,
            previousIssues = previousReport.summary.totalIssues,
            currentIssues = currentReport.summary.totalIssues,
            issueChange = currentReport.summary.totalIssues - previousReport.summary.totalIssues,
            improvedCategories = findImprovedCategories(previousReport, currentReport),
            regressedCategories = findRegressedCategories(previousReport, currentReport),
            newIssues = identifyNewIssues(previousReport, currentReport),
            resolvedIssues = identifyResolvedIssues(previousReport, currentReport)
        )
        
        val comparisonContent = generateComparisonContent(comparison, previousReport, currentReport)
        
        val output = outputManager.generateReport(
            report = currentReport,
            config = ReportOutputManager.OutputConfig(
                format = ReportOutputManager.OutputFormat.MARKDOWN,
                destination = ReportOutputManager.OutputDestination.FILE,
                outputDirectory = outputDirectory
            )
        )
        
        return ComparisonReportResult(
            comparison = comparison,
            reportContent = comparisonContent,
            output = output
        )
    }
    
    /**
     * Generate report summary for dashboard or API consumption
     */
    private fun generateReportSummary(
        validationReport: ValidationReport,
        detailedAnalysis: DetailedAnalysisReport
    ): ReportSummary {
        return ReportSummary(
            timestamp = validationReport.timestamp,
            projectPath = validationReport.projectPath,
            overallScore = validationReport.summary.overallScore,
            overallHealth = detailedAnalysis.executiveSummary.overallHealth,
            totalIssues = validationReport.summary.totalIssues,
            criticalIssues = validationReport.summary.criticalIssues,
            warningIssues = validationReport.summary.warningIssues,
            infoIssues = validationReport.summary.infoIssues,
            highPriorityRecommendations = validationReport.recommendations.count { it.priority == com.example.calorietracker.validation.Priority.HIGH },
            estimatedFixTime = detailedAnalysis.executiveSummary.estimatedFixTime,
            riskLevel = detailedAnalysis.riskAssessment.overallRiskLevel,
            categoryScores = validationReport.summary.validationCategories.mapValues { it.value.score },
            connectivityStatus = validationReport.webhookValidation.connectivity.isConnected
        )
    }
    
    /**
     * Determine overall status based on validation summary
     */
    private fun determineOverallStatus(summary: ValidationSummary): String {
        return when {
            summary.criticalIssues > 0 -> "CRITICAL"
            summary.overallScore >= 90 -> "EXCELLENT"
            summary.overallScore >= 75 -> "GOOD"
            summary.overallScore >= 60 -> "FAIR"
            summary.overallScore >= 40 -> "POOR"
            else -> "CRITICAL"
        }
    }
    
    /**
     * Extract top issues for quick summary
     */
    private fun extractTopIssues(report: ValidationReport): List<String> {
        val issues = mutableListOf<String>()
        
        // Add critical missing imports
        report.importValidation.missingImports.take(3).forEach { missing ->
            issues.add("Missing import: ${missing.missingClass} in ${missing.filePath}")
        }
        
        // Add architectural violations
        report.importValidation.architecturalViolations.take(2).forEach { violation ->
            issues.add("Architectural violation: ${violation.description}")
        }
        
        // Add connectivity issues
        if (!report.webhookValidation.connectivity.isConnected) {
            issues.add("Webhook connectivity failed")
        }
        
        // Add critical network issues
        report.webhookValidation.networkConfig.issues
            .filter { it.severity == com.example.calorietracker.validation.Severity.CRITICAL }
            .take(2)
            .forEach { issue ->
                issues.add("Network issue: ${issue.component} - ${issue.issue}")
            }
        
        return issues.take(5)
    }
    
    // Comparison report helper methods
    
    private fun findImprovedCategories(
        previous: ValidationReport,
        current: ValidationReport
    ): List<String> {
        return current.summary.validationCategories.mapNotNull { (category, currentSummary) ->
            val previousSummary = previous.summary.validationCategories[category]
            if (previousSummary != null && currentSummary.score > previousSummary.score) {
                category
            } else null
        }
    }
    
    private fun findRegressedCategories(
        previous: ValidationReport,
        current: ValidationReport
    ): List<String> {
        return current.summary.validationCategories.mapNotNull { (category, currentSummary) ->
            val previousSummary = previous.summary.validationCategories[category]
            if (previousSummary != null && currentSummary.score < previousSummary.score) {
                category
            } else null
        }
    }
    
    private fun identifyNewIssues(
        previous: ValidationReport,
        current: ValidationReport
    ): List<String> {
        // This would require more sophisticated comparison logic
        // For now, return a simple comparison based on counts
        val newIssues = mutableListOf<String>()
        
        if (current.importValidation.missingImports.size > previous.importValidation.missingImports.size) {
            newIssues.add("${current.importValidation.missingImports.size - previous.importValidation.missingImports.size} new missing imports")
        }
        
        if (current.webhookValidation.networkConfig.issues.size > previous.webhookValidation.networkConfig.issues.size) {
            newIssues.add("${current.webhookValidation.networkConfig.issues.size - previous.webhookValidation.networkConfig.issues.size} new network issues")
        }
        
        return newIssues
    }
    
    private fun identifyResolvedIssues(
        previous: ValidationReport,
        current: ValidationReport
    ): List<String> {
        val resolvedIssues = mutableListOf<String>()
        
        if (current.importValidation.missingImports.size < previous.importValidation.missingImports.size) {
            resolvedIssues.add("${previous.importValidation.missingImports.size - current.importValidation.missingImports.size} missing imports resolved")
        }
        
        if (current.importValidation.unusedImports.size < previous.importValidation.unusedImports.size) {
            resolvedIssues.add("${previous.importValidation.unusedImports.size - current.importValidation.unusedImports.size} unused imports cleaned up")
        }
        
        return resolvedIssues
    }
    
    private fun generateComparisonContent(
        comparison: ComparisonAnalysis,
        previous: ValidationReport,
        current: ValidationReport
    ): String {
        val content = StringBuilder()
        
        content.appendLine("# Validation Comparison Report")
        content.appendLine()
        content.appendLine("## Score Comparison")
        content.appendLine("- Previous Score: ${comparison.previousScore}/100")
        content.appendLine("- Current Score: ${comparison.currentScore}/100")
        content.appendLine("- Change: ${if (comparison.scoreChange >= 0) "+" else ""}${comparison.scoreChange}")
        content.appendLine()
        
        content.appendLine("## Issue Count Comparison")
        content.appendLine("- Previous Issues: ${comparison.previousIssues}")
        content.appendLine("- Current Issues: ${comparison.currentIssues}")
        content.appendLine("- Change: ${if (comparison.issueChange >= 0) "+" else ""}${comparison.issueChange}")
        content.appendLine()
        
        if (comparison.improvedCategories.isNotEmpty()) {
            content.appendLine("## Improved Categories")
            comparison.improvedCategories.forEach { category ->
                content.appendLine("- $category")
            }
            content.appendLine()
        }
        
        if (comparison.regressedCategories.isNotEmpty()) {
            content.appendLine("## Regressed Categories")
            comparison.regressedCategories.forEach { category ->
                content.appendLine("- $category")
            }
            content.appendLine()
        }
        
        return content.toString()
    }
}

/**
 * Progressive report builder for incremental validation
 */
class ProgressiveReportBuilder(
    private val projectPath: String,
    private val outputDirectory: String,
    private val reportSystem: ComprehensiveReportSystem
) {
    private val aggregator = ValidationResultAggregator()
    
    fun addImportResult(result: ImportValidationResult): ProgressiveReportBuilder {
        aggregator.addImportResult(result)
        return this
    }
    
    fun addWebhookResult(result: WebhookValidationResult): ProgressiveReportBuilder {
        aggregator.addWebhookResult(result)
        return this
    }
    
    fun addUIResult(result: UIValidationResult): ProgressiveReportBuilder {
        aggregator.addUIResult(result)
        return this
    }
    
    fun addDIResult(result: DIValidationResult): ProgressiveReportBuilder {
        aggregator.addDIResult(result)
        return this
    }
    
    fun generateCurrentReport(): CompleteReportResult {
        return reportSystem.generateCompleteReport(
            projectPath = projectPath,
            importResult = aggregator.getAggregatedImportResult(),
            webhookResult = aggregator.getAggregatedWebhookResult(),
            uiResult = aggregator.getAggregatedUIResult(),
            diResult = aggregator.getAggregatedDIResult(),
            outputDirectory = outputDirectory
        )
    }
}

// Result data classes

data class CompleteReportResult(
    val validationReport: ValidationReport,
    val detailedAnalysis: DetailedAnalysisReport,
    val reportOutputs: List<ReportOutput>,
    val executiveSummary: ReportOutput,
    val actionPlan: ReportOutput,
    val summary: ReportSummary
)

data class QuickSummaryResult(
    val overallScore: Int,
    val totalIssues: Int,
    val criticalIssues: Int,
    val status: String,
    val topIssues: List<String>,
    val consoleOutput: String
)

data class ComparisonReportResult(
    val comparison: ComparisonAnalysis,
    val reportContent: String,
    val output: ReportOutput
)

data class ComparisonAnalysis(
    val previousScore: Int,
    val currentScore: Int,
    val scoreChange: Int,
    val previousIssues: Int,
    val currentIssues: Int,
    val issueChange: Int,
    val improvedCategories: List<String>,
    val regressedCategories: List<String>,
    val newIssues: List<String>,
    val resolvedIssues: List<String>
)

data class ReportSummary(
    val timestamp: Long,
    val projectPath: String,
    val overallScore: Int,
    val overallHealth: HealthStatus,
    val totalIssues: Int,
    val criticalIssues: Int,
    val warningIssues: Int,
    val infoIssues: Int,
    val highPriorityRecommendations: Int,
    val estimatedFixTime: String,
    val riskLevel: RiskLevel,
    val categoryScores: Map<String, Int>,
    val connectivityStatus: Boolean
)