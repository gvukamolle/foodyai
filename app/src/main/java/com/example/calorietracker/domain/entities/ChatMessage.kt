package com.example.calorietracker.domain.entities

import com.example.calorietracker.domain.entities.common.MessageType
import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a chat message in the conversation
 * Note: UI-specific properties like animation states are handled in presentation layer
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val type: MessageType,
    val content: String,
    val imagePath: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val food: Food? = null,
    val isWelcome: Boolean = false,
    val inputMethod: String? = null,
    val isError: Boolean = false,
    val retryCount: Int = 0,
    val maxRetries: Int = 3
) {
    init {
        require(id.isNotBlank()) { "Message ID cannot be blank" }
        require(retryCount >= 0) { "Retry count cannot be negative" }
        require(maxRetries >= 0) { "Max retries cannot be negative" }
    }
    
    /**
     * Check if message contains food information
     */
    fun hasFood(): Boolean = food != null
    
    /**
     * Check if message is a food confirmation type
     */
    fun isFoodConfirmation(): Boolean = type == MessageType.FOOD_CONFIRMATION
    
    /**
     * Check if message can be retried
     */
    fun canRetry(): Boolean = isError && retryCount < maxRetries
    
    /**
     * Create a retry version of this message
     */
    fun createRetry(): ChatMessage {
        require(canRetry()) { "Message cannot be retried" }
        return copy(
            id = UUID.randomUUID().toString(),
            isError = false,
            retryCount = retryCount + 1,
            timestamp = LocalDateTime.now()
        )
    }
    
    /**
     * Mark message as error
     */
    fun markAsError(): ChatMessage {
        return copy(isError = true)
    }
    
    /**
     * Check if message is from user
     */
    fun isFromUser(): Boolean = type == MessageType.USER
    
    /**
     * Check if message is from AI
     */
    fun isFromAI(): Boolean = type == MessageType.AI
    
    /**
     * Get message age in minutes
     */
    fun getAgeInMinutes(): Long {
        return java.time.Duration.between(timestamp, LocalDateTime.now()).toMinutes()
    }
    
    companion object {
        /**
         * Create a welcome message
         */
        fun createWelcome(content: String): ChatMessage {
            return ChatMessage(
                type = MessageType.SYSTEM,
                content = content,
                isWelcome = true
            )
        }
        
        /**
         * Create a user message
         */
        fun createUserMessage(content: String, imagePath: String? = null): ChatMessage {
            return ChatMessage(
                type = MessageType.USER,
                content = content,
                imagePath = imagePath
            )
        }
        
        /**
         * Create an AI response message
         */
        fun createAIResponse(content: String, food: Food? = null): ChatMessage {
            return ChatMessage(
                type = MessageType.AI,
                content = content,
                food = food
            )
        }
        
        /**
         * Create a food confirmation message
         */
        fun createFoodConfirmation(food: Food): ChatMessage {
            return ChatMessage(
                type = MessageType.FOOD_CONFIRMATION,
                content = "Подтвердите добавление: ${food.name}",
                food = food
            )
        }
    }
}