package com.example.calorietracker.validation

import com.example.calorietracker.validation.interfaces.UIDataFlowValidator
import com.example.calorietracker.validation.models.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UI data flow validation functionality
 */
@Singleton
class UIDataFlowValidatorImpl @Inject constructor(
    private val viewModelValidator: ViewModelStateValidator,
    private val composeValidator: ComposeDataBindingValidator
) : UIDataFlowValidator {
    
    private val projectPath = System.getProperty("user.dir") ?: "."
    
    override suspend fun validateViewModels(): ViewModelValidationResult = coroutineScope {
        viewModelValidator.validateViewModels(projectPath)
    }
    
    override suspend fun validateDataBinding(): DataBindingResult = coroutineScope {
        composeValidator.validateDataBinding(projectPath)
    }
    
    override suspend fun validateStateManagement(): StateManagementResult = coroutineScope {
        composeValidator.validateStateManagement(projectPath)
    }
    
    override suspend fun validateUIComponents(): UIComponentResult = coroutineScope {
        composeValidator.validateUIComponents(projectPath)
    }
    
    override suspend fun validateDataMappers(): List<String> = coroutineScope {
        val issues = mutableListOf<String>()
        
        // Validate data mappers for correct transformations
        val mapperIssues = async { validateDataMapperImplementations() }
        
        issues.addAll(mapperIssues.await())
        issues
    }
    
    /**
     * Validates all UI data flow components together
     */
    suspend fun validateCompleteUIDataFlow(): UIValidationResult = coroutineScope {
        val viewModelValidation = async { validateViewModels() }
        val dataBinding = async { validateDataBinding() }
        val stateManagement = async { validateStateManagement() }
        val uiComponents = async { validateUIComponents() }
        
        UIValidationResult(
            viewModelValidation = viewModelValidation.await(),
            dataBinding = dataBinding.await(),
            stateManagement = stateManagement.await(),
            uiComponents = uiComponents.await()
        )
    }
    
    /**
     * Validates data mapper implementations for correct transformations
     */
    private suspend fun validateDataMapperImplementations(): List<String> {
        val issues = mutableListOf<String>()
        
        try {
            val mappersDir = java.io.File("$projectPath/app/src/main/java/com/example/calorietracker/data/mappers")
            if (!mappersDir.exists()) {
                issues.add("Data mappers directory not found")
                return issues
            }
            
            val mapperFiles = mappersDir.listFiles { file -> 
                file.isFile && file.name.endsWith("Mapper.kt")
            } ?: emptyArray()
            
            for (file in mapperFiles) {
                val content = file.readText()
                val fileName = file.name
                
                // Check for proper mapper interface implementation
                if (!content.contains("interface") && !content.contains("class")) {
                    issues.add("$fileName: Mapper should be properly structured as class or interface")
                }
                
                // Check for bidirectional mapping methods
                val hasDataToDomain = content.contains("mapDataToDomain") || content.contains("toDomain")
                val hasDomainToData = content.contains("mapDomainToData") || content.contains("toData")
                
                if (!hasDataToDomain) {
                    issues.add("$fileName: Missing data-to-domain mapping method")
                }
                
                if (!hasDomainToData) {
                    issues.add("$fileName: Missing domain-to-data mapping method")
                }
                
                // Check for null safety in mappings
                if (content.contains("!!") && !content.contains("requireNotNull")) {
                    issues.add("$fileName: Avoid using !! operator, use safe calls or requireNotNull")
                }
                
                // Check for proper error handling
                if (!content.contains("try") && !content.contains("runCatching")) {
                    issues.add("$fileName: Consider adding error handling for mapping operations")
                }
                
                // Check for validation in mappers
                if (content.contains("map") && !content.contains("validate") && !content.contains("check")) {
                    issues.add("$fileName: Consider adding validation for mapped data integrity")
                }
            }
            
        } catch (e: Exception) {
            issues.add("Error validating data mappers: ${e.message}")
        }
        
        return issues
    }
    
    override suspend fun validate(): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()
        
        try {
            val uiValidation = validateCompleteUIDataFlow()
            
            // Convert UI validation results to ValidationResult list
            if (uiValidation.viewModelValidation.stateFlowIssues.isNotEmpty()) {
                results.add(ValidationResult.Warning(
                    "StateFlow issues found",
                    "Found ${uiValidation.viewModelValidation.stateFlowIssues.size} StateFlow issues"
                ))
            }
            
            if (uiValidation.dataBinding.issues.isNotEmpty()) {
                results.add(ValidationResult.Warning(
                    "Data binding issues found", 
                    "Found ${uiValidation.dataBinding.issues.size} data binding issues"
                ))
            }
            
            if (results.isEmpty()) {
                results.add(ValidationResult.Success("UI data flow validation passed"))
            }
            
        } catch (e: Exception) {
            results.add(ValidationResult.Error(
                "UI validation failed",
                e.message ?: "Unknown error",
                "Check UI components and ViewModels"
            ))
        }
        
        return results
    }
    
    override fun getValidatorName(): String = "UI Data Flow Validator"
    
    override fun getCategory(): String = "UI Data Flow"
}