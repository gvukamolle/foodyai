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
    // Таймаут отключён, приложение остаётся в режиме LOADING до появления сети

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

    fun startLoading() {
        // Переходим в режим загрузки без таймаута. Смена режима произойдёт при изменении состояния сети
        _appMode.value = AppMode.LOADING
        
        loadingJob?.cancel()
        loadingJob = null
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
