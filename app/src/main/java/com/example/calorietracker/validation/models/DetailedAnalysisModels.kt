package com.example.calorietracker.validation.models

import com.example.calorietracker.validation.Priority
import com.example.calorietracker.validation.Severity

/**
 * Comprehensive detailed analysis report
 */
data class DetailedAnalysisReport(
    val executiveSummary: ExecutiveSummary,
    val statisticsAndMetrics: StatisticsAndMetrics,
    val prioritizedIssues: List<PrioritizedIssue>,
    val categoryAnalysis: List<CategoryAnalysis>,
    val actionPlan: ActionPlan,
    val riskAssessment: RiskAssessment,
    val qualityMetrics: QualityMetrics
)

/**
 * Executive summary for stakeholders
 */
data class ExecutiveSummary(
    val overallHealth: HealthStatus,
    val totalIssuesFound: Int,
    val criticalIssuesCount: Int,
    val overallScore: Int,
    val keyFindings: List<String>,
    val highPriorityActions: Int,
    val estimatedFixTime: String
)

/**
 * Health status enumeration
 */
enum class HealthStatus {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    CRITICAL
}

/**
 * Comprehensive statistics and metrics
 */
data class StatisticsAndMetrics(
    val issueDistribution: IssueDistribution,
    val categoryMetrics: List<CategoryMetrics>,
    val trendAnalysis: TrendAnalysis,
    val codeQualityIndex: Int,
    val maintainabilityScore: Int,
    val technicalDebtEstimate: String
)

/**
 * Issue distribution statistics
 */
data class IssueDistribution(
    val critical: Int,
    val warning: Int,
    val info: Int,
    val criticalPercentage: Int,
    val warningPercentage: Int,
    val infoPercentage: Int
)

/**
 * Category-specific metrics
 */
data class CategoryMetrics(
    val categoryName: String,
    val issueCount: Int,
    val score: Int,
    val criticalIssues: Int,
    val warningIssues: Int,
    val infoIssues: Int,
    val performanceRating: String
)

/**
 * Trend analysis data
 */
data class TrendAnalysis(
    val improvementAreas: List<String>,
    val regressionRisks: List<String>,
    val stabilityIndicators: List<String>
)

/**
 * Prioritized issue with detailed information
 */
data class PrioritizedIssue(
    val id: String,
    val title: String,
    val description: String,
    val severity: Severity,
    val priority: Priority,
    val category: String,
    val filePath: String?,
    val lineNumber: Int?,
    val impact: ImpactLevel,
    val effort: EffortLevel,
    val suggestedFix: String,
    val relatedIssues: List<String>
)

/**
 * Impact level enumeration
 */
enum class ImpactLevel {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Effort level enumeration
 */
enum class EffortLevel {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Category-specific analysis
 */
data class CategoryAnalysis(
    val categoryName: String,
    val summary: CategorySummary,
    val detailedFindings: List<String>,
    val recommendations: List<Recommendation>,
    val riskLevel: RiskLevel,
    val priorityActions: List<String>
)

/**
 * Risk level enumeration
 */
enum class RiskLevel {
    HIGH,
    MEDIUM,
    LOW,
    NONE
}

/**
 * Actionable plan with prioritized actions
 */
data class ActionPlan(
    val immediateActions: List<ActionItem>,
    val shortTermActions: List<ActionItem>,
    val longTermActions: List<ActionItem>,
    val totalEstimatedTime: String,
    val milestones: List<Milestone>
)

/**
 * Individual action item
 */
data class ActionItem(
    val title: String,
    val description: String,
    val priority: Priority,
    val estimatedTime: String,
    val dependencies: List<String>,
    val assignedTo: String?,
    val dueDate: String?
)

/**
 * Project milestone
 */
data class Milestone(
    val name: String,
    val description: String,
    val targetDate: String,
    val criteria: List<String>
)

/**
 * Risk assessment data
 */
data class RiskAssessment(
    val overallRiskLevel: RiskLevel,
    val risks: List<Risk>,
    val riskMatrix: Map<String, Int>,
    val recommendations: List<String>
)

/**
 * Individual risk item
 */
data class Risk(
    val category: String,
    val description: String,
    val probability: RiskProbability,
    val impact: RiskImpact,
    val mitigation: String,
    val timeline: String
)

/**
 * Risk probability enumeration
 */
enum class RiskProbability {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Risk impact enumeration
 */
enum class RiskImpact {
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Quality metrics data
 */
data class QualityMetrics(
    val codeQualityScore: Int,
    val maintainabilityIndex: Int,
    val technicalDebtRatio: Double,
    val testCoverage: Int?,
    val complexityScore: Int,
    val duplicationIndex: Double?,
    val securityScore: Int,
    val performanceScore: Int
)