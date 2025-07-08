package com.example.calorietracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.auth.SubscriptionPlan
import com.example.calorietracker.auth.UserData
import com.example.calorietracker.utils.AIUsageManager

/**
 * Компактный индикатор использования AI для главного экрана
 */
@Composable
fun AIUsageIndicator(
    userData: UserData?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    userData?.let { user ->
        when (user.subscriptionPlan) {
            SubscriptionPlan.FREE -> {
                // Для бесплатного плана показываем промо-баннер
                FreeUserPromoBanner(onClick = onClick, modifier = modifier)
            }
            SubscriptionPlan.PLUS -> {
                // Для PLUS показываем счетчик использований
                PlusUserCounter(userData = user, onClick = onClick, modifier = modifier)
            }
            SubscriptionPlan.PRO, -> {
                // Для PRO показываем значок безлимита
                ProUserBadge(modifier = modifier)
            }
        }
    }
}

@Composable
private fun FreeUserPromoBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF3E0)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Попробуйте AI-анализ еды!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "От 299₽/мес",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }

                IconButton(
                    onClick = { isVisible = false },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlusUserCounter(
    userData: UserData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remaining = AIUsageManager.getRemainingUsage(userData)
    val total = AIUsageManager.getPlanLimit(userData.subscriptionPlan)
    val progress = 1f - (remaining.toFloat() / total)

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        )
    )

    val color = when {
        remaining == 0 -> Color(0xFFF44336)
        remaining <= 2 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Круговой индикатор
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = color,
                    strokeWidth = 3.dp,
                    trackColor = Color(0xFFE0E0E0)
                )
                Text(
                    text = remaining.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Column {
                Text(
                    text = "AI-анализов осталось",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "из $total в месяц",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }

            if (remaining <= 2) {
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (remaining == 0) "Пополнить" else "Мало",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ProUserBadge(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF2196F3).copy(alpha = 0.1f),
                            Color(0xFF03A9F4).copy(alpha = 0.2f),
                            Color(0xFF2196F3).copy(alpha = 0.1f)
                        ),
                        startX = -300f + 600f * shimmerProgress,
                        endX = 300f * shimmerProgress
                    )
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
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "PRO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

/**
 * Мини-индикатор для отображения в toolbar
 */
@Composable
fun AIUsageMiniIndicator(
    userData: UserData?,
    onClick: () -> Unit
) {
    userData?.let { user ->
        when (user.subscriptionPlan) {
            SubscriptionPlan.PLUS -> {
                val remaining = AIUsageManager.getRemainingUsage(user)
                val color = when {
                    remaining == 0 -> Color(0xFFFF928B)
                    remaining <= 2 -> Color(0xFFFFC780)
                    else -> Color(0xFF69D96E)
                }

                Surface(
                    onClick = onClick,
                    color = color.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AI: $remaining",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = color
                        )
                    }
                }
            }
            SubscriptionPlan.PRO, -> {
                Surface(
                    color = Color(0xFF2196F3).copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.AllInclusive,
                        contentDescription = "Безлимит",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }
            }
            else -> { /* Ничего не показываем для FREE */ }
        }
    }
}
