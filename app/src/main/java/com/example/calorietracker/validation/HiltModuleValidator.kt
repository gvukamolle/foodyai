package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.BindingIssue
import com.example.calorietracker.validation.models.ModuleIssue
import com.example.calorietracker.validation.models.ScopeIssue
import javax.inject.Singleton

/**
 * Validator for Hilt module configurations
 */
@Singleton
class HiltModuleValidator {
    
    private val requiredHiltAnnotations = setOf("@Module", "@InstallIn")
    private val validScopes = setOf(
        "SingletonComponent",
        "ViewModelComponent", 
        "ActivityComponent",
        "FragmentComponent",
        "ServiceComponent"
    )
    private val validScopeAnnotations = setOf(
        "@Singleton",
        "@ViewModelScoped",
        "@ActivityScoped",
        "@FragmentScoped",
        "@ServiceScoped"
    )
    
    /**
     * Validates that modules have proper Hilt annotations
     */
    fun validateModuleAnnotations(moduleName: String, moduleContent: String): List<ModuleIssue> {
        val issues = mutableListOf<ModuleIssue>()
        
        // Check for @Module annotation
        if (!moduleContent.contains("@Module")) {
            issues.add(ModuleIssue(
                moduleName = moduleName,
                issue = "Missing @Module annotation",
                severity = Severity.CRITICAL,
                fix = "Add @Module annotation to the class"
            ))
        }
        
        // Check for @InstallIn annotation
        if (!moduleContent.contains("@InstallIn")) {
            issues.add(ModuleIssue(
                moduleName = moduleName,
                issue = "Missing @InstallIn annotation",
                severity = Severity.CRITICAL,
                fix = "Add @InstallIn annotation with appropriate component"
            ))
        } else {
            // Validate InstallIn component
            val installInPattern = "@InstallIn\\(([^)]+)\\)".toRegex()
            val match = installInPattern.find(moduleContent)
            if (match != null) {
                val component = match.groupValues[1].trim()
                val isValidComponent = validScopes.any { component.contains(it) }
                if (!isValidComponent) {
                    issues.add(ModuleIssue(
                        moduleName = moduleName,
                        issue = "Invalid component in @InstallIn: $component",
                        severity = Severity.WARNING,
                        fix = "Use one of: ${validScopes.joinToString(", ")}"
                    ))
                }
            }
        }
        
        // Check for proper import statements
        if (moduleContent.contains("@Module") && !moduleContent.contains("import dagger.Module")) {
            issues.add(ModuleIssue(
                moduleName = moduleName,
                issue = "Missing import for dagger.Module",
                severity = Severity.CRITICAL,
                fix = "Add import dagger.Module"
            ))
        }
        
        if (moduleContent.contains("@InstallIn") && !moduleContent.contains("import dagger.hilt.InstallIn")) {
            issues.add(ModuleIssue(
                moduleName = moduleName,
                issue = "Missing import for dagger.hilt.InstallIn",
                severity = Severity.CRITICAL,
                fix = "Add import dagger.hilt.InstallIn"
            ))
        }
        
        return issues
    }
    
    /**
     * Validates provider methods in modules
     */
    fun validateProviderMethods(moduleName: String, moduleContent: String): List<ModuleIssue> {
        val issues = mutableListOf<ModuleIssue>()
        
        // Find all provider methods
        val providerPattern = "@Provides\\s+(?:@\\w+\\s+)*fun\\s+(\\w+)\\s*\\(".toRegex(RegexOption.MULTILINE)
        val providerMatches = providerPattern.findAll(moduleContent)
        
        for (match in providerMatches) {
            val methodName = match.groupValues[1]
            val methodStart = match.range.first
            val methodEnd = findMethodEnd(moduleContent, methodStart)
            val methodContent = moduleContent.substring(methodStart, methodEnd)
            
            // Check for proper scope annotation
            if (moduleContent.contains("@InstallIn(SingletonComponent::class)") && 
                !methodContent.contains("@Singleton")) {
                issues.add(ModuleIssue(
                    moduleName = moduleName,
                    issue = "Provider method '$methodName' in SingletonComponent should have @Singleton scope",
                    severity = Severity.WARNING,
                    fix = "Add @Singleton annotation to the method"
                ))
            }
            
            if (moduleContent.contains("@InstallIn(ViewModelComponent::class)") && 
                !methodContent.contains("@ViewModelScoped")) {
                issues.add(ModuleIssue(
                    moduleName = moduleName,
                    issue = "Provider method '$methodName' in ViewModelComponent should have @ViewModelScoped",
                    severity = Severity.WARNING,
                    fix = "Add @ViewModelScoped annotation to the method"
                ))
            }
            
            // Check for missing @Provides annotation
            if (!methodContent.contains("@Provides")) {
                issues.add(ModuleIssue(
                    moduleName = moduleName,
                    issue = "Method '$methodName' appears to be a provider but missing @Provides annotation",
                    severity = Severity.CRITICAL,
                    fix = "Add @Provides annotation to the method"
                ))
            }
        }
        
        return issues
    }
    
    /**
     * Validates binding methods in abstract modules
     */
    fun validateBindings(moduleName: String, moduleContent: String): List<BindingIssue> {
        val issues = mutableListOf<BindingIssue>()
        
        // Check if module is abstract (for @Binds methods)
        val isAbstractModule = moduleContent.contains("abstract class")
        
        // Find all @Binds methods
        val bindsPattern = "@Binds\\s+(?:@\\w+\\s+)*abstract\\s+fun\\s+(\\w+)\\s*\\(([^)]+)\\)\\s*:\\s*(\\w+)".toRegex(RegexOption.MULTILINE)
        val bindsMatches = bindsPattern.findAll(moduleContent)
        
        for (match in bindsMatches) {
            val methodName = match.groupValues[1]
            val parameter = match.groupValues[2].trim()
            val returnType = match.groupValues[3]
            
            if (!isAbstractModule) {
                issues.add(BindingIssue(
                    interfaceName = returnType,
                    implementationName = parameter,
                    issue = "@Binds method '$methodName' found in non-abstract module",
                    fix = "Make the module abstract or use @Provides instead of @Binds"
                ))
            }
            
            // Validate that parameter type matches implementation pattern
            if (!parameter.contains(":")) {
                issues.add(BindingIssue(
                    interfaceName = returnType,
                    implementationName = parameter,
                    issue = "Invalid parameter format in @Binds method '$methodName'",
                    fix = "Parameter should be in format 'paramName: ImplementationType'"
                ))
            }
        }
        
        return issues
    }
    
    /**
     * Validates module scope configurations
     */
    fun validateModuleScopes(moduleName: String, moduleContent: String): List<ScopeIssue> {
        val issues = mutableListOf<ScopeIssue>()
        
        // Extract InstallIn component
        val installInPattern = "@InstallIn\\(([^)]+)\\)".toRegex()
        val installInMatch = installInPattern.find(moduleContent)
        
        if (installInMatch != null) {
            val component = installInMatch.groupValues[1].trim()
            
            // Check scope consistency
            when {
                component.contains("SingletonComponent") -> {
                    if (moduleContent.contains("@ViewModelScoped")) {
                        issues.add(ScopeIssue(
                            componentName = component,
                            scopeIssue = "ViewModelScoped annotation found in SingletonComponent module",
                            recommendation = "Use @Singleton scope for SingletonComponent or move to ViewModelComponent"
                        ))
                    }
                }
                component.contains("ViewModelComponent") -> {
                    if (moduleContent.contains("@Singleton")) {
                        issues.add(ScopeIssue(
                            componentName = component,
                            scopeIssue = "Singleton annotation found in ViewModelComponent module",
                            recommendation = "Use @ViewModelScoped scope for ViewModelComponent or move to SingletonComponent"
                        ))
                    }
                }
            }
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
}