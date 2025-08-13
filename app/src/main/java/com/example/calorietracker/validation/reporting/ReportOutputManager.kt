package com.example.calorietracker.validation.reporting

import com.example.calorietracker.validation.models.DetailedAnalysisReport
import com.example.calorietracker.validation.models.ValidationReport
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages report output to different formats and destinations
 */
class ReportOutputManager {
    
    private val formatter = ReportFormatter()
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    /**
     * Output formats supported by the report system
     */
    enum class OutputFormat {
        MARKDOWN,
        CONSOLE,
        JSON,
        SUMMARY
    }
    
    /**
     * Output destinations for reports
     */
    enum class OutputDestination {
        FILE,
        CONSOLE,
        BOTH
    }
    
    /**
     * Configuration for report output
     */
    data class OutputConfig(
        val format: OutputFormat,
        val destination: OutputDestination,
        val outputDirectory: String = "validation_reports",
        val includeTimestamp: Boolean = true,
        val includeDetailedAnalysis: Boolean = true
    )
    
    /**
     * Generate and output validation report according to configuration
     */
    fun generateReport(
        report: ValidationReport,
        detailedAnalysis: DetailedAnalysisReport? = null,
        config: OutputConfig = OutputConfig(
            format = OutputFormat.MARKDOWN,
            destination = OutputDestination.BOTH
        )
    ): ReportOutput {
        
        val formattedContent = when (config.format) {
            OutputFormat.MARKDOWN -> formatter.formatAsMarkdown(
                report, 
                if (config.includeDetailedAnalysis) detailedAnalysis else null
            )
            OutputFormat.CONSOLE -> formatter.formatForConsole(
                report, 
                if (config.includeDetailedAnalysis) detailedAnalysis else null
            )
            OutputFormat.JSON -> formatter.formatAsJson(
                report, 
                if (config.includeDetailedAnalysis) detailedAnalysis else null
            )
            OutputFormat.SUMMARY -> formatter.formatSummary(report)
        }
        
        val outputs = mutableListOf<String>()
        
        // Output to console if requested
        if (config.destination == OutputDestination.CONSOLE || config.destination == OutputDestination.BOTH) {
            println(formattedContent)
            outputs.add("console")
        }
        
        // Output to file if requested
        var filePath: String? = null
        if (config.destination == OutputDestination.FILE || config.destination == OutputDestination.BOTH) {
            filePath = writeToFile(formattedContent, config)
            filePath?.let { outputs.add(it) }
        }
        
        return ReportOutput(
            content = formattedContent,
            format = config.format,
            filePath = filePath,
            outputs = outputs,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Generate multiple report formats at once
     */
    fun generateMultipleFormats(
        report: ValidationReport,
        detailedAnalysis: DetailedAnalysisReport? = null,
        outputDirectory: String = "validation_reports"
    ): List<ReportOutput> {
        val outputs = mutableListOf<ReportOutput>()
        
        // Generate markdown report
        outputs.add(
            generateReport(
                report,
                detailedAnalysis,
                OutputConfig(
                    format = OutputFormat.MARKDOWN,
                    destination = OutputDestination.FILE,
                    outputDirectory = outputDirectory
                )
            )
        )
        
        // Generate JSON report
        outputs.add(
            generateReport(
                report,
                detailedAnalysis,
                OutputConfig(
                    format = OutputFormat.JSON,
                    destination = OutputDestination.FILE,
                    outputDirectory = outputDirectory
                )
            )
        )
        
        // Generate console summary
        outputs.add(
            generateReport(
                report,
                detailedAnalysis,
                OutputConfig(
                    format = OutputFormat.CONSOLE,
                    destination = OutputDestination.CONSOLE
                )
            )
        )
        
        return outputs
    }
    
    /**
     * Generate executive summary report for stakeholders
     */
    fun generateExecutiveSummary(
        report: ValidationReport,
        detailedAnalysis: DetailedAnalysisReport,
        outputDirectory: String = "validation_reports"
    ): ReportOutput {
        val executiveSummary = generateExecutiveSummaryContent(report, detailedAnalysis)
        
        val filePath = writeToFile(
            executiveSummary,
            OutputConfig(
                format = OutputFormat.MARKDOWN,
                destination = OutputDestination.FILE,
                outputDirectory = outputDirectory
            ),
            "executive_summary"
        )
        
        return ReportOutput(
            content = executiveSummary,
            format = OutputFormat.MARKDOWN,
            filePath = filePath,
            outputs = listOfNotNull(filePath),
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Generate action plan report
     */
    fun generateActionPlanReport(
        detailedAnalysis: DetailedAnalysisReport,
        outputDirectory: String = "validation_reports"
    ): ReportOutput {
        val actionPlan = generateActionPlanContent(detailedAnalysis)
        
        val filePath = writeToFile(
            actionPlan,
            OutputConfig(
                format = OutputFormat.MARKDOWN,
                destination = OutputDestination.FILE,
                outputDirectory = outputDirectory
            ),
            "action_plan"
        )
        
        return ReportOutput(
            content = actionPlan,
            format = OutputFormat.MARKDOWN,
            filePath = filePath,
            outputs = listOfNotNull(filePath),
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Write content to file
     */
    private fun writeToFile(
        content: String,
        config: OutputConfig,
        customPrefix: String? = null
    ): String? {
        return try {
            // Create output directory if it doesn't exist
            val outputDir = File(config.outputDirectory)
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            // Generate filename
            val timestamp = if (config.includeTimestamp) "_${dateFormat.format(Date())}" else ""
            val prefix = customPrefix ?: "validation_report"
            val extension = when (config.format) {
                OutputFormat.MARKDOWN -> "md"
                OutputFormat.JSON -> "json"
                OutputFormat.CONSOLE -> "txt"
                OutputFormat.SUMMARY -> "txt"
            }
            
            val filename = "${prefix}${timestamp}.${extension}"
            val file = File(outputDir, filename)
            
            // Write content to file
            file.writeText(content)
            
            file.absolutePath
        } catch (e: Exception) {
            println("Error writing report to file: ${e.message}")
            null
        }
    }
    
    /**
     * Generate executive summary content
     */
    private fun generateExecutiveSummaryContent(
        report: ValidationReport,
        detailedAnalysis: DetailedAnalysisReport
    ): String {
        val summary = StringBuilder()
        
        summary.appendLine("# Executive Summary - Code Validation Report")
        summary.appendLine()
        summary.appendLine("**Date:** ${SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(report.timestamp))}")
        summary.appendLine("**Project:** ${report.projectPath}")
        summary.appendLine()
        
        // Overall Health
        summary.appendLine("## Overall Assessment")
        summary.appendLine()
        summary.appendLine("**Health Status:** ${detailedAnalysis.executiveSummary.overallHealth}")
        summary.appendLine("**Quality Score:** ${detailedAnalysis.executiveSummary.overallScore}/100")
        summary.appendLine("**Total Issues Found:** ${detailedAnalysis.executiveSummary.totalIssuesFound}")
        summary.appendLine("**Critical Issues:** ${detailedAnalysis.executiveSummary.criticalIssuesCount}")
        summary.appendLine()
        
        // Key Findings
        if (detailedAnalysis.executiveSummary.keyFindings.isNotEmpty()) {
            summary.appendLine("## Key Findings")
            summary.appendLine()
            detailedAnalysis.executiveSummary.keyFindings.forEach { finding ->
                summary.appendLine("- $finding")
            }
            summary.appendLine()
        }
        
        // Risk Assessment
        summary.appendLine("## Risk Assessment")
        summary.appendLine()
        summary.appendLine("**Overall Risk Level:** ${detailedAnalysis.riskAssessment.overallRiskLevel}")
        summary.appendLine()
        
        if (detailedAnalysis.riskAssessment.risks.isNotEmpty()) {
            summary.appendLine("### Top Risks")
            detailedAnalysis.riskAssessment.risks.take(3).forEach { risk ->
                summary.appendLine("- **${risk.category}:** ${risk.description}")
            }
            summary.appendLine()
        }
        
        // Immediate Actions Required
        if (detailedAnalysis.actionPlan.immediateActions.isNotEmpty()) {
            summary.appendLine("## Immediate Actions Required")
            summary.appendLine()
            detailedAnalysis.actionPlan.immediateActions.forEach { action ->
                summary.appendLine("1. **${action.title}**")
                summary.appendLine("   - ${action.description}")
                summary.appendLine("   - Estimated time: ${action.estimatedTime}")
                summary.appendLine()
            }
        }
        
        // Resource Requirements
        summary.appendLine("## Resource Requirements")
        summary.appendLine()
        summary.appendLine("**Estimated Fix Time:** ${detailedAnalysis.executiveSummary.estimatedFixTime}")
        summary.appendLine("**High Priority Actions:** ${detailedAnalysis.executiveSummary.highPriorityActions}")
        summary.appendLine()
        
        // Recommendations
        summary.appendLine("## Strategic Recommendations")
        summary.appendLine()
        summary.appendLine("1. **Immediate Focus:** Address critical issues to prevent system failures")
        summary.appendLine("2. **Short-term:** Implement code quality measures and fix warning-level issues")
        summary.appendLine("3. **Long-term:** Establish automated validation processes and coding standards")
        summary.appendLine()
        
        return summary.toString()
    }
    
    /**
     * Generate action plan content
     */
    private fun generateActionPlanContent(detailedAnalysis: DetailedAnalysisReport): String {
        val actionPlan = StringBuilder()
        
        actionPlan.appendLine("# Action Plan - Code Validation")
        actionPlan.appendLine()
        actionPlan.appendLine("**Total Estimated Time:** ${detailedAnalysis.actionPlan.totalEstimatedTime}")
        actionPlan.appendLine()
        
        // Immediate Actions
        if (detailedAnalysis.actionPlan.immediateActions.isNotEmpty()) {
            actionPlan.appendLine("## Immediate Actions (This Week)")
            actionPlan.appendLine()
            detailedAnalysis.actionPlan.immediateActions.forEachIndexed { index, action ->
                actionPlan.appendLine("### ${index + 1}. ${action.title}")
                actionPlan.appendLine("**Priority:** ${action.priority}")
                actionPlan.appendLine("**Description:** ${action.description}")
                actionPlan.appendLine("**Estimated Time:** ${action.estimatedTime}")
                actionPlan.appendLine()
                actionPlan.appendLine("**Tasks:**")
                actionPlan.appendLine("- [ ] Start: ${action.title}")
                actionPlan.appendLine("- [ ] Test implementation")
                actionPlan.appendLine("- [ ] Verify fix")
                actionPlan.appendLine("- [ ] Update documentation")
                actionPlan.appendLine()
            }
        }
        
        // Short-term Actions
        if (detailedAnalysis.actionPlan.shortTermActions.isNotEmpty()) {
            actionPlan.appendLine("## Short-term Actions (Next 2-4 Weeks)")
            actionPlan.appendLine()
            detailedAnalysis.actionPlan.shortTermActions.forEachIndexed { index, action ->
                actionPlan.appendLine("### ${index + 1}. ${action.title}")
                actionPlan.appendLine("**Priority:** ${action.priority}")
                actionPlan.appendLine("**Description:** ${action.description}")
                actionPlan.appendLine("**Estimated Time:** ${action.estimatedTime}")
                actionPlan.appendLine()
            }
        }
        
        // Long-term Actions
        if (detailedAnalysis.actionPlan.longTermActions.isNotEmpty()) {
            actionPlan.appendLine("## Long-term Actions (Next 1-3 Months)")
            actionPlan.appendLine()
            detailedAnalysis.actionPlan.longTermActions.forEachIndexed { index, action ->
                actionPlan.appendLine("### ${index + 1}. ${action.title}")
                actionPlan.appendLine("**Priority:** ${action.priority}")
                actionPlan.appendLine("**Description:** ${action.description}")
                actionPlan.appendLine("**Estimated Time:** ${action.estimatedTime}")
                actionPlan.appendLine()
            }
        }
        
        // Milestones
        if (detailedAnalysis.actionPlan.milestones.isNotEmpty()) {
            actionPlan.appendLine("## Milestones")
            actionPlan.appendLine()
            detailedAnalysis.actionPlan.milestones.forEach { milestone ->
                actionPlan.appendLine("### ${milestone.name}")
                actionPlan.appendLine("**Target Date:** ${milestone.targetDate}")
                actionPlan.appendLine("**Description:** ${milestone.description}")
                actionPlan.appendLine()
                actionPlan.appendLine("**Success Criteria:**")
                milestone.criteria.forEach { criteria ->
                    actionPlan.appendLine("- $criteria")
                }
                actionPlan.appendLine()
            }
        }
        
        return actionPlan.toString()
    }
}

/**
 * Result of report generation
 */
data class ReportOutput(
    val content: String,
    val format: ReportOutputManager.OutputFormat,
    val filePath: String?,
    val outputs: List<String>,
    val timestamp: Long
)