package com.example.calorietracker.validation

import com.example.calorietracker.validation.interfaces.DIValidator
import com.example.calorietracker.validation.models.*
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of DIValidator for validating Dependency Injection configuration
 */
@Singleton
class DIValidatorImpl @Inject constructor() : DIValidator {
    
    private val hiltModuleValidator = HiltModuleValidator()
    private val dependencyGraphValidator = DependencyGraphValidator()
    private val scopeValidator = ScopeValidator()
    private val dependencyBindingValidator = DependencyBindingValidator()
    
    override suspend fun validateHiltModules(): HiltValidationResult {
        val moduleIssues = mutableListOf<ModuleIssue>()
        val bindingIssues = mutableListOf<BindingIssue>()
        val scopeIssues = mutableListOf<ScopeIssue>()
        
        // Get all DI module files
        val diModuleFiles = getDIModuleFiles()
        
        for (moduleFile in diModuleFiles) {
            val moduleContent = moduleFile.readText()
            val moduleName = moduleFile.nameWithoutExtension
            
            // Validate module annotations
            moduleIssues.addAll(hiltModuleValidator.validateModuleAnnotations(moduleName, moduleContent))
            
            // Validate provider methods
            moduleIssues.addAll(hiltModuleValidator.validateProviderMethods(moduleName, moduleContent))
            
            // Validate bindings
            bindingIssues.addAll(hiltModuleValidator.validateBindings(moduleName, moduleContent))
            
            // Validate scopes
            scopeIssues.addAll(hiltModuleValidator.validateModuleScopes(moduleName, moduleContent))
        }
        
        return HiltValidationResult(
            moduleIssues = moduleIssues,
            bindingIssues = bindingIssues,
            scopeIssues = scopeIssues
        )
    }
    
    override suspend fun validateDependencyGraph(): DependencyGraphResult {
        return dependencyGraphValidator.validateDependencyGraph()
    }
    
    override suspend fun validateScopes(): ScopeValidationResult {
        return scopeValidator.validateScopes()
    }
    
    override suspend fun validateBindings(): List<BindingIssue> {
        val bindingIssues = mutableListOf<BindingIssue>()
        
        // Validate Hilt module bindings
        val diModuleFiles = getDIModuleFiles()
        for (moduleFile in diModuleFiles) {
            val moduleContent = moduleFile.readText()
            val moduleName = moduleFile.nameWithoutExtension
            bindingIssues.addAll(hiltModuleValidator.validateBindings(moduleName, moduleContent))
        }
        
        // Validate repository bindings
        bindingIssues.addAll(dependencyBindingValidator.validateRepositoryBindings())
        
        // Validate use case bindings
        bindingIssues.addAll(dependencyBindingValidator.validateUseCaseBindings())
        
        // Analyze dependency graph completeness
        bindingIssues.addAll(dependencyBindingValidator.analyzeDependencyGraphCompleteness())
        
        return bindingIssues
    }
    
    override suspend fun detectCircularDependencies(): List<String> {
        return dependencyGraphValidator.detectCircularDependencies()
    }
    
    override suspend fun validate(): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()
        
        // Validate Hilt modules
        val hiltValidation = validateHiltModules()
        if (hiltValidation.moduleIssues.isNotEmpty() || hiltValidation.bindingIssues.isNotEmpty() || hiltValidation.scopeIssues.isNotEmpty()) {
            results.add(ValidationResult.Warning(
                "DI Configuration Issues Found",
                "Found ${hiltValidation.moduleIssues.size} module issues, ${hiltValidation.bindingIssues.size} binding issues, ${hiltValidation.scopeIssues.size} scope issues"
            ))
        } else {
            results.add(ValidationResult.Success("DI Configuration is valid"))
        }
        
        return results
    }
    
    override fun getValidatorName(): String = "DI Configuration Validator"
    
    override fun getCategory(): String = "Dependency Injection"
    
    private fun getDIModuleFiles(): List<File> {
        val diDirectory = File("app/src/main/java/com/example/calorietracker/di")
        return if (diDirectory.exists()) {
            diDirectory.listFiles { file -> 
                file.isFile && file.extension == "kt" && file.name.endsWith("Module.kt")
            }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
}