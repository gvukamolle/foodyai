package com.example.calorietracker.network

import android.util.Log

suspend fun <T> safeApiCall(
    apiCall: suspend () -> T
): Result<T> {
    return try {
        Result.success(apiCall())
    } catch (e: Exception) {
        Log.e("Network", "API call failed", e)
        Result.failure(e)
    }
}