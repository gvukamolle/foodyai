package com.example.calorietracker.validation.reporting

import com.example.calorietracker.validation.Priority
import com.example.calorietracker.validation.Severity
import com.example.calorietracker.validation.models.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Formats validation reports for different output formats (Markdown, Console, JSON)
 */
class ReportFormatter {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * Format validation report as Markdown
     */
    fun formatAsMarkdown(report: ValidationReport, detailedAnalysis: DetailedAnalysisReport? = null): String {
        val markdown = StringBuilder()
        
        // Header
        markdown.appendLine("# Code Validation Report")
        markdown.appendLine()
        markdown.appendLine("**Generated:** ${dateFormat.format(Date(report.timestamp))}")
        markdown.appendLine("**Project:** ${report.projectPath}")
        markdown.appendLine()
        
        // Executive Summary
        markdown.appendLine("## Executive Summary")
        markdown.appendLine()
        detailedAnalysis?.let { analysis ->
            markdown.appendLine("**Overall Health:** ${analysis.executiveSummary.overallHealth}")
            markdown.appendLine("**Overall Score:** ${analysis.executiveSummary.overallScore}/100")
            markdown.appendLine("**Total Issues:** ${analysis.executiveSummary.totalIssuesFound}")
            markdown.appendLine("**Critical Issues:** ${analysis.executiveSummary.criticalIssuesCount}")
            markdown.appendLine("**Estimated Fix Time:** ${analysis.executiveSummary.estimatedFixTime}")
            markdown.appendLine()
            
            if (analysis.executiveSummary.keyFindings.isNotEmpty()) {
                markdown.appendLine("### Key Findings")
                analysis.executiveSummary.keyFindings.forEach { finding ->
                    markdown.appendLine("- $finding")
                }
                markdown.appendLine()
            }
        }
        
        // Summary Statistics
        markdown.appendLine("## Summary Statistics")
        markdown.appendLine()
        markdown.appendLine("| Metric | Count | Percentage |")
        markdown.appendLine("|--------|-------|------------|")
        markdown.appendLine("| Total Issues | ${report.summary.totalIssues} | 100% |")
        markdown.appendLine("| Critical | ${report.summary.criticalIssues} | ${calculatePercentage(report.summary.criticalIssues, report.summary.totalIssues)}% |")
        markdown.appendLine("| Warning | ${report.summary.warningIssues} | ${calculatePercentage(report.summary.warningIssues, report.summary.totalIssues)}% |")
        markdown.appendLine("| Info | ${report.summary.infoIssues} | ${calculatePercentage(report.summary.infoIssues, report.summary.totalIssues)}% |")
        markdown.appendLine()
        
        // Category Breakdown
        markdown.appendLine("## Category Breakdown")
        markdown.appendLine()
        markdown.appendLine("| Category | Score | Critical | Warning | Info | Total |")
        markdown.appendLine("|----------|-------|----------|---------|------|-------|")
        report.summary.validationCategories.forEach { (category, summary) ->
            markdown.appendLine("| $category | ${summary.score}/100 | ${summary.criticalIssues} | ${summary.warningIssues} | ${summary.infoIssues} | ${summary.totalIssues} |")
        }
        markdown.appendLine()
        
        // Detailed Issues by Category
        markdown.appendLine("## Detailed Issues")
        markdown.appendLine()
        
        // Import Validation Issues
        if (report.importValidation.unusedImports.isNotEmpty() || 
            report.importValidation.missingImports.isNotEmpty() || 
            report.importValidation.architecturalViolations.isNotEmpty()) {
            
            markdown.appendLine("### Import Validation Issues")
            markdown.appendLine()
            
            if (report.importValidation.missingImports.isNotEmpty()) {
                markdown.appendLine("#### Missing Imports (Critical)")
                report.importValidation.missingImports.forEach { missing ->
                    markdown.appendLine("- **File:** `${missing.filePath}:${missing.lineNumber}`")
                    markdown.appendLine("  - **Missing:** ${missing.missingClass}")
                    missing.suggestedImport?.let { suggestion ->
                        markdown.appendLine("  - **Suggested:** `$suggestion`")
                    }
                    markdown.appendLine()
                }
            }
            
            if (report.importValidation.architecturalViolations.isNotEmpty()) {
                markdown.appendLine("#### Architectural Violations (Critical)")
                report.importValidation.architecturalViolations.forEach { violation ->
                    markdown.appendLine("- **File:** `${violation.filePath}`")
                    markdown.appendLine("  - **Issue:** ${violation.description}")
                    markdown.appendLine("  - **Suggestion:** ${violation.suggestion}")
                    markdown.appendLine()
                }
            }
            
            if (report.importValidation.unusedImports.isNotEmpty()) {
                markdown.appendLine("#### Unused Imports (Warning)")
                val groupedByFile = report.importValidation.unusedImports.groupBy { it.filePath }
                groupedByFile.forEach { (filePath, imports) ->
                    markdown.appendLine("- **File:** `$filePath`")
                    imports.forEach { import ->
                        markdown.appendLine("  - Line ${import.lineNumber}: `${import.importStatement}`")
                    }
                    markdown.appendLine()
                }
            }
        }
        
        // Webhook Validation Issues
        if (report.webhookValidation.networkConfig.issues.isNotEmpty() || 
            report.webhookValidation.apiEndpoints.issues.isNotEmpty() ||
            !report.webhookValidation.connectivity.isConnected) {
            
            markdown.appendLine("### Webhook Validation Issues")
            markdown.appendLine()
            
            if (report.webhookValidation.networkConfig.issues.isNotEmpty()) {
                markdown.appendLine("#### Network Configuration Issues")
                report.webhookValidation.networkConfig.issues.forEach { issue ->
                    markdown.appendLine("- **Component:** ${issue.component}")
                    markdown.appendLine("  - **Severity:** ${issue.severity}")
                    markdown.appendLine("  - **Issue:** ${issue.issue}")
                    issue.fix?.let { fix ->
                        markdown.appendLine("  - **Fix:** $fix")
                    }
                    markdown.appendLine()
                }
            }
            
            if (!report.webhookValidation.connectivity.isConnected) {
                markdown.appendLine("#### Connectivity Issues")
                markdown.appendLine("- **Status:** Failed to connect")
                report.webhookValidation.connectivity.errorMessage?.let { error ->
                    markdown.appendLine("- **Error:** $error")
                }
                markdown.appendLine()
            }
            
            if (report.webhookValidation.apiEndpoints.issues.isNotEmpty()) {
                markdown.appendLine("#### API Endpoint Issues")
                report.webhookValidation.apiEndpoints.issues.forEach { issue ->
                    markdown.appendLine("- **Endpoint:** ${issue.endpoint} (${issue.method})")
                    markdown.appendLine("  - **Issue:** ${issue.issue}")
                    issue.expectedFormat?.let { format ->
                        markdown.appendLine("  - **Expected:** $format")
                    }
                    markdown.appendLine()
                }
            }
        }
        
        // UI Validation Issues
        if (report.uiValidation.viewModelValidation.stateFlowIssues.isNotEmpty() ||
            report.uiValidation.viewModelValidation.dataBindingIssues.isNotEmpty()) {
            
            markdown.appendLine("### UI Data Flow Issues")
            markdown.appendLine()
            
            if (report.uiValidation.viewModelValidation.stateFlowIssues.isNotEmpty()) {
                markdown.appendLine("#### StateFlow Issues")
                report.uiValidation.viewModelValidation.stateFlowIssues.forEach { issue ->
                    markdown.appendLine("- **ViewModel:** ${issue.viewModelClass}")
                    markdown.appendLine("  - **Property:** ${issue.stateProperty}")
                    markdown.appendLine("  - **Issue:** ${issue.issue}")
                    markdown.appendLine("  - **Recommendation:** ${issue.recommendation}")
                    markdown.appendLine()
                }
            }
            
            if (report.uiValidation.viewModelValidation.dataBindingIssues.isNotEmpty()) {
                markdown.appendLine("#### Data Binding Issues")
                report.uiValidation.viewModelValidation.dataBindingIssues.forEach { issue ->
                    markdown.appendLine("- **Component:** ${issue.componentFile}")
                    markdown.appendLine("  - **Property:** ${issue.bindingProperty}")
                    markdown.appendLine("  - **Issue:** ${issue.issue}")
                    issue.fix?.let { fix ->
                        markdown.appendLine("  - **Fix:** $fix")
                    }
                    markdown.appendLine()
                }
            }
        }
        
        // DI Validation Issues
        if (report.diValidation.hiltValidation.moduleIssues.isNotEmpty() ||
            report.diValidation.hiltValidation.bindingIssues.isNotEmpty()) {
            
            markdown.appendLine("### Dependency Injection Issues")
            markdown.appendLine()
            
            if (report.diValidation.hiltValidation.moduleIssues.isNotEmpty()) {
                markdown.appendLine("#### Module Issues")
                report.diValidation.hiltValidation.moduleIssues.forEach { issue ->
                    markdown.appendLine("- **Module:** ${issue.moduleName}")
                    markdown.appendLine("  - **Severity:** ${issue.severity}")
                    markdown.appendLine("  - **Issue:** ${issue.issue}")
                    issue.fix?.let { fix ->
                        markdown.appendLine("  - **Fix:** $fix")
                    }
                    markdown.appendLine()
                }
            }
            
            if (report.diValidation.hiltValidation.bindingIssues.isNotEmpty()) {
                markdown.appendLine("#### Binding Issues")
                report.diValidation.hiltValidation.bindingIssues.forEach { issue ->
                    markdown.appendLine("- **Interface:** ${issue.interfaceName}")
                    issue.implementationName?.let { impl ->
                        markdown.appendLine("  - **Implementation:** $impl")
                    }
                    markdown.appendLine("  - **Issue:** ${issue.issue}")
                    issue.fix?.let { fix ->
                        markdown.appendLine("  - **Fix:** $fix")
                    }
                    markdown.appendLine()
                }
            }
        }
        
        // Recommendations
        if (report.recommendations.isNotEmpty()) {
            markdown.appendLine("## Recommendations")
            markdown.appendLine()
            
            val groupedRecommendations = report.recommendations.groupBy { it.priority }
            
            groupedRecommendations[Priority.HIGH]?.let { highPriority ->
                markdown.appendLine("### High Priority")
                highPriority.forEach { recommendation ->
                    markdown.appendLine("#### ${recommendation.title}")
                    markdown.appendLine(recommendation.description)
                    markdown.appendLine()
                    markdown.appendLine("**Action Items:**")
                    recommendation.actionItems.forEach { action ->
                        markdown.appendLine("- $action")
                    }
                    recommendation.estimatedEffort?.let { effort ->
                        markdown.appendLine("**Estimated Effort:** $effort")
                    }
                    markdown.appendLine()
                }
            }
            
            groupedRecommendations[Priority.MEDIUM]?.let { mediumPriority ->
                markdown.appendLine("### Medium Priority")
                mediumPriority.forEach { recommendation ->
                    markdown.appendLine("#### ${recommendation.title}")
                    markdown.appendLine(recommendation.description)
                    markdown.appendLine()
                    markdown.appendLine("**Action Items:**")
                    recommendation.actionItems.forEach { action ->
                        markdown.appendLine("- $action")
                    }
                    recommendation.estimatedEffort?.let { effort ->
                        markdown.appendLine("**Estimated Effort:** $effort")
                    }
                    markdown.appendLine()
                }
            }
            
            groupedRecommendations[Priority.LOW]?.let { lowPriority ->
                markdown.appendLine("### Low Priority")
                lowPriority.forEach { recommendation ->
                    markdown.appendLine("#### ${recommendation.title}")
                    markdown.appendLine(recommendation.description)
                    markdown.appendLine()
                }
            }
        }
        
        // Risk Assessment (if detailed analysis is available)
        detailedAnalysis?.riskAssessment?.let { riskAssessment ->
            markdown.appendLine("## Risk Assessment")
            markdown.appendLine()
            markdown.appendLine("**Overall Risk Level:** ${riskAssessment.overallRiskLevel}")
            markdown.appendLine()
            
            if (riskAssessment.risks.isNotEmpty()) {
                markdown.appendLine("### Identified Risks")
                riskAssessment.risks.forEach { risk ->
                    markdown.appendLine("#### ${risk.category}")
                    markdown.appendLine("- **Description:** ${risk.description}")
                    markdown.appendLine("- **Probability:** ${risk.probability}")
                    markdown.appendLine("- **Impact:** ${risk.impact}")
                    markdown.appendLine("- **Mitigation:** ${risk.mitigation}")
                    markdown.appendLine("- **Timeline:** ${risk.timeline}")
                    markdown.appendLine()
                }
            }
        }
        
        // Footer
        markdown.appendLine("---")
        markdown.appendLine("*Report generated by Code Validation System*")
        
        return markdown.toString()
    }
    
    /**
     * Format validation report for console output
     */
    fun formatForConsole(report: ValidationReport, detailedAnalysis: DetailedAnalysisReport? = null): String {
        val console = StringBuilder()
        
        // Header
        console.appendLine("╔══════════════════════════════════════════════════════════════════════════════╗")
        console.appendLine("║                           CODE VALIDATION REPORT                            ║")
        console.appendLine("╚══════════════════════════════════════════════════════════════════════════════╝")
        console.appendLine()
        
        console.appendLine("Generated: ${dateFormat.format(Date(report.timestamp))}")
        console.appendLine("Project: ${report.projectPath}")
        console.appendLine()
        
        // Summary
        console.appendLine("┌─ SUMMARY ─────────────────────────────────────────────────────────────────────┐")
        console.appendLine("│ Overall Score: ${report.summary.overallScore}/100")
        console.appendLine("│ Total Issues:  ${report.summary.totalIssues}")
        console.appendLine("│ Critical:      ${report.summary.criticalIssues}")
        console.appendLine("│ Warning:       ${report.summary.warningIssues}")
        console.appendLine("│ Info:          ${report.summary.infoIssues}")
        console.appendLine("└───────────────────────────────────────────────────────────────────────────────┘")
        console.appendLine()
        
        // Category Breakdown
        console.appendLine("┌─ CATEGORY BREAKDOWN ──────────────────────────────────────────────────────────┐")
        report.summary.validationCategories.forEach { (category, summary) ->
            val statusIcon = when {
                summary.criticalIssues > 0 -> "❌"
                summary.warningIssues > 0 -> "⚠️"
                summary.totalIssues > 0 -> "ℹ️"
                else -> "✅"
            }
            console.appendLine("│ $statusIcon $category (Score: ${summary.score}/100)")
            console.appendLine("│   Critical: ${summary.criticalIssues}, Warning: ${summary.warningIssues}, Info: ${summary.infoIssues}")
        }
        console.appendLine("└───────────────────────────────────────────────────────────────────────────────┘")
        console.appendLine()
        
        // Critical Issues (if any)
        if (report.summary.criticalIssues > 0) {
            console.appendLine("┌─ CRITICAL ISSUES (IMMEDIATE ATTENTION REQUIRED) ─────────────────────────────┐")
            
            // Missing imports
            report.importValidation.missingImports.take(5).forEach { missing ->
                console.appendLine("│ ❌ Missing Import: ${missing.missingClass}")
                console.appendLine("│    File: ${missing.filePath}:${missing.lineNumber}")
                missing.suggestedImport?.let { suggestion ->
                    console.appendLine("│    Suggested: $suggestion")
                }
                console.appendLine("│")
            }
            
            // Architectural violations
            report.importValidation.architecturalViolations.take(3).forEach { violation ->
                console.appendLine("│ ❌ Architectural Violation")
                console.appendLine("│    File: ${violation.filePath}")
                console.appendLine("│    Issue: ${violation.description}")
                console.appendLine("│")
            }
            
            // Network issues
            report.webhookValidation.networkConfig.issues
                .filter { it.severity == Severity.CRITICAL }
                .take(3)
                .forEach { issue ->
                    console.appendLine("│ ❌ Network Issue: ${issue.component}")
                    console.appendLine("│    ${issue.issue}")
                    console.appendLine("│")
                }
            
            console.appendLine("└───────────────────────────────────────────────────────────────────────────────┘")
            console.appendLine()
        }
        
        // High Priority Recommendations
        val highPriorityRecs = report.recommendations.filter { it.priority == Priority.HIGH }
        if (highPriorityRecs.isNotEmpty()) {
            console.appendLine("┌─ HIGH PRIORITY RECOMMENDATIONS ──────────────────────────────────────────────┐")
            highPriorityRecs.take(5).forEach { recommendation ->
                console.appendLine("│ 🔥 ${recommendation.title}")
                console.appendLine("│    ${recommendation.description}")
                recommendation.estimatedEffort?.let { effort ->
                    console.appendLine("│    Estimated effort: $effort")
                }
                console.appendLine("│")
            }
            console.appendLine("└───────────────────────────────────────────────────────────────────────────────┘")
            console.appendLine()
        }
        
        // Quick Stats
        console.appendLine("┌─ QUICK STATS ─────────────────────────────────────────────────────────────────┐")
        console.appendLine("│ Import Issues:    ${report.importValidation.unusedImports.size + report.importValidation.missingImports.size + report.importValidation.architecturalViolations.size}")
        console.appendLine("│ Webhook Issues:   ${report.webhookValidation.networkConfig.issues.size + report.webhookValidation.apiEndpoints.issues.size}")
        console.appendLine("│ UI Issues:        ${report.uiValidation.viewModelValidation.stateFlowIssues.size + report.uiValidation.viewModelValidation.dataBindingIssues.size}")
        console.appendLine("│ DI Issues:        ${report.diValidation.hiltValidation.moduleIssues.size + report.diValidation.hiltValidation.bindingIssues.size}")
        console.appendLine("│ Connectivity:     ${if (report.webhookValidation.connectivity.isConnected) "✅ Connected" else "❌ Failed"}")
        console.appendLine("└───────────────────────────────────────────────────────────────────────────────┘")
        console.appendLine()
        
        // Next Steps
        console.appendLine("┌─ NEXT STEPS ──────────────────────────────────────────────────────────────────┐")
        if (report.summary.criticalIssues > 0) {
            console.appendLine("│ 1. Address ${report.summary.criticalIssues} critical issues immediately")
        }
        if (report.summary.warningIssues > 0) {
            console.appendLine("│ 2. Review and fix ${report.summary.warningIssues} warning issues")
        }
        console.appendLine("│ 3. Generate detailed markdown report for comprehensive analysis")
        console.appendLine("│ 4. Set up automated validation in CI/CD pipeline")
        console.appendLine("└───────────────────────────────────────────────────────────────────────────────┘")
        
        return console.toString()
    }
    
    /**
     * Format validation report as structured JSON
     */
    fun formatAsJson(report: ValidationReport, detailedAnalysis: DetailedAnalysisReport? = null): String {
        // This would typically use a JSON library like Gson or kotlinx.serialization
        // For now, we'll create a basic JSON structure manually
        val json = StringBuilder()
        
        json.appendLine("{")
        json.appendLine("  \"timestamp\": ${report.timestamp},")
        json.appendLine("  \"projectPath\": \"${report.projectPath}\",")
        json.appendLine("  \"summary\": {")
        json.appendLine("    \"totalIssues\": ${report.summary.totalIssues},")
        json.appendLine("    \"criticalIssues\": ${report.summary.criticalIssues},")
        json.appendLine("    \"warningIssues\": ${report.summary.warningIssues},")
        json.appendLine("    \"infoIssues\": ${report.summary.infoIssues},")
        json.appendLine("    \"overallScore\": ${report.summary.overallScore}")
        json.appendLine("  },")
        
        // Categories
        json.appendLine("  \"categories\": {")
        val categories = report.summary.validationCategories.entries.toList()
        categories.forEachIndexed { index, (category, summary) ->
            json.appendLine("    \"${category.replace(" ", "_").lowercase()}\": {")
            json.appendLine("      \"score\": ${summary.score},")
            json.appendLine("      \"totalIssues\": ${summary.totalIssues},")
            json.appendLine("      \"criticalIssues\": ${summary.criticalIssues},")
            json.appendLine("      \"warningIssues\": ${summary.warningIssues},")
            json.appendLine("      \"infoIssues\": ${summary.infoIssues}")
            json.append("    }")
            if (index < categories.size - 1) json.appendLine(",")
            else json.appendLine()
        }
        json.appendLine("  },")
        
        // Recommendations count by priority
        json.appendLine("  \"recommendations\": {")
        json.appendLine("    \"high\": ${report.recommendations.count { it.priority == Priority.HIGH }},")
        json.appendLine("    \"medium\": ${report.recommendations.count { it.priority == Priority.MEDIUM }},")
        json.appendLine("    \"low\": ${report.recommendations.count { it.priority == Priority.LOW }}")
        json.appendLine("  },")
        
        // Connectivity status
        json.appendLine("  \"connectivity\": {")
        json.appendLine("    \"isConnected\": ${report.webhookValidation.connectivity.isConnected},")
        json.appendLine("    \"responseTime\": ${report.webhookValidation.connectivity.responseTime}")
        json.appendLine("  }")
        
        detailedAnalysis?.let { analysis ->
            json.appendLine(",")
            json.appendLine("  \"detailedAnalysis\": {")
            json.appendLine("    \"overallHealth\": \"${analysis.executiveSummary.overallHealth}\",")
            json.appendLine("    \"estimatedFixTime\": \"${analysis.executiveSummary.estimatedFixTime}\",")
            json.appendLine("    \"riskLevel\": \"${analysis.riskAssessment.overallRiskLevel}\"")
            json.appendLine("  }")
        }
        
        json.appendLine("}")
        
        return json.toString()
    }
    
    /**
     * Format a summary report for quick overview
     */
    fun formatSummary(report: ValidationReport): String {
        val summary = StringBuilder()
        
        summary.appendLine("VALIDATION SUMMARY")
        summary.appendLine("==================")
        summary.appendLine("Score: ${report.summary.overallScore}/100")
        summary.appendLine("Issues: ${report.summary.totalIssues} (${report.summary.criticalIssues} critical)")
        summary.appendLine("Status: ${getOverallStatus(report.summary.overallScore, report.summary.criticalIssues)}")
        
        if (report.summary.criticalIssues > 0) {
            summary.appendLine()
            summary.appendLine("⚠️  CRITICAL ISSUES REQUIRE IMMEDIATE ATTENTION")
        }
        
        return summary.toString()
    }
    
    // Helper methods
    
    private fun calculatePercentage(value: Int, total: Int): Int {
        return if (total > 0) (value * 100) / total else 0
    }
    
    private fun getOverallStatus(score: Int, criticalIssues: Int): String {
        return when {
            criticalIssues > 0 -> "❌ CRITICAL"
            score >= 90 -> "✅ EXCELLENT"
            score >= 75 -> "✅ GOOD"
            score >= 60 -> "⚠️ FAIR"
            score >= 40 -> "⚠️ POOR"
            else -> "❌ CRITICAL"
        }
    }
}