package com.example.calorietracker.pages.subscription

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.calorietracker.auth.SubscriptionPlan
import com.example.calorietracker.auth.UserData
import com.example.calorietracker.utils.AIUsageManager
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer


@Composable
fun AILimitDialog(
    userData: UserData,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    val remainingUsage = AIUsageManager.getRemainingUsage(userData)
    val planLimit = AIUsageManager.getPlanLimit(userData.subscriptionPlan)
    val upgradeProposal = AIUsageManager.getUpgradeProposal(userData.subscriptionPlan)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Иконка с анимацией
                AnimatedLimitIcon(
                    hasAccess = remainingUsage > 0,
                    isUnlimited = planLimit == Int.MAX_VALUE
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Заголовок
                Text(
                    text = when {
                        remainingUsage == 0 && userData.subscriptionPlan == SubscriptionPlan.FREE ->
                            "AI-анализ недоступен"
                        remainingUsage == 0 ->
                            "Лимит исчерпан"
                        planLimit == Int.MAX_VALUE ->
                            "Безлимитный доступ"
                        else ->
                            "Осталось: $remainingUsage из $planLimit"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Описание
                Text(
                    text = AIUsageManager.getLimitExceededMessage(userData.subscriptionPlan),
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                // Предложение апгрейда
                upgradeProposal?.let { (plan, message) ->
                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (plan) {
                                SubscriptionPlan.PRO -> Color(0xFFE3F2FD)
                                else -> Color(0xFFF5F5F5)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = when (plan) {
                                    SubscriptionPlan.PRO -> Color(0xFF2196F3)
                                    else -> Color.Black
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = message,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (upgradeProposal != null) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Позже")
                        }

                        Button(
                            onClick = onUpgrade,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (userData.subscriptionPlan) {
                                    SubscriptionPlan.PRO -> Color(0xFF2196F3)
                                    else -> Color.Black
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Улучшить план")
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Понятно")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedLimitIcon(
    hasAccess: Boolean,
    isUnlimited: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition()

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isUnlimited) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isUnlimited) {
            // Безлимит - вращающийся круг
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = rotation }
            ) {
                val gradient = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF2196F3),
                        Color(0xFF03A9F4),
                        Color(0xFF00BCD4),
                        Color(0xFF2196F3)
                    )
                )
                drawCircle(
                    brush = gradient,
                    radius = size.minDimension / 2,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
        }

        Icon(
            imageVector = when {
                isUnlimited -> Icons.Default.AllInclusive
                hasAccess -> Icons.Default.CheckCircle
                else -> Icons.Default.Block
            },
            contentDescription = null,
            tint = when {
                isUnlimited -> Color(0xFF2196F3)
                hasAccess -> Color(0xFF4CAF50)
                else -> Color(0xFFF44336)
            },
            modifier = Modifier
                .size(60.dp)
                .graphicsLayer {
                    scaleX = if (!isUnlimited && !hasAccess) scale else 1f
                    scaleY = if (!isUnlimited && !hasAccess) scale else 1f
                }
        )
    }
}

@Composable
fun AIFeatureLockedDialog(
    feature: String,
    currentPlan: SubscriptionPlan,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заблокированная иконка
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Color(0xFFF5F5F5),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Функция недоступна",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$feature доступна только в ${getRequiredPlan(feature)} плане",
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onUpgrade,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Посмотреть планы",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Закрыть", color = Color(0xFF666666))
                }
            }
        }
    }
}

private fun getRequiredPlan(feature: String): String {
    return when (feature) {
        "AI-анализ фото", "Foody Insights", "Дневные сводки", "Планы питания", "Продвинутая аналитика" -> "PRO"
        else -> "платном"
    }
}