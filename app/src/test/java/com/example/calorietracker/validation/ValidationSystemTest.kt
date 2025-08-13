package com.example.calorietracker.validation

import com.example.calorietracker.validation.interfaces.*
import com.example.calorietracker.validation.models.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

/**
 * Test class for ValidationSystem infrastructure
 */
class ValidationSystemTest {
    
    private val mockImportValidator = mockk<ImportValidator>()
    private val mockWebhookValidator = mockk<WebhookValidator>()
    private val mockUIDataFlowValidator = mockk<UIDataFlowValidator>()
    private val mockDIValidator = mockk<DIValidator>()
    
    private val validationSystem = ValidationSystemFactory.create(
        importValidator = mockImportValidator,
        webhookValidator = mockWebhookValidator,
        uiDataFlowValidator = mockUIDataFlowValidator,
        diValidator = mockDIValidator
    )
    
    @Test
    fun `should return system status correctly`() {
        // Given
        coEvery { mockImportValidator.getValidatorName() } returns "ImportValidator"
        coEvery { mockWebhookValidator.getValidatorName() } returns "WebhookValidator"
        coEvery { mockUIDataFlowValidator.getValidatorName() } returns "UIDataFlowValidator"
        coEvery { mockDIValidator.getValidatorName() } returns "DIValidator"
        
        // When
        val status = validationSystem.getSystemStatus()
        
        // Then
        assertTrue(status.isInitialized)
        assertEquals("1.0.0", status.version)
        assertEquals(4, status.availableValidators.size)
        assertTrue(status.availableValidators.contains("ImportValidator"))
        assertTrue(status.availableValidators.contains("WebhookValidator"))
        assertTrue(status.availableValidators.contains("UIDataFlowValidator"))
        assertTrue(status.availableValidators.contains("DIValidator"))
    }
    
    @Test
    fun `should execute comprehensive validation successfully`() = runTest {
        // Given
        val projectPath = "/test/project"
        
        coEvery { mockImportValidator.validateImports(projectPath) } returns ImportValidationResult(
            unusedImports = emptyList(),
            missingImports = emptyList(),
            architecturalViolations = emptyList(),
            circularDependencies = emptyList()
        )
        
        coEvery { mockWebhookValidator.validateMakeService() } returns WebhookValidationResult(
            networkConfig = NetworkConfigResult(emptyList(), true),
            apiEndpoints = ApiEndpointResult(emptyList(), 1, 1),
            connectivity = ConnectivityResult(true, 100L, null),
            jsonSerialization = SerializationResult(emptyList(), true)
        )
        
        coEvery { mockUIDataFlowValidator.validateViewModels() } returns ViewModelValidationResult(
            stateFlowIssues = emptyList(),
            dataBindingIssues = emptyList(),
            lifecycleIssues = emptyList()
        )
        
        coEvery { mockUIDataFlowValidator.validateDataBinding() } returns DataBindingResult(
            issues = emptyList(),
            isValid = true
        )
        
        coEvery { mockUIDataFlowValidator.validateStateManagement() } returns StateManagementResult(
            issues = emptyList(),
            isValid = true
        )
        
        coEvery { mockUIDataFlowValidator.validateUIComponents() } returns UIComponentResult(
            issues = emptyList(),
            validComponents = 1,
            totalComponents = 1
        )
        
        coEvery { mockDIValidator.validateHiltModules() } returns HiltValidationResult(
            moduleIssues = emptyList(),
            bindingIssues = emptyList(),
            scopeIssues = emptyList()
        )
        
        coEvery { mockDIValidator.validateDependencyGraph() } returns DependencyGraphResult(
            circularDependencies = emptyList(),
            missingDependencies = emptyList(),
            isValid = true
        )
        
        coEvery { mockDIValidator.validateScopes() } returns ScopeValidationResult(
            issues = emptyList(),
            isValid = true
        )
        
        // When
        val report = validationSystem.executeComprehensiveValidation(projectPath)
        
        // Then
        assertNotNull(report)
        assertEquals(projectPath, report.projectPath)
        assertEquals(0, report.summary.totalIssues)
        assertTrue(report.summary.overallScore > 90)
    }
    
    @Test
    fun `should format report correctly`() = runTest {
        // Given
        val projectPath = "/test/project"
        val report = ValidationReport(
            timestamp = System.currentTimeMillis(),
            projectPath = projectPath,
            summary = ValidationSummary(0, 0, 0, 0, 100),
            importValidation = ImportValidationResult(emptyList(), emptyList(), emptyList(), emptyList()),
            webhookValidation = WebhookValidationResult(
                NetworkConfigResult(emptyList(), true),
                ApiEndpointResult(emptyList(), 1, 1),
                ConnectivityResult(true, 100L, null),
                SerializationResult(emptyList(), true)
            ),
            uiValidation = UIValidationResult(
                ViewModelValidationResult(emptyList(), emptyList(), emptyList()),
                DataBindingResult(emptyList(), true),
                StateManagementResult(emptyList(), true),
                UIComponentResult(emptyList(), 1, 1)
            ),
            diValidation = DIValidationResult(
                HiltValidationResult(emptyList(), emptyList(), emptyList()),
                DependencyGraphResult(emptyList(), emptyList(), true),
                ScopeValidationResult(emptyList(), true)
            ),
            recommendations = emptyList()
        )
        
        // When
        val markdownReport = validationSystem.formatReport(report, ReportFormat.MARKDOWN)
        val consoleReport = validationSystem.formatReport(report, ReportFormat.CONSOLE)
        
        // Then
        assertTrue(markdownReport.contains("# Code Validation Report"))
        assertTrue(markdownReport.contains("Overall Score | 100/100"))
        assertTrue(consoleReport.contains("CODE VALIDATION REPORT"))
        assertTrue(consoleReport.contains("Overall Score: 100/100"))
    }
}