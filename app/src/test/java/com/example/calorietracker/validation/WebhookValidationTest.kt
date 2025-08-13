package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.io.IOException

/**
 * Тесты для валидации webhook сервисов
 */
class WebhookValidationTest {
    
    private lateinit var webhookValidator: WebhookValidatorImpl
    private lateinit var networkConfigValidator: NetworkConfigValidator
    private lateinit var makeServiceValidator: MakeServiceValidator
    private lateinit var jsonSerializationValidator: JsonSerializationValidator
    private lateinit var errorHandlingValidator: ErrorHandlingValidator
    private lateinit var connectivityValidator: WebhookConnectivityValidator
    
    private val testProjectPath = "test_project"
    
    @Before
    fun setUp() {
        webhookValidator = WebhookValidatorImpl()
        networkConfigValidator = NetworkConfigValidator()
        makeServiceValidator = MakeServiceValidator()
        jsonSerializationValidator = JsonSerializationValidator()
        errorHandlingValidator = ErrorHandlingValidator()
        connectivityValidator = WebhookConnectivityValidator()
        
        // Создаем тестовую структуру проекта
        createTestProjectStructure()
    }
    
    @After
    fun tearDown() {
        webhookValidator.cleanup()
        connectivityValidator.cleanup()
        
        // Очищаем тестовые файлы
        cleanupTestProject()
    }
    
    @Test
    fun `test network configuration validation with valid config`() {
        // Создаем валидный NetworkModule
        createValidNetworkModule()
        
        val result = networkConfigValidator.validateNetworkModule(testProjectPath)
        
        assertTrue("Network configuration should be valid", result.isValid)
        assertTrue("Should have no critical issues", 
            result.issues.none { it.severity == Severity.CRITICAL })
    }
    
    @Test
    fun `test network configuration validation with missing file`() {
        // Не создаем NetworkModule файл
        
        val result = networkConfigValidator.validateNetworkModule(testProjectPath)
        
        assertFalse("Network configuration should be invalid", result.isValid)
        assertTrue("Should have critical issue about missing file",
            result.issues.any { it.severity == Severity.CRITICAL && it.issue.contains("не найден") })
    }
    
    @Test
    fun `test network configuration validation with missing timeouts`() {
        // Создаем NetworkModule без таймаутов
        createNetworkModuleWithoutTimeouts()
        
        val result = networkConfigValidator.validateNetworkModule(testProjectPath)
        
        assertTrue("Should have timeout warnings",
            result.issues.any { it.issue.contains("таймаут") })
    }
    
    @Test
    fun `test MakeService validation with valid service`() {
        // Создаем валидный MakeService
        createValidMakeService()
        
        val result = makeServiceValidator.validateMakeService(testProjectPath)
        
        assertTrue("Should have valid endpoints", result.validEndpoints > 0)
        assertTrue("Should have total endpoints", result.totalEndpoints > 0)
    }
    
    @Test
    fun `test MakeService validation with missing file`() {
        // Не создаем MakeService файл
        
        val result = makeServiceValidator.validateMakeService(testProjectPath)
        
        assertEquals("Should have no valid endpoints", 0, result.validEndpoints)
        assertEquals("Should have no total endpoints", 0, result.totalEndpoints)
        assertTrue("Should have file missing issue",
            result.issues.any { it.issue.contains("не найден") })
    }
    
    @Test
    fun `test JSON serialization validation with valid data classes`() {
        // Создаем валидный MakeService с data классами
        createValidMakeService()
        
        val result = jsonSerializationValidator.validateJsonSerialization(testProjectPath)
        
        // Может иметь предупреждения, но не критические ошибки
        assertTrue("JSON serialization should be mostly valid", 
            result.issues.size < 10) // Разумное количество предупреждений
    }
    
    @Test
    fun `test error handling validation with valid safeApiCall`() {
        // Создаем валидный safeApiCall
        createValidSafeApiCall()
        
        val issues = errorHandlingValidator.validateErrorHandling(testProjectPath)
        
        // Может иметь рекомендации, но не критические ошибки
        assertTrue("Should have reasonable number of issues", issues.size < 5)
    }
    
    @Test
    fun `test error handling validation with missing safeApiCall`() {
        // Не создаем safeApiCall файл
        
        val issues = errorHandlingValidator.validateErrorHandling(testProjectPath)
        
        assertTrue("Should have issue about missing safeApiCall",
            issues.any { it.contains("safeApiCall.kt файл не найден") })
    }
    
    @Test
    fun `test webhook connectivity with mock server`() = runBlocking {
        // Тестируем с недоступным URL для проверки обработки ошибок
        val result = connectivityValidator.testWebhookConnectivity(
            "https://nonexistent.example.com/", 
            "test_webhook_id"
        )
        
        assertFalse("Should not be connected to nonexistent server", result.isConnected)
        assertNotNull("Should have error message", result.errorMessage)
        assertNull("Should not have response time", result.responseTime)
    }
    
    @Test
    fun `test comprehensive webhook validation`() = runBlocking {
        // Создаем полную тестовую конфигурацию
        createValidNetworkModule()
        createValidMakeService()
        createValidSafeApiCall()
        
        val report = webhookValidator.performComprehensiveValidation()
        
        assertNotNull("Report should not be null", report)
        assertTrue("Overall score should be reasonable", report.overallScore >= 0)
        
        // Проверяем, что все компоненты были проверены
        assertNotNull("Network config should be checked", report.networkConfiguration)
        assertNotNull("API endpoints should be checked", report.apiEndpoints)
        assertNotNull("JSON serialization should be checked", report.jsonSerialization)
        assertNotNull("Error handling should be checked", report.errorHandling)
        assertNotNull("Connectivity should be checked", report.connectivity)
    }
    
    @Test
    fun `test webhook validation report generation`() = runBlocking {
        createValidNetworkModule()
        createValidMakeService()
        createValidSafeApiCall()
        
        val report = webhookValidator.performComprehensiveValidation()
        
        val summary = report.getSummary()
        assertNotNull("Summary should not be null", summary)
        assertTrue("Summary should contain score", summary.contains("Overall Score"))
        assertTrue("Summary should contain total issues", summary.contains("Total Issues"))
        
        val detailedReport = report.getDetailedReport()
        assertNotNull("Detailed report should not be null", detailedReport)
        assertTrue("Detailed report should contain summary", detailedReport.contains("Overall Score"))
    }
    
    // Вспомогательные методы для создания тестовых файлов
    
    private fun createTestProjectStructure() {
        val projectDir = File(testProjectPath)
        projectDir.mkdirs()
        
        val networkDir = File("$testProjectPath/app/src/main/java/com/example/calorietracker/network")
        networkDir.mkdirs()
        
        val repositoriesDir = File("$testProjectPath/app/src/main/java/com/example/calorietracker/data/repositories")
        repositoriesDir.mkdirs()
    }
    
    private fun createValidNetworkModule() {
        val networkModuleContent = """
            package com.example.calorietracker.network
            
            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import okhttp3.OkHttpClient
            import okhttp3.logging.HttpLoggingInterceptor
            import retrofit2.Retrofit
            import retrofit2.converter.gson.GsonConverterFactory
            import java.util.concurrent.TimeUnit
            import javax.inject.Singleton
            
            @Module
            @InstallIn(SingletonComponent::class)
            object NetworkModule {
                @Provides
                @Singleton
                fun provideLoggingInterceptor(): HttpLoggingInterceptor =
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            
                @Provides
                @Singleton
                fun provideOkHttpClient(logger: HttpLoggingInterceptor): OkHttpClient =
                    OkHttpClient.Builder()
                        .addInterceptor(logger)
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build()
            
                @Provides
                @Singleton
                fun provideRetrofit(client: OkHttpClient): Retrofit =
                    Retrofit.Builder()
                        .baseUrl(MakeService.BASE_URL)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
            
                @Provides
                @Singleton
                fun provideMakeService(retrofit: Retrofit): MakeService =
                    retrofit.create(MakeService::class.java)
            }
        """.trimIndent()
        
        File("$testProjectPath/app/src/main/java/com/example/calorietracker/network/NetworkModule.kt")
            .writeText(networkModuleContent)
    }
    
    private fun createNetworkModuleWithoutTimeouts() {
        val networkModuleContent = """
            package com.example.calorietracker.network
            
            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import okhttp3.OkHttpClient
            import retrofit2.Retrofit
            import retrofit2.converter.gson.GsonConverterFactory
            import javax.inject.Singleton
            
            @Module
            @InstallIn(SingletonComponent::class)
            object NetworkModule {
                @Provides
                @Singleton
                fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()
            
                @Provides
                @Singleton
                fun provideRetrofit(client: OkHttpClient): Retrofit =
                    Retrofit.Builder()
                        .baseUrl("https://hook.us2.make.com/")
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
            }
        """.trimIndent()
        
        File("$testProjectPath/app/src/main/java/com/example/calorietracker/network/NetworkModule.kt")
            .writeText(networkModuleContent)
    }
    
    private fun createValidMakeService() {
        val makeServiceContent = """
            package com.example.calorietracker.network
            
            import retrofit2.http.*
            
            data class FoodAnalysisRequest(
                val message: String,
                val userId: String,
                val userProfile: UserProfileData
            )
            
            data class UserProfileData(
                val age: Int,
                val weight: Int,
                val height: Int,
                val gender: String,
                val activityLevel: String,
                val goal: String
            )
            
            data class FoodAnalysisResponse(
                val status: String,
                val answer: String?
            )
            
            interface MakeService {
                companion object {
                    const val BASE_URL = "https://hook.us2.make.com/"
                    const val WEBHOOK_ID = "test_webhook_id"
                }
            
                @Headers("Content-Type: application/json")
                @POST("{webhookId}")
                suspend fun analyzeFood(
                    @Path("webhookId") webhookId: String,
                    @Body request: FoodAnalysisRequest
                ): FoodAnalysisResponse
            }
        """.trimIndent()
        
        File("$testProjectPath/app/src/main/java/com/example/calorietracker/network/MakeService.kt")
            .writeText(makeServiceContent)
    }
    
    private fun createValidSafeApiCall() {
        val safeApiCallContent = """
            package com.example.calorietracker.network
            
            import android.util.Log
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.withContext
            
            suspend fun <T> safeApiCall(
                apiCall: suspend () -> T
            ): Result<T> = withContext(Dispatchers.IO) {
                try {
                    Result.success(apiCall())
                } catch (e: Exception) {
                    Log.e("Network", "API call failed", e)
                    Result.failure(e)
                }
            }
        """.trimIndent()
        
        File("$testProjectPath/app/src/main/java/com/example/calorietracker/network/safeApiCall.kt")
            .writeText(safeApiCallContent)
    }
    
    private fun cleanupTestProject() {
        val projectDir = File(testProjectPath)
        if (projectDir.exists()) {
            projectDir.deleteRecursively()
        }
    }
}