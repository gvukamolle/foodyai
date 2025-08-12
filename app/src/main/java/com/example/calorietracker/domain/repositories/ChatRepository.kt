package com.example.calorietracker.domain.repositories

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.ChatMessage
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.common.DateRange

/**
 * Repository interface for chat and AI interaction operations
 */
interface ChatRepository {
    
    /**
     * Send a message and get AI response
     */
    suspend fun sendMessage(message: ChatMessage): Result<ChatMessage>
    
    /**
     * Get chat history for a date range
     */
    suspend fun getChatHistory(dateRange: DateRange): Result<List<ChatMessage>>
    
    /**
     * Get recent chat messages
     */
    suspend fun getRecentMessages(limit: Int = 50): Result<List<ChatMessage>>
    
    /**
     * Save chat message
     */
    suspend fun saveChatMessage(message: ChatMessage): Result<Unit>
    
    /**
     * Delete chat message
     */
    suspend fun deleteChatMessage(messageId: String): Result<Unit>
    
    /**
     * Clear chat history
     */
    suspend fun clearChatHistory(): Result<Unit>
    
    /**
     * Process AI response for food analysis
     */
    suspend fun processAIFoodAnalysis(response: String): Result<Food>
    
    /**
     * Check AI usage limits
     */
    suspend fun checkAIUsageLimits(): Result<AIUsageInfo>
    
    /**
     * Record AI usage
     */
    suspend fun recordAIUsage(operationType: AIOperationType): Result<Unit>
    
    /**
     * Get AI conversation context
     */
    suspend fun getConversationContext(): Result<List<ChatMessage>>
    
    /**
     * Update conversation context
     */
    suspend fun updateConversationContext(messages: List<ChatMessage>): Result<Unit>
    
    /**
     * Retry failed message
     */
    suspend fun retryMessage(messageId: String): Result<ChatMessage>
}

/**
 * AI usage information
 */
data class AIUsageInfo(
    val dailyUsage: Int,
    val dailyLimit: Int,
    val monthlyUsage: Int,
    val monthlyLimit: Int,
    val canUseAI: Boolean,
    val resetTime: java.time.LocalDateTime
)

/**
 * Types of AI operations for usage tracking
 */
enum class AIOperationType {
    PHOTO_ANALYSIS,
    TEXT_ANALYSIS,
    CHAT_RESPONSE,
    NUTRITION_ADVICE,
    RECIPE_ANALYSIS
}