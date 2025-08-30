package com.example.calorietracker.network

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Protocol
import java.util.concurrent.TimeUnit
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

data class MakeWebhookResult(
    val httpCode: Int,
    val body: String
)

class MakeWebhookClient @Inject constructor(
    private val client: OkHttpClient
) {
    // Use base client; let OkHttp negotiate HTTP/2 when possible
    private val tunedClient: OkHttpClient by lazy {
        client.newBuilder()
            .callTimeout(120, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }
    private fun webhookUrl(webhookId: String): String =
        "https://hook.us2.make.com/$webhookId"

    fun postMultipartPhoto(
        webhookId: String,
        photoFile: File,
        userProfileJson: String,
        userId: String,
        caption: String,
        messageType: String,
        isFirstMessageOfDay: Boolean
    ): MakeWebhookResult {
        val photoBody = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            // Match previously working Make payload: binary part named "photo"
            .addFormDataPart("photo", photoFile.name, photoBody)
            // userProfile as plain-text JSON string
            .addFormDataPart("userProfile", userProfileJson)
            .addFormDataPart("note", caption)
            .addFormDataPart("userId", null,
                userId.toRequestBody("text/plain".toMediaType()))
            .addFormDataPart("caption", null,
                caption.toRequestBody("text/plain".toMediaType()))
            .addFormDataPart("messageType", null,
                messageType.toRequestBody("text/plain".toMediaType()))
            .addFormDataPart("isFirstMessageOfDay", null,
                isFirstMessageOfDay.toString().toRequestBody("text/plain".toMediaType()))
            .build()

        val req = Request.Builder()
            .url(webhookUrl(webhookId))
            .post(multipart)
            .header("Accept", "application/json")
            .header("User-Agent", "curl/8.5.0")
            .header("Expect", "100-continue")
            .build()

        tunedClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            Log.d("MakeWebhookClient", "photo resp: code=${resp.code} len=${body.length}")
            return MakeWebhookResult(httpCode = resp.code, body = body)
        }
    }

    fun postImageBase64(
        webhookId: String,
        imageBase64: String,
        userProfile: UserProfileData,
        caption: String,
        messageType: String,
        isFirstMessageOfDay: Boolean
    ): MakeWebhookResult {
        val json = JSONObject().apply {
            put("imageBase64", imageBase64)
            put("userProfile", JSONObject().apply {
                put("age", userProfile.age)
                put("weight", userProfile.weight)
                put("height", userProfile.height)
                put("gender", userProfile.gender)
                put("activityLevel", userProfile.activityLevel)
                put("goal", userProfile.goal)
            })
            put("caption", caption)
            put("messageType", messageType)
            put("isFirstMessageOfDay", isFirstMessageOfDay)
        }
        val req = Request.Builder()
            .url(webhookUrl(webhookId))
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .header("User-Agent", "okhttp")
            .build()
        tunedClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            Log.d("MakeWebhookClient", "base64 resp: code=${resp.code} len=${body.length}")
            return MakeWebhookResult(httpCode = resp.code, body = body)
        }
    }

    fun postImageUrl(
        webhookId: String,
        imageUrl: String,
        userProfile: UserProfileData,
        caption: String,
        messageType: String,
        isFirstMessageOfDay: Boolean
    ): MakeWebhookResult {
        val json = JSONObject().apply {
            put("imageUrl", imageUrl)
            put("userProfile", JSONObject().apply {
                put("age", userProfile.age)
                put("weight", userProfile.weight)
                put("height", userProfile.height)
                put("gender", userProfile.gender)
                put("activityLevel", userProfile.activityLevel)
                put("goal", userProfile.goal)
            })
            put("caption", caption)
            put("messageType", messageType)
            put("isFirstMessageOfDay", isFirstMessageOfDay)
        }
        val req = Request.Builder()
            .url(webhookUrl(webhookId))
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .header("User-Agent", "okhttp")
            .build()
        tunedClient.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            Log.d("MakeWebhookClient", "url resp: code=${resp.code} len=${body.length}")
            return MakeWebhookResult(httpCode = resp.code, body = body)
        }
    }
}
