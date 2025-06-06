package com.example.calorietracker.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

object NetworkUtils {

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            if (network == null) {
                Log.d("NetworkUtils", "No active network")
                return false
            }

            val capabilities = connectivityManager.getNetworkCapabilities(network)
            if (capabilities == null) {
                Log.d("NetworkUtils", "No network capabilities for active network")
                return false
            }

            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            Log.d(
                "NetworkUtils",
                "NetworkCapabilities: INTERNET=$hasInternet, VALIDATED=$isValidated"
            )

            hasInternet && isValidated
        } else {
            // For legacy devices
            val networkInfo = connectivityManager.activeNetworkInfo
            val result = networkInfo != null && networkInfo.isConnected
            Log.d(
                "NetworkUtils",
                "Legacy networkInfo: $networkInfo, isConnected=$result"
            )
            result
        }
    }
}