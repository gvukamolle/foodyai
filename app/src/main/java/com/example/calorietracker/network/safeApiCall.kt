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