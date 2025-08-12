package com.example.calorietracker.data.repositories

import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.mappers.ChatMapper
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.ChatMessage
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.common.DateRange
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.AIOperationType
import com.example.calorietracker.domain.repositories.AIUsageInfo
import com.example.calorietracker.domain.repositories.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ChatRepository that handles chat and AI operations
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val dataRepository: DataRepository,
    private val chatMapper: ChatMapper
) : ChatRepository {
    
    override suspend fun sendMessage(message: ChatMessage): Result<ChatMessage> {
        return withContext(Dispatchers.IO) {
            try {
                // Convert to data message
                val dataMessage = chatMapper.mapDomainToData(message)
                
                // Send message using DataRepository (this would call AI service)
                val response = dataRepository.sendChatMessage(dataMessage.content)
                
                // Create AI response message
                val aiResponse = chatMapper.createAIResponse(response)
                
                Result.success(aiResponse)
            } catch (e: Exception) {
                Result.error(
                    DomainException.NetworkException(
                        "Failed to send message: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun getChatHistory(dateRange: DateRange): Result<List<ChatMessage>> {
        return withContext(Dispatchers.IO) {
            try {
                // Get chat history from DataRepository
                val dataMessages = dataRepository.getChatHistory()
                
                // Convert to domain messages
                val domainMessages = chatMapper.mapDataListToDomain(dataMessages)
                
                // TODO: Filter by date range when timestamp filtering is implemented
                
                Result.success(domainMessages)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get chat history: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun getRecentMessages(limit: Int): Result<List<ChatMessage>> {
        return withContext(Dispatchers.IO) {
            try {
                val dataMessages = dataRepository.getChatHistory()
                val recent = dataMessages.takeLast(limit)
                
                val domainMessages = chatMapper.mapDataListToDomain(recent)
                Result.success(domainMessages)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get recent messages: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun saveChatMessage(message: ChatMessage): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val dataMessage = chatMapper.mapDomainToData(message)
                dataRepository.saveChatMessage(dataMessage)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to save chat message: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun deleteChatMessage(messageId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                dataRepository.deleteChatMessage(messageId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to delete chat message: ${e.message}",
                        e
                    )
                )
            }
        }
    }    
 
   override suspend fun clearChatHistory(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                dataRepository.clearChatHistory()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to clear chat history: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun processAIFoodAnalysis(response: String): Result<Food> {
        return withContext(Dispatchers.IO) {
            try {
                // TODO: Implement proper AI response parsing
                // For now, create a placeholder Food object
                val food = Food(
                    name = "Parsed Food",
                    calories = 150,
                    protein = 8.0,
                    fat = 5.0,
                    carbs = 20.0,
                    weight = "100г",
                    source = com.example.calorietracker.domain.entities.common.FoodSource.AI_TEXT_ANALYSIS,
                    aiOpinion = response
                )
                
                Result.success(food)
            } catch (e: Exception) {
                Result.error(
                    DomainException.AIAnalysisException(
                        "Failed to process AI food analysis: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun checkAIUsageLimits(): Result<AIUsageInfo> {
        return withContext(Dispatchers.IO) {
            try {
                // Get usage info from DataRepository
                val usageData = dataRepository.getAIUsageInfo()
                
                val usageInfo = AIUsageInfo(
                    dailyUsage = usageData["dailyUsage"] as? Int ?: 0,
                    dailyLimit = usageData["dailyLimit"] as? Int ?: 10,
                    monthlyUsage = usageData["monthlyUsage"] as? Int ?: 0,
                    monthlyLimit = usageData["monthlyLimit"] as? Int ?: 100,
                    canUseAI = (usageData["canUseAI"] as? Boolean) ?: true,
                    resetTime = LocalDateTime.now().plusDays(1)
                )
                
                Result.success(usageInfo)
            } catch (e: Exception) {
                Result.error(
                    DomainException.BusinessLogicException(
                        "Failed to check AI usage limits: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun recordAIUsage(operationType: AIOperationType): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                dataRepository.recordAIUsage(operationType.name)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to record AI usage: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun getConversationContext(): Result<List<ChatMessage>> {
        return withContext(Dispatchers.IO) {
            try {
                // Get recent messages for context
                getRecentMessages(10)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to get conversation context: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun updateConversationContext(messages: List<ChatMessage>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Save messages to update context
                for (message in messages) {
                    saveChatMessage(message)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to update conversation context: ${e.message}",
                        e
                    )
                )
            }
        }
    }
    
    override suspend fun retryMessage(messageId: String): Result<ChatMessage> {
        return withContext(Dispatchers.IO) {
            try {
                // Get original message
                val historyResult = getChatHistory(DateRange.currentWeek())
                when (historyResult) {
                    is Result.Success -> {
                        val originalMessage = historyResult.data.find { it.id == messageId }
                        if (originalMessage != null && originalMessage.canRetry()) {
                            val retryMessage = originalMessage.createRetry()
                            sendMessage(retryMessage)
                        } else {
                            Result.error(
                                DomainException.BusinessLogicException("Message cannot be retried")
                            )
                        }
                    }
                    is Result.Error -> historyResult
                }
            } catch (e: Exception) {
                Result.error(
                    DomainException.StorageException(
                        "Failed to retry message: ${e.message}",
                        e
                    )
                )
            }
        }
    }
}