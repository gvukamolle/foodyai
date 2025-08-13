package com.example.calorietracker.validation.interfaces

import com.example.calorietracker.validation.models.*

/**
 * Interface for import validation functionality
 */
interface ImportValidator : BaseValidator {
    /**
     * Validates all imports in the project
     */
    suspend fun validateImports(projectPath: String): ImportValidationResult
    
    /**
     * Finds unused imports in a specific file
     */
    suspend fun findUnusedImports(filePath: String): List<UnusedImport>
    
    /**
     * Finds missing imports in a specific file
     */
    suspend fun findMissingImports(filePath: String): List<MissingImport>
    
    /**
     * Validates architectural dependencies according to Clean Architecture rules
     */
    suspend fun validateArchitecturalDependencies(filePath: String): List<ArchitecturalViolation>
    
    /**
     * Detects circular dependencies between files
     */
    suspend fun detectCircularDependencies(projectPath: String): List<CircularDependency>
}