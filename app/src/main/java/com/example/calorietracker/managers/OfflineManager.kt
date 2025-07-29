package com.example.calorietracker.managers

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppMode {
    ONLINE,
    LOADING,
    OFFLINE
}

class OfflineManager(
    private val networkManager: NetworkManager,
    private val scope: CoroutineScope
) {
    companion object {
        private const val LOADING_TIMEOUT_MS = 5000L // 5 секунд
    }
    
    private val _appMode = MutableStateFlow(AppMode.LOADING)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()
    
    private var loadingJob: Job? = null
    private var networkObserverJob: Job? = null
    
    init {
        startNetworkObserver()
    }
    
    private fun startNetworkObserver() {
        networkObserverJob = scope.launch {
            networkManager.isOnline.collect { isOnline ->
                when {
                    isOnline -> {
                        // Если есть интернет - переключаемся в онлайн режим
                        _appMode.value = AppMode.ONLINE
                        loadingJob?.cancel()
                    }
                    _appMode.value == AppMode.ONLINE -> {
                        // Если был онлайн, а интернет пропал - переходим в офлайн
                        _appMode.value = AppMode.OFFLINE
                    }
                }
            }
        }
    }
    
    fun startLoadingWithTimeout(onTimeout: () -> Unit = {}) {
        _appMode.value = AppMode.LOADING
        
        loadingJob?.cancel()
        loadingJob = scope.launch {
            delay(LOADING_TIMEOUT_MS)
            
            // Если через 5 секунд все еще загружаемся - переходим в офлайн режим
            if (_appMode.value == AppMode.LOADING) {
                _appMode.value = AppMode.OFFLINE
                onTimeout()
            }
        }
    }
    
    fun stopLoading() {
        loadingJob?.cancel()
        _appMode.value = if (networkManager.isOnline.value) AppMode.ONLINE else AppMode.OFFLINE
    }
    
    fun forceOfflineMode() {
        loadingJob?.cancel()
        _appMode.value = AppMode.OFFLINE
    }
    
    fun cleanup() {
        loadingJob?.cancel()
        networkObserverJob?.cancel()
    }
}
