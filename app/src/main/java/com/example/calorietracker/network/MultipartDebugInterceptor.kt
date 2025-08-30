package com.example.calorietracker.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.Response

/**
 * Logs multipart parts (names, filenames, sizes) only for Make webhook requests.
 * Avoids dumping raw image bytes.
 */
class MultipartDebugInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        if (url.host == "hook.us2.make.com") {
            val body = request.body
            if (body is MultipartBody) {
                val sb = StringBuilder()
                sb.append("Multipart parts: ")
                body.parts.forEachIndexed { idx, part ->
                    val headers = part.headers
                    val cd = headers?.get("Content-Disposition") ?: ""
                    val name = Regex("name=\"([^\"]+)\"").find(cd)?.groupValues?.get(1) ?: "?"
                    val filename = Regex("filename=\"([^\"]+)\"").find(cd)?.groupValues?.get(1)
                    val contentType = part.body.contentType()?.toString() ?: "?"
                    val length = try { part.body.contentLength() } catch (_: Exception) { -1 }
                    sb.append("#${idx+1} name=").append(name)
                        .append(", type=").append(contentType)
                        .append(", size=").append(length)
                    if (filename != null) sb.append(", filename=").append(filename)
                    if (idx < body.parts.size - 1) sb.append(" | ")
                }
                Log.d("MultipartDebug", sb.toString())
            }
        }
        return chain.proceed(request)
    }
}

