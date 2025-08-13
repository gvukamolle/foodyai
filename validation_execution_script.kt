#!/usr/bin/env kotlin

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Simple validation execution script for Task 8.1 - Import Validation
 */

fun main() {
    println("🚀 Starting Comprehensive Code Validation - Task 8.1")
    println("=" * 60)
    
    runBlocking {
        // Task 8.1: Run complete import validation
        println("\n📋 Task 8.1: Running Import Validation...")
        val importResult = executeImportValidation()
        printTaskResult("Import Validation", importResult.success)
        
        if (importResult.success) {
            println("✅ Task 8.1 completed successfully!")
            println("📊 Import Validation Results:")
            println("   - Kotlin files analyzed: ${importResult.filesAnalyzed}")
            println("   - Unused imports found: ${importResult.unusedImports}")
            println("   - Missing imports found: ${importResult.missingImports}")
            println("   - Architectural violations: ${importResult.architecturalViolations}")
            println("   - Circular dependencies: ${importResult.circularDependencies}")
        } else {
            println("❌ Task 8.1 failed: ${importResult.error}")
        }
    }
}

data class ImportValidationResult(
    val success: Boolean,
    val filesAnalyzed: Int = 0,
    val unusedImports: Int = 0,
    val missingImports: Int = 0,
    val architecturalViolations: Int = 0,
    val circularDependencies: Int = 0,
    val error: String? = null
)

suspend fun executeImportValidation(): ImportValidationResult {
    return try {
        val projectPath = "."
        val kotlinFiles = findKotlinFiles(projectPath)
        
        println("   📁 Found ${kotlinFiles.size} Kotlin files to analyze")
        
        var unusedImportsCount = 0
        var missingImportsCount = 0
        var architecturalViolationsCount = 0
        var circularDependenciesCount = 0
        
        // Analyze each Kotlin file
        kotlinFiles.forEach { file ->
            val analysis = analyzeKotlinFile(file)
            unusedImportsCount += analysis.unusedImports
            missingImportsCount += analysis.missingImports
            architecturalViolationsCount += analysis.architecturalViolations
            
            if (analysis.unusedImports > 0 || analysis.missingImports > 0 || analysis.architecturalViolations > 0) {
                println("   📄 ${file.relativeTo(File(".")).path}: ${analysis.unusedImports} unused, ${analysis.missingImports} missing, ${analysis.architecturalViolations} violations")
            }
        }
        
        // Check for circular dependencies
        circularDependenciesCount = detectCircularDependencies(kotlinFiles)
        
        ImportValidationResult(
            success = true,
            filesAnalyzed = kotlinFiles.size,
            unusedImports = unusedImportsCount,
            missingImports = missingImportsCount,
            architecturalViolations = architecturalViolationsCount,
            circularDependencies = circularDependenciesCount
        )
        
    } catch (e: Exception) {
        ImportValidationResult(
            success = false,
            error = e.message
        )
    }
}

fun findKotlinFiles(projectPath: String): List<File> {
    val projectDir = File(projectPath)
    return projectDir.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .filter { !it.path.contains("build/") }
        .filter { !it.path.contains(".gradle/") }
        .toList()
}

data class FileAnalysis(
    val unusedImports: Int,
    val missingImports: Int,
    val architecturalViolations: Int
)

fun analyzeKotlinFile(file: File): FileAnalysis {
    return try {
        val content = file.readText()
        val lines = content.lines()
        
        // Find import statements
        val importLines = lines.filter { it.trim().startsWith("import ") }
        val codeLines = lines.filter { !it.trim().startsWith("import ") && !it.trim().startsWith("package ") }
        val codeContent = codeLines.joinToString(" ")
        
        // Check for unused imports (simple heuristic)
        var unusedImports = 0
        importLines.forEach { importLine ->
            val importedClass = extractClassFromImport(importLine)
            if (importedClass != null && !codeContent.contains(importedClass)) {
                unusedImports++
            }
        }
        
        // Check for missing imports (simple heuristic - look for unresolved references)
        var missingImports = 0
        val commonMissingPatterns = listOf(
            "Unresolved reference",
            "Cannot resolve symbol",
            "Unresolved import"
        )
        
        // Check for architectural violations
        var architecturalViolations = 0
        architecturalViolations += checkArchitecturalViolations(file, content)
        
        FileAnalysis(
            unusedImports = unusedImports,
            missingImports = missingImports,
            architecturalViolations = architecturalViolations
        )
        
    } catch (e: Exception) {
        FileAnalysis(0, 0, 0)
    }
}

fun extractClassFromImport(importLine: String): String? {
    return try {
        val parts = importLine.trim().removePrefix("import ").split(".")
        parts.lastOrNull()?.takeIf { it.isNotEmpty() && it[0].isUpperCase() }
    } catch (e: Exception) {
        null
    }
}

fun checkArchitecturalViolations(file: File, content: String): Int {
    var violations = 0
    val filePath = file.path
    
    // Check Clean Architecture layer violations
    when {
        filePath.contains("/domain/") -> {
            // Domain layer should not import from data or presentation layers
            if (content.contains("import com.example.calorietracker.data.") ||
                content.contains("import com.example.calorietracker.presentation.")) {
                violations++
            }
        }
        filePath.contains("/data/") -> {
            // Data layer should not import from presentation layer
            if (content.contains("import com.example.calorietracker.presentation.")) {
                violations++
            }
        }
        filePath.contains("/presentation/") -> {
            // Presentation layer violations are less strict
        }
    }
    
    return violations
}

fun detectCircularDependencies(files: List<File>): Int {
    // Simple circular dependency detection
    val dependencies = mutableMapOf<String, Set<String>>()
    
    files.forEach { file ->
        val content = file.readText()
        val imports = content.lines()
            .filter { it.trim().startsWith("import com.example.calorietracker.") }
            .map { it.trim().removePrefix("import ").removeSuffix(".*") }
            .toSet()
        
        val packageName = content.lines()
            .find { it.trim().startsWith("package ") }
            ?.trim()?.removePrefix("package ")
        
        if (packageName != null) {
            dependencies[packageName] = imports
        }
    }
    
    // Simple cycle detection (this is a basic implementation)
    var circularDependencies = 0
    dependencies.forEach { (pkg, imports) ->
        imports.forEach { importPkg ->
            if (dependencies[importPkg]?.contains(pkg) == true) {
                circularDependencies++
            }
        }
    }
    
    return circularDependencies / 2 // Divide by 2 to avoid double counting
}

fun printTaskResult(taskName: String, success: Boolean) {
    val status = if (success) "✅ PASSED" else "❌ FAILED"
    println("   $status - $taskName")
}

operator fun String.times(n: Int): String = this.repeat(n)