package com.example.calorietracker.data.mappers

import com.example.calorietracker.data.ChatMessage as DataChatMessage
import com.example.calorietracker.data.MessageType as DataMessageType
import com.example.calorietracker.domain.entities.ChatMessage
import com.example.calorietracker.domain.entities.common.MessageType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for transforming ChatMessage entities between domain and data layers
 */
@Singleton
class ChatMapper @Inject constructor(
    private val foodMapper: FoodMapper
) {
    
    /**
     * Convert domain ChatMessage to data ChatMessage
     */
    fun mapDomainToData(message: ChatMessage): DataChatMessage {
        return DataChatMessage(
            id = message.id,
            type = mapDomainMessageTypeToData(message.type),
            content = message.content,
            imagePath = message.imagePath,
            timestamp = message.timestamp,
            foodItem = message.food?.let { foodMapper.mapDomainToData(it) },
            isExpandable = message.food != null,
            isWelcome = message.isWelcome,
            animate = true, // Always animate new messages
            isProcessing = false, // Domain doesn't track processing state
            inputMethod = message.inputMethod,
            isVisible = true, // Always visible in domain
            isFoodConfirmation = message.isFoodConfirmation(),
            isError = message.isError,
            retryAction = null, // Retry actions are handled in presentation layer
            retryCount = message.retryCount,
            maxRetries = message.maxRetries
        )
    }
    
    /**
     * Convert data ChatMessage to domain ChatMessage
     */
    fun mapDataToDomain(message: DataChatMessage): ChatMessage {
        return ChatMessage(
            id = message.id,
            type = mapDataMessageTypeToDomain(message.type),
            content = message.content,
            imagePath = message.imagePath,
            timestamp = message.timestamp,
            food = message.foodItem?.let { foodMapper.mapDataToDomain(it) },
            isWelcome = message.isWelcome,
            inputMethod = message.inputMethod,
            isError = message.isError,
            retryCount = message.retryCount,
            maxRetries = message.maxRetries
        )
    }
    
    /**
     * Convert domain MessageType to data MessageType
     */
    private fun mapDomainMessageTypeToData(type: MessageType): DataMessageType {
        return when (type) {
            MessageType.USER -> DataMessageType.USER
            MessageType.AI -> DataMessageType.AI
            MessageType.FOOD_CONFIRMATION -> DataMessageType.FOOD_CONFIRMATION
            MessageType.SYSTEM -> DataMessageType.AI // Map system messages to AI for legacy compatibility
        }
    }
    
    /**
     * Convert data MessageType to domain MessageType
     */
    private fun mapDataMessageTypeToDomain(type: DataMessageType): MessageType {
        return when (type) {
            DataMessageType.USER -> MessageType.USER
            DataMessageType.AI -> MessageType.AI
            DataMessageType.FOOD_CONFIRMATION -> MessageType.FOOD_CONFIRMATION
        }
    }
    
    /**
     * Convert list of domain ChatMessages to data ChatMessages
     */
    fun mapDomainListToData(messages: List<ChatMessage>): List<DataChatMessage> {
        return messages.map { mapDomainToData(it) }
    }
    
    /**
     * Convert list of data ChatMessages to domain ChatMessages
     */
    fun mapDataListToDomain(messages: List<DataChatMessage>): List<ChatMessage> {
        return messages.map { mapDataToDomain(it) }
    }
    
    /**
     * Create domain welcome message
     */
    fun createWelcomeMessage(content: String): ChatMessage {
        return ChatMessage.createWelcome(content)
    }
    
    /**
     * Create domain user message
     */
    fun createUserMessage(content: String, imagePath: String? = null): ChatMessage {
        return ChatMessage.createUserMessage(content, imagePath)
    }
    
    /**
     * Create domain AI response message
     */
    fun createAIResponse(content: String, food: com.example.calorietracker.domain.entities.Food? = null): ChatMessage {
        return ChatMessage.createAIResponse(content, food)
    }
    
    /**
     * Create domain food confirmation message
     */
    fun createFoodConfirmation(food: com.example.calorietracker.domain.entities.Food): ChatMessage {
        return ChatMessage.createFoodConfirmation(food)
    }
    
    /**
     * Filter messages by type
     */
    fun filterMessagesByType(messages: List<ChatMessage>, type: MessageType): List<ChatMessage> {
        return messages.filter { it.type == type }
    }
    
    /**
     * Get messages with food data
     */
    fun getMessagesWithFood(messages: List<ChatMessage>): List<ChatMessage> {
        return messages.filter { it.hasFood() }
    }
    
    /**
     * Get error messages that can be retried
     */
    fun getRetryableMessages(messages: List<ChatMessage>): List<ChatMessage> {
        return messages.filter { it.canRetry() }
    }
}