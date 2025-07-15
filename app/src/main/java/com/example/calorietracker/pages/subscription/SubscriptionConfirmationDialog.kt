package com.example.calorietracker.pages.subscription

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.calorietracker.auth.SubscriptionPlan
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import com.example.calorietracker.extensions.fancyShadow

@Composable
fun SubscriptionConfirmationDialog(
    plan: SubscriptionPlan,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Определяем цвета в зависимости от плана
    val isPro = plan == SubscriptionPlan.PRO
    val primaryColor = if (isPro) Color(0xFF2196F3) else Color(0xFF424242)
    val shadowColor = if (isPro) Color(0xFF2196F3) else Color(0xFF9E9E9E)
    val backgroundColor = if (isPro) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fancyShadow(
                    borderRadius = 24.dp,
                    shadowRadius = if (isPro) 10.dp else 8.dp,
                    alpha = if (isPro) 0.3f else 0.25f,
                    color = shadowColor
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Box {
                // Декоративный элемент для PRO
                if (isPro) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 30.dp, y = (-20).dp)
                            .size(120.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF2196F3).copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }

                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Анимированная иконка
                    AnimatedPlanIcon(isPro = isPro)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Подтверждение перехода",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = primaryColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Информация о плане
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fancyShadow(
                                borderRadius = 16.dp,
                                shadowRadius = 6.dp,
                                alpha = 0.2f,
                                color = shadowColor
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = backgroundColor
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Вы переходите на план",
                                fontSize = 14.sp,
                                color = if (isPro) Color(0xFF1976D2) else Color(0xFF757575)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (plan) {
                                        SubscriptionPlan.FREE -> Icons.Default.AccountCircle
                                        SubscriptionPlan.PRO -> Icons.Default.AllInclusive
                                    },
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = plan.displayName,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            }

                            // Цена для PRO
                            if (isPro) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "399₽ в месяц",
                                    fontSize = 16.sp,
                                    color = Color(0xFF1976D2),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = getConfirmationMessage(plan),
                        fontSize = 16.sp,
                        color = if (isPro) Color(0xFF666666) else Color(0xFF9E9E9E),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    // Дополнительные фичи для PRO
                    if (isPro) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MiniFeatureIcon(Icons.Default.AllInclusive, "Безлимит")
                            MiniFeatureIcon(Icons.Default.Analytics, "Аналитика")
                            MiniFeatureIcon(Icons.Default.Restaurant, "Планы питания")
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Кнопки действий
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isPro) Color(0xFF2196F3) else Color(0xFF757575)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isPro) Color(0xFFBBDEFB) else Color(0xFFE0E0E0)
                            )
                        ) {
                            Text(
                                "Отмена",
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryColor
                            )
                        ) {
                            Text(
                                "Подтвердить",
                                modifier = Modifier.padding(vertical = 4.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedPlanIcon(isPro: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "plan")

    // Пульсация
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Вращение для PRO
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPro) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Фоновый круг
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = if (isPro) Color(0xFFE3F2FD) else Color(0xFFF5F5F5),
                    shape = CircleShape
                )
        )

        // Вращающийся градиент для PRO
        if (isPro) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = rotation
                    }
                    .background(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color(0xFF2196F3).copy(alpha = 0.3f),
                                Color(0xFF03A9F4).copy(alpha = 0.1f),
                                Color(0xFF00BCD4).copy(alpha = 0.1f),
                                Color(0xFF2196F3).copy(alpha = 0.3f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Иконка
        Icon(
            when {
                isPro -> Icons.Default.Star
                else -> Icons.Default.CheckCircle
            },
            contentDescription = null,
            tint = if (isPro) Color(0xFF2196F3) else Color(0xFF757575),
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

@Composable
private fun MiniFeatureIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    Color(0xFF2196F3).copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF1976D2),
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getConfirmationMessage(plan: SubscriptionPlan): String {
    return when (plan) {
        SubscriptionPlan.FREE -> "Вы переходите на бесплатный план. Доступ к AI-анализу будет ограничен базовыми функциями."
        SubscriptionPlan.PRO -> "Превосходно! Безлимитный доступ к AI-анализу и все премиум функции помогут достичь ваших целей быстрее."
    }
}