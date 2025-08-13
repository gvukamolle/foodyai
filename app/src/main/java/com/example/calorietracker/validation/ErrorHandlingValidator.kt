package com.example.calorietracker.validation

import java.io.File

/**
 * Валидатор обработки ошибок для проверки паттернов обработки ошибок в сетевом слое
 */
class ErrorHandlingValidator {
    
    /**
     * Анализирует реализацию safeApiCall.kt и паттерны обработки ошибок
     */
    fun validateErrorHandling(projectPath: String): List<String> {
        val issues = mutableListOf<String>()
        
        // Проверяем safeApiCall.kt
        validateSafeApiCall(projectPath, issues)
        
        // Проверяем использование safeApiCall в репозиториях
        validateRepositoryErrorHandling(projectPath, issues)
        
        // Проверяем обработку ошибок в NetworkModule
        validateNetworkModuleErrorHandling(projectPath, issues)
        
        // Проверяем логирование ошибок
        validateErrorLogging(projectPath, issues)
        
        return issues
    }
    
    private fun validateSafeApiCall(projectPath: String, issues: MutableList<String>) {
        val safeApiCallPath = "$projectPath/app/src/main/java/com/example/calorietracker/network/safeApiCall.kt"
        val file = File(safeApiCallPath)
        
        if (!file.exists()) {
            issues.add("safeApiCall.kt файл не найден")
            return
        }
        
        val content = file.readText()
        
        // Проверяем базовую структуру
        if (!content.contains("suspend fun <T> safeApiCall")) {
            issues.add("safeApiCall: Отсутствует основная функция safeApiCall")
            return
        }
        
        // Проверяем использование withContext(Dispatchers.IO)
        if (!content.contains("withContext(Dispatchers.IO)")) {
            issues.add("safeApiCall: Должен использовать withContext(Dispatchers.IO) для сетевых операций")
        }
        
        // Проверяем try-catch блок
        if (!content.contains("try {") || !content.contains("catch")) {
            issues.add("safeApiCall: Отсутствует try-catch блок для обработки исключений")
        }
        
        // Проверяем возвращение Result
        if (!content.contains("Result.success") || !content.contains("Result.failure")) {
            issues.add("safeApiCall: Должен возвращать Result.success() и Result.failure()")
        }
        
        // Проверяем логирование ошибок
        if (!content.contains("Log.e")) {
            issues.add("safeApiCall: Отсутствует логирование ошибок")
        }
        
        // Проверяем специфичные типы исключений
        validateSpecificExceptionHandling(content, issues)
        
        // Проверяем параметры функции
        validateSafeApiCallParameters(content, issues)
    }
    
    private fun validateSpecificExceptionHandling(content: String, issues: MutableList<String>) {
        // Проверяем, что обрабатываются разные типы исключений
        val exceptionTypes = listOf(
            "IOException", "HttpException", "SocketTimeoutException", 
            "UnknownHostException", "ConnectException"
        )
        
        // Если есть специфичная обработка исключений, проверяем её
        if (content.contains("when (e)") || content.contains("is IOException")) {
            exceptionTypes.forEach { exceptionType ->
                if (content.contains("import") && !content.contains("import java.io.$exceptionType") && 
                    !content.contains("import retrofit2.HttpException") && exceptionType == "HttpException") {
                    // Это нормально, если не импортируется, но используется
                }
            }
        } else {
            issues.add("safeApiCall: Рекомендуется добавить специфичную обработку для разных типов сетевых исключений")
        }
    }
    
    private fun validateSafeApiCallParameters(content: String, issues: MutableList<String>) {
        // Проверяем generic параметр
        if (!content.contains("<T>")) {
            issues.add("safeApiCall: Функция должна быть generic с параметром <T>")
        }
        
        // Проверяем suspend lambda параметр
        if (!content.contains("suspend () -> T")) {
            issues.add("safeApiCall: Параметр должен быть suspend lambda: suspend () -> T")
        }
        
        // Проверяем возвращаемый тип
        if (!content.contains("): Result<T>")) {
            issues.add("safeApiCall: Функция должна возвращать Result<T>")
        }
    }
    
    private fun validateRepositoryErrorHandling(projectPath: String, issues: MutableList<String>) {
        val repositoriesPath = "$projectPath/app/src/main/java/com/example/calorietracker/data/repositories"
        val repositoriesDir = File(repositoriesPath)
        
        if (!repositoriesDir.exists()) {
            issues.add("Директория repositories не найдена")
            return
        }
        
        val repositoryFiles = repositoriesDir.listFiles { file -> 
            file.name.endsWith("RepositoryImpl.kt") 
        } ?: emptyArray()
        
        repositoryFiles.forEach { file ->
            validateRepositoryFile(file, issues)
        }
    }
    
    private fun validateRepositoryFile(file: File, issues: MutableList<String>) {
        val content = file.readText()
        val fileName = file.name
        
        // Проверяем использование safeApiCall
        if (content.contains("makeService.") && !content.contains("safeApiCall")) {
            issues.add("$fileName: Сетевые вызовы должны использовать safeApiCall для обработки ошибок")
        }
        
        // Проверяем обработку Result
        if (content.contains("safeApiCall") && !content.contains("Result.")) {
            issues.add("$fileName: Должна быть обработка Result возвращаемого safeApiCall")
        }
        
        // Проверяем паттерны обработки ошибок
        validateRepositoryErrorPatterns(content, fileName, issues)
    }
    
    private fun validateRepositoryErrorPatterns(content: String, fileName: String, issues: MutableList<String>) {
        // Проверяем использование fold, map, или других методов Result
        if (content.contains("safeApiCall")) {
            val hasResultHandling = content.contains(".fold(") || 
                                  content.contains(".map(") || 
                                  content.contains(".getOrNull()") ||
                                  content.contains(".isSuccess") ||
                                  content.contains(".isFailure")
            
            if (!hasResultHandling) {
                issues.add("$fileName: Отсутствует обработка Result (используйте fold, map, getOrNull и т.д.)")
            }
        }
        
        // Проверяем логирование в репозиториях
        if (content.contains("safeApiCall") && !content.contains("Log.")) {
            issues.add("$fileName: Рекомендуется добавить логирование для отладки")
        }
        
        // Проверяем возвращение default значений при ошибках
        if (content.contains("safeApiCall") && content.contains("suspend fun")) {
            // Проверяем, что есть fallback значения
            if (!content.contains("getOrNull()") && !content.contains("getOrElse") && !content.contains("fold(")) {
                issues.add("$fileName: Рекомендуется предусмотреть fallback значения при ошибках API")
            }
        }
    }
    
    private fun validateNetworkModuleErrorHandling(projectPath: String, issues: MutableList<String>) {
        val networkModulePath = "$projectPath/app/src/main/java/com/example/calorietracker/network/NetworkModule.kt"
        val file = File(networkModulePath)
        
        if (!file.exists()) {
            issues.add("NetworkModule.kt файл не найден")
            return
        }
        
        val content = file.readText()
        
        // Проверяем кастомный интерцептор для обработки ошибок
        if (content.contains("addInterceptor { chain ->")) {
            validateCustomInterceptor(content, issues)
        }
        
        // Проверяем конфигурацию таймаутов (влияет на обработку ошибок)
        validateTimeoutErrorHandling(content, issues)
    }
    
    private fun validateCustomInterceptor(content: String, issues: MutableList<String>) {
        // Проверяем try-catch в кастомном интерцепторе
        if (!content.contains("try {") || !content.contains("catch (e: Exception)")) {
            issues.add("NetworkModule: Кастомный интерцептор должен содержать try-catch для обработки ошибок")
        }
        
        // Проверяем логирование ошибок в интерцепторе
        if (!content.contains("Log.e")) {
            issues.add("NetworkModule: Кастомный интерцептор должен логировать ошибки")
        }
        
        // Проверяем, что исключение пробрасывается дальше
        if (!content.contains("throw e")) {
            issues.add("NetworkModule: Кастомный интерцептор должен пробрасывать исключения дальше после логирования")
        }
        
        // Проверяем логирование успешных запросов
        if (!content.contains("Log.d")) {
            issues.add("NetworkModule: Рекомендуется логировать успешные запросы для отладки")
        }
    }
    
    private fun validateTimeoutErrorHandling(content: String, issues: MutableList<String>) {
        val timeouts = mapOf(
            "connectTimeout" to 60,
            "readTimeout" to 60,
            "writeTimeout" to 60
        )
        
        timeouts.forEach { (timeoutType, expectedMinimum) ->
            val timeoutRegex = Regex("\\.$timeoutType\\((\\d+),\\s*TimeUnit\\.SECONDS\\)")
            val match = timeoutRegex.find(content)
            
            if (match != null) {
                val timeout = match.groupValues[1].toIntOrNull()
                if (timeout != null && timeout < expectedMinimum) {
                    issues.add("NetworkModule: $timeoutType слишком короткий ($timeout сек), может вызывать преждевременные таймауты")
                }
            }
        }
    }
    
    private fun validateErrorLogging(projectPath: String, issues: MutableList<String>) {
        // Проверяем использование правильных тегов для логирования
        val networkFiles = listOf(
            "$projectPath/app/src/main/java/com/example/calorietracker/network/safeApiCall.kt",
            "$projectPath/app/src/main/java/com/example/calorietracker/network/NetworkModule.kt"
        )
        
        networkFiles.forEach { filePath ->
            val file = File(filePath)
            if (file.exists()) {
                val content = file.readText()
                validateLoggingInFile(content, file.name, issues)
            }
        }
    }
    
    private fun validateLoggingInFile(content: String, fileName: String, issues: MutableList<String>) {
        if (content.contains("Log.")) {
            // Проверяем использование правильных тегов
            val logRegex = Regex("Log\\.[a-z]\\(\"([^\"]+)\"")
            val matches = logRegex.findAll(content)
            
            matches.forEach { match ->
                val tag = match.groupValues[1]
                if (tag.isEmpty()) {
                    issues.add("$fileName: Пустой тег для логирования")
                } else if (tag.length > 23) {
                    issues.add("$fileName: Тег логирования слишком длинный: '$tag' (максимум 23 символа)")
                }
            }
            
            // Проверяем, что есть разные уровни логирования
            val hasDebugLog = content.contains("Log.d")
            val hasErrorLog = content.contains("Log.e")
            
            if (!hasErrorLog && fileName.contains("safeApiCall")) {
                issues.add("$fileName: Должно быть логирование ошибок (Log.e)")
            }
            
            if (!hasDebugLog && fileName.contains("NetworkModule")) {
                issues.add("$fileName: Рекомендуется добавить debug логирование для отладки")
            }
        }
    }
}