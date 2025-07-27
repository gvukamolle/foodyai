package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.extensions.fancyShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

// Цвета для макронутриентов
object MacroColors {
    val Proteins = Color(0xFF00BFA5) // Бирюзовый
    val Fats = Color(0xFFFFB74D) // Оранжевый
    val Carbs = Color(0xFF64B5F6) // Голубой
    val Calories = Color(0xFFFF4F8A) // Розовый
}

// Модель данных для кнопки вызова
data class ButtonPosition(val x: Float, val y: Float, val width: Int, val height: Int)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedStatisticsCard(
    viewModel: CalorieTrackerViewModel,
    onDismiss: () -> Unit,
    buttonPosition: ButtonPosition? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(5)
        isVisible = true
    }

    fun animatedDismiss() {
        coroutineScope.launch {
            isVisible = false
            delay(200)
            onDismiss()
        }
    }

    Popup(
        onDismissRequest = { animatedDismiss() },
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Кликабельный фон для закрытия
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        animatedDismiss()
                    }
            )

            // Полупрозрачный фон
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(100))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.7f))
                )
            }

            // Карточка статистики с позиционированием под кнопкой
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.9f,
                            transformOrigin = TransformOrigin(0.5f, 0f),
                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(animationSpec = tween(150)) +
                        scaleOut(
                            targetScale = 0.9f,
                            transformOrigin = TransformOrigin(0.5f, 0f),
                            animationSpec = tween(150)
                        ),
                modifier = Modifier.then(
                    if (buttonPosition != null) {
                        Modifier.offset {
                            IntOffset(
                                x = buttonPosition.x.toInt() - (360.dp.toPx() / 2).toInt() + (buttonPosition.width / 2),
                                y = (buttonPosition.y + buttonPosition.height + 8.dp.toPx()).toInt()
                            )
                        }
                    } else {
                        Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
                    }
                )
            ) {
                // ВАЖНО: Box с padding для тени
                Box(modifier = Modifier.padding(16.dp)) {
                    Card(
                        modifier = Modifier
                            .width(360.dp)
                            .pointerInput(Unit) {
                                detectTapGestures { /* Блокируем клики внутри карточки */ }
                            }
                            .fancyShadow(
                                borderRadius = 24.dp,
                                shadowRadius = 12.dp,
                                alpha = 0.35f,
                                color = Color.Black
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            // Заголовок с кнопкой закрытия
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Статистика за сегодня",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF212121)
                                    )
                                    Text(
                                        text = "Подробная информация",
                                        fontSize = 14.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        animatedDismiss()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Закрыть",
                                        tint = Color(0xFF757575)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Основной контент статистики
                            StatisticsContent(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsContent(viewModel: CalorieTrackerViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Секция калорий
        CaloriesSection(
            current = viewModel.dailyCalories,
            target = viewModel.userProfile.dailyCalories,
            color = MacroColors.Calories
        )

        // Макронутриенты
        MacronutrientsSection(
            proteins = MacroData(viewModel.dailyProtein, viewModel.userProfile.dailyProteins.toFloat()),
            fats = MacroData(viewModel.dailyFat, viewModel.userProfile.dailyFats.toFloat()),
            carbs = MacroData(viewModel.dailyCarbs, viewModel.userProfile.dailyCarbs.toFloat())
        )
    }
}

@Composable
private fun CaloriesSection(
    current: Int,
    target: Int,
    color: Color
) {
    val progress = if (target > 0) current.toFloat() / target else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "calories_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Калории",
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$current",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = " / $target",
                    fontSize = 18.sp,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Прогресс-бар
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE0E0E0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (current < target)
                    "Осталось ${target - current} ккал"
                else
                    "Превышено на ${current - target} ккал",
                fontSize = 13.sp,
                color = if (current <= target) Color(0xFF757575) else Color(0xFFE91E63)
            )
        }
    }
}

@Composable
private fun MacronutrientsSection(
    proteins: MacroData,
    fats: MacroData,
    carbs: MacroData
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MacroCard(
            title = "Белки",
            current = proteins.current,
            target = proteins.target,
            color = MacroColors.Proteins,
            modifier = Modifier.weight(1f)
        )

        MacroCard(
            title = "Жиры",
            current = fats.current,
            target = fats.target,
            color = MacroColors.Fats,
            modifier = Modifier.weight(1f)
        )

        MacroCard(
            title = "Углеводы",
            current = carbs.current,
            target = carbs.target,
            color = MacroColors.Carbs,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MacroCard(
    title: String,
    current: Float,
    target: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) current / target else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "${title}_progress"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Круговой прогресс
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 6.dp.toPx()
                    val startAngle = -90f
                    val sweepAngle = 360f * animatedProgress

                    // Фоновый круг
                    drawArc(
                        color = Color(0xFFE0E0E0),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )

                    // Прогресс
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = "${current.toInt()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "из ${target.toInt()}г",
                fontSize = 11.sp,
                color = Color(0xFF757575)
            )
        }
    }
}
// Модель данных для макронутриента
data class MacroData(
    val current: Float,
    val target: Float
)