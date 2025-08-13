package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.ArchitecturalViolation
import com.example.calorietracker.validation.models.CircularDependency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Specialized validator for Clean Architecture dependency rules
 */
@Singleton
class ArchitecturalDependencyValidator @Inject constructor() {

    companion object {
        // Clean Architecture layer patterns - more comprehensive
        private val DOMAIN_PATTERNS = listOf(
            Pattern.compile(".*\\.domain\\.entities\\b"),
            Pattern.compile(".*\\.domain\\.repositories\\b"),
            Pattern.compile(".*\\.domain\\.usecases\\b"),
            Pattern.compile(".*\\.domain\\.common\\b"),
            Pattern.compile(".*\\.domain\\.exceptions\\b")
        )
        
        private val DATA_PATTERNS = listOf(
            Pattern.compile(".*\\.data\\.repositories\\b"),
            Pattern.compile(".*\\.data\\.mappers\\b"),
            Pattern.compile(".*\\.data\\b"),
            Pattern.compile(".*\\.network\\b"),
            Pattern.compile(".*\\.database\\b")
        )
        
        private val PRESENTATION_PATTERNS = listOf(
            Pattern.compile(".*\\.presentation\\.viewmodels\\b"),
            Pattern.compile(".*\\.presentation\\b")
        )
        
        private val UI_PATTERNS = listOf(
            Pattern.compile(".*\\.ui\\b"),
            Pattern.compile(".*\\.pages\\b"),
            Pattern.compile(".*\\.components\\b"),
            Pattern.compile(".*\\.screens\\b")
        )
        
        private val DI_PATTERNS = listOf(
            Pattern.compile(".*\\.di\\b")
        )
        
        private val IMPORT_PATTERN = Pattern.compile("^import\\s+([\\w.]+)(?:\\.\\*)?(?:\\s+as\\s+\\w+)?\\s*$")
        private val PACKAGE_PATTERN = Pattern.compile("^package\\s+([\\w.]+)\\s*$")
    }

    /**
     * Validates Clean Architecture layer dependencies
     */
    suspend fun validateLayerDependencies(filePath: String): List<ArchitecturalViolation> = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists() || !file.name.endsWith(".kt")) {
            return@withContext emptyList()
        }

        val lines = file.readLines()
        val imports = extractImports(lines)
        val currentLayer = determineLayer(filePath)
        val violations = mutableListOf<ArchitecturalViolation>()

        imports.forEach { importStatement ->
            val importedLayer = determineLayer(importStatement)
            val violation = checkLayerViolation(currentLayer, importedLayer, filePath, importStatement)
            if (violation != null) {
                violations.add(violation)
            }
        }

        violations
    }

    /**
     * Detects circular dependencies with enhanced analysis
     */
    suspend fun detectAdvancedCircularDependencies(projectPath: String): List<CircularDependency> = withContext(Dispatchers.IO) {
        val kotlinFiles = findKotlinFiles(projectPath)
        val dependencyGraph = buildEnhancedDependencyGraph(kotlinFiles)
        val circularDependencies = mutableListOf<CircularDependency>()

        // Tarjan's algorithm for strongly connected components
        val index = mutableMapOf<String, Int>()
        val lowLink = mutableMapOf<String, Int>()
        val onStack = mutableSetOf<String>()
        val stack = mutableListOf<String>()
        var currentIndex = 0

        fun strongConnect(node: String) {
            index[node] = currentIndex
            lowLink[node] = currentIndex
            currentIndex++
            stack.add(node)
            onStack.add(node)

            dependencyGraph[node]?.forEach { neighbor ->
                when {
                    neighbor !in index -> {
                        strongConnect(neighbor)
                        lowLink[node] = minOf(lowLink[node]!!, lowLink[neighbor]!!)
                    }
                    neighbor in onStack -> {
                        lowLink[node] = minOf(lowLink[node]!!, index[neighbor]!!)
                    }
                }
            }

            if (lowLink[node] == index[node]) {
                val component = mutableListOf<String>()
                do {
                    val w = stack.removeAt(stack.size - 1)
                    onStack.remove(w)
                    component.add(w)
                } while (w != node)

                if (component.size > 1) {
                    circularDependencies.add(
                        CircularDependency(
                            filePaths = component,
                            description = "Circular dependency detected in strongly connected component: ${component.map { File(it).name }.joinToString(" <-> ")}"
                        )
                    )
                }
            }
        }

        dependencyGraph.keys.forEach { node ->
            if (node !in index) {
                strongConnect(node)
            }
        }

        circularDependencies
    }

    /**
     * Validates specific architectural patterns
     */
    suspend fun validateArchitecturalPatterns(projectPath: String): List<ArchitecturalViolation> = withContext(Dispatchers.IO) {
        val violations = mutableListOf<ArchitecturalViolation>()
        val kotlinFiles = findKotlinFiles(projectPath)

        kotlinFiles.forEach { file ->
            violations.addAll(validateRepositoryPattern(file))
            violations.addAll(validateUseCasePattern(file))
            violations.addAll(validateViewModelPattern(file))
            violations.addAll(validateDIPattern(file))
        }

        violations
    }

    private fun extractImports(lines: List<String>): List<String> {
        return lines.mapNotNull { line ->
            val trimmedLine = line.trim()
            val matcher = IMPORT_PATTERN.matcher(trimmedLine)
            if (matcher.matches()) matcher.group(1) else null
        }
    }

    private fun determineLayer(path: String): ArchitecturalLayer {
        return when {
            DOMAIN_PATTERNS.any { it.matcher(path).find() } -> ArchitecturalLayer.DOMAIN
            DATA_PATTERNS.any { it.matcher(path).find() } -> ArchitecturalLayer.DATA
            PRESENTATION_PATTERNS.any { it.matcher(path).find() } -> ArchitecturalLayer.PRESENTATION
            UI_PATTERNS.any { it.matcher(path).find() } -> ArchitecturalLayer.UI
            DI_PATTERNS.any { it.matcher(path).find() } -> ArchitecturalLayer.DI
            else -> ArchitecturalLayer.UNKNOWN
        }
    }

    private fun checkLayerViolation(
        currentLayer: ArchitecturalLayer,
        importedLayer: ArchitecturalLayer,
        filePath: String,
        importStatement: String
    ): ArchitecturalViolation? {
        val isViolation = when (currentLayer) {
            ArchitecturalLayer.DOMAIN -> importedLayer in listOf(
                ArchitecturalLayer.DATA,
                ArchitecturalLayer.PRESENTATION,
                ArchitecturalLayer.UI
            )
            ArchitecturalLayer.DATA -> importedLayer in listOf(
                ArchitecturalLayer.PRESENTATION,
                ArchitecturalLayer.UI
            )
            ArchitecturalLayer.PRESENTATION -> importedLayer == ArchitecturalLayer.UI
            ArchitecturalLayer.UI -> false // UI can depend on any layer
            ArchitecturalLayer.DI -> false // DI can depend on any layer
            ArchitecturalLayer.UNKNOWN -> false
        }

        return if (isViolation) {
            ArchitecturalViolation(
                filePath = filePath,
                violationType = ViolationType.LAYER_DEPENDENCY_VIOLATION,
                description = "Layer ${currentLayer.name.lowercase()} should not depend on layer ${importedLayer.name.lowercase()}. Import: $importStatement",
                suggestion = getSuggestionForViolation(currentLayer, importedLayer)
            )
        } else null
    }

    private fun getSuggestionForViolation(currentLayer: ArchitecturalLayer, importedLayer: ArchitecturalLayer): String {
        return when (currentLayer to importedLayer) {
            ArchitecturalLayer.DOMAIN to ArchitecturalLayer.DATA -> 
                "Use dependency inversion: create an interface in domain layer and implement it in data layer"
            ArchitecturalLayer.DOMAIN to ArchitecturalLayer.PRESENTATION -> 
                "Domain layer should not know about presentation. Move shared logic to domain entities or use cases"
            ArchitecturalLayer.DOMAIN to ArchitecturalLayer.UI -> 
                "Domain layer should not depend on UI. Use dependency inversion or move UI-specific logic to presentation layer"
            ArchitecturalLayer.DATA to ArchitecturalLayer.PRESENTATION -> 
                "Data layer should not depend on presentation. Use dependency inversion or move logic to domain layer"
            ArchitecturalLayer.DATA to ArchitecturalLayer.UI -> 
                "Data layer should not depend on UI. Move UI-specific logic to presentation layer"
            ArchitecturalLayer.PRESENTATION to ArchitecturalLayer.UI -> 
                "Consider if this dependency is necessary. ViewModels should be UI-agnostic when possible"
            else -> "Review the dependency and consider architectural boundaries"
        }
    }

    private fun validateRepositoryPattern(file: File): List<ArchitecturalViolation> {
        val violations = mutableListOf<ArchitecturalViolation>()
        val content = file.readText()
        val fileName = file.name

        if (fileName.contains("Repository") && !fileName.contains("RepositoryImpl")) {
            // This should be an interface in domain layer
            if (!file.absolutePath.contains("domain")) {
                violations.add(
                    ArchitecturalViolation(
                        filePath = file.absolutePath,
                        violationType = ViolationType.IMPROPER_IMPORT,
                        description = "Repository interface should be in domain layer",
                        suggestion = "Move repository interface to domain/repositories package"
                    )
                )
            }
        }

        if (fileName.contains("RepositoryImpl")) {
            // This should be in data layer
            if (!file.absolutePath.contains("data")) {
                violations.add(
                    ArchitecturalViolation(
                        filePath = file.absolutePath,
                        violationType = ViolationType.IMPROPER_IMPORT,
                        description = "Repository implementation should be in data layer",
                        suggestion = "Move repository implementation to data/repositories package"
                    )
                )
            }
        }

        return violations
    }

    private fun validateUseCasePattern(file: File): List<ArchitecturalViolation> {
        val violations = mutableListOf<ArchitecturalViolation>()
        val fileName = file.name

        if (fileName.contains("UseCase")) {
            if (!file.absolutePath.contains("domain")) {
                violations.add(
                    ArchitecturalViolation(
                        filePath = file.absolutePath,
                        violationType = ViolationType.IMPROPER_IMPORT,
                        description = "Use cases should be in domain layer",
                        suggestion = "Move use case to domain/usecases package"
                    )
                )
            }
        }

        return violations
    }

    private fun validateViewModelPattern(file: File): List<ArchitecturalViolation> {
        val violations = mutableListOf<ArchitecturalViolation>()
        val fileName = file.name

        if (fileName.contains("ViewModel")) {
            if (!file.absolutePath.contains("presentation")) {
                violations.add(
                    ArchitecturalViolation(
                        filePath = file.absolutePath,
                        violationType = ViolationType.IMPROPER_IMPORT,
                        description = "ViewModels should be in presentation layer",
                        suggestion = "Move ViewModel to presentation/viewmodels package"
                    )
                )
            }
        }

        return violations
    }

    private fun validateDIPattern(file: File): List<ArchitecturalViolation> {
        val violations = mutableListOf<ArchitecturalViolation>()
        val content = file.readText()

        if (content.contains("@Module") || content.contains("@Component")) {
            if (!file.absolutePath.contains("di")) {
                violations.add(
                    ArchitecturalViolation(
                        filePath = file.absolutePath,
                        violationType = ViolationType.IMPROPER_IMPORT,
                        description = "DI modules should be in di package",
                        suggestion = "Move DI configuration to di package"
                    )
                )
            }
        }

        return violations
    }

    private fun findKotlinFiles(projectPath: String): List<File> {
        val projectDir = File(projectPath)
        return projectDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".kt") }
            .toList()
    }

    private fun buildEnhancedDependencyGraph(files: List<File>): Map<String, List<String>> {
        val graph = mutableMapOf<String, MutableList<String>>()
        val packageToFiles = mutableMapOf<String, MutableList<String>>()

        // Build package to files mapping
        files.forEach { file ->
            val lines = file.readLines()
            val packageName = extractPackage(lines)
            if (packageName != null) {
                packageToFiles.getOrPut(packageName) { mutableListOf() }.add(file.absolutePath)
            }
        }

        // Build dependency graph
        files.forEach { file ->
            val imports = extractImports(file.readLines())
            val dependencies = mutableListOf<String>()

            imports.forEach { importStatement ->
                // Find files that could be referenced by this import
                val packageName = importStatement.substringBeforeLast('.', "")
                val className = importStatement.substringAfterLast('.')

                packageToFiles[packageName]?.forEach { dependentFile ->
                    val dependentFileName = File(dependentFile).nameWithoutExtension
                    if (dependentFileName == className && dependentFile != file.absolutePath) {
                        dependencies.add(dependentFile)
                    }
                }
            }

            graph[file.absolutePath] = dependencies
        }

        return graph
    }

    private fun extractPackage(lines: List<String>): String? {
        return lines.firstNotNullOfOrNull { line ->
            val matcher = PACKAGE_PATTERN.matcher(line.trim())
            if (matcher.matches()) matcher.group(1) else null
        }
    }

    enum class ArchitecturalLayer {
        DOMAIN,
        DATA,
        PRESENTATION,
        UI,
        DI,
        UNKNOWN
    }
}