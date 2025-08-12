package com.example.calorietracker.data

import java.time.LocalDateTime
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val type: MessageType,
    val content: String,
    val imagePath: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val foodItem: FoodItem? = null,
    val isExpandable: Boolean = false,
    val isWelcome: Boolean = false,
    val animate: Boolean = true,
    val isProcessing: Boolean = false,
    val inputMethod: String? = null,
    val isVisible: Boolean = true,
    val isFoodConfirmation: Boolean = false,
    val isError: Boolean = false,
    val retryAction: (() -> Unit)? = null,
    val retryCount: Int = 0,
    val maxRetries: Int = 3
)

enum class MessageType {
    USER, AI, FOOD_CONFIRMATION
}