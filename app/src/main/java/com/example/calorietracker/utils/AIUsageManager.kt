package com.example.calorietracker.utils

import com.example.calorietracker.auth.SubscriptionPlan
import com.example.calorietracker.auth.UserData
import java.time.LocalDateTime
import java.time.ZoneId

object AIUsageManager {

    private val planLimits = mapOf(
        SubscriptionPlan.FREE to 0,
        SubscriptionPlan.PRO to Int.MAX_VALUE,
    )

    fun canUseAI(userData: UserData): Boolean {
        val limit = planLimits[userData.subscriptionPlan] ?: 0
        if (limit == Int.MAX_VALUE) return true
        if (limit == 0) return false
        val shouldReset = shouldResetUsage(userData.aiUsageResetDate)
        val currentUsage = if (shouldReset) 0 else userData.aiUsageCount
        return currentUsage < limit
    }

    fun getRemainingUsage(userData: UserData): Int {
        val limit = planLimits[userData.subscriptionPlan] ?: 0
        if (limit == Int.MAX_VALUE) return Int.MAX_VALUE
        if (limit == 0) return 0
        val shouldReset = shouldResetUsage(userData.aiUsageResetDate)
        val currentUsage = if (shouldReset) 0 else userData.aiUsageCount
        return (limit - currentUsage).coerceAtLeast(0)
    }

    fun getPlanLimit(plan: SubscriptionPlan): Int {
        return planLimits[plan] ?: 0
    }

    private fun shouldResetUsage(lastResetDate: Long): Boolean {
        if (lastResetDate == 0L) return true
        val lastReset = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(lastResetDate),
            ZoneId.systemDefault()
        )
        val now = LocalDateTime.now()
        return lastReset.month != now.month || lastReset.year != now.year
    }

    fun incrementUsage(userData: UserData): UserData {
        val shouldReset = shouldResetUsage(userData.aiUsageResetDate)
        return if (shouldReset) {
            userData.copy(
                aiUsageCount = 1,
                aiUsageResetDate = System.currentTimeMillis()
            )
        } else {
            userData.copy(
                aiUsageCount = userData.aiUsageCount + 1
            )
        }
    }

    fun getLimitExceededMessage(plan: SubscriptionPlan): String {
        return when (plan) {
            SubscriptionPlan.FREE ->
                "AI-анализ недоступен в бесплатном плане. Перейдите на PRO для использования этой функции."
            else ->
                "Произошла ошибка. Пожалуйста, попробуйте позже."
        }
    }

    fun getUpgradeProposal(currentPlan: SubscriptionPlan): Pair<SubscriptionPlan, String>? {
        return when (currentPlan) {
            SubscriptionPlan.FREE ->
                SubscriptionPlan.PRO to "Безлимитный AI всего за 399₽/мес в PRO плане!"
            else -> null
        }
    }
}