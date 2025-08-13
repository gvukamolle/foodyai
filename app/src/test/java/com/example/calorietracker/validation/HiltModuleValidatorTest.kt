package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.BindingIssue
import com.example.calorietracker.validation.models.ModuleIssue
import com.example.calorietracker.validation.models.ScopeIssue
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for HiltModuleValidator
 */
class HiltModuleValidatorTest {
    
    private lateinit var hiltModuleValidator: HiltModuleValidator
    
    @Before
    fun setUp() {
        hiltModuleValidator = HiltModuleValidator()
    }
    
    @Test
    fun `validateModuleAnnotations should detect missing Module annotation`() {
        // Given
        val moduleContent = """
            package com.example.test
            
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            
            @InstallIn(SingletonComponent::class)
            object TestModule
        """.trimIndent()
        
        // When
        val issues = hiltModuleValidator.validateModuleAnnotations("TestModule", moduleContent)
        
        // Then
        assertTrue("Should detect missing @Module annotation", 
            issues.any { it.issue.contains("Missing @Module annotation") })
    }
    
    @Test
    fun `validateModuleAnnotations should detect missing InstallIn annotation`() {
        // Given
        val moduleContent = """
            package com.example.test
            
            import dagger.Module
            
            @Module
            object TestModule
        """.trimIndent()
        
        // When
        val issues = hiltModuleValidator.validateModuleAnnotations("TestModule", moduleContent)
        
        // Then
        assertTrue("Should detect missing @InstallIn annotation", 
            issues.any { it.issue.contains("Missing @InstallIn annotation") })
    }
    
    @Test
    fun `validateModuleAnnotations should detect missing imports`() {
        // Given
        val moduleContent = """
            package com.example.test
            
            @Module
            @InstallIn(SingletonComponent::class)
            object TestModule
        """.trimIndent()
        
        // When
        val issues = hiltModuleValidator.validateModuleAnnotations("TestModule", moduleContent)
        
        // Then
        assertTrue("Should detect missing dagger.Module import", 
            issues.any { it.issue.contains("Missing import for dagger.Module") })
        assertTrue("Should detect missing dagger.hilt.InstallIn import", 
            issues.any { it.issue.contains("Missing import for dagger.hilt.InstallIn") })
    }
    
    @Test
    fun `validateModuleAnnotations should pass for valid module`() {
        // Given
        val moduleContent = """
            package com.example.test
            
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            
            @Module
            @InstallIn(SingletonComponent::class)
            object TestModule
        """.trimIndent()
        
        // When
        val issues = hiltModuleValidator.validateModuleAnnotations("TestModule", moduleContent)
        
        // Then
        assertTrue("Should have no critical issues for valid module", 
            issues.none { it.severity == Severity.CRITICAL })
    }
    
    @Test
    fun `validateProviderMethods should detect missing scope annotation`() {
        // Given
        val moduleContent = """
            @Module
            @InstallIn(SingletonComponent::class)
            object TestModule {
                
                @Provides
                fun provideTestService(): TestService {
                    return TestService()
                }
            }
        """.trimIndent()
        
        // When
        val issues = hiltModuleValidator.validateProviderMethods("TestModule", moduleContent)
        
        // Then
        assertTrue("Should detect missing @Singleton scope", 
            issues.any { it.issue.contains("should have @Singleton scope") })
    }
    
    @Test
    fun `validateProviderMethods should detect missing Provides annotation`() {
        // Given
        val moduleContent = """
            @Module
            @InstallIn(SingletonComponent::class)
            object TestModule {
                
                @Singleton
                fun provideTestService(): TestService {
                    return TestService()
                }
            }
        """.trimIndent()
        
        // When
        val issues = hiltModuleValidator.validateProviderMethods("TestModule", moduleContent)
        
        // Then
        assertTrue("Should detect missing @Provides annotation", 
            issues.any { it.issue.contains("missing @Provides annotation") })
    }
    
    @Test
    fun `validateBindings should detect Binds in non-abstract module`() {
        // Given
        val moduleContent = """
            @Module
            @InstallIn(SingletonComponent::class)
            object TestModule {
                
                @Binds
                abstract fun bindTestRepository(impl: TestRepositoryImpl): TestRepository
            }
        """.trimIndent()
        
        // When
        val issues = hiltModuleValidator.validateBindings("TestModule", moduleContent)
        
        // Then
        assertTrue("Should detect @Binds in non-abstract module", 
            issues.any { it.issue.contains("@Binds method") && it.issue.contains("non-abstract module") })
    }
    
    @Test
    fun `validateBindings should validate parameter format`() {
        // Given
        val moduleContent = """
            @Module
            @InstallIn(SingletonComponent::class)
            abstract class TestModule {
                
                @Binds
                abstract fun bindTestRepository(impl): TestRepository
            }
        """.trimIndent()
        
        // When
        val issues = hiltModuleValidator.validateBindings("TestModule", moduleContent)
        
        // Then
        assertTrue("Should detect invalid parameter format", 
            issues.any { it.issue.contains("Invalid parameter format") })
    }
    
    @Test
    fun `validateModuleScopes should detect scope mismatch`() {
        // Given
        val moduleContent = """
            @Module
            @InstallIn(SingletonComponent::class)
            object TestModule {
                
                @Provides
                @ViewModelScoped
                fun provideTestService(): TestService {
                    return TestService()
                }
            }
        """.trimIndent()
        
        // When
        val issues = hiltModuleValidator.validateModuleScopes("TestModule", moduleContent)
        
        // Then
        assertTrue("Should detect ViewModelScoped in SingletonComponent", 
            issues.any { it.scopeIssue.contains("ViewModelScoped annotation found in SingletonComponent") })
    }
    
    @Test
    fun `validateModuleScopes should pass for correct scope`() {
        // Given
        val moduleContent = """
            @Module
            @InstallIn(ViewModelComponent::class)
            object TestModule {
                
                @Provides
                @ViewModelScoped
                fun provideTestService(): TestService {
                    return TestService()
                }
            }
        """.trimIndent()
        
        // When
        val issues = hiltModuleValidator.validateModuleScopes("TestModule", moduleContent)
        
        // Then
        assertTrue("Should have no scope issues for correct scope", issues.isEmpty())
    }
}