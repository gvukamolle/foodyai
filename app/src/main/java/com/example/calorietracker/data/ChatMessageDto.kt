package com.example.calorietracker.data

import com.example.calorietracker.ChatMessage
import com.example.calorietracker.MessageType
import com.example.calorietracker.FoodItem
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Lightweight DTO for persisting [ChatMessage] in SharedPreferences.
 * Stores timestamps as epoch millis to avoid issues with Gson and [LocalDateTime].
 */
data class ChatMessageDto(
    val id: String,
    val type: MessageType,
    val content: String,
    val imagePath: String? = null,
    val timestamp: Long,
    val foodItem: FoodItem? = null,
    val isExpandable: Boolean = false,
    val isWelcome: Boolean = false,
    val animate: Boolean = true,
    val isProcessing: Boolean = false,
    val isVisible: Boolean = true
)

fun ChatMessage.toDto(): ChatMessageDto = ChatMessageDto(
    id = id,
    type = type,
    content = content,
    imagePath = imagePath,
    timestamp = timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    foodItem = foodItem,
    isExpandable = isExpandable,
    isWelcome = isWelcome,
    animate = animate,
    isProcessing = isProcessing,
    isVisible = isVisible
)

fun ChatMessageDto.toChatMessage(): ChatMessage = ChatMessage(
    id = id,
    type = type,
    content = content,
    imagePath = imagePath,
    timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()),
    foodItem = foodItem,
    isExpandable = isExpandable,
    isWelcome = isWelcome,
    animate = animate,
    isProcessing = isProcessing,
    isVisible = isVisible
)
