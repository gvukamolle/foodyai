package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.BindingIssue
import java.io.File
import javax.inject.Singleton

/**
 * Validator for dependency binding validation between interfaces and implementations
 */
@Singleton
class DependencyBindingValidator {
    
    private data class InterfaceBinding(
        val interfaceName: String,
        val implementationName: String,
        val bindingMethod: String,
        val moduleName: String
    )
    
    private data class RepositoryInfo(
        val interfaceName: String,
        val implementationName: String,
        val interfaceFile: String,
        val implementationFile: String
    )
    
    /**
     * Validates RepositoryModule interface bindings
     */
    fun validateRepositoryBindings(): List<BindingIssue> {
        val issues = mutableListOf<BindingIssue>()
        
        // Get repository interfaces and implementations
        val repositoryInterfaces = getRepositoryInterfaces()
        val repositoryImplementations = getRepositoryImplementations()
        val domainModuleBindings = getDomainModuleBindings()
        
        // Check that all interfaces have implementations
        for (repoInterface in repositoryInterfaces) {
            val expectedImplName = "${repoInterface.removeSuffix("Repository")}RepositoryImpl"
            val hasImplementation = repositoryImplementations.any { it.contains(expectedImplName) }
            
            if (!hasImplementation) {
                issues.add(BindingIssue(
                    interfaceName = repoInterface,
                    implementationName = null,
                    issue = "Repository interface $repoInterface has no corresponding implementation",
                    fix = "Create implementation class $expectedImplName"
                ))
            }
        }
        
        // Check that all implementations have bindings
        for (repoImpl in repositoryImplementations) {
            val implClassName = File(repoImpl).nameWithoutExtension
            val hasBinding = domainModuleBindings.any { it.implementationName.contains(implClassName) }
            
            if (!hasBinding) {
                issues.add(BindingIssue(
                    interfaceName = "Unknown",
                    implementationName = implClassName,
                    issue = "Repository implementation $implClassName is not bound in DomainModule",
                    fix = "Add @Binds method in DomainModule to bind this implementation"
                ))
            }
        }
        
        // Validate binding correctness in DomainModule
        issues.addAll(validateDomainModuleBindings(domainModuleBindings))
        
        return issues
    }
    
    /**
     * Validates UseCaseModule dependency injection
     */
    fun validateUseCaseBindings(): List<BindingIssue> {
        val issues = mutableListOf<BindingIssue>()
        
        val useCaseFiles = getUseCaseFiles()
        val useCaseModuleProviders = getUseCaseModuleProviders()
        
        // Check that all use cases have providers
        for (useCaseFile in useCaseFiles) {
            val useCaseClassName = File(useCaseFile).nameWithoutExtension
            val hasProvider = useCaseModuleProviders.any { it.contains(useCaseClassName) }
            
            if (!hasProvider) {
                issues.add(BindingIssue(
                    interfaceName = useCaseClassName,
                    implementationName = null,
                    issue = "Use case $useCaseClassName has no provider in UseCaseModule",
                    fix = "Add @Provides method for $useCaseClassName in UseCaseModule"
                ))
            }
        }
        
        // Validate use case dependencies
        issues.addAll(validateUseCaseDependencies())
        
        return issues
    }
    
    /**
     * Analyzes dependency graph completeness
     */
    fun analyzeDependencyGraphCompleteness(): List<BindingIssue> {
        val issues = mutableListOf<BindingIssue>()
        
        // Check repository layer completeness
        issues.addAll(validateRepositoryLayerCompleteness())
        
        // Check use case layer completeness
        issues.addAll(validateUseCaseLayerCompleteness())
        
        // Check mapper dependencies
        issues.addAll(validateMapperDependencies())
        
        return issues
    }
    
    private fun getRepositoryInterfaces(): List<String> {
        val repositoryDir = File("app/src/main/java/com/example/calorietracker/domain/repositories")
        return if (repositoryDir.exists()) {
            repositoryDir.listFiles { file ->
                file.isFile && file.extension == "kt" && !file.name.contains(".gitkeep")
            }?.map { it.nameWithoutExtension } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    private fun getRepositoryImplementations(): List<String> {
        val repositoryImplDir = File("app/src/main/java/com/example/calorietracker/data/repositories")
        return if (repositoryImplDir.exists()) {
            repositoryImplDir.listFiles { file ->
                file.isFile && file.extension == "kt" && file.name.endsWith("Impl.kt")
            }?.map { it.absolutePath } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    private fun getDomainModuleBindings(): List<InterfaceBinding> {
        val domainModuleFile = File("app/src/main/java/com/example/calorietracker/di/DomainModule.kt")
        if (!domainModuleFile.exists()) return emptyList()
        
        val content = domainModuleFile.readText()
        val bindings = mutableListOf<InterfaceBinding>()
        
        val bindsPattern = "@Binds\\s+(?:@\\w+\\s+)*abstract\\s+fun\\s+(\\w+)\\s*\\(([^)]+)\\)\\s*:\\s*(\\w+)".toRegex(RegexOption.MULTILINE)
        val matches = bindsPattern.findAll(content)
        
        for (match in matches) {
            val methodName = match.groupValues[1]
            val parameter = match.groupValues[2].trim()
            val returnType = match.groupValues[3]
            
            // Extract implementation type from parameter
            val paramTypePattern = ":\\s*(\\w+)".toRegex()
            val paramTypeMatch = paramTypePattern.find(parameter)
            
            if (paramTypeMatch != null) {
                val implementationType = paramTypeMatch.groupValues[1]
                bindings.add(InterfaceBinding(
                    interfaceName = returnType,
                    implementationName = implementationType,
                    bindingMethod = methodName,
                    moduleName = "DomainModule"
                ))
            }
        }
        
        return bindings
    }
    
    private fun validateDomainModuleBindings(bindings: List<InterfaceBinding>): List<BindingIssue> {
        val issues = mutableListOf<BindingIssue>()
        
        for (binding in bindings) {
            // Check if interface exists
            val interfaceFile = File("app/src/main/java/com/example/calorietracker/domain/repositories/${binding.interfaceName}.kt")
            if (!interfaceFile.exists()) {
                issues.add(BindingIssue(
                    interfaceName = binding.interfaceName,
                    implementationName = binding.implementationName,
                    issue = "Bound interface ${binding.interfaceName} does not exist",
                    fix = "Create interface file or fix binding in ${binding.moduleName}"
                ))
            }
            
            // Check if implementation exists
            val implFile = File("app/src/main/java/com/example/calorietracker/data/repositories/${binding.implementationName}.kt")
            if (!implFile.exists()) {
                issues.add(BindingIssue(
                    interfaceName = binding.interfaceName,
                    implementationName = binding.implementationName,
                    issue = "Bound implementation ${binding.implementationName} does not exist",
                    fix = "Create implementation file or fix binding in ${binding.moduleName}"
                ))
            } else {
                // Check if implementation actually implements the interface
                val implContent = implFile.readText()
                if (!implContent.contains(": ${binding.interfaceName}")) {
                    issues.add(BindingIssue(
                        interfaceName = binding.interfaceName,
                        implementationName = binding.implementationName,
                        issue = "Implementation ${binding.implementationName} does not implement ${binding.interfaceName}",
                        fix = "Make ${binding.implementationName} implement ${binding.interfaceName}"
                    ))
                }
            }
        }
        
        return issues
    }
    
    private fun getUseCaseFiles(): List<String> {
        val useCaseDir = File("app/src/main/java/com/example/calorietracker/domain/usecases")
        return if (useCaseDir.exists()) {
            useCaseDir.listFiles { file ->
                file.isFile && file.extension == "kt" && !file.name.contains(".gitkeep") && !file.name.contains("base")
            }?.map { it.absolutePath } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    private fun getUseCaseModuleProviders(): List<String> {
        val useCaseModuleFile = File("app/src/main/java/com/example/calorietracker/di/UseCaseModule.kt")
        if (!useCaseModuleFile.exists()) return emptyList()
        
        val content = useCaseModuleFile.readText()
        val providers = mutableListOf<String>()
        
        val providesPattern = "@Provides\\s+(?:@\\w+\\s+)*fun\\s+(\\w+)\\s*\\([^)]*\\)\\s*:\\s*(\\w+)".toRegex(RegexOption.MULTILINE)
        val matches = providesPattern.findAll(content)
        
        for (match in matches) {
            val returnType = match.groupValues[2]
            providers.add(returnType)
        }
        
        return providers
    }
    
    private fun validateUseCaseDependencies(): List<BindingIssue> {
        val issues = mutableListOf<BindingIssue>()
        val useCaseModuleFile = File("app/src/main/java/com/example/calorietracker/di/UseCaseModule.kt")
        
        if (!useCaseModuleFile.exists()) {
            issues.add(BindingIssue(
                interfaceName = "UseCaseModule",
                implementationName = null,
                issue = "UseCaseModule.kt file not found",
                fix = "Create UseCaseModule.kt file"
            ))
            return issues
        }
        
        val content = useCaseModuleFile.readText()
        
        // Check for proper repository dependencies
        val repositoryInterfaces = getRepositoryInterfaces()
        for (repoInterface in repositoryInterfaces) {
            if (!content.contains(repoInterface)) {
                // This might be okay if the repository is not used by any use case
                // But we should check if there are use cases that should use it
                val useCaseFiles = getUseCaseFiles()
                val shouldUseRepo = useCaseFiles.any { useCaseFile ->
                    val useCaseContent = File(useCaseFile).readText()
                    useCaseContent.contains(repoInterface)
                }
                
                if (shouldUseRepo) {
                    issues.add(BindingIssue(
                        interfaceName = repoInterface,
                        implementationName = null,
                        issue = "Repository $repoInterface is used by use cases but not injected in UseCaseModule",
                        fix = "Add $repoInterface parameter to relevant use case providers"
                    ))
                }
            }
        }
        
        return issues
    }
    
    private fun validateRepositoryLayerCompleteness(): List<BindingIssue> {
        val issues = mutableListOf<BindingIssue>()
        
        // Check if all expected repositories exist
        val expectedRepositories = listOf("FoodRepository", "UserRepository", "NutritionRepository", "ChatRepository")
        
        for (expectedRepo in expectedRepositories) {
            val interfaceFile = File("app/src/main/java/com/example/calorietracker/domain/repositories/$expectedRepo.kt")
            val implFile = File("app/src/main/java/com/example/calorietracker/data/repositories/${expectedRepo}Impl.kt")
            
            if (!interfaceFile.exists()) {
                issues.add(BindingIssue(
                    interfaceName = expectedRepo,
                    implementationName = null,
                    issue = "Expected repository interface $expectedRepo not found",
                    fix = "Create $expectedRepo interface in domain/repositories"
                ))
            }
            
            if (!implFile.exists()) {
                issues.add(BindingIssue(
                    interfaceName = expectedRepo,
                    implementationName = "${expectedRepo}Impl",
                    issue = "Expected repository implementation ${expectedRepo}Impl not found",
                    fix = "Create ${expectedRepo}Impl class in data/repositories"
                ))
            }
        }
        
        return issues
    }
    
    private fun validateUseCaseLayerCompleteness(): List<BindingIssue> {
        val issues = mutableListOf<BindingIssue>()
        
        // Check if use cases have proper constructor injection
        val useCaseFiles = getUseCaseFiles()
        
        for (useCaseFile in useCaseFiles) {
            val content = File(useCaseFile).readText()
            val className = File(useCaseFile).nameWithoutExtension
            
            // Check if use case has @Inject constructor
            if (!content.contains("@Inject") && !content.contains("constructor")) {
                issues.add(BindingIssue(
                    interfaceName = className,
                    implementationName = null,
                    issue = "Use case $className missing @Inject constructor annotation",
                    fix = "Add @Inject annotation to constructor or make it injectable"
                ))
            }
        }
        
        return issues
    }
    
    private fun validateMapperDependencies(): List<BindingIssue> {
        val issues = mutableListOf<BindingIssue>()
        
        // Check MapperModule
        val mapperModuleFile = File("app/src/main/java/com/example/calorietracker/di/MapperModule.kt")
        if (!mapperModuleFile.exists()) {
            issues.add(BindingIssue(
                interfaceName = "MapperModule",
                implementationName = null,
                issue = "MapperModule.kt not found",
                fix = "Create MapperModule.kt for mapper dependencies"
            ))
            return issues
        }
        
        val content = mapperModuleFile.readText()
        val expectedMappers = listOf("FoodMapper", "UserMapper", "NutritionMapper", "ChatMapper")
        
        for (mapper in expectedMappers) {
            if (!content.contains("provide$mapper")) {
                issues.add(BindingIssue(
                    interfaceName = mapper,
                    implementationName = null,
                    issue = "Mapper $mapper not provided in MapperModule",
                    fix = "Add provider method for $mapper in MapperModule"
                ))
            }
        }
        
        return issues
    }
}