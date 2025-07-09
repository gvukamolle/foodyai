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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.auth.SubscriptionPlan
import com.example.calorietracker.pages.AnimatedDialogContainer // Импортируем контейнер

@Composable
fun SubscriptionConfirmationDialog(
    plan: SubscriptionPlan,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Определяем акцентный цвет в зависимости от плана
    val accentColor = when (plan) {
        SubscriptionPlan.PRO -> Color(0xFF2196F3)
        else -> Color(0xFF4CAF50)
    }

    // Используем AnimatedDialogContainer для красивых эффектов
    AnimatedDialogContainer(
        onDismiss = onDismiss,
        accentColor = accentColor
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Анимированная иконка успеха
            AnimatedSuccessIcon()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Подтверждение перехода",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Информация о плане с градиентным фоном
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (plan) {
                        SubscriptionPlan.FREE -> Color(0xFFF5F5F5)
                        SubscriptionPlan.PRO -> Color(0xFFE3F2FD)
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Вы переходите на план",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (plan != SubscriptionPlan.FREE) {
                            Icon(
                                imageVector = when (plan) {
                                    SubscriptionPlan.PRO -> Icons.Default.AllInclusive
                                    else -> Icons.Default.AccountCircle
                                },
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = plan.displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = getConfirmationMessage(plan),
                fontSize = 16.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

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
                        contentColor = Color(0xFF666666)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
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
                        containerColor = accentColor
                    )
                ) {
                    Text(
                        "Подтвердить",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedSuccessIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "success")

    // Пульсация
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Вращение ореола
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
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
        // Вращающийся градиентный фон
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation
                }
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF4CAF50).copy(alpha = 0.3f),
                            Color(0xFF81C784).copy(alpha = 0.1f),
                            Color(0xFF4CAF50).copy(alpha = 0.3f)
                        )
                    )
                )
        )

        // Пульсирующая иконка
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

private fun getConfirmationMessage(plan: SubscriptionPlan): String {
    return when (plan) {
        SubscriptionPlan.FREE -> "Вы переходите на бесплатный план. Доступ к AI-анализу будет ограничен."
        SubscriptionPlan.PRO -> "Превосходно! Безлимитный доступ к AI-анализу поможет достичь ваших целей быстрее."
    }
}