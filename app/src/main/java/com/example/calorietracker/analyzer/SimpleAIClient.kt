package com.example.calorietracker.analyzer

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

// Простой результат анализа
data class AIFoodResult(
    val success: Boolean,
    val foodName: String = "",
    val calories: Int = 0,
    val proteins: Int = 0,
    val fats: Int = 0,
    val carbs: Int = 0,
    val weight: Int = 100,
    val recommendations: String = "",
    val errorMessage: String = ""
)

class SimpleAIClient {
    companion object {
        // ВАЖНО: Замените на ваш webhook ID из Make.com
        private const val WEBHOOK_ID = "653st2c10rmg92nlltf3y0m8sggxaac6"
        private const val MAKE_URL = "https://hook.us2.make.com/653st2c10rmg92nlltf3y0m8sggxaac6"
    }

    // Проверка доступности интернета
    suspend fun checkInternetConnection(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL("https://www.google.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = 3000
                connect()
            }
            connection.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    // Анализ фото через Make.com -> OpenAI
    suspend fun analyzeFood(bitmap: Bitmap): AIFoodResult = withContext(Dispatchers.IO) {
        try {
            // 1. Проверяем интернет
            if (!checkInternetConnection()) {
                return@withContext AIFoodResult(
                    success = false,
                    errorMessage = "Нет подключения к интернету"
                )
            }

            // 2. Конвертируем изображение в Base64
            val base64Image = bitmapToBase64(bitmap)

            // 3. Создаем JSON запрос
            val requestJson = JSONObject().apply {
                put("image", base64Image)
                put("request_type", "analyze_food")
            }

            // 4. Отправляем запрос
            val url = URL(MAKE_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 30000
            }

            // Отправляем данные
            connection.outputStream.use { outputStream ->
                outputStream.write(requestJson.toString().toByteArray())
            }

            // 5. Читаем ответ
            val responseCode = connection.responseCode
            if (responseCode != 200) {
                return@withContext AIFoodResult(
                    success = false,
                    errorMessage = "Ошибка сервера: $responseCode"
                )
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }

            // 6. Парсим JSON ответ от Make.com
            val jsonResponse = JSONObject(response)

            return@withContext AIFoodResult(
                success = true,
                foodName = jsonResponse.optString("food_name", "Неизвестный продукт"),
                calories = jsonResponse.optInt("calories", 0),
                proteins = jsonResponse.optInt("proteins", 0),
                fats = jsonResponse.optInt("fats", 0),
                carbs = jsonResponse.optInt("carbs", 0),
                weight = jsonResponse.optInt("weight", 100),
                recommendations = jsonResponse.optString("recommendations", "")
            )

        } catch (e: Exception) {
            Log.e("SimpleAIClient", "Error analyzing food", e)
            return@withContext AIFoodResult(
                success = false,
                errorMessage = "Ошибка анализа: ${e.message}"
            )
        }
    }

    // Простой текстовый запрос к AI (для чата)
    suspend fun askAI(question: String): String = withContext(Dispatchers.IO) {
        try {
            if (!checkInternetConnection()) {
                return@withContext "AI недоступен без интернета"
            }

            val requestJson = JSONObject().apply {
                put("question", question)
                put("request_type", "chat")
            }

            val url = URL(MAKE_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            connection.outputStream.use { outputStream ->
                outputStream.write(requestJson.toString().toByteArray())
            }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                return@withContext jsonResponse.optString("answer", "Не удалось получить ответ")
            }

            return@withContext "Ошибка связи с AI"

        } catch (e: Exception) {
            return@withContext "AI временно недоступен"
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Сжимаем изображение для уменьшения размера
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
