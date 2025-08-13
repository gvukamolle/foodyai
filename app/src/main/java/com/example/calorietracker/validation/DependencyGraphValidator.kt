package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.DependencyGraphResult
import java.io.File
import javax.inject.Singleton

/**
 * Validator for dependency graph completeness and circular dependencies
 */
@Singleton
class DependencyGraphValidator {
    
    private data class DependencyNode(
        val className: String,
        val dependencies: MutableSet<String> = mutableSetOf(),
        var providedBy: String? = null
    )
    
    /**
     * Validates the complete dependency graph
     */
    fun validateDependencyGraph(): DependencyGraphResult {
        val dependencyGraph = buildDependencyGraph()
        val circularDependencies = detectCircularDependencies()
        val missingDependencies = findMissingDependencies(dependencyGraph)
        
        return DependencyGraphResult(
            circularDependencies = circularDependencies,
            missingDependencies = missingDependencies,
            isValid = circularDependencies.isEmpty() && missingDependencies.isEmpty()
        )
    }
    
    /**
     * Detects circular dependencies in the DI graph
     */
    fun detectCircularDependencies(): List<String> {
        val dependencyGraph = buildDependencyGraph()
        val circularDependencies = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        
        for (node in dependencyGraph.keys) {
            if (node !in visited) {
                val cycle = detectCycleFromNode(node, dependencyGraph, visited, recursionStack, mutableListOf())
                if (cycle.isNotEmpty()) {
                    circularDependencies.add("Circular dependency detected: ${cycle.joinToString(" -> ")}")
                }
            }
        }
        
        return circularDependencies
    }
    
    private fun buildDependencyGraph(): Map<String, DependencyNode> {
        val graph = mutableMapOf<String, DependencyNode>()
        val diModuleFiles = getDIModuleFiles()
        
        for (moduleFile in diModuleFiles) {
            val moduleContent = moduleFile.readText()
            parseDependenciesFromModule(moduleContent, graph)
        }
        
        return graph
    }
    
    private fun parseDependenciesFromModule(moduleContent: String, graph: MutableMap<String, DependencyNode>) {
        // Parse @Provides methods
        val providesPattern = "@Provides\\s+(?:@\\w+\\s+)*fun\\s+(\\w+)\\s*\\(([^)]*)\\)\\s*:\\s*(\\w+)".toRegex(RegexOption.MULTILINE)
        val providesMatches = providesPattern.findAll(moduleContent)
        
        for (match in providesMatches) {
            val methodName = match.groupValues[1]
            val parameters = match.groupValues[2]
            val returnType = match.groupValues[3]
            
            val node = graph.getOrPut(returnType) { DependencyNode(returnType) }
            node.providedBy = methodName
            
            // Parse dependencies from parameters
            if (parameters.isNotBlank()) {
                val paramPattern = "(\\w+)\\s*:\\s*(\\w+)".toRegex()
                val paramMatches = paramPattern.findAll(parameters)
                
                for (paramMatch in paramMatches) {
                    val paramType = paramMatch.groupValues[2]
                    node.dependencies.add(paramType)
                    
                    // Ensure dependency node exists
                    graph.getOrPut(paramType) { DependencyNode(paramType) }
                }
            }
        }
        
        // Parse @Binds methods
        val bindsPattern = "@Binds\\s+(?:@\\w+\\s+)*abstract\\s+fun\\s+(\\w+)\\s*\\(([^)]+)\\)\\s*:\\s*(\\w+)".toRegex(RegexOption.MULTILINE)
        val bindsMatches = bindsPattern.findAll(moduleContent)
        
        for (match in bindsMatches) {
            val methodName = match.groupValues[1]
            val parameter = match.groupValues[2].trim()
            val returnType = match.groupValues[3]
            
            // Extract implementation type from parameter
            val paramTypePattern = ":\\s*(\\w+)".toRegex()
            val paramTypeMatch = paramTypePattern.find(parameter)
            
            if (paramTypeMatch != null) {
                val implementationType = paramTypeMatch.groupValues[1]
                
                val interfaceNode = graph.getOrPut(returnType) { DependencyNode(returnType) }
                interfaceNode.providedBy = methodName
                interfaceNode.dependencies.add(implementationType)
                
                // Ensure implementation node exists
                graph.getOrPut(implementationType) { DependencyNode(implementationType) }
            }
        }
    }
    
    private fun detectCycleFromNode(
        node: String,
        graph: Map<String, DependencyNode>,
        visited: MutableSet<String>,
        recursionStack: MutableSet<String>,
        currentPath: MutableList<String>
    ): List<String> {
        visited.add(node)
        recursionStack.add(node)
        currentPath.add(node)
        
        val dependencies = graph[node]?.dependencies ?: emptySet()
        
        for (dependency in dependencies) {
            if (dependency in recursionStack) {
                // Found a cycle
                val cycleStart = currentPath.indexOf(dependency)
                return currentPath.subList(cycleStart, currentPath.size) + dependency
            }
            
            if (dependency !in visited) {
                val cycle = detectCycleFromNode(dependency, graph, visited, recursionStack, currentPath)
                if (cycle.isNotEmpty()) {
                    return cycle
                }
            }
        }
        
        recursionStack.remove(node)
        currentPath.removeAt(currentPath.size - 1)
        return emptyList()
    }
    
    private fun findMissingDependencies(graph: Map<String, DependencyNode>): List<String> {
        val missingDependencies = mutableListOf<String>()
        
        for ((className, node) in graph) {
            for (dependency in node.dependencies) {
                val dependencyNode = graph[dependency]
                if (dependencyNode?.providedBy == null) {
                    // Check if it's a system class or external dependency
                    if (!isSystemOrExternalClass(dependency)) {
                        missingDependencies.add("$className depends on $dependency but no provider found")
                    }
                }
            }
        }
        
        return missingDependencies
    }
    
    private fun isSystemOrExternalClass(className: String): Boolean {
        val systemPrefixes = listOf(
            "String", "Int", "Long", "Boolean", "Float", "Double",
            "List", "Set", "Map", "Array",
            "Context", "Application", "Activity", "Fragment",
            "FirebaseAuth", "FirebaseFirestore", "OkHttpClient", "Retrofit",
            "Room", "Database", "Dao"
        )
        
        return systemPrefixes.any { className.startsWith(it) } ||
               className.startsWith("java.") ||
               className.startsWith("kotlin.") ||
               className.startsWith("android.") ||
               className.startsWith("androidx.") ||
               className.startsWith("com.google.") ||
               className.startsWith("retrofit2.") ||
               className.startsWith("okhttp3.")
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