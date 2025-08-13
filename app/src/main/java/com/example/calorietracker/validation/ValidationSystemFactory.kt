package com.example.calorietracker.validation

import com.example.calorietracker.validation.error.ValidationErrorHandler
import com.example.calorietracker.validation.interfaces.*
import com.example.calorietracker.validation.reporting.ReportFormatter
import com.example.calorietracker.validation.reporting.ReportGenerator

/**
 * Factory class for creating ValidationSystem instances
 */
object ValidationSystemFactory {
    
    /**
     * Creates a ValidationSystem with the provided validators
     */
    fun create(
        importValidator: ImportValidator,
        webhookValidator: WebhookValidator,
        uiDataFlowValidator: UIDataFlowValidator,
        diValidator: DIValidator
    ): ValidationSystem {
        return ValidationSystem(
            importValidator = importValidator,
            webhookValidator = webhookValidator,
            uiDataFlowValidator = uiDataFlowValidator,
            diValidator = diValidator,
            errorHandler = ValidationErrorHandler(),
            reportGenerator = ReportGenerator(),
            reportFormatter = ReportFormatter()
        )
    }
    
    /**
     * Creates a ValidationSystem with custom components
     */
    fun createCustom(
        importValidator: ImportValidator,
        webhookValidator: WebhookValidator,
        uiDataFlowValidator: UIDataFlowValidator,
        diValidator: DIValidator,
        errorHandler: ValidationErrorHandler,
        reportGenerator: ReportGenerator,
        reportFormatter: ReportFormatter
    ): ValidationSystem {
        return ValidationSystem(
            importValidator = importValidator,
            webhookValidator = webhookValidator,
            uiDataFlowValidator = uiDataFlowValidator,
            diValidator = diValidator,
            errorHandler = errorHandler,
            reportGenerator = reportGenerator,
            reportFormatter = reportFormatter
        )
    }
}