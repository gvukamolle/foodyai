package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.ChatMessage
import com.example.calorietracker.domain.entities.common.DateRange
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.ChatRepository
import com.example.calorietracker.domain.usecases.base.UseCase
import javax.inject.Inject

/**
 * Use case for retrieving chat history
 */
class GetChatHistoryUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) : UseCase<GetChatHistoryUseCase.Params, List<ChatMessage>>() {
    
    override suspend fun execute(parameters: Params): Result<List<ChatMessage>> {
        return when (parameters.dateRange) {
            null -> {
                // Get recent messages if no date range specified
                chatRepository.getRecentMessages(parameters.limit)
            }
            else -> {
                // Validate date range
                if (parameters.dateRange.dayCount() > 90) {
                    return Result.error(
                        DomainException.ValidationException("Date range cannot exceed 90 days")
                    )
                }
                
                val result = chatRepository.getChatHistory(parameters.dateRange)
                
                // Apply limit if specified
                when (result) {
                    is Result.Success -> {
                        val limitedMessages = if (parameters.limit > 0) {
                            result.data.take(parameters.limit)
                        } else {
                            result.data
                        }
                        Result.success(limitedMessages)
                    }
                    is Result.Error -> result
                }
            }
        }
    }
    
    data class Params(
        val dateRange: DateRange? = null,
        val limit: Int = 50
    )
}