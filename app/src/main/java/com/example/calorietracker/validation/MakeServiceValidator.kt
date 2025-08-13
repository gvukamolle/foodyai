package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.ApiEndpointIssue
import com.example.calorietracker.validation.models.ApiEndpointResult
import java.io.File

/**
 * Валидатор MakeService API для проверки определений эндпоинтов
 */
class MakeServiceValidator {
    
    /**
     * Валидирует все определения API эндпоинтов в MakeService.kt
     */
    fun validateMakeService(projectPath: String): ApiEndpointResult {
        val issues = mutableListOf<ApiEndpointIssue>()
        val makeServicePath = "$projectPath/app/src/main/java/com/example/calorietracker/network/MakeService.kt"
        
        val makeServiceFile = File(makeServicePath)
        if (!makeServiceFile.exists()) {
            issues.add(ApiEndpointIssue(
                endpoint = "MakeService",
                method = "FILE",
                issue = "MakeService.kt файл не найден",
                expectedFormat = "Создайте MakeService.kt файл с определениями API"
            ))
            return ApiEndpointResult(issues, 0, 0)
        }
        
        val content = makeServiceFile.readText()
        
        // Проверка базовых настроек
        validateBaseConfiguration(content, issues)
        
        // Проверка эндпоинтов
        val endpoints = extractEndpoints(content)
        validateEndpoints(content, endpoints, issues)
        
        // Проверка моделей данных
        validateDataModels(content, issues)
        
        val validEndpoints = endpoints.size - issues.count { it.endpoint != "BASE_CONFIG" }
        
        return ApiEndpointResult(issues, validEndpoints, endpoints.size)
    }
    
    private fun validateBaseConfiguration(content: String, issues: MutableList<ApiEndpointIssue>) {
        // Проверка BASE_URL
        if (!content.contains("const val BASE_URL")) {
            issues.add(ApiEndpointIssue(
                endpoint = "BASE_CONFIG",
                method = "CONFIG",
                issue = "Отсутствует BASE_URL константа",
                expectedFormat = "const val BASE_URL = \"https://hook.us2.make.com/\""
            ))
        } else {
            val baseUrlRegex = Regex("const val BASE_URL = \"([^\"]+)\"")
            val match = baseUrlRegex.find(content)
            match?.let {
                val url = it.groupValues[1]
                if (!url.startsWith("https://")) {
                    issues.add(ApiEndpointIssue(
                        endpoint = "BASE_CONFIG",
                        method = "CONFIG",
                        issue = "BASE_URL должен использовать HTTPS",
                        expectedFormat = "https://hook.us2.make.com/"
                    ))
                }
                if (!url.endsWith("/")) {
                    issues.add(ApiEndpointIssue(
                        endpoint = "BASE_CONFIG",
                        method = "CONFIG",
                        issue = "BASE_URL должен заканчиваться на '/'",
                        expectedFormat = "https://hook.us2.make.com/"
                    ))
                }
            }
        }
        
        // Проверка WEBHOOK_ID
        if (!content.contains("const val WEBHOOK_ID")) {
            issues.add(ApiEndpointIssue(
                endpoint = "BASE_CONFIG",
                method = "CONFIG",
                issue = "Отсутствует WEBHOOK_ID константа",
                expectedFormat = "const val WEBHOOK_ID = \"your_webhook_id\""
            ))
        }
    }
    
    private fun extractEndpoints(content: String): List<String> {
        val endpointRegex = Regex("suspend fun (\\w+)\\(")
        return endpointRegex.findAll(content).map { it.groupValues[1] }.toList()
    }
    
    private fun validateEndpoints(content: String, endpoints: List<String>, issues: MutableList<ApiEndpointIssue>) {
        endpoints.forEach { endpoint ->
            validateEndpoint(content, endpoint, issues)
        }
    }
    
    private fun validateEndpoint(content: String, endpointName: String, issues: MutableList<ApiEndpointIssue>) {
        val endpointPattern = Regex("suspend fun $endpointName\\([^)]*\\):[^{]*")
        val match = endpointPattern.find(content)
        
        if (match == null) {
            issues.add(ApiEndpointIssue(
                endpoint = endpointName,
                method = "UNKNOWN",
                issue = "Не удалось найти определение эндпоинта",
                expectedFormat = "suspend fun $endpointName(...): ResponseType"
            ))
            return
        }
        
        val endpointDefinition = match.value
        
        // Проверка HTTP метода
        validateHttpMethod(content, endpointName, endpointDefinition, issues)
        
        // Проверка заголовков
        validateHeaders(content, endpointName, issues)
        
        // Проверка параметров
        validateParameters(content, endpointName, endpointDefinition, issues)
        
        // Проверка типа возвращаемого значения
        validateReturnType(endpointDefinition, endpointName, issues)
    }
    
    private fun validateHttpMethod(content: String, endpointName: String, endpointDefinition: String, issues: MutableList<ApiEndpointIssue>) {
        val httpMethods = listOf("@GET", "@POST", "@PUT", "@DELETE", "@PATCH")
        val hasHttpMethod = httpMethods.any { method ->
            content.contains("$method") && content.indexOf(method) < content.indexOf("suspend fun $endpointName")
        }
        
        if (!hasHttpMethod) {
            issues.add(ApiEndpointIssue(
                endpoint = endpointName,
                method = "MISSING",
                issue = "Отсутствует HTTP метод аннотация",
                expectedFormat = "@POST(\"{webhookId}\") или другой HTTP метод"
            ))
        } else {
            // Проверяем, что используется правильный путь
            val postPattern = Regex("@POST\\(\"([^\"]+)\"\\)")
            val postMatch = postPattern.find(content)
            postMatch?.let {
                val path = it.groupValues[1]
                if (path != "{webhookId}") {
                    issues.add(ApiEndpointIssue(
                        endpoint = endpointName,
                        method = "POST",
                        issue = "Неправильный путь в @POST аннотации: $path",
                        expectedFormat = "@POST(\"{webhookId}\")"
                    ))
                }
            }
        }
    }
    
    private fun validateHeaders(content: String, endpointName: String, issues: MutableList<ApiEndpointIssue>) {
        val endpointStartIndex = content.indexOf("suspend fun $endpointName")
        if (endpointStartIndex == -1) return
        
        // Ищем аннотации перед функцией
        val beforeEndpoint = content.substring(0, endpointStartIndex)
        val lastAnnotationIndex = beforeEndpoint.lastIndexOf("@")
        
        if (lastAnnotationIndex != -1) {
            val annotationsSection = content.substring(lastAnnotationIndex, endpointStartIndex)
            
            // Проверяем наличие Content-Type для POST запросов
            if (annotationsSection.contains("@POST") || annotationsSection.contains("@Multipart")) {
                if (!annotationsSection.contains("@Headers") && !annotationsSection.contains("@Multipart")) {
                    issues.add(ApiEndpointIssue(
                        endpoint = endpointName,
                        method = "POST",
                        issue = "Отсутствует @Headers аннотация с Content-Type",
                        expectedFormat = "@Headers(\"Content-Type: application/json\")"
                    ))
                } else if (annotationsSection.contains("@Headers") && !annotationsSection.contains("Content-Type")) {
                    issues.add(ApiEndpointIssue(
                        endpoint = endpointName,
                        method = "POST",
                        issue = "В @Headers отсутствует Content-Type",
                        expectedFormat = "@Headers(\"Content-Type: application/json\")"
                    ))
                }
            }
        }
    }
    
    private fun validateParameters(content: String, endpointName: String, endpointDefinition: String, issues: MutableList<ApiEndpointIssue>) {
        // Проверяем наличие webhookId параметра
        if (!endpointDefinition.contains("@Path(\"webhookId\") webhookId: String")) {
            issues.add(ApiEndpointIssue(
                endpoint = endpointName,
                method = "PARAMETER",
                issue = "Отсутствует параметр webhookId",
                expectedFormat = "@Path(\"webhookId\") webhookId: String"
            ))
        }
        
        // Для POST запросов проверяем наличие @Body параметра
        val beforeEndpoint = content.substring(0, content.indexOf("suspend fun $endpointName"))
        if (beforeEndpoint.contains("@POST") && !endpointDefinition.contains("@Body")) {
            // Исключение для Multipart запросов
            if (!beforeEndpoint.contains("@Multipart")) {
                issues.add(ApiEndpointIssue(
                    endpoint = endpointName,
                    method = "POST",
                    issue = "POST запрос должен содержать @Body параметр",
                    expectedFormat = "@Body request: RequestType"
                ))
            }
        }
        
        // Для Multipart запросов проверяем @Part параметры
        if (beforeEndpoint.contains("@Multipart")) {
            if (!endpointDefinition.contains("@Part")) {
                issues.add(ApiEndpointIssue(
                    endpoint = endpointName,
                    method = "MULTIPART",
                    issue = "Multipart запрос должен содержать @Part параметры",
                    expectedFormat = "@Part photo: MultipartBody.Part"
                ))
            }
        }
    }
    
    private fun validateReturnType(endpointDefinition: String, endpointName: String, issues: MutableList<ApiEndpointIssue>) {
        val returnTypeRegex = Regex(": (\\w+)")
        val match = returnTypeRegex.find(endpointDefinition)
        
        if (match == null) {
            issues.add(ApiEndpointIssue(
                endpoint = endpointName,
                method = "RETURN_TYPE",
                issue = "Не указан тип возвращаемого значения",
                expectedFormat = ": ResponseType"
            ))
        } else {
            val returnType = match.groupValues[1]
            // Проверяем, что тип возвращаемого значения является Response классом
            if (!returnType.endsWith("Response")) {
                issues.add(ApiEndpointIssue(
                    endpoint = endpointName,
                    method = "RETURN_TYPE",
                    issue = "Тип возвращаемого значения должен быть Response классом: $returnType",
                    expectedFormat = "${returnType}Response или подходящий Response тип"
                ))
            }
        }
    }
    
    private fun validateDataModels(content: String, issues: MutableList<ApiEndpointIssue>) {
        // Проверяем, что все Request и Response классы определены
        val requestClasses = extractClassNames(content, "Request")
        val responseClasses = extractClassNames(content, "Response")
        
        // Проверяем базовые Request классы
        val expectedRequestClasses = listOf(
            "FoodAnalysisRequest", "ImageAnalysisRequest", "MealPlanRequest",
            "NutritionRequest", "RecommendationRequest", "LogFoodRequest", "AiChatRequest"
        )
        
        expectedRequestClasses.forEach { className ->
            if (!requestClasses.contains(className)) {
                issues.add(ApiEndpointIssue(
                    endpoint = "DATA_MODEL",
                    method = "REQUEST",
                    issue = "Отсутствует класс данных: $className",
                    expectedFormat = "data class $className(...)"
                ))
            }
        }
        
        // Проверяем базовые Response классы
        val expectedResponseClasses = listOf(
            "FoodAnalysisResponse", "MealPlanResponse", "NutritionResponse",
            "RecommendationResponse", "LogFoodResponse", "AiChatResponse"
        )
        
        expectedResponseClasses.forEach { className ->
            if (!responseClasses.contains(className)) {
                issues.add(ApiEndpointIssue(
                    endpoint = "DATA_MODEL",
                    method = "RESPONSE",
                    issue = "Отсутствует класс данных: $className",
                    expectedFormat = "data class $className(...)"
                ))
            }
        }
    }
    
    private fun extractClassNames(content: String, suffix: String): List<String> {
        val classRegex = Regex("data class (\\w*$suffix)\\(")
        return classRegex.findAll(content).map { it.groupValues[1] }.toList()
    }
}