package com.example.calorietracker.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to diagnose network connectivity issues
 * Helps identify if the problem is DNS, SSL, or DPI related
 */
@Singleton
class NetworkDiagnosticHelper @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "NetworkDiagnostic"
        private const val MAKE_COM_HOST = "hook.us2.make.com"
        private const val WEBHOOK_ID = "653st2c10rmg92nlltf3y0m8sggxaac6"
    }
    
    data class DiagnosticResult(
        val dnsResolvable: Boolean,
        val dnsIP: String? = null,
        val dnsOverHttpsWorks: Boolean = false,
        val directIPAccessWorks: Boolean = false,
        val httpsConnectivityOk: Boolean = false,
        val makeComReachable: Boolean = false,
        val suspectedIssue: String? = null
    )
    
    suspend fun runFullDiagnostic(): DiagnosticResult = withContext(Dispatchers.IO) {
        var result = DiagnosticResult(dnsResolvable = false)
        
        // 1. Test DNS resolution
        val dnsIP = testDNSResolution()
        result = result.copy(
            dnsResolvable = dnsIP != null,
            dnsIP = dnsIP
        )
        
        // 2. Test DNS-over-HTTPS
        val dohIP = testDNSOverHTTPS()
        result = result.copy(dnsOverHttpsWorks = dohIP != null)
        
        // 3. Test direct IP access if we have an IP
        val testIP = dnsIP ?: dohIP ?: "35.174.94.163" // Fallback to known IP
        if (testIP != null) {
            val ipWorks = testDirectIPAccess(testIP)
            result = result.copy(directIPAccessWorks = ipWorks)
        }
        
        // 4. Test general HTTPS connectivity
        val httpsWorks = testGeneralHTTPS()
        result = result.copy(httpsConnectivityOk = httpsWorks)
        
        // 5. Test Make.com with different methods
        val makeReachable = testMakeComConnectivity()
        result = result.copy(makeComReachable = makeReachable)
        
        // Analyze results
        val issue = when {
            !result.dnsResolvable && result.dnsOverHttpsWorks -> "DNS блокировка оператором"
            !result.dnsResolvable && !result.dnsOverHttpsWorks -> "Полная блокировка DNS"
            result.dnsResolvable && !result.makeComReachable -> "DPI блокировка webhook трафика"
            result.directIPAccessWorks && !result.makeComReachable -> "SNI блокировка"
            !result.httpsConnectivityOk -> "Общие проблемы с HTTPS"
            else -> null
        }
        
        result.copy(suspectedIssue = issue)
    }
    
    private suspend fun testDNSResolution(): String? {
        return try {
            val addresses = InetAddress.getAllByName(MAKE_COM_HOST)
            addresses.firstOrNull()?.hostAddress.also {
                Log.d(TAG, "DNS resolution successful: $it")
            }
        } catch (e: Exception) {
            Log.e(TAG, "DNS resolution failed", e)
            null
        }
    }
    
    private suspend fun testDNSOverHTTPS(): String? {
        return try {
            val url = "https://cloudflare-dns.com/dns-query?name=$MAKE_COM_HOST&type=A"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/dns-json")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val answers = json.optJSONArray("Answer")
                if (answers != null && answers.length() > 0) {
                    val ip = answers.getJSONObject(0).getString("data")
                    Log.d(TAG, "DoH resolution successful: $ip")
                    return ip
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "DoH resolution failed", e)
            null
        }
    }
    
    private suspend fun testDirectIPAccess(ip: String): Boolean {
        return try {
            val url = URL("https://$ip/")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                setRequestProperty("Host", MAKE_COM_HOST)
                connectTimeout = 5000
                readTimeout = 5000
                instanceFollowRedirects = false
            }
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Direct IP access response: $responseCode")
            responseCode in 200..599 // Any response means connection worked
        } catch (e: Exception) {
            Log.e(TAG, "Direct IP access failed", e)
            false
        }
    }
    
    private suspend fun testGeneralHTTPS(): Boolean {
        return try {
            val testUrls = listOf(
                "https://www.google.com",
                "https://cloudflare.com"
            )
            
            testUrls.any { url ->
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.responseCode in 200..299
                } catch (e: Exception) {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "General HTTPS test failed", e)
            false
        }
    }
    
    private suspend fun testMakeComConnectivity(): Boolean {
        return try {
            // Simple HEAD request to test connectivity
            val url = "https://$MAKE_COM_HOST/$WEBHOOK_ID"
            val request = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            Log.d(TAG, "Make.com connectivity test: ${response.code}")
            true // Any response means we can reach the server
        } catch (e: Exception) {
            Log.e(TAG, "Make.com connectivity test failed", e)
            false
        }
    }
    
    /**
     * Test sending a minimal JSON request to Make.com
     * Uses browser-like headers to avoid DPI detection
     */
    suspend fun testMinimalRequest(): Boolean = withContext(Dispatchers.IO) {
        try {
            val testData = JSONObject().apply {
                put("test", true)
                put("timestamp", System.currentTimeMillis())
            }
            
            val url = "https://$MAKE_COM_HOST/$WEBHOOK_ID"
            val connection = URL(url).openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                setRequestProperty("Accept", "*/*")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                connectTimeout = 15000
                readTimeout = 15000
            }
            
            connection.outputStream.use { it.write(testData.toString().toByteArray()) }
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Minimal test request response: $responseCode")
            
            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Minimal test request failed", e)
            false
        }
    }
}
