package com.example.calorietracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.auth.SubscriptionPlan
import com.example.calorietracker.auth.UserData
import com.example.calorietracker.utils.AIUsageManager
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Универсальный индикатор использования AI для toolbar
 * Отображается в виде стильной таблетки с анимациями
 */
@Composable
fun AIUsageToolbarIndicator(
    userData: UserData?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    userData?.let { user ->
        when (user.subscriptionPlan) {
            SubscriptionPlan.FREE -> {
                FreeUserPill(userData = user, onClick = onClick, modifier = modifier)
            }
            SubscriptionPlan.PRO -> {
                ProUserPill(onClick = onClick, modifier = modifier)
            }
        }
    }
}

@Composable
private fun FreeUserPill(
    userData: UserData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val remainingUsage = AIUsageManager.getRemainingUsage(userData)
    val planLimit = AIUsageManager.getPlanLimit(userData.subscriptionPlan)

    val infiniteTransition = rememberInfiniteTransition()
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val isLow = remainingUsage <= 2
    val backgroundColor = if (isLow) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
    val contentColor = if (isLow) Color(0xFFD32F2F) else Color(0xFFE65100)

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                if (remainingUsage == 0) Icons.Default.Block else Icons.Default.Stars,
                contentDescription = null,
                tint = contentColor.copy(alpha = if (isLow) animatedAlpha else 1f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "AI: $remainingUsage/$planLimit",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun ProUserPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition()

    // Анимация градиента для PRO
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Пульсация иконки
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1976D2).copy(alpha = 0.1f),
                            Color(0xFF2196F3).copy(alpha = 0.15f),
                            Color(0xFF03A9F4).copy(alpha = 0.2f),
                            Color(0xFF2196F3).copy(alpha = 0.15f),
                            Color(0xFF1976D2).copy(alpha = 0.1f)
                        ),
                        startX = -300f + 600f * shimmerProgress,
                        endX = 300f + 600f * shimmerProgress
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.AllInclusive,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer(
                            scaleX = iconScale,
                            scaleY = iconScale
                        )
                )
                Text(
                    text = "AI PRO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
        }
    }
}

/**
 * Компактная версия для очень ограниченного пространства
 */
@Composable
fun AIUsageCompactIndicator(
    userData: UserData?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    userData?.let { user ->
        when (user.subscriptionPlan) {
            SubscriptionPlan.FREE -> {
                val remaining = AIUsageManager.getRemainingUsage(user)
                CompactPill(
                    text = if (remaining > 0) "$remaining" else "0",
                    icon = if (remaining == 0) Icons.Default.Lock else Icons.Default.Star,
                    backgroundColor = if (remaining <= 2) Color(0xFFFFEBEE) else Color(0xFFFFF3E0),
                    contentColor = if (remaining <= 2) Color(0xFFD32F2F) else Color(0xFFFF9800),
                    onClick = onClick,
                    modifier = modifier
                )
            }
            SubscriptionPlan.PRO -> {
                CompactPill(
                    text = "∞",
                    icon = null,
                    backgroundColor = Color(0xFF2196F3).copy(alpha = 0.1f),
                    contentColor = Color(0xFF2196F3),
                    onClick = onClick,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun CompactPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.let {
                Icon(
                    it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}