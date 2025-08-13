package com.example.calorietracker.validation

import com.example.calorietracker.validation.interfaces.UIDataFlowValidator
import com.example.calorietracker.validation.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validator for ViewModel state management patterns
 */
@Singleton
class ViewModelStateValidator @Inject constructor() {
    
    /**
     * Validates ViewModels for proper StateFlow usage and data flow patterns
     */
    suspend fun validateViewModels(projectPath: String): ViewModelValidationResult = withContext(Dispatchers.IO) {
        val stateFlowIssues = mutableListOf<StateFlowIssue>()
        val dataBindingIssues = mutableListOf<DataBindingIssue>()
        val lifecycleIssues = mutableListOf<String>()
        
        val viewModelFiles = findViewModelFiles(projectPath)
        
        for (file in viewModelFiles) {
            val content = file.readText()
            val className = extractClassName(file.name)
            
            // Validate StateFlow usage
            stateFlowIssues.addAll(validateStateFlowUsage(className, content))
            
            // Validate data binding patterns
            dataBindingIssues.addAll(validateDataBindingPatterns(className, content))
            
            // Validate lifecycle handling
            lifecycleIssues.addAll(validateLifecycleHandling(className, content))
        }
        
        ViewModelValidationResult(
            stateFlowIssues = stateFlowIssues,
            dataBindingIssues = dataBindingIssues,
            lifecycleIssues = lifecycleIssues
        )
    }
    
    /**
     * Validates StateFlow usage patterns in ViewModels
     */
    private fun validateStateFlowUsage(className: String, content: String): List<StateFlowIssue> {
        val issues = mutableListOf<StateFlowIssue>()
        
        // Check for proper StateFlow declaration patterns
        val stateFlowPattern = Regex("""private\s+val\s+_(\w+)\s*=\s*MutableStateFlow""")
        val publicStateFlowPattern = Regex("""val\s+(\w+):\s*StateFlow""")
        
        val privateStateFlows = stateFlowPattern.findAll(content).map { it.groupValues[1] }.toSet()
        val publicStateFlows = publicStateFlowPattern.findAll(content).map { it.groupValues[1] }.toSet()
        
        // Check if private StateFlows have corresponding public exposures
        for (privateFlow in privateStateFlows) {
            val expectedPublicName = if (privateFlow.startsWith("_")) privateFlow.substring(1) else "${privateFlow}Flow"
            if (!publicStateFlows.contains(expectedPublicName) && !content.contains("$expectedPublicName: StateFlow")) {
                issues.add(StateFlowIssue(
                    viewModelClass = className,
                    stateProperty = "_$privateFlow",
                    issue = "Private MutableStateFlow should have corresponding public StateFlow exposure",
                    recommendation = "Add: val $expectedPublicName: StateFlow<T> = _$privateFlow.asStateFlow()"
                ))
            }
        }
        
        // Check for direct MutableStateFlow exposure (anti-pattern)
        val directMutableExposure = Regex("""val\s+(\w+):\s*MutableStateFlow""")
        directMutableExposure.findAll(content).forEach { match ->
            issues.add(StateFlowIssue(
                viewModelClass = className,
                stateProperty = match.groupValues[1],
                issue = "MutableStateFlow should not be directly exposed publicly",
                recommendation = "Make it private and expose as StateFlow using asStateFlow()"
            ))
        }
        
        // Check for proper initialization
        val uninitializedStateFlow = Regex("""MutableStateFlow\(\s*\)""")
        if (uninitializedStateFlow.containsMatchIn(content)) {
            issues.add(StateFlowIssue(
                viewModelClass = className,
                stateProperty = "unspecified",
                issue = "MutableStateFlow should be initialized with a default value",
                recommendation = "Provide initial state: MutableStateFlow(InitialState())"
            ))
        }
        
        // Check for state updates outside viewModelScope
        val stateUpdatePattern = Regex("""_(\w+)\.value\s*=""")
        stateUpdatePattern.findAll(content).forEach { match ->
            val propertyName = match.groupValues[1]
            val beforeMatch = content.substring(0, match.range.first)
            val lastViewModelScopeIndex = beforeMatch.lastIndexOf("viewModelScope.launch")
            val lastFunctionIndex = beforeMatch.lastIndexOf("fun ")
            
            if (lastViewModelScopeIndex == -1 || lastFunctionIndex > lastViewModelScopeIndex) {
                issues.add(StateFlowIssue(
                    viewModelClass = className,
                    stateProperty = "_$propertyName",
                    issue = "State updates should be performed within viewModelScope",
                    recommendation = "Wrap state update in viewModelScope.launch { ... }"
                ))
            }
        }
        
        return issues
    }
    
    /**
     * Validates data binding patterns between ViewModels and UI
     */
    private fun validateDataBindingPatterns(className: String, content: String): List<DataBindingIssue> {
        val issues = mutableListOf<DataBindingIssue>()
        
        // Check for mutable state usage (should prefer StateFlow)
        val mutableStatePattern = Regex("""var\s+(\w+)\s+by\s+mutableStateOf""")
        mutableStatePattern.findAll(content).forEach { match ->
            val propertyName = match.groupValues[1]
            issues.add(DataBindingIssue(
                componentFile = className,
                bindingProperty = propertyName,
                issue = "Consider using StateFlow instead of mutableStateOf for better state management",
                fix = "Replace with private val _$propertyName = MutableStateFlow() and expose as StateFlow"
            ))
        }
        
        // Check for proper error handling in state updates
        if (content.contains("_uiState.value =") && !content.contains("try") && !content.contains("catch")) {
            issues.add(DataBindingIssue(
                componentFile = className,
                bindingProperty = "uiState",
                issue = "State updates should include proper error handling",
                fix = "Wrap state updates in try-catch blocks or use Result wrapper"
            ))
        }
        
        // Check for loading state management
        if (content.contains("isLoading") && !content.contains("updateUiState")) {
            issues.add(DataBindingIssue(
                componentFile = className,
                bindingProperty = "isLoading",
                issue = "Loading states should be managed through centralized state update functions",
                fix = "Use updateUiState { copy(isLoading = true/false) } pattern"
            ))
        }
        
        return issues
    }
    
    /**
     * Validates lifecycle handling in ViewModels
     */
    private fun validateLifecycleHandling(className: String, content: String): List<String> {
        val issues = mutableListOf<String>()
        
        // Check for proper viewModelScope usage
        if (content.contains("launch {") && !content.contains("viewModelScope.launch")) {
            issues.add("$className: Use viewModelScope.launch instead of plain launch for coroutines")
        }
        
        // Check for proper cleanup in onCleared
        if (content.contains("MutableStateFlow") && !content.contains("onCleared")) {
            issues.add("$className: Consider implementing onCleared() for proper resource cleanup")
        }
        
        // Check for memory leaks with context references
        if (content.contains("Context") && !content.contains("@ApplicationContext")) {
            issues.add("$className: Use @ApplicationContext to avoid memory leaks with Context references")
        }
        
        // Check for proper initialization
        if (!content.contains("init {") && content.contains("MutableStateFlow")) {
            issues.add("$className: Consider initializing state in init block for better predictability")
        }
        
        return issues
    }
    
    /**
     * Find all ViewModel files in the project
     */
    private fun findViewModelFiles(projectPath: String): List<File> {
        val viewModelDir = File("$projectPath/app/src/main/java/com/example/calorietracker/presentation/viewmodels")
        return if (viewModelDir.exists()) {
            viewModelDir.listFiles { file -> 
                file.isFile && file.name.endsWith("ViewModel.kt")
            }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * Extract class name from file name
     */
    private fun extractClassName(fileName: String): String {
        return fileName.removeSuffix(".kt")
    }
}