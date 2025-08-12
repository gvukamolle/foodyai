package com.example.calorietracker.presentation.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.ChatMessage
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.common.DateRange
import com.example.calorietracker.domain.usecases.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for chat and AI interactions
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val getChatHistoryUseCase: GetChatHistoryUseCase,
    private val processAIResponseUseCase: ProcessAIResponseUseCase,
    private val validateAIUsageLimitsUseCase: ValidateAIUsageLimitsUseCase
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // Chat messages
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    // Current input
    var currentInput by mutableStateOf("")
    var isProcessing by mutableStateOf(false)
    
    // AI usage info
    private val _aiUsageInfo = MutableStateFlow<AIUsageValidation?>(null)
    val aiUsageInfo: StateFlow<AIUsageValidation?> = _aiUsageInfo.asStateFlow()
    
    init {
        loadChatHistory()
        checkAIUsageLimits()
    }
    
    /**
     * Load chat history
     */
    fun loadChatHistory() {
        viewModelScope.launch {
            updateUiState { copy(isLoading = true) }
            
            when (val result = getChatHistoryUseCase(
                GetChatHistoryUseCase.Params(limit = 50)
            )) {
                is Result.Success -> {
                    _messages.value = result.data
                    updateUiState { copy(isLoading = false) }
                }
                is Result.Error -> {
                    updateUiState { 
                        copy(
                            isLoading = false,
                            error = "Failed to load chat history: ${result.exception.message}"
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Send a chat message
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) return
        
        viewModelScope.launch {
            isProcessing = true
            updateUiState { copy(isLoading = true) }
            
            // Create user message
            val userMessage = ChatMessage.createUserMessage(content)
            
            // Add user message to list
            _messages.value = _messages.value + userMessage
            
            // Send message and get AI response
            when (val result = sendChatMessageUseCase(
                SendChatMessageUseCase.Params(userMessage)
            )) {
                is Result.Success -> {
                    // Add AI response to messages
                    _messages.value = _messages.value + result.data
                    
                    // Process AI response for food extraction
                    processAIResponse(result.data.content)
                    
                    updateUiState { copy(isLoading = false) }
                }
                is Result.Error -> {
                    // Add error message
                    val errorMessage = ChatMessage.createAIResponse(
                        "Sorry, I couldn't process your message: ${result.exception.message}"
                    ).markAsError()
                    
                    _messages.value = _messages.value + errorMessage
                    
                    updateUiState { 
                        copy(
                            isLoading = false,
                            error = "Failed to send message: ${result.exception.message}"
                        )
                    }
                }
            }
            
            isProcessing = false
            currentInput = ""
        }
    }
    
    /**
     * Process AI response for food information
     */
    private fun processAIResponse(response: String) {
        viewModelScope.launch {
            when (val result = processAIResponseUseCase(
                ProcessAIResponseUseCase.Params(response)
            )) {
                is Result.Success -> {
                    val processedResponse = result.data
                    if (processedResponse.hasFoodData() && processedResponse.isReliable()) {
                        // Add food confirmation message
                        val foodConfirmation = ChatMessage.createFoodConfirmation(
                            processedResponse.extractedFood!!
                        )
                        _messages.value = _messages.value + foodConfirmation
                    }
                }
                is Result.Error -> {
                    // Ignore processing errors - the original response is still shown
                }
            }
        }
    }
    
    /**
     * Check AI usage limits
     */
    fun checkAIUsageLimits() {
        viewModelScope.launch {
            when (val result = validateAIUsageLimitsUseCase(
                ValidateAIUsageLimitsUseCase.Params(
                    com.example.calorietracker.domain.repositories.AIOperationType.CHAT_RESPONSE
                )
            )) {
                is Result.Success -> {
                    _aiUsageInfo.value = result.data
                }
                is Result.Error -> {
                    updateUiState { 
                        copy(error = "Failed to check AI limits: ${result.exception.message}")
                    }
                }
            }
        }
    }
    
    /**
     * Retry failed message
     */
    fun retryMessage(messageId: String) {
        viewModelScope.launch {
            val message = _messages.value.find { it.id == messageId }
            if (message != null && message.canRetry()) {
                val retryMessage = message.createRetry()
                sendMessage(retryMessage.content)
            }
        }
    }
    
    /**
     * Clear chat history
     */
    fun clearChatHistory() {
        _messages.value = emptyList()
    }
    
    /**
     * Add welcome message
     */
    fun addWelcomeMessage(content: String) {
        val welcomeMessage = ChatMessage.createWelcome(content)
        _messages.value = listOf(welcomeMessage) + _messages.value
    }
    
    /**
     * Check if AI can be used
     */
    fun canUseAI(): Boolean {
        return _aiUsageInfo.value?.canProceed ?: true
    }
    
    /**
     * Get remaining AI usage
     */
    fun getRemainingAIUsage(): Int {
        return _aiUsageInfo.value?.getRemainingDailyUsage() ?: 0
    }
    
    /**
     * Clear error messages
     */
    fun clearError() {
        updateUiState { copy(error = null) }
    }
    
    // Helper function to update UI state
    private fun updateUiState(update: ChatUiState.() -> ChatUiState) {
        _uiState.value = _uiState.value.update()
    }
}

/**
 * UI State for Chat screen
 */
data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConnected: Boolean = true
)