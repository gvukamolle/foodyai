package com.example.calorietracker.validation

/**
 * Configuration class for the validation system
 */
data class ValidationConfig(
    val projectPath: String,
    val enableParallelValidation: Boolean = true,
    val enableDetailedReporting: Boolean = true,
    val includeWarnings: Boolean = true,
    val includeInfoMessages: Boolean = false,
    val maxConcurrentValidations: Int = 4,
    val timeoutMillis: Long = 30000L, // 30 seconds
    val outputFormat: ReportFormat = ReportFormat.MARKDOWN,
    val validationCategories: Set<ValidationCategory> = setOf(
        ValidationCategory.IMPORTS,
        ValidationCategory.WEBHOOKS,
        ValidationCategory.UI_DATA_FLOW,
        ValidationCategory.DEPENDENCY_INJECTION
    )
) {
    companion object {
        /**
         * Creates a default configuration for the given project path
         */
        fun default(projectPath: String): ValidationConfig {
            return ValidationConfig(projectPath = projectPath)
        }
        
        /**
         * Creates a configuration optimized for CI/CD environments
         */
        fun forCICD(projectPath: String): ValidationConfig {
            return ValidationConfig(
                projectPath = projectPath,
                enableParallelValidation = true,
                enableDetailedReporting = false,
                includeWarnings = true,
                includeInfoMessages = false,
                outputFormat = ReportFormat.CONSOLE,
                timeoutMillis = 60000L // 1 minute for CI
            )
        }
        
        /**
         * Creates a configuration for development environments with detailed reporting
         */
        fun forDevelopment(projectPath: String): ValidationConfig {
            return ValidationConfig(
                projectPath = projectPath,
                enableParallelValidation = true,
                enableDetailedReporting = true,
                includeWarnings = true,
                includeInfoMessages = true,
                outputFormat = ReportFormat.MARKDOWN,
                timeoutMillis = 120000L // 2 minutes for detailed analysis
            )
        }
    }
}