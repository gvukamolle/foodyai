package com.example.calorietracker.validation

import com.example.calorietracker.validation.models.SerializationResult
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.File
import java.lang.reflect.Type

/**
 * Валидатор JSON сериализации для проверки совместимости data классов с Gson
 */
class JsonSerializationValidator {
    
    private val gson = Gson()
    
    /**
     * Валидирует все request/response data классы на совместимость с Gson
     */
    fun validateJsonSerialization(projectPath: String): SerializationResult {
        val issues = mutableListOf<String>()
        val makeServicePath = "$projectPath/app/src/main/java/com/example/calorietracker/network/MakeService.kt"
        
        val makeServiceFile = File(makeServicePath)
        if (!makeServiceFile.exists()) {
            issues.add("MakeService.kt файл не найден")
            return SerializationResult(issues, false)
        }
        
        val content = makeServiceFile.readText()
        
        // Извлекаем все data классы
        val dataClasses = extractDataClasses(content)
        
        // Проверяем каждый data класс
        dataClasses.forEach { dataClass ->
            validateDataClass(dataClass, issues)
        }
        
        // Проверяем специфичные для JSON проблемы
        validateJsonSpecificIssues(content, issues)
        
        return SerializationResult(issues, issues.isEmpty())
    }
    
    private fun extractDataClasses(content: String): List<DataClassInfo> {
        val dataClasses = mutableListOf<DataClassInfo>()
        val dataClassRegex = Regex("data class (\\w+)\\(([^)]+)\\)")
        
        dataClassRegex.findAll(content).forEach { match ->
            val className = match.groupValues[1]
            val parameters = match.groupValues[2]
            val fields = extractFields(parameters)
            
            dataClasses.add(DataClassInfo(className, fields))
        }
        
        return dataClasses
    }
    
    private fun extractFields(parameters: String): List<FieldInfo> {
        val fields = mutableListOf<FieldInfo>()
        
        // Разбираем параметры конструктора
        val fieldRegex = Regex("(val|var)\\s+(\\w+):\\s*([^,=]+)(?:\\s*=\\s*([^,]+))?")
        
        fieldRegex.findAll(parameters).forEach { match ->
            val name = match.groupValues[2]
            val type = match.groupValues[3].trim()
            val defaultValue = match.groupValues[4].takeIf { it.isNotBlank() }
            val isNullable = type.endsWith("?")
            
            fields.add(FieldInfo(name, type, isNullable, defaultValue))
        }
        
        return fields
    }
    
    private fun validateDataClass(dataClass: DataClassInfo, issues: MutableList<String>) {
        // Проверяем naming convention для JSON полей
        validateFieldNaming(dataClass, issues)
        
        // Проверяем nullable поля
        validateNullableFields(dataClass, issues)
        
        // Проверяем типы данных
        validateFieldTypes(dataClass, issues)
        
        // Проверяем default значения
        validateDefaultValues(dataClass, issues)
        
        // Проверяем специфичные для API классы
        validateApiSpecificRules(dataClass, issues)
    }
    
    private fun validateFieldNaming(dataClass: DataClassInfo, issues: MutableList<String>) {
        dataClass.fields.forEach { field ->
            // Проверяем camelCase naming
            if (!field.name.matches(Regex("^[a-z][a-zA-Z0-9]*$"))) {
                issues.add("${dataClass.name}.${field.name}: Поле должно использовать camelCase naming convention")
            }
            
            // Проверяем, что boolean поля не начинаются с 'is'
            if (field.type.contains("Boolean") && field.name.startsWith("is")) {
                issues.add("${dataClass.name}.${field.name}: Boolean поля не должны начинаться с 'is' для правильной JSON сериализации")
            }
        }
    }
    
    private fun validateNullableFields(dataClass: DataClassInfo, issues: MutableList<String>) {
        dataClass.fields.forEach { field ->
            // Для Response классов проверяем, что поля nullable где это уместно
            if (dataClass.name.endsWith("Response")) {
                if (!field.isNullable && field.defaultValue == null && !isRequiredResponseField(field.name)) {
                    issues.add("${dataClass.name}.${field.name}: Response поле должно быть nullable или иметь default значение")
                }
            }
            
            // Для Request классов проверяем обязательные поля
            if (dataClass.name.endsWith("Request")) {
                if (isRequiredRequestField(dataClass.name, field.name) && field.isNullable && field.defaultValue == null) {
                    issues.add("${dataClass.name}.${field.name}: Обязательное Request поле не должно быть nullable без default значения")
                }
            }
        }
    }
    
    private fun validateFieldTypes(dataClass: DataClassInfo, issues: MutableList<String>) {
        dataClass.fields.forEach { field ->
            val cleanType = field.type.replace("?", "").trim()
            
            // Проверяем поддерживаемые Gson типы
            if (!isSupportedGsonType(cleanType)) {
                issues.add("${dataClass.name}.${field.name}: Тип '$cleanType' может не поддерживаться Gson без кастомного адаптера")
            }
            
            // Проверяем коллекции
            if (cleanType.startsWith("List<") || cleanType.startsWith("Map<")) {
                validateCollectionType(dataClass.name, field, issues)
            }
            
            // Проверяем числовые типы для API
            if (isNumericApiField(field.name) && !isNumericType(cleanType)) {
                issues.add("${dataClass.name}.${field.name}: Поле '${field.name}' должно быть числовым типом")
            }
        }
    }
    
    private fun validateDefaultValues(dataClass: DataClassInfo, issues: MutableList<String>) {
        dataClass.fields.forEach { field ->
            field.defaultValue?.let { defaultValue ->
                // Проверяем корректность default значений
                if (!isValidDefaultValue(field.type, defaultValue)) {
                    issues.add("${dataClass.name}.${field.name}: Некорректное default значение '$defaultValue' для типа '${field.type}'")
                }
                
                // Проверяем, что default значения не конфликтуют с API
                if (dataClass.name.endsWith("Request") && isApiRequiredField(field.name) && isEmptyDefault(defaultValue)) {
                    issues.add("${dataClass.name}.${field.name}: Default значение для обязательного API поля не должно быть пустым")
                }
            }
        }
    }
    
    private fun validateApiSpecificRules(dataClass: DataClassInfo, issues: MutableList<String>) {
        when {
            dataClass.name.contains("UserProfile") -> validateUserProfileClass(dataClass, issues)
            dataClass.name.contains("Food") -> validateFoodClass(dataClass, issues)
            dataClass.name.endsWith("Response") -> validateResponseClass(dataClass, issues)
            dataClass.name.endsWith("Request") -> validateRequestClass(dataClass, issues)
        }
    }
    
    private fun validateUserProfileClass(dataClass: DataClassInfo, issues: MutableList<String>) {
        val requiredFields = listOf("age", "weight", "height", "gender", "activityLevel", "goal")
        
        requiredFields.forEach { requiredField ->
            if (!dataClass.fields.any { it.name == requiredField }) {
                issues.add("${dataClass.name}: Отсутствует обязательное поле '$requiredField'")
            }
        }
        
        // Проверяем типы специфичных полей
        dataClass.fields.forEach { field ->
            when (field.name) {
                "age", "weight", "height" -> {
                    if (!field.type.contains("Int")) {
                        issues.add("${dataClass.name}.${field.name}: Должно быть типа Int")
                    }
                }
                "gender", "activityLevel", "goal" -> {
                    if (!field.type.contains("String")) {
                        issues.add("${dataClass.name}.${field.name}: Должно быть типа String")
                    }
                }
            }
        }
    }
    
    private fun validateFoodClass(dataClass: DataClassInfo, issues: MutableList<String>) {
        val nutritionFields = listOf("calories", "protein", "fat", "carbs")
        
        nutritionFields.forEach { nutritionField ->
            val field = dataClass.fields.find { it.name == nutritionField }
            if (field != null) {
                val cleanType = field.type.replace("?", "").trim()
                if (nutritionField == "calories" && !cleanType.contains("Int")) {
                    issues.add("${dataClass.name}.${field.name}: Калории должны быть типа Int")
                } else if (nutritionField != "calories" && !isNumericType(cleanType)) {
                    issues.add("${dataClass.name}.${field.name}: Питательные вещества должны быть числовыми типами")
                }
            }
        }
    }
    
    private fun validateResponseClass(dataClass: DataClassInfo, issues: MutableList<String>) {
        // Все Response классы должны иметь поле status
        if (!dataClass.fields.any { it.name == "status" }) {
            issues.add("${dataClass.name}: Response класс должен содержать поле 'status'")
        }
        
        // Проверяем, что поле status имеет правильный тип
        val statusField = dataClass.fields.find { it.name == "status" }
        if (statusField != null && !statusField.type.contains("String")) {
            issues.add("${dataClass.name}.status: Поле status должно быть типа String")
        }
    }
    
    private fun validateRequestClass(dataClass: DataClassInfo, issues: MutableList<String>) {
        // Проверяем, что Request классы содержат необходимые поля для API
        when (dataClass.name) {
            "FoodAnalysisRequest" -> {
                val requiredFields = listOf("userProfile", "message", "userId")
                requiredFields.forEach { field ->
                    if (!dataClass.fields.any { it.name == field }) {
                        issues.add("${dataClass.name}: Отсутствует обязательное поле '$field'")
                    }
                }
            }
            "LogFoodRequest" -> {
                val requiredFields = listOf("userId", "foodData", "userProfile", "timestamp")
                requiredFields.forEach { field ->
                    if (!dataClass.fields.any { it.name == field }) {
                        issues.add("${dataClass.name}: Отсутствует обязательное поле '$field'")
                    }
                }
            }
        }
    }
    
    private fun validateJsonSpecificIssues(content: String, issues: MutableList<String>) {
        // Проверяем использование @SerializedName аннотаций (если есть)
        if (content.contains("@SerializedName")) {
            val serializedNameRegex = Regex("@SerializedName\\(\"([^\"]+)\"\\)")
            serializedNameRegex.findAll(content).forEach { match ->
                val jsonName = match.groupValues[1]
                if (!isValidJsonFieldName(jsonName)) {
                    issues.add("@SerializedName: Некорректное имя JSON поля '$jsonName'")
                }
            }
        }
        
        // Проверяем потенциальные проблемы с циклическими ссылками
        validateCircularReferences(content, issues)
    }
    
    private fun validateCircularReferences(content: String, issues: MutableList<String>) {
        val dataClasses = extractDataClasses(content)
        val classNames = dataClasses.map { it.name }.toSet()
        
        dataClasses.forEach { dataClass ->
            dataClass.fields.forEach { field ->
                val fieldType = field.type.replace("?", "").replace("List<", "").replace(">", "").trim()
                
                // Проверяем самоссылки
                if (fieldType == dataClass.name) {
                    issues.add("${dataClass.name}.${field.name}: Обнаружена потенциальная циклическая ссылка (самоссылка)")
                }
                
                // Проверяем взаимные ссылки (упрощенная проверка)
                if (classNames.contains(fieldType)) {
                    val referencedClass = dataClasses.find { it.name == fieldType }
                    referencedClass?.fields?.forEach { refField ->
                        val refFieldType = refField.type.replace("?", "").replace("List<", "").replace(">", "").trim()
                        if (refFieldType == dataClass.name) {
                            issues.add("${dataClass.name} <-> $fieldType: Обнаружена потенциальная взаимная циклическая ссылка")
                        }
                    }
                }
            }
        }
    }
    
    // Вспомогательные методы
    private fun isSupportedGsonType(type: String): Boolean {
        val supportedTypes = setOf(
            "String", "Int", "Long", "Double", "Float", "Boolean",
            "List", "Map", "Array"
        )
        
        return supportedTypes.any { type.contains(it) } || 
               type.startsWith("List<") || 
               type.startsWith("Map<") ||
               type.endsWith("Data") ||
               type.endsWith("Request") ||
               type.endsWith("Response")
    }
    
    private fun validateCollectionType(className: String, field: FieldInfo, issues: MutableList<String>) {
        if (field.type.startsWith("List<") && !field.type.contains(">")) {
            issues.add("$className.${field.name}: Некорректное определение List типа")
        }
        
        if (field.type.startsWith("Map<") && !field.type.contains(">")) {
            issues.add("$className.${field.name}: Некорректное определение Map типа")
        }
    }
    
    private fun isNumericApiField(fieldName: String): Boolean {
        return fieldName in setOf("calories", "weight", "age", "height", "timestamp")
    }
    
    private fun isNumericType(type: String): Boolean {
        return type in setOf("Int", "Long", "Double", "Float")
    }
    
    private fun isRequiredResponseField(fieldName: String): Boolean {
        return fieldName in setOf("status")
    }
    
    private fun isRequiredRequestField(className: String, fieldName: String): Boolean {
        return when (className) {
            "FoodAnalysisRequest" -> fieldName in setOf("userProfile", "message", "userId")
            "LogFoodRequest" -> fieldName in setOf("userId", "foodData", "userProfile")
            "AiChatRequest" -> fieldName in setOf("message", "userProfile", "userId")
            else -> false
        }
    }
    
    private fun isValidDefaultValue(type: String, defaultValue: String): Boolean {
        val cleanType = type.replace("?", "").trim()
        
        return when (cleanType) {
            "String" -> defaultValue.startsWith("\"") && defaultValue.endsWith("\"")
            "Int", "Long" -> defaultValue.toIntOrNull() != null
            "Double", "Float" -> defaultValue.toDoubleOrNull() != null
            "Boolean" -> defaultValue in setOf("true", "false")
            else -> true // Для сложных типов не проверяем
        }
    }
    
    private fun isApiRequiredField(fieldName: String): Boolean {
        return fieldName in setOf("userId", "userProfile", "message", "foodData")
    }
    
    private fun isEmptyDefault(defaultValue: String): Boolean {
        return defaultValue in setOf("\"\"", "null", "emptyList()", "emptyMap()")
    }
    
    private fun isValidJsonFieldName(jsonName: String): Boolean {
        return jsonName.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))
    }
    
    // Data классы для внутреннего использования
    private data class DataClassInfo(
        val name: String,
        val fields: List<FieldInfo>
    )
    
    private data class FieldInfo(
        val name: String,
        val type: String,
        val isNullable: Boolean,
        val defaultValue: String?
    )
}