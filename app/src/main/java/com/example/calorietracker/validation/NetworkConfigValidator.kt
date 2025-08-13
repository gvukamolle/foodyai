package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.NetworkConfigIssue
import com.example.calorietracker.validation.models.NetworkConfigResult
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Валидатор конфигурации сети для проверки настроек OkHttp/Retrofit
 */
class NetworkConfigValidator {
    
    /**
     * Анализирует NetworkModule.kt на предмет правильной настройки OkHttp/Retrofit
     */
    fun validateNetworkModule(projectPath: String): NetworkConfigResult {
        val issues = mutableListOf<NetworkConfigIssue>()
        val networkModulePath = "$projectPath/app/src/main/java/com/example/calorietracker/network/NetworkModule.kt"
        
        val networkModuleFile = File(networkModulePath)
        if (!networkModuleFile.exists()) {
            issues.add(NetworkConfigIssue(
                component = "NetworkModule",
                issue = "NetworkModule.kt файл не найден",
                severity = Severity.CRITICAL,
                fix = "Создайте NetworkModule.kt файл с правильной конфигурацией Retrofit и OkHttp"
            ))
            return NetworkConfigResult(issues, false)
        }
        
        val content = networkModuleFile.readText()
        
        // Проверка наличия необходимых импортов
        validateImports(content, issues)
        
        // Проверка конфигурации таймаутов
        validateTimeoutConfiguration(content, issues)
        
        // Проверка конфигурации логирования
        validateLoggingConfiguration(content, issues)
        
        // Проверка конфигурации Retrofit
        validateRetrofitConfiguration(content, issues)
        
        // Проверка конфигурации OkHttpClient
        validateOkHttpConfiguration(content, issues)
        
        return NetworkConfigResult(issues, issues.none { it.severity == Severity.CRITICAL })
    }
    
    private fun validateImports(content: String, issues: MutableList<NetworkConfigIssue>) {
        val requiredImports = listOf(
            "okhttp3.OkHttpClient",
            "okhttp3.logging.HttpLoggingInterceptor",
            "retrofit2.Retrofit",
            "retrofit2.converter.gson.GsonConverterFactory",
            "java.util.concurrent.TimeUnit",
            "dagger.Module",
            "dagger.Provides",
            "dagger.hilt.InstallIn",
            "javax.inject.Singleton"
        )
        
        requiredImports.forEach { import ->
            if (!content.contains("import $import")) {
                issues.add(NetworkConfigIssue(
                    component = "Imports",
                    issue = "Отсутствует необходимый импорт: $import",
                    severity = Severity.CRITICAL,
                    fix = "Добавьте импорт: import $import"
                ))
            }
        }
    }
    
    private fun validateTimeoutConfiguration(content: String, issues: MutableList<NetworkConfigIssue>) {
        val timeoutMethods = listOf("connectTimeout", "readTimeout", "writeTimeout")
        
        timeoutMethods.forEach { method ->
            if (!content.contains(".$method(")) {
                issues.add(NetworkConfigIssue(
                    component = "OkHttpClient",
                    issue = "Отсутствует конфигурация $method",
                    severity = Severity.WARNING,
                    fix = "Добавьте .$method(60, TimeUnit.SECONDS) в конфигурацию OkHttpClient"
                ))
            } else {
                // Проверяем, что таймаут не слишком короткий
                val timeoutRegex = Regex("\\.$method\\((\\d+),\\s*TimeUnit\\.SECONDS\\)")
                val match = timeoutRegex.find(content)
                match?.let {
                    val timeout = it.groupValues[1].toIntOrNull()
                    if (timeout != null && timeout < 30) {
                        issues.add(NetworkConfigIssue(
                            component = "OkHttpClient",
                            issue = "$method слишком короткий ($timeout секунд)",
                            severity = Severity.WARNING,
                            fix = "Рекомендуется установить $method не менее 30 секунд"
                        ))
                    }
                }
            }
        }
    }
    
    private fun validateLoggingConfiguration(content: String, issues: MutableList<NetworkConfigIssue>) {
        if (!content.contains("HttpLoggingInterceptor")) {
            issues.add(NetworkConfigIssue(
                component = "Logging",
                issue = "Отсутствует HttpLoggingInterceptor",
                severity = Severity.WARNING,
                fix = "Добавьте HttpLoggingInterceptor для отладки сетевых запросов"
            ))
        } else {
            // Проверяем уровень логирования
            if (!content.contains("level = HttpLoggingInterceptor.Level.BODY")) {
                if (content.contains("level = HttpLoggingInterceptor.Level.NONE")) {
                    issues.add(NetworkConfigIssue(
                        component = "Logging",
                        issue = "Уровень логирования установлен в NONE",
                        severity = Severity.INFO,
                        fix = "Рассмотрите использование BODY уровня для отладки (только в debug режиме)"
                    ))
                }
            }
        }
        
        // Проверяем добавление интерцептора в клиент
        if (content.contains("HttpLoggingInterceptor") && !content.contains("addInterceptor(logger)")) {
            issues.add(NetworkConfigIssue(
                component = "Logging",
                issue = "HttpLoggingInterceptor создан, но не добавлен в OkHttpClient",
                severity = Severity.CRITICAL,
                fix = "Добавьте .addInterceptor(logger) в конфигурацию OkHttpClient"
            ))
        }
    }
    
    private fun validateRetrofitConfiguration(content: String, issues: MutableList<NetworkConfigIssue>) {
        if (!content.contains("Retrofit.Builder()")) {
            issues.add(NetworkConfigIssue(
                component = "Retrofit",
                issue = "Отсутствует конфигурация Retrofit",
                severity = Severity.CRITICAL,
                fix = "Создайте Retrofit instance с помощью Retrofit.Builder()"
            ))
            return
        }
        
        // Проверка baseUrl
        if (!content.contains(".baseUrl(")) {
            issues.add(NetworkConfigIssue(
                component = "Retrofit",
                issue = "Отсутствует baseUrl в конфигурации Retrofit",
                severity = Severity.CRITICAL,
                fix = "Добавьте .baseUrl() в конфигурацию Retrofit"
            ))
        }
        
        // Проверка converter factory
        if (!content.contains("GsonConverterFactory.create()")) {
            issues.add(NetworkConfigIssue(
                component = "Retrofit",
                issue = "Отсутствует GsonConverterFactory",
                severity = Severity.CRITICAL,
                fix = "Добавьте .addConverterFactory(GsonConverterFactory.create())"
            ))
        }
        
        // Проверка клиента
        if (!content.contains(".client(")) {
            issues.add(NetworkConfigIssue(
                component = "Retrofit",
                issue = "Отсутствует настройка OkHttpClient в Retrofit",
                severity = Severity.CRITICAL,
                fix = "Добавьте .client(client) в конфигурацию Retrofit"
            ))
        }
    }
    
    private fun validateOkHttpConfiguration(content: String, issues: MutableList<NetworkConfigIssue>) {
        if (!content.contains("OkHttpClient.Builder()")) {
            issues.add(NetworkConfigIssue(
                component = "OkHttpClient",
                issue = "Отсутствует конфигурация OkHttpClient",
                severity = Severity.CRITICAL,
                fix = "Создайте OkHttpClient instance с помощью OkHttpClient.Builder()"
            ))
            return
        }
        
        // Проверка кастомного интерцептора для логирования
        if (content.contains("addInterceptor { chain ->")) {
            // Проверяем обработку ошибок в кастомном интерцепторе
            if (!content.contains("try {") || !content.contains("catch")) {
                issues.add(NetworkConfigIssue(
                    component = "OkHttpClient",
                    issue = "Кастомный интерцептор не содержит обработку ошибок",
                    severity = Severity.WARNING,
                    fix = "Добавьте try-catch блок в кастомный интерцептор"
                ))
            }
        }
        
        // Проверка Hilt аннотаций
        validateHiltAnnotations(content, issues)
    }
    
    private fun validateHiltAnnotations(content: String, issues: MutableList<NetworkConfigIssue>) {
        if (!content.contains("@Module")) {
            issues.add(NetworkConfigIssue(
                component = "Hilt",
                issue = "Отсутствует @Module аннотация",
                severity = Severity.CRITICAL,
                fix = "Добавьте @Module аннотацию к NetworkModule"
            ))
        }
        
        if (!content.contains("@InstallIn(SingletonComponent::class)")) {
            issues.add(NetworkConfigIssue(
                component = "Hilt",
                issue = "Отсутствует @InstallIn аннотация",
                severity = Severity.CRITICAL,
                fix = "Добавьте @InstallIn(SingletonComponent::class) к NetworkModule"
            ))
        }
        
        // Проверка @Provides и @Singleton аннотаций для методов
        val providesMethods = listOf("provideOkHttpClient", "provideRetrofit", "provideMakeService", "provideLoggingInterceptor")
        
        providesMethods.forEach { method ->
            if (content.contains("fun $method")) {
                if (!content.contains("@Provides") || !content.contains("@Singleton")) {
                    issues.add(NetworkConfigIssue(
                        component = "Hilt",
                        issue = "Метод $method должен иметь @Provides и @Singleton аннотации",
                        severity = Severity.CRITICAL,
                        fix = "Добавьте @Provides и @Singleton аннотации к методу $method"
                    ))
                }
            }
        }
    }
}