package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.ConnectivityResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.system.measureTimeMillis

/**
 * Валидатор подключения к вебхукам для тестирования сетевого соединения
 */
class WebhookConnectivityValidator {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    /**
     * Тестирует подключение к вебхуку Make.com
     */
    suspend fun testWebhookConnectivity(baseUrl: String, webhookId: String): ConnectivityResult {
        return withContext(Dispatchers.IO) {
            try {
                val responseTime = measureTimeMillis {
                    testBasicConnectivity(baseUrl, webhookId)
                }
                
                ConnectivityResult(
                    isConnected = true,
                    responseTime = responseTime,
                    errorMessage = null
                )
            } catch (e: Exception) {
                ConnectivityResult(
                    isConnected = false,
                    responseTime = null,
                    errorMessage = getErrorMessage(e)
                )
            }
        }
    }
    
    /**
     * Выполняет комплексную проверку подключения к вебхуку
     */
    suspend fun performComprehensiveConnectivityTest(
        baseUrl: String, 
        webhookId: String
    ): List<String> {
        val issues = mutableListOf<String>()
        
        // Тест 1: Базовое подключение
        testBasicConnectivityWithIssues(baseUrl, webhookId, issues)
        
        // Тест 2: Тест с mock данными
        testMockWebhookRequest(baseUrl, webhookId, issues)
        
        // Тест 3: Тест обработки ошибок
        testErrorHandling(baseUrl, webhookId, issues)
        
        // Тест 4: Тест таймаутов
        testTimeoutHandling(baseUrl, webhookId, issues)
        
        // Тест 5: Тест различных HTTP методов
        testHttpMethods(baseUrl, webhookId, issues)
        
        return issues
    }
    
    private suspend fun testBasicConnectivity(baseUrl: String, webhookId: String) {
        val url = "${baseUrl.trimEnd('/')}/$webhookId"
        val request = Request.Builder()
            .url(url)
            .head() // Используем HEAD для минимального запроса
            .build()
        
        withTimeoutOrNull(10000) { // 10 секунд таймаут
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 405) { // 405 Method Not Allowed ожидаем для HEAD
                    throw IOException("Unexpected response code: ${response.code}")
                }
            }
        } ?: throw SocketTimeoutException("Connection timeout")
    }
    
    private suspend fun testBasicConnectivityWithIssues(
        baseUrl: String, 
        webhookId: String, 
        issues: MutableList<String>
    ) {
        try {
            val responseTime = measureTimeMillis {
                testBasicConnectivity(baseUrl, webhookId)
            }
            
            if (responseTime > 5000) {
                issues.add("Подключение к вебхуку медленное: ${responseTime}мс (рекомендуется < 5000мс)")
            }
            
        } catch (e: UnknownHostException) {
            issues.add("Не удается разрешить хост: ${e.message}")
        } catch (e: SocketTimeoutException) {
            issues.add("Таймаут подключения к вебхуку: ${e.message}")
        } catch (e: IOException) {
            issues.add("Ошибка подключения к вебхуку: ${e.message}")
        } catch (e: Exception) {
            issues.add("Неожиданная ошибка при подключении: ${e.message}")
        }
    }
    
    private suspend fun testMockWebhookRequest(
        baseUrl: String, 
        webhookId: String, 
        issues: MutableList<String>
    ) {
        try {
            val mockRequest = createMockHealthCheckRequest()
            val response = sendMockRequest(baseUrl, webhookId, mockRequest)
            
            validateMockResponse(response, issues)
            
        } catch (e: Exception) {
            issues.add("Ошибка при отправке mock запроса: ${e.message}")
        }
    }
    
    private fun createMockHealthCheckRequest(): String {
        return """
            {
                "ping": "health",
                "timestamp": ${System.currentTimeMillis()},
                "test": true
            }
        """.trimIndent()
    }
    
    private suspend fun sendMockRequest(
        baseUrl: String, 
        webhookId: String, 
        jsonBody: String
    ): Response {
        val url = "${baseUrl.trimEnd('/')}/$webhookId"
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()
        
        return withContext(Dispatchers.IO) {
            client.newCall(request).execute()
        }
    }
    
    private fun validateMockResponse(response: Response, issues: MutableList<String>) {
        response.use {
            when {
                response.isSuccessful -> {
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        issues.add("Вебхук возвращает пустой ответ")
                    } else {
                        validateResponseFormat(responseBody, issues)
                    }
                }
                response.code == 404 -> {
                    issues.add("Вебхук не найден (404). Проверьте WEBHOOK_ID")
                }
                response.code == 403 -> {
                    issues.add("Доступ к вебхуку запрещен (403). Проверьте права доступа")
                }
                response.code == 500 -> {
                    issues.add("Внутренняя ошибка сервера вебхука (500)")
                }
                else -> {
                    issues.add("Неожиданный код ответа от вебхука: ${response.code}")
                }
            }
        }
    }
    
    private fun validateResponseFormat(responseBody: String, issues: MutableList<String>) {
        try {
            // Простая проверка JSON формата
            if (!responseBody.trim().startsWith("{") && !responseBody.trim().startsWith("[")) {
                issues.add("Ответ вебхука не в JSON формате")
            }
            
            // Проверяем наличие базовых полей
            if (!responseBody.contains("status") && !responseBody.contains("error")) {
                issues.add("Ответ вебхука не содержит поле 'status' или 'error'")
            }
            
        } catch (e: Exception) {
            issues.add("Ошибка при валидации формата ответа: ${e.message}")
        }
    }
    
    private suspend fun testErrorHandling(
        baseUrl: String, 
        webhookId: String, 
        issues: MutableList<String>
    ) {
        // Тест с неправильным JSON
        try {
            val invalidJson = "{ invalid json }"
            val response = sendMockRequest(baseUrl, webhookId, invalidJson)
            
            response.use {
                if (response.isSuccessful) {
                    issues.add("Вебхук принимает некорректный JSON без ошибки")
                }
            }
        } catch (e: Exception) {
            // Ожидаемое поведение
        }
        
        // Тест с пустым телом запроса
        try {
            val response = sendMockRequest(baseUrl, webhookId, "")
            
            response.use {
                if (response.isSuccessful) {
                    issues.add("Вебхук принимает пустое тело запроса")
                }
            }
        } catch (e: Exception) {
            // Ожидаемое поведение
        }
    }
    
    private suspend fun testTimeoutHandling(
        baseUrl: String, 
        webhookId: String, 
        issues: MutableList<String>
    ) {
        val shortTimeoutClient = OkHttpClient.Builder()
            .connectTimeout(1, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(1, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        try {
            val url = "${baseUrl.trimEnd('/')}/$webhookId"
            val request = Request.Builder()
                .url(url)
                .head()
                .build()
            
            shortTimeoutClient.newCall(request).execute().use { response ->
                // Если запрос прошел слишком быстро, это может указывать на проблемы
                if (response.isSuccessful) {
                    issues.add("Предупреждение: Вебхук отвечает очень быстро, возможно не обрабатывает запросы")
                }
            }
        } catch (e: SocketTimeoutException) {
            // Ожидаемое поведение для короткого таймаута
        } catch (e: Exception) {
            issues.add("Неожиданная ошибка при тестировании таймаутов: ${e.message}")
        }
    }
    
    private suspend fun testHttpMethods(
        baseUrl: String, 
        webhookId: String, 
        issues: MutableList<String>
    ) {
        val url = "${baseUrl.trimEnd('/')}/$webhookId"
        
        // Тест GET метода (должен не поддерживаться)
        try {
            val getRequest = Request.Builder()
                .url(url)
                .get()
                .build()
            
            client.newCall(getRequest).execute().use { response ->
                if (response.isSuccessful) {
                    issues.add("Вебхук неожиданно поддерживает GET запросы")
                }
            }
        } catch (e: Exception) {
            // Ожидаемое поведение
        }
        
        // Тест PUT метода (должен не поддерживаться)
        try {
            val putRequest = Request.Builder()
                .url(url)
                .put("{}".toRequestBody("application/json".toMediaType()))
                .build()
            
            client.newCall(putRequest).execute().use { response ->
                if (response.isSuccessful) {
                    issues.add("Вебхук неожиданно поддерживает PUT запросы")
                }
            }
        } catch (e: Exception) {
            // Ожидаемое поведение
        }
    }
    
    /**
     * Тестирует валидацию ответов вебхука
     */
    suspend fun validateWebhookResponses(
        baseUrl: String, 
        webhookId: String
    ): List<String> {
        val issues = mutableListOf<String>()
        
        // Тест различных типов запросов
        val testRequests = createTestRequests()
        
        testRequests.forEach { (requestName, requestBody) ->
            try {
                val response = sendMockRequest(baseUrl, webhookId, requestBody)
                validateSpecificResponse(requestName, response, issues)
            } catch (e: Exception) {
                issues.add("Ошибка при тестировании $requestName: ${e.message}")
            }
        }
        
        return issues
    }
    
    private fun createTestRequests(): Map<String, String> {
        return mapOf(
            "HealthCheck" to """{"ping": "health"}""",
            "FoodAnalysis" to """
                {
                    "weight": 100,
                    "userProfile": {
                        "age": 25,
                        "weight": 70,
                        "height": 175,
                        "gender": "male",
                        "activityLevel": "moderate",
                        "goal": "maintain"
                    },
                    "message": "test food",
                    "userId": "test_user",
                    "messageType": "analysis"
                }
            """.trimIndent(),
            "ChatRequest" to """
                {
                    "message": "test message",
                    "userProfile": {
                        "age": 25,
                        "weight": 70,
                        "height": 175,
                        "gender": "male",
                        "activityLevel": "moderate",
                        "goal": "maintain"
                    },
                    "userId": "test_user",
                    "messageType": "chat"
                }
            """.trimIndent()
        )
    }
    
    private fun validateSpecificResponse(
        requestName: String, 
        response: Response, 
        issues: MutableList<String>
    ) {
        response.use {
            if (!response.isSuccessful) {
                issues.add("$requestName: Неуспешный ответ (${response.code})")
                return
            }
            
            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                issues.add("$requestName: Пустой ответ")
                return
            }
            
            // Проверяем специфичные поля для разных типов запросов
            when (requestName) {
                "HealthCheck" -> {
                    if (!responseBody.contains("status")) {
                        issues.add("$requestName: Ответ должен содержать поле 'status'")
                    }
                }
                "FoodAnalysis" -> {
                    if (!responseBody.contains("status") && !responseBody.contains("answer")) {
                        issues.add("$requestName: Ответ должен содержать поле 'status' или 'answer'")
                    }
                }
                "ChatRequest" -> {
                    if (!responseBody.contains("status") && !responseBody.contains("answer")) {
                        issues.add("$requestName: Ответ должен содержать поле 'status' или 'answer'")
                    }
                }
            }
        }
    }
    
    private fun getErrorMessage(exception: Exception): String {
        return when (exception) {
            is UnknownHostException -> "Не удается разрешить хост: ${exception.message}"
            is SocketTimeoutException -> "Таймаут подключения: ${exception.message}"
            is IOException -> "Ошибка ввода/вывода: ${exception.message}"
            else -> "Неожиданная ошибка: ${exception.message}"
        }
    }
    
    /**
     * Освобождает ресурсы
     */
    fun cleanup() {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}