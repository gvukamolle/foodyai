package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validator for Compose component data binding patterns
 */
@Singleton
class ComposeDataBindingValidator @Inject constructor() {
    
    /**
     * Validates data binding in Compose components
     */
    suspend fun validateDataBinding(projectPath: String): DataBindingResult = withContext(Dispatchers.IO) {
        val issues = mutableListOf<DataBindingIssue>()
        
        val composeFiles = findComposeFiles(projectPath)
        
        for (file in composeFiles) {
            val content = file.readText()
            val fileName = file.name
            
            // Validate data binding patterns
            issues.addAll(validateComposeDataBinding(fileName, content))
        }
        
        DataBindingResult(
            issues = issues,
            isValid = issues.isEmpty()
        )
    }
    
    /**
     * Validates state management patterns in Compose components
     */
    suspend fun validateStateManagement(projectPath: String): StateManagementResult = withContext(Dispatchers.IO) {
        val issues = mutableListOf<StateFlowIssue>()
        
        val composeFiles = findComposeFiles(projectPath)
        
        for (file in composeFiles) {
            val content = file.readText()
            val fileName = file.name
            
            // Validate state management patterns
            issues.addAll(validateComposeStateManagement(fileName, content))
        }
        
        StateManagementResult(
            issues = issues,
            isValid = issues.isEmpty()
        )
    }
    
    /**
     * Validates UI components for proper data flow
     */
    suspend fun validateUIComponents(projectPath: String): UIComponentResult = withContext(Dispatchers.IO) {
        val issues = mutableListOf<String>()
        var validComponents = 0
        var totalComponents = 0
        
        val composeFiles = findComposeFiles(projectPath)
        
        for (file in composeFiles) {
            val content = file.readText()
            val fileName = file.name
            
            val componentIssues = validateUIComponentDataFlow(fileName, content)
            issues.addAll(componentIssues)
            
            totalComponents++
            if (componentIssues.isEmpty()) {
                validComponents++
            }
        }
        
        UIComponentResult(
            issues = issues,
            validComponents = validComponents,
            totalComponents = totalComponents
        )
    }
    
    /**
     * Validates data binding patterns in Compose components
     */
    private fun validateComposeDataBinding(fileName: String, content: String): List<DataBindingIssue> {
        val issues = mutableListOf<DataBindingIssue>()
        
        // Check for proper ViewModel injection
        if (content.contains("@Composable") && content.contains("ViewModel") && !content.contains("hiltViewModel()")) {
            issues.add(DataBindingIssue(
                componentFile = fileName,
                bindingProperty = "viewModel",
                issue = "ViewModel should be injected using hiltViewModel() for proper lifecycle management",
                fix = "Use: val viewModel: YourViewModel = hiltViewModel()"
            ))
        }
        
        // Check for direct state access instead of collectAsState
        val directStateAccess = Regex("""(\w+)\.(\w+)\.value""")
        directStateAccess.findAll(content).forEach { match ->
            val propertyName = match.groupValues[2]
            if (content.contains("StateFlow") || content.contains("MutableStateFlow")) {
                issues.add(DataBindingIssue(
                    componentFile = fileName,
                    bindingProperty = propertyName,
                    issue = "StateFlow should be collected using collectAsState() in Compose",
                    fix = "Use: val $propertyName by viewModel.${propertyName}Flow.collectAsState()"
                ))
            }
        }
        
        // Check for missing remember for derived state
        val derivedStatePattern = Regex("""val\s+(\w+)\s*=\s*[^r].*\.(\w+)""")
        derivedStatePattern.findAll(content).forEach { match ->
            val varName = match.groupValues[1]
            if (!content.contains("remember { $varName") && !content.contains("by remember")) {
                issues.add(DataBindingIssue(
                    componentFile = fileName,
                    bindingProperty = varName,
                    issue = "Derived state should be wrapped in remember to avoid recomputation",
                    fix = "Use: val $varName by remember { derivedStateOf { ... } }"
                ))
            }
        }
        
        // Check for proper LaunchedEffect usage
        if (content.contains("LaunchedEffect") && !content.contains("LaunchedEffect(")) {
            issues.add(DataBindingIssue(
                componentFile = fileName,
                bindingProperty = "LaunchedEffect",
                issue = "LaunchedEffect should have proper key dependencies",
                fix = "Use: LaunchedEffect(key1, key2) { ... }"
            ))
        }
        
        // Check for side effects in composition
        val sideEffectPattern = Regex("""viewModel\.(\w+)\(\)""")
        sideEffectPattern.findAll(content).forEach { match ->
            val methodName = match.groupValues[1]
            val beforeMatch = content.substring(0, match.range.first)
            
            // Check if it's inside a proper side effect
            if (!beforeMatch.contains("LaunchedEffect") && 
                !beforeMatch.contains("onClick") && 
                !beforeMatch.contains("onValueChange") &&
                !methodName.startsWith("get") &&
                !methodName.startsWith("is")) {
                issues.add(DataBindingIssue(
                    componentFile = fileName,
                    bindingProperty = methodName,
                    issue = "ViewModel methods should be called in side effects or event handlers",
                    fix = "Wrap in LaunchedEffect, onClick, or other event handler"
                ))
            }
        }
        
        // Check for proper error handling in UI
        if (content.contains("error") && !content.contains("ErrorDialog") && !content.contains("Snackbar")) {
            issues.add(DataBindingIssue(
                componentFile = fileName,
                bindingProperty = "error",
                issue = "Error states should be properly displayed to user",
                fix = "Add error handling UI components like ErrorDialog or Snackbar"
            ))
        }
        
        return issues
    }
    
    /**
     * Validates state management patterns in Compose components
     */
    private fun validateComposeStateManagement(fileName: String, content: String): List<StateFlowIssue> {
        val issues = mutableListOf<StateFlowIssue>()
        
        // Check for proper state hoisting
        val mutableStatePattern = Regex("""var\s+(\w+)\s+by\s+remember\s*\{\s*mutableStateOf""")
        mutableStatePattern.findAll(content).forEach { match ->
            val stateName = match.groupValues[1]
            
            // Check if this state is used by child components
            val childComponentPattern = Regex("""(\w+)\s*\(\s*[^)]*$stateName""")
            if (childComponentPattern.containsMatchIn(content)) {
                issues.add(StateFlowIssue(
                    viewModelClass = fileName,
                    stateProperty = stateName,
                    issue = "State used by child components should be hoisted to parent",
                    recommendation = "Move state to parent component and pass as parameter"
                ))
            }
        }
        
        // Check for proper key usage in LaunchedEffect
        val launchedEffectPattern = Regex("""LaunchedEffect\(([^)]*)\)""")
        launchedEffectPattern.findAll(content).forEach { match ->
            val keys = match.groupValues[1]
            if (keys.isEmpty() || keys == "Unit") {
                issues.add(StateFlowIssue(
                    viewModelClass = fileName,
                    stateProperty = "LaunchedEffect",
                    issue = "LaunchedEffect with Unit key will run on every recomposition",
                    recommendation = "Use specific keys that should trigger the effect"
                ))
            }
        }
        
        // Check for state updates in composition
        val stateUpdateInComposition = Regex("""(\w+)\s*=\s*[^=]""")
        stateUpdateInComposition.findAll(content).forEach { match ->
            val beforeMatch = content.substring(0, match.range.first)
            val afterMatch = content.substring(match.range.last)
            
            // Check if it's inside a Composable but not in a side effect
            if (beforeMatch.contains("@Composable") && 
                !beforeMatch.contains("onClick") &&
                !beforeMatch.contains("onValueChange") &&
                !beforeMatch.contains("LaunchedEffect") &&
                !beforeMatch.contains("DisposableEffect") &&
                !beforeMatch.contains("SideEffect")) {
                
                issues.add(StateFlowIssue(
                    viewModelClass = fileName,
                    stateProperty = match.groupValues[1],
                    issue = "State updates should not happen during composition",
                    recommendation = "Move state updates to event handlers or side effects"
                ))
            }
        }
        
        // Check for missing collectAsState
        if (content.contains("StateFlow") && !content.contains("collectAsState")) {
            issues.add(StateFlowIssue(
                viewModelClass = fileName,
                stateProperty = "StateFlow",
                issue = "StateFlow should be collected using collectAsState() in Compose",
                recommendation = "Use: val state by viewModel.stateFlow.collectAsState()"
            ))
        }
        
        return issues
    }
    
    /**
     * Validates UI component data flow patterns
     */
    private fun validateUIComponentDataFlow(fileName: String, content: String): List<String> {
        val issues = mutableListOf<String>()
        
        // Check for proper parameter validation
        val composablePattern = Regex("""@Composable\s+fun\s+(\w+)\s*\([^)]*\)""")
        composablePattern.findAll(content).forEach { match ->
            val functionName = match.groupValues[1]
            val parameters = match.groupValues[0]
            
            // Check for nullable parameters without default values
            if (parameters.contains("?") && !parameters.contains("= null")) {
                issues.add("$fileName: $functionName has nullable parameters without default values")
            }
            
            // Check for missing Modifier parameter
            if (!parameters.contains("modifier: Modifier") && !parameters.contains("Modifier = Modifier")) {
                issues.add("$fileName: $functionName should accept a Modifier parameter for flexibility")
            }
        }
        
        // Check for hardcoded values that should be parameterized
        val hardcodedColorPattern = Regex("""Color\(0x[A-Fa-f0-9]{8}\)""")
        if (hardcodedColorPattern.containsMatchIn(content)) {
            issues.add("$fileName: Contains hardcoded colors, consider using theme colors")
        }
        
        // Check for hardcoded dimensions
        val hardcodedDimensionPattern = Regex("""\d+\.dp""")
        val dimensionMatches = hardcodedDimensionPattern.findAll(content).count()
        if (dimensionMatches > 5) {
            issues.add("$fileName: Contains many hardcoded dimensions, consider extracting to constants")
        }
        
        // Check for proper preview annotations
        if (content.contains("@Composable") && !content.contains("@Preview")) {
            issues.add("$fileName: Consider adding @Preview functions for better development experience")
        }
        
        // Check for accessibility
        if (content.contains("Button") && !content.contains("contentDescription")) {
            issues.add("$fileName: Interactive elements should have contentDescription for accessibility")
        }
        
        // Check for performance issues
        if (content.contains("LazyColumn") && content.contains("items(") && !content.contains("key =")) {
            issues.add("$fileName: LazyColumn items should have stable keys for better performance")
        }
        
        return issues
    }
    
    /**
     * Find all Compose files in the project
     */
    private fun findComposeFiles(projectPath: String): List<File> {
        val pagesDir = File("$projectPath/app/src/main/java/com/example/calorietracker/pages")
        val componentsDir = File("$projectPath/app/src/main/java/com/example/calorietracker/components")
        val uiDir = File("$projectPath/app/src/main/java/com/example/calorietracker/ui")
        
        val files = mutableListOf<File>()
        
        listOf(pagesDir, componentsDir, uiDir).forEach { dir ->
            if (dir.exists()) {
                dir.walkTopDown()
                    .filter { it.isFile && it.name.endsWith(".kt") }
                    .forEach { file ->
                        val content = file.readText()
                        if (content.contains("@Composable")) {
                            files.add(file)
                        }
                    }
            }
        }
        
        return files
    }
}