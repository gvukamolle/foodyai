package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.ScopeIssue
import com.example.calorietracker.validation.models.ScopeValidationResult
import java.io.File
import javax.inject.Singleton

/**
 * Validator for Hilt scope configurations
 */
@Singleton
class ScopeValidator {
    
    private data class ScopeMapping(
        val component: String,
        val expectedScope: String,
        val validScopes: Set<String>
    )
    
    private val scopeMappings = listOf(
        ScopeMapping(
            component = "SingletonComponent",
            expectedScope = "@Singleton",
            validScopes = setOf("@Singleton")
        ),
        ScopeMapping(
            component = "ViewModelComponent",
            expectedScope = "@ViewModelScoped",
            validScopes = setOf("@ViewModelScoped")
        ),
        ScopeMapping(
            component = "ActivityComponent",
            expectedScope = "@ActivityScoped",
            validScopes = setOf("@ActivityScoped")
        ),
        ScopeMapping(
            component = "FragmentComponent",
            expectedScope = "@FragmentScoped",
            validScopes = setOf("@FragmentScoped")
        ),
        ScopeMapping(
            component = "ServiceComponent",
            expectedScope = "@ServiceScoped",
            validScopes = setOf("@ServiceScoped")
        )
    )
    
    /**
     * Validates scope configurations across all modules
     */
    fun validateScopes(): ScopeValidationResult {
        val issues = mutableListOf<ScopeIssue>()
        val diModuleFiles = getDIModuleFiles()
        
        for (moduleFile in diModuleFiles) {
            val moduleContent = moduleFile.readText()
            val moduleName = moduleFile.nameWithoutExtension
            
            issues.addAll(validateModuleScopes(moduleName, moduleContent))
        }
        
        return ScopeValidationResult(
            issues = issues,
            isValid = issues.isEmpty()
        )
    }
    
    private fun validateModuleScopes(moduleName: String, moduleContent: String): List<ScopeIssue> {
        val issues = mutableListOf<ScopeIssue>()
        
        // Extract the component from @InstallIn annotation
        val installInPattern = "@InstallIn\\(([^)]+)\\)".toRegex()
        val installInMatch = installInPattern.find(moduleContent)
        
        if (installInMatch == null) {
            issues.add(ScopeIssue(
                componentName = "Unknown",
                scopeIssue = "Module $moduleName is missing @InstallIn annotation",
                recommendation = "Add @InstallIn annotation with appropriate component"
            ))
            return issues
        }
        
        val component = installInMatch.groupValues[1].trim()
        val scopeMapping = scopeMappings.find { component.contains(it.component) }
        
        if (scopeMapping == null) {
            issues.add(ScopeIssue(
                componentName = component,
                scopeIssue = "Unknown component type in module $moduleName",
                recommendation = "Use one of the standard Hilt components: ${scopeMappings.map { it.component }.joinToString(", ")}"
            ))
            return issues
        }
        
        // Validate provider method scopes
        issues.addAll(validateProviderScopes(moduleName, moduleContent, scopeMapping))
        
        // Validate binding method scopes
        issues.addAll(validateBindingScopes(moduleName, moduleContent, scopeMapping))
        
        // Check for scope mismatches
        issues.addAll(validateScopeMismatches(moduleName, moduleContent, scopeMapping))
        
        return issues
    }
    
    private fun validateProviderScopes(
        moduleName: String,
        moduleContent: String,
        scopeMapping: ScopeMapping
    ): List<ScopeIssue> {
        val issues = mutableListOf<ScopeIssue>()
        
        // Find all @Provides methods
        val providesPattern = "@Provides\\s+(?:@\\w+\\s+)*fun\\s+(\\w+)".toRegex(RegexOption.MULTILINE)
        val providesMatches = providesPattern.findAll(moduleContent)
        
        for (match in providesMatches) {
            val methodName = match.groupValues[1]
            val methodStart = match.range.first
            val methodEnd = findMethodEnd(moduleContent, methodStart)
            val methodContent = moduleContent.substring(methodStart, methodEnd)
            
            // Check if method has appropriate scope annotation
            val hasValidScope = scopeMapping.validScopes.any { methodContent.contains(it) }
            
            if (!hasValidScope) {
                // Check if it has any scope annotation
                val hasScopeAnnotation = listOf("@Singleton", "@ViewModelScoped", "@ActivityScoped", "@FragmentScoped", "@ServiceScoped")
                    .any { methodContent.contains(it) }
                
                if (hasScopeAnnotation) {
                    val wrongScope = listOf("@Singleton", "@ViewModelScoped", "@ActivityScoped", "@FragmentScoped", "@ServiceScoped")
                        .find { methodContent.contains(it) }
                    
                    issues.add(ScopeIssue(
                        componentName = scopeMapping.component,
                        scopeIssue = "Provider method '$methodName' in $moduleName has incorrect scope $wrongScope",
                        recommendation = "Use ${scopeMapping.expectedScope} for ${scopeMapping.component}"
                    ))
                } else {
                    issues.add(ScopeIssue(
                        componentName = scopeMapping.component,
                        scopeIssue = "Provider method '$methodName' in $moduleName is missing scope annotation",
                        recommendation = "Add ${scopeMapping.expectedScope} annotation to the method"
                    ))
                }
            }
        }
        
        return issues
    }
    
    private fun validateBindingScopes(
        moduleName: String,
        moduleContent: String,
        scopeMapping: ScopeMapping
    ): List<ScopeIssue> {
        val issues = mutableListOf<ScopeIssue>()
        
        // Find all @Binds methods
        val bindsPattern = "@Binds\\s+(?:@\\w+\\s+)*abstract\\s+fun\\s+(\\w+)".toRegex(RegexOption.MULTILINE)
        val bindsMatches = bindsPattern.findAll(moduleContent)
        
        for (match in bindsMatches) {
            val methodName = match.groupValues[1]
            val methodStart = match.range.first
            val methodEnd = findMethodEnd(moduleContent, methodStart)
            val methodContent = moduleContent.substring(methodStart, methodEnd)
            
            // Check if method has appropriate scope annotation
            val hasValidScope = scopeMapping.validScopes.any { methodContent.contains(it) }
            
            if (!hasValidScope) {
                issues.add(ScopeIssue(
                    componentName = scopeMapping.component,
                    scopeIssue = "Binding method '$methodName' in $moduleName is missing scope annotation",
                    recommendation = "Add ${scopeMapping.expectedScope} annotation to the method"
                ))
            }
        }
        
        return issues
    }
    
    private fun validateScopeMismatches(
        moduleName: String,
        moduleContent: String,
        scopeMapping: ScopeMapping
    ): List<ScopeIssue> {
        val issues = mutableListOf<ScopeIssue>()
        
        // Check for common scope mismatches
        when (scopeMapping.component) {
            "SingletonComponent" -> {
                if (moduleContent.contains("@ViewModelScoped")) {
                    issues.add(ScopeIssue(
                        componentName = scopeMapping.component,
                        scopeIssue = "Module $moduleName installed in SingletonComponent but contains @ViewModelScoped",
                        recommendation = "Move ViewModelScoped dependencies to a module installed in ViewModelComponent"
                    ))
                }
            }
            "ViewModelComponent" -> {
                if (moduleContent.contains("@Singleton")) {
                    issues.add(ScopeIssue(
                        componentName = scopeMapping.component,
                        scopeIssue = "Module $moduleName installed in ViewModelComponent but contains @Singleton",
                        recommendation = "Move Singleton dependencies to a module installed in SingletonComponent"
                    ))
                }
            }
        }
        
        // Check for unscoped dependencies in scoped components
        val hasUnscopedProviders = moduleContent.contains("@Provides") && 
                                  !scopeMapping.validScopes.any { moduleContent.contains(it) }
        
        if (hasUnscopedProviders) {
            issues.add(ScopeIssue(
                componentName = scopeMapping.component,
                scopeIssue = "Module $moduleName has unscoped providers in scoped component",
                recommendation = "Add appropriate scope annotations to all provider methods"
            ))
        }
        
        return issues
    }
    
    private fun findMethodEnd(content: String, start: Int): Int {
        var braceCount = 0
        var inMethod = false
        var i = start
        
        while (i < content.length) {
            when (content[i]) {
                '{' -> {
                    braceCount++
                    inMethod = true
                }
                '}' -> {
                    braceCount--
                    if (inMethod && braceCount == 0) {
                        return i + 1
                    }
                }
            }
            i++
        }
        
        return content.length
    }
    
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