package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.repositories.ChatRepository
import com.example.calorietracker.domain.usecases.base.UseCase
import javax.inject.Inject

/**
 * Use case for validating AI usage limits before making requests
 */
class ValidateAIUsageLimitsUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) : UseCase<ValidateAIUsageLimitsUseCase.Params, AIUsageValidation>() {
    
    override suspend fun execute(parameters: Params): Result<AIUsageValidation> {
        // Check current usage limits
        val usageResult = chatRepository.checkAIUsageLimits()
        
        return when (usageResult) {
            is Result.Success -> {
                val usage = usageResult.data
                
                // Validate if user can make the requested operation
                val validation = when {
                    !usage.canUseAI -> {
                        AIUsageValidation(
                            canProceed = false,
                            reason = "AI usage limit exceeded",
                            dailyUsage = usage.dailyUsage,
                            dailyLimit = usage.dailyLimit,
                            monthlyUsage = usage.monthlyUsage,
                            monthlyLimit = usage.monthlyLimit,
                            resetTime = usage.resetTime
                        )
                    }
                    usage.dailyUsage >= usage.dailyLimit -> {
                        AIUsageValidation(
                            canProceed = false,
                            reason = "Daily AI usage limit reached",
                            dailyUsage = usage.dailyUsage,
                            dailyLimit = usage.dailyLimit,
                            monthlyUsage = usage.monthlyUsage,
                            monthlyLimit = usage.monthlyLimit,
                            resetTime = usage.resetTime
                        )
                    }
                    usage.monthlyUsage >= usage.monthlyLimit -> {
                        AIUsageValidation(
                            canProceed = false,
                            reason = "Monthly AI usage limit reached",
                            dailyUsage = usage.dailyUsage,
                            dailyLimit = usage.dailyLimit,
                            monthlyUsage = usage.monthlyUsage,
                            monthlyLimit = usage.monthlyLimit,
                            resetTime = usage.resetTime
                        )
                    }
                    else -> {
                        AIUsageValidation(
                            canProceed = true,
                            reason = "Usage within limits",
                            dailyUsage = usage.dailyUsage,
                            dailyLimit = usage.dailyLimit,
                            monthlyUsage = usage.monthlyUsage,
                            monthlyLimit = usage.monthlyLimit,
                            resetTime = usage.resetTime
                        )
                    }
                }
                
                Result.success(validation)
            }
            is Result.Error -> {
                Result.error(
                    DomainException.BusinessLogicException(
                        "Failed to check AI usage limits: ${usageResult.exception.message}",
                        usageResult.exception
                    )
                )
            }
        }
    }
    
    data class Params(
        val operationType: com.example.calorietracker.domain.repositories.AIOperationType
    )
}

/**
 * Result of AI usage validation
 */
data class AIUsageValidation(
    val canProceed: Boolean,
    val reason: String,
    val dailyUsage: Int,
    val dailyLimit: Int,
    val monthlyUsage: Int,
    val monthlyLimit: Int,
    val resetTime: java.time.LocalDateTime
) {
    /**
     * Get remaining daily usage
     */
    fun getRemainingDailyUsage(): Int = (dailyLimit - dailyUsage).coerceAtLeast(0)
    
    /**
     * Get remaining monthly usage
     */
    fun getRemainingMonthlyUsage(): Int = (monthlyLimit - monthlyUsage).coerceAtLeast(0)
    
    /**
     * Get usage percentage for daily limit
     */
    fun getDailyUsagePercentage(): Double = if (dailyLimit > 0) {
        (dailyUsage.toDouble() / dailyLimit * 100).coerceAtMost(100.0)
    } else 0.0
    
    /**
     * Get usage percentage for monthly limit
     */
    fun getMonthlyUsagePercentage(): Double = if (monthlyLimit > 0) {
        (monthlyUsage.toDouble() / monthlyLimit * 100).coerceAtMost(100.0)
    } else 0.0
    
    /**
     * Check if approaching daily limit (>80%)
     */
    fun isApproachingDailyLimit(): Boolean = getDailyUsagePercentage() > 80.0
    
    /**
     * Check if approaching monthly limit (>80%)
     */
    fun isApproachingMonthlyLimit(): Boolean = getMonthlyUsagePercentage() > 80.0
}