package com.example.calorietracker.validation

import com.example.calorietracker.validation.interfaces.ImportValidator
import com.example.calorietracker.validation.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ImportValidator for analyzing Kotlin file imports
 */
@Singleton
class ImportValidatorImpl @Inject constructor() : ImportValidator {

    private val architecturalValidator = ArchitecturalDependencyValidator()

    override suspend fun validate(): List<ValidationResult> {
        // This method can be used for general validation without specific project path
        return emptyList()
    }

    override fun getValidatorName(): String = "Import Validator"

    override fun getCategory(): String = "Import Analysis"

    companion object {
        private val IMPORT_PATTERN = Pattern.compile("^import\\s+([\\w.]+)(?:\\.\\*)?(?:\\s+as\\s+\\w+)?\\s*$")
        private val PACKAGE_PATTERN = Pattern.compile("^package\\s+([\\w.]+)\\s*$")
        private val CLASS_USAGE_PATTERN = Pattern.compile("\\b([A-Z][\\w]*)")
    }

    override suspend fun validateImports(projectPath: String): ImportValidationResult = withContext(Dispatchers.IO) {
        val kotlinFiles = findKotlinFiles(projectPath)
        val unusedImports = mutableListOf<UnusedImport>()
        val missingImports = mutableListOf<MissingImport>()
        val architecturalViolations = mutableListOf<ArchitecturalViolation>()
        
        kotlinFiles.forEach { file ->
            unusedImports.addAll(findUnusedImports(file.absolutePath))
            missingImports.addAll(findMissingImports(file.absolutePath))
            architecturalViolations.addAll(validateArchitecturalDependencies(file.absolutePath))
        }
        
        val circularDependencies = detectCircularDependencies(projectPath)
        
        ImportValidationResult(
            unusedImports = unusedImports,
            missingImports = missingImports,
            architecturalViolations = architecturalViolations,
            circularDependencies = circularDependencies
        )
    }

    override suspend fun findUnusedImports(filePath: String): List<UnusedImport> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists() || !file.name.endsWith(".kt")) {
            return@withContext emptyList()
        }

        val lines = file.readLines()
        val imports = extractImports(lines)
        val fileContent = file.readText()
        val unusedImports = mutableListOf<UnusedImport>()

        imports.forEach { (importStatement, lineNumber) ->
            val className = extractClassNameFromImport(importStatement)
            if (className != null && !isClassUsedInFile(className, fileContent, importStatement)) {
                unusedImports.add(
                    UnusedImport(
                        filePath = filePath,
                        importStatement = importStatement,
                        lineNumber = lineNumber
                    )
                )
            }
        }

        unusedImports
    }

    override suspend fun findMissingImports(filePath: String): List<MissingImport> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists() || !file.name.endsWith(".kt")) {
            return@withContext emptyList()
        }

        val lines = file.readLines()
        val existingImports = extractImports(lines).map { it.first }
        val fileContent = file.readText()
        val missingImports = mutableListOf<MissingImport>()

        // Find all class references in the file
        val classReferences = findClassReferences(fileContent)
        val currentPackage = extractPackage(lines)

        classReferences.forEach { className ->
            if (!isClassImported(className, existingImports, currentPackage)) {
                val suggestedImport = suggestImportForClass(className, filePath)
                if (suggestedImport != null) {
                    missingImports.add(
                        MissingImport(
                            filePath = filePath,
                            missingClass = className,
                            suggestedImport = suggestedImport,
                            lineNumber = findClassUsageLine(className, lines)
                        )
                    )
                }
            }
        }

        missingImports.distinctBy { it.missingClass }
    }

    override suspend fun validateArchitecturalDependencies(filePath: String): List<ArchitecturalViolation> {
        return architecturalValidator.validateLayerDependencies(filePath)
    }

    override suspend fun detectCircularDependencies(projectPath: String): List<CircularDependency> {
        return architecturalValidator.detectAdvancedCircularDependencies(projectPath)
    }

    private fun findKotlinFiles(projectPath: String): List<File> {
        val projectDir = File(projectPath)
        return projectDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".kt") }
            .toList()
    }

    private fun extractImports(lines: List<String>): List<Pair<String, Int>> {
        return lines.mapIndexedNotNull { index, line ->
            val trimmedLine = line.trim()
            val matcher = IMPORT_PATTERN.matcher(trimmedLine)
            if (matcher.matches()) {
                trimmedLine to (index + 1)
            } else null
        }
    }

    private fun extractPackage(lines: List<String>): String? {
        return lines.firstNotNullOfOrNull { line ->
            val matcher = PACKAGE_PATTERN.matcher(line.trim())
            if (matcher.matches()) matcher.group(1) else null
        }
    }

    private fun extractClassNameFromImport(importStatement: String): String? {
        val matcher = IMPORT_PATTERN.matcher(importStatement)
        return if (matcher.matches()) {
            val fullImport = matcher.group(1)
            fullImport.substringAfterLast('.')
        } else null
    }

    private fun isClassUsedInFile(className: String, fileContent: String, importStatement: String): Boolean {
        // Skip checking for wildcard imports
        if (importStatement.contains(".*")) return true
        
        // Check if class is used in the file content
        val classPattern = Pattern.compile("\\b$className\\b")
        val matcher = classPattern.matcher(fileContent)
        
        // Count occurrences (should be more than just the import statement)
        var count = 0
        while (matcher.find()) {
            count++
        }
        
        // If found only once, it's likely just the import statement
        return count > 1 || fileContent.contains("$className(") || 
               fileContent.contains("$className.") || fileContent.contains(": $className")
    }

    private fun findClassReferences(fileContent: String): List<String> {
        val matcher = CLASS_USAGE_PATTERN.matcher(fileContent)
        val classes = mutableSetOf<String>()
        
        while (matcher.find()) {
            val className = matcher.group(1)
            // Filter out common Kotlin/Java classes that don't need imports
            if (!isBuiltInClass(className)) {
                classes.add(className)
            }
        }
        
        return classes.toList()
    }

    private fun isBuiltInClass(className: String): Boolean {
        val builtInClasses = setOf(
            "String", "Int", "Long", "Double", "Float", "Boolean", "Char", "Byte", "Short",
            "List", "Map", "Set", "Array", "Any", "Unit", "Nothing", "Throwable", "Exception",
            "Override", "Suppress", "JvmStatic", "JvmField", "Deprecated"
        )
        return className in builtInClasses
    }

    private fun isClassImported(className: String, imports: List<String>, currentPackage: String?): Boolean {
        // Check if class is explicitly imported
        if (imports.any { it.endsWith(".$className") }) return true
        
        // Check for wildcard imports
        if (imports.any { it.endsWith(".*") }) {
            val wildcardPackages = imports.filter { it.endsWith(".*") }
                .map { it.removeSuffix(".*") }
            // This is a simplified check - in reality, we'd need to check if the class exists in those packages
            return wildcardPackages.isNotEmpty()
        }
        
        // Check if it's in the same package
        return currentPackage != null && className.startsWith(currentPackage.substringAfterLast('.', ""))
    }

    private fun suggestImportForClass(className: String, @Suppress("UNUSED_PARAMETER") filePath: String): String? {
        // This is a simplified implementation
        // In a real implementation, we would scan the project for classes with this name
        val commonImports = mapOf(
            "Inject" to "javax.inject.Inject",
            "Singleton" to "javax.inject.Singleton",
            "HiltViewModel" to "dagger.hilt.android.lifecycle.HiltViewModel",
            "Composable" to "androidx.compose.runtime.Composable",
            "State" to "androidx.compose.runtime.State",
            "MutableState" to "androidx.compose.runtime.MutableState",
            "StateFlow" to "kotlinx.coroutines.flow.StateFlow",
            "MutableStateFlow" to "kotlinx.coroutines.flow.MutableStateFlow",
            "ViewModel" to "androidx.lifecycle.ViewModel",
            "LiveData" to "androidx.lifecycle.LiveData",
            "MutableLiveData" to "androidx.lifecycle.MutableLiveData"
        )
        
        return commonImports[className]
    }

    private fun findClassUsageLine(className: String, lines: List<String>): Int {
        lines.forEachIndexed { index, line ->
            if (line.contains(className)) {
                return index + 1
            }
        }
        return 1
    }


}