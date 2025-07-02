package com.example.calorietracker.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// Улучшенный AI индикатор загрузки с пульсацией и вращением
@Composable
fun EnhancedAILoadingIndicator(
    modifier: Modifier = Modifier,
    text: String = "AI анализирует",
    accentColor: Color = Color(0xFFFF9800)
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Анимированный логотип
        AIBrainAnimation(color = accentColor)

        // Текст с точками
        AnimatedAIText(text = text, color = Color.Black)

        // Прогресс-индикатор
        NeuralNetworkLoader(color = accentColor)
    }
}

// Анимация "мозга" AI
@Composable
fun AIBrainAnimation(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFF9800)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "brain")

    // Основное вращение
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Пульсация
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Альфа-канал для свечения
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        // Внешние круги свечения
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size((100 + index * 20).dp)
                    .scale(scale)
                    .graphicsLayer {
                        this.alpha = alpha * (1f - index * 0.3f)
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Центральный элемент с нейронными связями
        Canvas(
            modifier = Modifier
                .size(80.dp)
                .rotate(rotation)
        ) {
            drawNeuralNetwork(color, alpha)
        }

        // Иконка в центре
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .scale(scale * 0.9f),
            tint = Color.White
        )
    }
}

// Рисование нейронной сети
private fun DrawScope.drawNeuralNetwork(color: Color, alpha: Float) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val radius = size.minDimension / 3

    // Нейроны
    val neurons = 6
    val angleStep = 360f / neurons

    val neuronPositions = List(neurons) { index ->
        val angle = Math.toRadians((angleStep * index).toDouble())
        Offset(
            x = centerX + (radius * cos(angle)).toFloat(),
            y = centerY + (radius * sin(angle)).toFloat()
        )
    }

    // Связи между нейронами
    neuronPositions.forEachIndexed { i, start ->
        neuronPositions.forEachIndexed { j, end ->
            if (i != j) {
                drawLine(
                    color = color.copy(alpha = alpha * 0.3f),
                    start = start,
                    end = end,
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(10f, 5f),
                        phase = alpha * 20f
                    )
                )
            }
        }
    }

    // Сами нейроны
    neuronPositions.forEach { position ->
        // Внешний круг
        drawCircle(
            color = color.copy(alpha = alpha * 0.5f),
            radius = 8.dp.toPx(),
            center = position,
            style = Stroke(width = 2.dp.toPx())
        )
        // Внутренний круг
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = 4.dp.toPx(),
            center = position
        )
    }

    // Центральный нейрон
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color,
                color.copy(alpha = 0.5f)
            ),
            center = Offset(centerX, centerY),
            radius = 12.dp.toPx()
        ),
        radius = 12.dp.toPx(),
        center = Offset(centerX, centerY)
    )
}

// Анимированный текст с эффектом печатной машинки
@Composable
fun AnimatedAIText(
    text: String,
    color: Color = Color.Black
) {
    var displayText by remember { mutableStateOf("") }
    var showDots by remember { mutableStateOf(false) }

    LaunchedEffect(text) {
        displayText = ""
        text.forEachIndexed { index, _ ->
            delay(50)
            displayText = text.substring(0, index + 1)
        }
        showDots = true
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )

        if (showDots) {
            AnimatedDotsRow()
        }
    }
}

@Composable
fun AnimatedDotsRow() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )

            Text(
                text = ".",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = alpha)
            )
        }
    }
}

// Загрузчик в виде нейронной сети
@Composable
fun NeuralNetworkLoader(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFF9800)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "network")

    // Анимация прогресса
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
    ) {
        // Фоновая линия
        drawRect(
            color = color.copy(alpha = 0.2f),
            size = size
        )

        // Анимированный прогресс с градиентом
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    color.copy(alpha = 0.5f),
                    color,
                    color.copy(alpha = 0.5f),
                    Color.Transparent
                ),
                startX = size.width * (progress - 0.3f),
                endX = size.width * (progress + 0.3f)
            ),
            size = Size(size.width * 0.3f, size.height),
            topLeft = Offset(size.width * (progress - 0.15f), 0f)
        )
    }
}

// Компактный AI индикатор для диалогов
@Composable
fun CompactAILoader(
    modifier: Modifier = Modifier,
    text: String = "Обрабатываю",
    color: Color = Color(0xFFFF9800)
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn() +
                androidx.compose.animation.scaleIn(initialScale = 0.8f)
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AISpinner(size = 32.dp, color = color)

                Column {
                    Text(
                        text = text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = "Это займет пару секунд",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// Минималистичный спиннер AI
@Composable
fun AISpinner(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    color: Color = Color(0xFFFF9800)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .rotate(rotation)
        ) {
            val strokeWidth = size.toPx() / 10f
            val radius = (size.toPx() - strokeWidth) / 2f

            // Градиентная дуга
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        color.copy(alpha = 0.3f),
                        color,
                        color.copy(alpha = 0.3f),
                        Color.Transparent
                    )
                ),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                ),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            )
        }

        // Центральная иконка
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(size * 0.4f),
            tint = color
        )
    }
}
