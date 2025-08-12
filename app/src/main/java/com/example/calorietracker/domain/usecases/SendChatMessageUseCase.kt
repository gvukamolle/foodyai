package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.ChatMessage
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.ChatRepository
import com.example.calorietracker.domain.usecases.base.UseCase
import javax.inject.Inject

/**
 * Use case for sending chat messages and getting AI responses
 */
class SendChatMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) : UseCase<SendChatMessageUseCase.Params, ChatMessage>() {
    
    override suspend fun execute(parameters: Params): Result<ChatMessage> {
        val message = parameters.message
        
        // Validate message
        if (message.content.isBlank() && message.imagePath == null) {
            return Result.error(
                DomainException.ValidationException("Message cannot be empty")
            )
        }
        
        if (message.content.length > 2000) {
            return Result.error(
                DomainException.ValidationException("Message too long (max 2000 characters)")
            )
        }
        
        // Check for inappropriate content
        if (containsInappropriateContent(message.content)) {
            return Result.error(
                DomainException.ValidationException("Message contains inappropriate content")
            )
        }
        
        // Save user message first
        val saveResult = chatRepository.saveChatMessage(message)
        if (saveResult is Result.Error) {
            return Result.error(saveResult.exception)
        }
        
        // Send message and get AI response
        return chatRepository.sendMessage(message)
    }
    
    private fun containsInappropriateContent(content: String): Boolean {
        // Basic content filtering
        val inappropriateWords = listOf(
            "script", "<script", "javascript:", "eval(",
            "onclick", "onerror", "onload"
        )
        val lowerContent = content.lowercase()
        return inappropriateWords.any { lowerContent.contains(it) }
    }
    
    data class Params(
        val message: ChatMessage
    )
}