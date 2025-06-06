package com.example.calorietracker.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

data class NetworkStatus(
    val isOnline: Boolean = false,
    val makeComAvailable: Boolean = false,
    val lastChecked: Long = 0L
)

@Singleton
class NetworkMonitor @Inject constructor(
    private val context: Context,
    private val makeService: MakeService
) {
    private val _networkStatus = MutableStateFlow(NetworkStatus())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var healthCheckJob: Job? = null

    init {
        startNetworkMonitoring()
        startHealthChecking()
    }

    private fun updateNetworkStatus(isOnline: Boolean) {
        Log.d("NetworkMonitor", "updateNetworkStatus called with isOnline=$isOnline")
        scope.launch {
            val currentStatus = _networkStatus.value
            if (currentStatus.isOnline != isOnline) {
                Log.d("NetworkMonitor", "Network status actually changed to $isOnline")
                _networkStatus.value = currentStatus.copy(
                    isOnline = isOnline,
                    lastChecked = System.currentTimeMillis()
                )
                if (isOnline) {
                    checkMakeComAvailability()
                } else {
                    _networkStatus.value = _networkStatus.value.copy(
                        makeComAvailable = false
                    )
                }
            }
        }
    }

    private fun startNetworkMonitoring() {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateNetworkStatus(true)
            }

            override fun onLost(network: Network) {
                updateNetworkStatus(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                updateNetworkStatus(hasInternet)
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun startHealthChecking() {
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch {
            while (isActive) {
                if (_networkStatus.value.isOnline) {
                    checkMakeComAvailability()
                }
                delay(30_000) // Check every 30 seconds
            }
        }
    }

    private suspend fun checkMakeComAvailability() {
        try {
            withTimeout(5000) {
                val response = makeService.checkHealth()
                val isAvailable = response.status == "ok"

                _networkStatus.value = _networkStatus.value.copy(
                    makeComAvailable = isAvailable,
                    lastChecked = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            _networkStatus.value = _networkStatus.value.copy(
                makeComAvailable = false,
                lastChecked = System.currentTimeMillis()
            )
        }
    }

    fun isMakeComAvailable(): Boolean = _networkStatus.value.makeComAvailable

    fun isOnline(): Boolean = _networkStatus.value.isOnline

    fun cleanup() {
        healthCheckJob?.cancel()
        scope.cancel()
    }
}
