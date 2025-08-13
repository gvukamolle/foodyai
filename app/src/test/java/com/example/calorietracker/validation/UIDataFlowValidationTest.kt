package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.io.IOException

/**
 * Tests for UI data flow validation functionality
 */
class UIDataFlowValidationTest {
    
    private lateinit var viewModelValidator: ViewModelStateValidator
    private lateinit var composeValidator: ComposeDataBindingValidator
    private lateinit var testProjectPath: String
    
    @Before
    fun setup() {
        viewModelValidator = ViewModelStateValidator()
        composeValidator = ComposeDataBindingValidator()
        testProjectPath = createTestProject()
    }
    
    @Test
    fun `validateViewModels should detect StateFlow issues`() = runTest {
        // Create test ViewModel with issues
        createTestViewModel(
            "TestViewModel.kt",
            """
            package com.example.test
            
            import androidx.lifecycle.ViewModel
            import kotlinx.coroutines.flow.MutableStateFlow
            import kotlinx.coroutines.flow.StateFlow
            
            class TestViewModel : ViewModel() {
                // Issue: Direct MutableStateFlow exposure
                val directState: MutableStateFlow<String> = MutableStateFlow("")
                
                // Issue: Private StateFlow without public exposure
                private val _hiddenState = MutableStateFlow("")
                
                // Issue: Uninitialized StateFlow
                private val _uninitializedState = MutableStateFlow()
                
                // Issue: State update outside viewModelScope
                fun updateState() {
                    _hiddenState.value = "updated"
                }
            }
            """.trimIndent()
        )
        
        val result = viewModelValidator.validateViewModels(testProjectPath)
        
        // Verify issues are detected
        assertTrue("Should detect direct MutableStateFlow exposure", 
            result.stateFlowIssues.any { it.issue.contains("should not be directly exposed") })
        
        assertTrue("Should detect missing public StateFlow", 
            result.stateFlowIssues.any { it.issue.contains("should have corresponding public StateFlow") })
        
        assertTrue("Should detect uninitialized StateFlow", 
            result.stateFlowIssues.any { it.issue.contains("should be initialized with a default value") })
        
        assertTrue("Should detect state updates outside viewModelScope", 
            result.stateFlowIssues.any { it.issue.contains("should be performed within viewModelScope") })
    }
    
    @Test
    fun `validateViewModels should pass for correct StateFlow usage`() = runTest {
        createTestViewModel(
            "GoodViewModel.kt",
            """
            package com.example.test
            
            import androidx.lifecycle.ViewModel
            import androidx.lifecycle.viewModelScope
            import kotlinx.coroutines.flow.MutableStateFlow
            import kotlinx.coroutines.flow.StateFlow
            import kotlinx.coroutines.flow.asStateFlow
            import kotlinx.coroutines.launch
            
            class GoodViewModel : ViewModel() {
                private val _uiState = MutableStateFlow(UiState())
                val uiState: StateFlow<UiState> = _uiState.asStateFlow()
                
                fun updateState() {
                    viewModelScope.launch {
                        _uiState.value = UiState(loading = true)
                    }
                }
            }
            
            data class UiState(val loading: Boolean = false)
            """.trimIndent()
        )
        
        val result = viewModelValidator.validateViewModels(testProjectPath)
        
        // Should have no StateFlow issues for this good example
        assertTrue("Good ViewModel should have no StateFlow issues", 
            result.stateFlowIssues.isEmpty())
    }
    
    @Test
    fun `validateDataBinding should detect Compose issues`() = runTest {
        createTestComposeFile(
            "TestScreen.kt",
            """
            package com.example.test
            
            import androidx.compose.runtime.*
            import androidx.compose.material3.*
            
            @Composable
            fun TestScreen(viewModel: TestViewModel) {
                // Issue: Direct state access instead of collectAsState
                val state = viewModel.uiState.value
                
                // Issue: Side effect in composition
                viewModel.updateData()
                
                // Issue: Missing remember for derived state
                val derivedValue = state.someProperty.uppercase()
                
                // Issue: LaunchedEffect without proper keys
                LaunchedEffect(Unit) {
                    viewModel.loadData()
                }
                
                Text(text = derivedValue)
            }
            """.trimIndent()
        )
        
        val result = composeValidator.validateDataBinding(testProjectPath)
        
        assertFalse("Should detect data binding issues", result.isValid)
        assertTrue("Should detect direct state access", 
            result.issues.any { it.issue.contains("collectAsState()") })
        assertTrue("Should detect side effects in composition", 
            result.issues.any { it.issue.contains("side effects or event handlers") })
        assertTrue("Should detect missing remember", 
            result.issues.any { it.issue.contains("wrapped in remember") })
    }
    
    @Test
    fun `validateStateManagement should detect state hoisting issues`() = runTest {
        createTestComposeFile(
            "StateTestScreen.kt",
            """
            package com.example.test
            
            import androidx.compose.runtime.*
            import androidx.compose.material3.*
            
            @Composable
            fun StateTestScreen() {
                // Issue: State used by child should be hoisted
                var localState by remember { mutableStateOf("") }
                
                ChildComponent(value = localState)
                
                // Issue: LaunchedEffect with Unit key
                LaunchedEffect(Unit) {
                    // Some effect
                }
                
                // Issue: State update in composition
                localState = "updated"
            }
            
            @Composable
            fun ChildComponent(value: String) {
                Text(text = value)
            }
            """.trimIndent()
        )
        
        val result = composeValidator.validateStateManagement(testProjectPath)
        
        assertFalse("Should detect state management issues", result.isValid)
        assertTrue("Should detect state hoisting issue", 
            result.issues.any { it.issue.contains("should be hoisted") })
        assertTrue("Should detect Unit key in LaunchedEffect", 
            result.issues.any { it.issue.contains("Unit key will run on every recomposition") })
        assertTrue("Should detect state update in composition", 
            result.issues.any { it.issue.contains("should not happen during composition") })
    }
    
    @Test
    fun `validateUIComponents should detect component issues`() = runTest {
        createTestComposeFile(
            "ComponentTestScreen.kt",
            """
            package com.example.test
            
            import androidx.compose.runtime.*
            import androidx.compose.material3.*
            import androidx.compose.ui.graphics.Color
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            import androidx.compose.ui.unit.dp
            
            // Issue: Missing Modifier parameter
            @Composable
            fun ComponentTestScreen() {
                // Issue: Hardcoded colors
                val hardcodedColor = Color(0xFF123456)
                
                // Issue: Many hardcoded dimensions
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .height(200.dp)
                        .width(300.dp)
                ) {
                    // Issue: Button without contentDescription
                    Button(onClick = {}) {
                        Text("Click me")
                    }
                    
                    // Issue: LazyColumn without keys
                    LazyColumn {
                        items(listOf("a", "b", "c")) { item ->
                            Text(item)
                        }
                    }
                }
            }
            """.trimIndent()
        )
        
        val result = composeValidator.validateUIComponents(testProjectPath)
        
        assertTrue("Should detect component issues", result.issues.isNotEmpty())
        assertTrue("Should detect missing Modifier parameter", 
            result.issues.any { it.contains("should accept a Modifier parameter") })
        assertTrue("Should detect hardcoded colors", 
            result.issues.any { it.contains("hardcoded colors") })
        assertTrue("Should detect hardcoded dimensions", 
            result.issues.any { it.contains("hardcoded dimensions") })
        assertTrue("Should detect missing contentDescription", 
            result.issues.any { it.contains("contentDescription for accessibility") })
        assertTrue("Should detect missing LazyColumn keys", 
            result.issues.any { it.contains("should have stable keys") })
    }
    
    @Test
    fun `validateUIComponents should pass for good component`() = runTest {
        createTestComposeFile(
            "GoodComponentScreen.kt",
            """
            package com.example.test
            
            import androidx.compose.runtime.*
            import androidx.compose.material3.*
            import androidx.compose.ui.Modifier
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            import androidx.compose.ui.tooling.preview.Preview
            
            @Composable
            fun GoodComponentScreen(
                items: List<String>,
                modifier: Modifier = Modifier
            ) {
                LazyColumn(modifier = modifier) {
                    items(items, key = { it }) { item ->
                        Button(
                            onClick = {},
                            contentDescription = "Button for ${'$'}item"
                        ) {
                            Text(item)
                        }
                    }
                }
            }
            
            @Preview
            @Composable
            fun GoodComponentScreenPreview() {
                GoodComponentScreen(items = listOf("Item 1", "Item 2"))
            }
            """.trimIndent()
        )
        
        val result = composeValidator.validateUIComponents(testProjectPath)
        
        // Should have minimal issues for this good example
        assertEquals("Good component should have 1 valid component", 1, result.validComponents)
        assertEquals("Good component should have 1 total component", 1, result.totalComponents)
    }
    
    @Test
    fun `integration test should validate complete UI data flow`() = runTest {
        // Create a complete example with ViewModel and Compose components
        createTestViewModel(
            "IntegrationViewModel.kt",
            """
            package com.example.test
            
            import androidx.lifecycle.ViewModel
            import androidx.lifecycle.viewModelScope
            import kotlinx.coroutines.flow.MutableStateFlow
            import kotlinx.coroutines.flow.StateFlow
            import kotlinx.coroutines.flow.asStateFlow
            import kotlinx.coroutines.launch
            
            class IntegrationViewModel : ViewModel() {
                private val _uiState = MutableStateFlow(IntegrationUiState())
                val uiState: StateFlow<IntegrationUiState> = _uiState.asStateFlow()
                
                fun loadData() {
                    viewModelScope.launch {
                        _uiState.value = _uiState.value.copy(loading = true)
                        // Simulate data loading
                        _uiState.value = _uiState.value.copy(loading = false, data = "Loaded")
                    }
                }
            }
            
            data class IntegrationUiState(
                val loading: Boolean = false,
                val data: String = "",
                val error: String? = null
            )
            """.trimIndent()
        )
        
        createTestComposeFile(
            "IntegrationScreen.kt",
            """
            package com.example.test
            
            import androidx.compose.runtime.*
            import androidx.compose.material3.*
            import androidx.compose.ui.Modifier
            import androidx.hilt.navigation.compose.hiltViewModel
            
            @Composable
            fun IntegrationScreen(
                modifier: Modifier = Modifier,
                viewModel: IntegrationViewModel = hiltViewModel()
            ) {
                val uiState by viewModel.uiState.collectAsState()
                
                LaunchedEffect(viewModel) {
                    viewModel.loadData()
                }
                
                Column(modifier = modifier) {
                    if (uiState.loading) {
                        CircularProgressIndicator()
                    } else {
                        Text(text = uiState.data)
                    }
                    
                    uiState.error?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            """.trimIndent()
        )
        
        // Validate both ViewModel and Compose components
        val viewModelResult = viewModelValidator.validateViewModels(testProjectPath)
        val dataBindingResult = composeValidator.validateDataBinding(testProjectPath)
        val stateManagementResult = composeValidator.validateStateManagement(testProjectPath)
        val uiComponentResult = composeValidator.validateUIComponents(testProjectPath)
        
        // This integration example should have minimal issues
        assertTrue("Integration ViewModel should have no major issues", 
            viewModelResult.stateFlowIssues.isEmpty())
        assertTrue("Integration data binding should be valid", 
            dataBindingResult.isValid)
        assertTrue("Integration state management should be valid", 
            stateManagementResult.isValid)
        assertTrue("Integration UI components should be mostly valid", 
            uiComponentResult.validComponents > 0)
    }
    
    // Helper methods for creating test files
    
    private fun createTestProject(): String {
        val tempDir = createTempDir("ui_validation_test")
        val projectPath = tempDir.absolutePath
        
        // Create directory structure
        File("$projectPath/app/src/main/java/com/example/calorietracker/presentation/viewmodels").mkdirs()
        File("$projectPath/app/src/main/java/com/example/calorietracker/pages").mkdirs()
        File("$projectPath/app/src/main/java/com/example/calorietracker/components").mkdirs()
        File("$projectPath/app/src/main/java/com/example/calorietracker/ui").mkdirs()
        
        return projectPath
    }
    
    private fun createTestViewModel(fileName: String, content: String) {
        val file = File("$testProjectPath/app/src/main/java/com/example/calorietracker/presentation/viewmodels/$fileName")
        file.writeText(content)
    }
    
    private fun createTestComposeFile(fileName: String, content: String) {
        val file = File("$testProjectPath/app/src/main/java/com/example/calorietracker/pages/$fileName")
        file.writeText(content)
    }
}