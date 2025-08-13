package com.example.calorietracker.validation.reporting

import com.example.calorietracker.validation.Priority
import com.example.calorietracker.validation.Severity
import com.example.calorietracker.validation.models.*

/**
 * Generates detailed analysis reports with statistics, metrics, and prioritized recommendations
 */
class DetailedAnalysisReporter {
    
    /**
     * Generate detailed analysis report with comprehensive statistics and metrics
     */
    fun generateDetailedAnalysis(report: ValidationReport): DetailedAnalysisReport {
        return DetailedAnalysisReport(
            executiveSummary = generateExecutiveSummary(report),
            statisticsAndMetrics = generateStatisticsAndMetrics(report),
            prioritizedIssues = generatePrioritizedIssues(report),
            categoryAnalysis = generateCategoryAnalysis(report),
            actionPlan = generateActionPlan(report),
            riskAssessment = generateRiskAssessment(report),
            qualityMetrics = generateQualityMetrics(report)
        )
    }
    
    /**
     * Generate executive summary of validation results
     */
    private fun generateExecutiveSummary(report: ValidationReport): ExecutiveSummary {
        val summary = report.summary
        val highPriorityRecommendations = report.recommendations.count { it.priority == Priority.HIGH }
        
        val overallHealth = when {
            summary.overallScore >= 90 -> HealthStatus.EXCELLENT
            summary.overallScore >= 75 -> HealthStatus.GOOD
            summary.overallScore >= 60 -> HealthStatus.FAIR
            summary.overallScore >= 40 -> HealthStatus.POOR
            else -> HealthStatus.CRITICAL
        }
        
        val keyFindings = mutableListOf<String>()
        
        if (summary.criticalIssues > 0) {
            keyFindings.add("${summary.criticalIssues} critical issues require immediate attention")
        }
        
        if (summary.warningIssues > 10) {
            keyFindings.add("High number of warning issues (${summary.warningIssues}) may impact code quality")
        }
        
        // Analyze category-specific findings
        summary.validationCategories.forEach { (category, categorySummary) ->
            if (categorySummary.criticalIssues > 0) {
                keyFindings.add("$category has ${categorySummary.criticalIssues} critical issues")
            }
        }
        
        return ExecutiveSummary(
            overallHealth = overallHealth,
            totalIssuesFound = summary.totalIssues,
            criticalIssuesCount = summary.criticalIssues,
            overallScore = summary.overallScore,
            keyFindings = keyFindings,
            highPriorityActions = highPriorityRecommendations,
            estimatedFixTime = estimateOverallFixTime(report.recommendations)
        )
    }
    
    /**
     * Generate comprehensive statistics and metrics
     */
    private fun generateStatisticsAndMetrics(report: ValidationReport): StatisticsAndMetrics {
        val summary = report.summary
        
        // Calculate distribution percentages
        val total = summary.totalIssues.toDouble()
        val criticalPercentage = if (total > 0) (summary.criticalIssues / total * 100).toInt() else 0
        val warningPercentage = if (total > 0) (summary.warningIssues / total * 100).toInt() else 0
        val infoPercentage = if (total > 0) (summary.infoIssues / total * 100).toInt() else 0
        
        // Category performance metrics
        val categoryMetrics = summary.validationCategories.map { (category, categorySummary) ->
            CategoryMetrics(
                categoryName = category,
                issueCount = categorySummary.totalIssues,
                score = categorySummary.score,
                criticalIssues = categorySummary.criticalIssues,
                warningIssues = categorySummary.warningIssues,
                infoIssues = categorySummary.infoIssues,
                performanceRating = when {
                    categorySummary.score >= 90 -> "Excellent"
                    categorySummary.score >= 75 -> "Good"
                    categorySummary.score >= 60 -> "Fair"
                    categorySummary.score >= 40 -> "Poor"
                    else -> "Critical"
                }
            )
        }
        
        // Trend analysis (would be enhanced with historical data)
        val trendAnalysis = TrendAnalysis(
            improvementAreas = identifyImprovementAreas(summary),
            regressionRisks = identifyRegressionRisks(summary),
            stabilityIndicators = calculateStabilityIndicators(report)
        )
        
        return StatisticsAndMetrics(
            issueDistribution = IssueDistribution(
                critical = summary.criticalIssues,
                warning = summary.warningIssues,
                info = summary.infoIssues,
                criticalPercentage = criticalPercentage,
                warningPercentage = warningPercentage,
                infoPercentage = infoPercentage
            ),
            categoryMetrics = categoryMetrics,
            trendAnalysis = trendAnalysis,
            codeQualityIndex = calculateCodeQualityIndex(summary),
            maintainabilityScore = calculateMaintainabilityScore(report),
            technicalDebtEstimate = estimateTechnicalDebt(report)
        )
    }
    
    /**
     * Generate prioritized list of issues with detailed information
     */
    private fun generatePrioritizedIssues(report: ValidationReport): List<PrioritizedIssue> {
        val allIssues = mutableListOf<PrioritizedIssue>()
        
        // Convert import issues
        report.importValidation.missingImports.forEach { missing ->
            allIssues.add(
                PrioritizedIssue(
                    id = "IMPORT_${missing.hashCode()}",
                    title = "Missing Import: ${missing.missingClass}",
                    description = "Missing import for '${missing.missingClass}' in ${missing.filePath}",
                    severity = Severity.CRITICAL,
                    priority = Priority.HIGH,
                    category = "Import Validation",
                    filePath = missing.filePath,
                    lineNumber = missing.lineNumber,
                    impact = ImpactLevel.HIGH,
                    effort = EffortLevel.LOW,
                    suggestedFix = missing.suggestedImport?.let { "Add import: $it" } ?: "Add missing import",
                    relatedIssues = emptyList()
                )
            )
        }
        
        report.importValidation.architecturalViolations.forEach { violation ->
            allIssues.add(
                PrioritizedIssue(
                    id = "ARCH_${violation.hashCode()}",
                    title = "Architectural Violation",
                    description = violation.description,
                    severity = Severity.CRITICAL,
                    priority = Priority.HIGH,
                    category = "Architecture Compliance",
                    filePath = violation.filePath,
                    lineNumber = null,
                    impact = ImpactLevel.HIGH,
                    effort = EffortLevel.MEDIUM,
                    suggestedFix = violation.suggestion,
                    relatedIssues = emptyList()
                )
            )
        }
        
        // Convert webhook issues
        report.webhookValidation.networkConfig.issues.forEach { issue ->
            allIssues.add(
                PrioritizedIssue(
                    id = "WEBHOOK_${issue.hashCode()}",
                    title = "Network Configuration Issue",
                    description = "${issue.component}: ${issue.issue}",
                    severity = issue.severity,
                    priority = when (issue.severity) {
                        Severity.CRITICAL -> Priority.HIGH
                        Severity.WARNING -> Priority.MEDIUM
                        Severity.INFO -> Priority.LOW
                    },
                    category = "Webhook Validation",
                    filePath = null,
                    lineNumber = null,
                    impact = when (issue.severity) {
                        Severity.CRITICAL -> ImpactLevel.HIGH
                        Severity.WARNING -> ImpactLevel.MEDIUM
                        Severity.INFO -> ImpactLevel.LOW
                    },
                    effort = EffortLevel.MEDIUM,
                    suggestedFix = issue.fix ?: "Review network configuration",
                    relatedIssues = emptyList()
                )
            )
        }
        
        // Convert UI issues
        report.uiValidation.viewModelValidation.stateFlowIssues.forEach { issue ->
            allIssues.add(
                PrioritizedIssue(
                    id = "UI_${issue.hashCode()}",
                    title = "StateFlow Issue",
                    description = "${issue.viewModelClass}.${issue.stateProperty}: ${issue.issue}",
                    severity = Severity.WARNING,
                    priority = Priority.MEDIUM,
                    category = "UI Data Flow",
                    filePath = null,
                    lineNumber = null,
                    impact = ImpactLevel.MEDIUM,
                    effort = EffortLevel.MEDIUM,
                    suggestedFix = issue.recommendation,
                    relatedIssues = emptyList()
                )
            )
        }
        
        // Convert DI issues
        report.diValidation.hiltValidation.moduleIssues.forEach { issue ->
            allIssues.add(
                PrioritizedIssue(
                    id = "DI_${issue.hashCode()}",
                    title = "DI Module Issue",
                    description = "${issue.moduleName}: ${issue.issue}",
                    severity = issue.severity,
                    priority = when (issue.severity) {
                        Severity.CRITICAL -> Priority.HIGH
                        Severity.WARNING -> Priority.MEDIUM
                        Severity.INFO -> Priority.LOW
                    },
                    category = "Dependency Injection",
                    filePath = null,
                    lineNumber = null,
                    impact = when (issue.severity) {
                        Severity.CRITICAL -> ImpactLevel.HIGH
                        Severity.WARNING -> ImpactLevel.MEDIUM
                        Severity.INFO -> ImpactLevel.LOW
                    },
                    effort = EffortLevel.MEDIUM,
                    suggestedFix = issue.fix ?: "Review module configuration",
                    relatedIssues = emptyList()
                )
            )
        }
        
        // Sort by priority and impact
        return allIssues.sortedWith(
            compareByDescending<PrioritizedIssue> { it.priority }
                .thenByDescending { it.impact }
                .thenByDescending { it.severity }
        )
    }
    
    /**
     * Generate category-specific analysis
     */
    private fun generateCategoryAnalysis(report: ValidationReport): List<CategoryAnalysis> {
        return report.summary.validationCategories.map { (category, summary) ->
            CategoryAnalysis(
                categoryName = category,
                summary = summary,
                detailedFindings = generateCategoryFindings(category, report),
                recommendations = report.recommendations.filter { it.category.contains(category, ignoreCase = true) },
                riskLevel = calculateCategoryRiskLevel(summary),
                priorityActions = generateCategoryPriorityActions(category, report)
            )
        }
    }
    
    /**
     * Generate actionable plan based on analysis
     */
    private fun generateActionPlan(report: ValidationReport): ActionPlan {
        val immediateActions = report.recommendations
            .filter { it.priority == Priority.HIGH }
            .take(5)
            .map { recommendation ->
                ActionItem(
                    title = recommendation.title,
                    description = recommendation.description,
                    priority = recommendation.priority,
                    estimatedTime = recommendation.estimatedEffort ?: "Unknown",
                    dependencies = emptyList(),
                    assignedTo = null,
                    dueDate = null
                )
            }
        
        val shortTermActions = report.recommendations
            .filter { it.priority == Priority.MEDIUM }
            .take(10)
            .map { recommendation ->
                ActionItem(
                    title = recommendation.title,
                    description = recommendation.description,
                    priority = recommendation.priority,
                    estimatedTime = recommendation.estimatedEffort ?: "Unknown",
                    dependencies = emptyList(),
                    assignedTo = null,
                    dueDate = null
                )
            }
        
        val longTermActions = report.recommendations
            .filter { it.priority == Priority.LOW }
            .map { recommendation ->
                ActionItem(
                    title = recommendation.title,
                    description = recommendation.description,
                    priority = recommendation.priority,
                    estimatedTime = recommendation.estimatedEffort ?: "Unknown",
                    dependencies = emptyList(),
                    assignedTo = null,
                    dueDate = null
                )
            }
        
        return ActionPlan(
            immediateActions = immediateActions,
            shortTermActions = shortTermActions,
            longTermActions = longTermActions,
            totalEstimatedTime = calculateTotalEstimatedTime(report.recommendations),
            milestones = generateMilestones(report)
        )
    }
    
    /**
     * Generate risk assessment
     */
    private fun generateRiskAssessment(report: ValidationReport): RiskAssessment {
        val risks = mutableListOf<Risk>()
        
        // Critical issues pose high risk
        if (report.summary.criticalIssues > 0) {
            risks.add(
                Risk(
                    category = "Critical Issues",
                    description = "${report.summary.criticalIssues} critical issues may cause application failures",
                    probability = RiskProbability.HIGH,
                    impact = RiskImpact.HIGH,
                    mitigation = "Address critical issues immediately",
                    timeline = "Immediate"
                )
            )
        }
        
        // Architectural violations
        if (report.importValidation.architecturalViolations.isNotEmpty()) {
            risks.add(
                Risk(
                    category = "Architecture",
                    description = "Architectural violations may lead to maintenance difficulties",
                    probability = RiskProbability.MEDIUM,
                    impact = RiskImpact.MEDIUM,
                    mitigation = "Refactor to comply with Clean Architecture principles",
                    timeline = "Short term"
                )
            )
        }
        
        // Connectivity issues
        if (!report.webhookValidation.connectivity.isConnected) {
            risks.add(
                Risk(
                    category = "Integration",
                    description = "Webhook connectivity issues may affect data synchronization",
                    probability = RiskProbability.HIGH,
                    impact = RiskImpact.HIGH,
                    mitigation = "Fix network configuration and test connectivity",
                    timeline = "Immediate"
                )
            )
        }
        
        return RiskAssessment(
            overallRiskLevel = calculateOverallRiskLevel(risks),
            risks = risks,
            riskMatrix = generateRiskMatrix(risks),
            recommendations = generateRiskMitigationRecommendations(risks)
        )
    }
    
    /**
     * Generate quality metrics
     */
    private fun generateQualityMetrics(report: ValidationReport): QualityMetrics {
        return QualityMetrics(
            codeQualityScore = report.summary.overallScore,
            maintainabilityIndex = calculateMaintainabilityScore(report),
            technicalDebtRatio = calculateTechnicalDebtRatio(report),
            testCoverage = null, // Would need additional data
            complexityScore = calculateComplexityScore(report),
            duplicationIndex = null, // Would need code analysis
            securityScore = calculateSecurityScore(report),
            performanceScore = calculatePerformanceScore(report)
        )
    }
    
    // Helper methods
    
    private fun estimateOverallFixTime(recommendations: List<Recommendation>): String {
        val totalMinutes = recommendations.mapNotNull { recommendation ->
            recommendation.estimatedEffort?.let { effort ->
                parseTimeEstimate(effort)
            }
        }.sum()
        
        return when {
            totalMinutes < 60 -> "${totalMinutes} minutes"
            totalMinutes < 480 -> "${totalMinutes / 60} hours"
            else -> "${totalMinutes / 480} days"
        }
    }
    
    private fun parseTimeEstimate(effort: String): Int {
        return when {
            effort.contains("minutes") -> effort.filter { it.isDigit() }.toIntOrNull() ?: 30
            effort.contains("hour") -> (effort.filter { it.isDigit() }.toIntOrNull() ?: 1) * 60
            effort.contains("day") -> (effort.filter { it.isDigit() }.toIntOrNull() ?: 1) * 480
            else -> 30 // Default 30 minutes
        }
    }
    
    private fun identifyImprovementAreas(summary: ValidationSummary): List<String> {
        return summary.validationCategories
            .filter { it.value.score < 75 }
            .map { "${it.key} (Score: ${it.value.score})" }
    }
    
    private fun identifyRegressionRisks(summary: ValidationSummary): List<String> {
        val risks = mutableListOf<String>()
        if (summary.criticalIssues > 0) {
            risks.add("Critical issues may cause runtime failures")
        }
        if (summary.warningIssues > 20) {
            risks.add("High number of warnings may indicate code quality degradation")
        }
        return risks
    }
    
    private fun calculateStabilityIndicators(report: ValidationReport): List<String> {
        val indicators = mutableListOf<String>()
        
        if (report.importValidation.circularDependencies.isEmpty()) {
            indicators.add("No circular dependencies detected")
        }
        
        if (report.webhookValidation.connectivity.isConnected) {
            indicators.add("Webhook connectivity is stable")
        }
        
        if (report.diValidation.dependencyGraph.isValid) {
            indicators.add("Dependency injection graph is valid")
        }
        
        return indicators
    }
    
    private fun calculateCodeQualityIndex(summary: ValidationSummary): Int {
        return summary.overallScore
    }
    
    private fun calculateMaintainabilityScore(report: ValidationReport): Int {
        val architecturalViolations = report.importValidation.architecturalViolations.size
        val circularDependencies = report.importValidation.circularDependencies.size
        val diIssues = report.diValidation.hiltValidation.moduleIssues.size
        
        val totalMaintainabilityIssues = architecturalViolations + circularDependencies + diIssues
        
        return maxOf(0, 100 - (totalMaintainabilityIssues * 5))
    }
    
    private fun estimateTechnicalDebt(report: ValidationReport): String {
        val totalIssues = report.summary.totalIssues
        val criticalIssues = report.summary.criticalIssues
        
        val debtHours = (criticalIssues * 4) + ((totalIssues - criticalIssues) * 1)
        
        return when {
            debtHours < 8 -> "Low (< 1 day)"
            debtHours < 40 -> "Medium (1-5 days)"
            debtHours < 80 -> "High (1-2 weeks)"
            else -> "Very High (> 2 weeks)"
        }
    }
    
    private fun generateCategoryFindings(category: String, report: ValidationReport): List<String> {
        return when (category) {
            "Import Validation" -> listOf(
                "${report.importValidation.unusedImports.size} unused imports found",
                "${report.importValidation.missingImports.size} missing imports detected",
                "${report.importValidation.architecturalViolations.size} architectural violations identified"
            )
            "Webhook Validation" -> listOf(
                "${report.webhookValidation.networkConfig.issues.size} network configuration issues",
                "${report.webhookValidation.apiEndpoints.issues.size} API endpoint issues",
                "Connectivity status: ${if (report.webhookValidation.connectivity.isConnected) "Connected" else "Failed"}"
            )
            else -> emptyList()
        }
    }
    
    private fun calculateCategoryRiskLevel(summary: CategorySummary): RiskLevel {
        return when {
            summary.criticalIssues > 0 -> RiskLevel.HIGH
            summary.warningIssues > 5 -> RiskLevel.MEDIUM
            summary.totalIssues > 0 -> RiskLevel.LOW
            else -> RiskLevel.NONE
        }
    }
    
    private fun generateCategoryPriorityActions(category: String, report: ValidationReport): List<String> {
        return report.recommendations
            .filter { it.category.contains(category, ignoreCase = true) && it.priority == Priority.HIGH }
            .take(3)
            .map { it.title }
    }
    
    private fun calculateTotalEstimatedTime(recommendations: List<Recommendation>): String {
        return estimateOverallFixTime(recommendations)
    }
    
    private fun generateMilestones(report: ValidationReport): List<Milestone> {
        val milestones = mutableListOf<Milestone>()
        
        if (report.summary.criticalIssues > 0) {
            milestones.add(
                Milestone(
                    name = "Critical Issues Resolution",
                    description = "Resolve all critical issues",
                    targetDate = "Week 1",
                    criteria = listOf("All critical issues fixed", "Application stability verified")
                )
            )
        }
        
        milestones.add(
            Milestone(
                name = "Code Quality Improvement",
                description = "Address warning-level issues",
                targetDate = "Week 2-3",
                criteria = listOf("Warning issues reduced by 80%", "Code quality score > 80")
            )
        )
        
        return milestones
    }
    
    private fun calculateOverallRiskLevel(risks: List<Risk>): RiskLevel {
        return when {
            risks.any { it.impact == RiskImpact.HIGH && it.probability == RiskProbability.HIGH } -> RiskLevel.HIGH
            risks.any { it.impact == RiskImpact.MEDIUM || it.probability == RiskProbability.MEDIUM } -> RiskLevel.MEDIUM
            risks.isNotEmpty() -> RiskLevel.LOW
            else -> RiskLevel.NONE
        }
    }
    
    private fun generateRiskMatrix(risks: List<Risk>): Map<String, Int> {
        return mapOf(
            "High-High" to risks.count { it.impact == RiskImpact.HIGH && it.probability == RiskProbability.HIGH },
            "High-Medium" to risks.count { it.impact == RiskImpact.HIGH && it.probability == RiskProbability.MEDIUM },
            "Medium-High" to risks.count { it.impact == RiskImpact.MEDIUM && it.probability == RiskProbability.HIGH },
            "Medium-Medium" to risks.count { it.impact == RiskImpact.MEDIUM && it.probability == RiskProbability.MEDIUM }
        )
    }
    
    private fun generateRiskMitigationRecommendations(risks: List<Risk>): List<String> {
        return risks
            .filter { it.impact == RiskImpact.HIGH || it.probability == RiskProbability.HIGH }
            .map { it.mitigation }
            .distinct()
    }
    
    private fun calculateTechnicalDebtRatio(report: ValidationReport): Double {
        val totalIssues = report.summary.totalIssues
        val criticalIssues = report.summary.criticalIssues
        
        if (totalIssues == 0) return 0.0
        
        return (criticalIssues * 3.0 + (totalIssues - criticalIssues)) / (totalIssues * 3.0)
    }
    
    private fun calculateComplexityScore(report: ValidationReport): Int {
        val circularDeps = report.importValidation.circularDependencies.size
        val archViolations = report.importValidation.architecturalViolations.size
        
        return maxOf(0, 100 - ((circularDeps * 10) + (archViolations * 5)))
    }
    
    private fun calculateSecurityScore(report: ValidationReport): Int {
        // Basic security assessment based on available data
        val networkIssues = report.webhookValidation.networkConfig.issues
            .count { it.issue.contains("security", ignoreCase = true) }
        
        return maxOf(0, 100 - (networkIssues * 10))
    }
    
    private fun calculatePerformanceScore(report: ValidationReport): Int {
        val responseTime = report.webhookValidation.connectivity.responseTime
        
        return when {
            responseTime == null -> 50 // Unknown
            responseTime < 1000 -> 100 // < 1 second
            responseTime < 3000 -> 80  // < 3 seconds
            responseTime < 5000 -> 60  // < 5 seconds
            else -> 40 // > 5 seconds
        }
    }
}