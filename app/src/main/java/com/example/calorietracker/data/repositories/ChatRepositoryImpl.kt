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
import com.example.calorietracker.network.AiChatRequest
import com.example.calorietracker.network.AiChatResponse
import com.example.calorietracker.network.MakeService
import com.example.calorietracker.network.MakeWebhookClient
import com.example.calorietracker.network.MakeWebhookResult
import com.example.calorietracker.network.UserProfileData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ChatRepository with Make.com integration
 * FIXED: Добавлена интеграция с реальным API вместо заглушек
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val dataRepository: DataRepository,
    private val chatMapper: ChatMapper,
    private val makeService: MakeService,
    private val makeWebhookClient: MakeWebhookClient
) : ChatRepository {
    
    private val gson = Gson()
    
    override suspend fun sendMessage(message: ChatMessage): Result<ChatMessage> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if this is the first message of the day
                val isFirstMessageOfDay = dataRepository.isFirstMessageOfDay()
                
                // Record message time
                dataRepository.recordLastUserMessageTime()
                
                // Get user profile from DataRepository
                val userProfile = getUserProfileForAI()
                
                // Determine message type based on content
                val messageType = determineMessageType(message.content)
                
                // Generate userId
                val userId = "user_${System.currentTimeMillis()}"
                
                // If image attached → send multipart photo, else JSON chat
                val response: AiChatResponse = if (message.imagePath != null) {
                    val profileJson = gson.toJson(userProfile)
                    val inferredType = if (messageType == "recipe") "recipe_photo" else "photo"
                    val httpResp: MakeWebhookResult = try {
                        makeWebhookClient.postMultipartPhoto(
                            webhookId = MakeService.WEBHOOK_ID,
                            photoFile = java.io.File(message.imagePath),
                            userProfileJson = profileJson,
                            userId = userId,
                            caption = message.content,
                            messageType = inferredType,
                            isFirstMessageOfDay = isFirstMessageOfDay
                        )
                    } catch (e: Exception) {
                        // As a last resort: fall back to legacy local stub
                        return@withContext Result.error(
                            DomainException.NetworkException(
                                "Failed to send photo message: ${e.message}", e
                            )
                        )
                    }
                    AiChatResponse(
                        status = if (httpResp.httpCode in 200..299) "success" else "error",
                        answer = httpResp.body
                    )
                } else {
                    val request = AiChatRequest(
                        message = message.content,
                        userProfile = userProfile,
                        userId = userId,
                        isFirstMessageOfDay = isFirstMessageOfDay,
                        messageType = messageType
                    )
                    try {
                        makeService.askAiDietitian(MakeService.WEBHOOK_ID, request)
                    } catch (e: Exception) {
                        AiChatResponse(
                            status = "error",
                            answer = dataRepository.sendChatMessage(message.content)
                        )
                    }
                }
                
                // Create AI response message
                val aiResponse = chatMapper.createAIResponse(response.answer)
                
                // Save both messages to history
                val dataMessage = chatMapper.mapDomainToData(message)
                dataRepository.saveChatMessage(dataMessage)
                
                val aiDataMessage = chatMapper.mapDomainToData(aiResponse)
                dataRepository.saveChatMessage(aiDataMessage)
                
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
    
    /**
     * Determine message type based on content
     */
    private fun determineMessageType(content: String): String {
        val lowerContent = content.lowercase()
        
        return when {
            // Food analysis keywords
            lowerContent.contains("калори") ||
            lowerContent.contains("кбжу") ||
            lowerContent.contains("съел") ||
            lowerContent.contains("скушал") ||
            (lowerContent.contains("это") && lowerContent.contains("грамм")) -> "analysis"
            
            // Watch my food keywords
            lowerContent.contains("что я ел") ||
            lowerContent.contains("мой рацион") ||
            lowerContent.contains("сколько я съел") ||
            lowerContent.contains("анализ дня") -> "watch_myfood"
            
            // Recipe keywords
            lowerContent.contains("рецепт") ||
            lowerContent.contains("приготовить") ||
            lowerContent.contains("как готовить") -> "recipe"
            
            // Default to chat
            else -> "chat"
        }
    }
    
    /**
     * Get user profile data for AI requests
     */
    private fun getUserProfileForAI(): UserProfileData {
        return try {
            val profile = dataRepository.getUserProfile()
            if (profile != null) {
                UserProfileData(
                    age = com.example.calorietracker.utils.calculateAge(profile.birthday),
                    weight = profile.weight,
                    height = profile.height,
                    gender = profile.gender.lowercase(),
                    activityLevel = profile.condition.lowercase(),
                    goal = profile.goal.lowercase()
                )
            } else {
                getDefaultUserProfile()
            }
        } catch (e: Exception) {
            getDefaultUserProfile()
        }
    }
    
    /**
     * Get default user profile
     */
    private fun getDefaultUserProfile(): UserProfileData {
        return UserProfileData(
            age = 25,
            weight = 70,
            height = 170,
            gender = "other",
            activityLevel = "moderately_active",
            goal = "maintain_weight"
        )
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
                val jsonObject = org.json.JSONObject(response)
                
                val food = Food(
                    name = jsonObject.optString("name", "Неизвестный продукт"),
                    calories = jsonObject.optInt("calories", 0),
                    protein = jsonObject.optDouble("protein", 0.0),
                    fat = jsonObject.optDouble("fat", 0.0),
                    carbs = jsonObject.optDouble("carbs", 0.0),
                    weight = jsonObject.optString("weight", "100г"),
                    source = com.example.calorietracker.domain.entities.common.FoodSource.AI_TEXT_ANALYSIS,
                    aiOpinion = jsonObject.optString("opinion", "")
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
