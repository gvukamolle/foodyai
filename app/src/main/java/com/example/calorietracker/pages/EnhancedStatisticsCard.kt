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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.drawToBitmap
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.extensions.fancyShadow
import com.example.calorietracker.utils.NutritionFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image

// Цвета для макронутриентов
object MacroColors {
    val Proteins = Color(0xFF00BFA5) // Бирюзовый
    val Fats = Color(0xFFFFB74D) // Оранжевый
    val Carbs = Color(0xFF64B5F6) // Голубой
    val Calories = Color(0xFFE91E63) // Розовый
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedStatisticsCard(
    viewModel: CalorieTrackerViewModel,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    
    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(5)
        try {
            backgroundBitmap = view.drawToBitmap()
        } catch (e: Exception) { /* ignore */ }
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
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { animatedDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Размытый фон
            AnimatedVisibility(
                visible = isVisible && backgroundBitmap != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(100))
            ) {
                backgroundBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(
                                radiusX = animateDpAsState(
                                    if (isVisible) 20.dp else 0.dp, 
                                    tween(200), 
                                    "blur"
                                ).value,
                                radiusY = animateDpAsState(
                                    if (isVisible) 20.dp else 0.dp, 
                                    tween(200), 
                                    "blur"
                                ).value
                            ),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.7f))
                    )
                }
            }

            // Карточка статистики
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.9f, 
                            transformOrigin = TransformOrigin.Center, 
                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(tween(150)) + 
                      scaleOut(targetScale = 0.9f, transformOrigin = TransformOrigin.Center)
            ) {
                Card(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 360.dp)
                        .fancyShadow(
                            borderRadius = 24.dp, 
                            shadowRadius = 12.dp, 
                            alpha = 0.35f, 
                            color = MacroColors.Calories
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Заголовок с кнопкой закрытия
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Статистика дня",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    animatedDismiss()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        // Дата с навигацией
                        Spacer(Modifier.height(8.dp))
                        DateNavigator(
                            currentDate = viewModel.currentDate,
                            onDateChange = { newDate ->
                                // Здесь можно добавить логику загрузки данных для другого дня
                                // viewModel.loadDataForDate(newDate)
                            }
                        )
                        
                        // Кнопка "Сегодня" если не на сегодняшней дате
                        AnimatedVisibility(
                            visible = viewModel.currentDate != java.time.LocalDate.now(),
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // viewModel.loadDataForDate(java.time.LocalDate.now())
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Перейти к сегодня",
                                    color = MacroColors.Calories,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Основной прогресс калорий
                        CaloriesProgressBar(
                            current = viewModel.dailyIntake.calories,
                            target = viewModel.userProfile.dailyCalories,
                            color = viewModel.getProgressColor(
                                viewModel.dailyIntake.calories,
                                viewModel.userProfile.dailyCalories
                            )
                        )

                        Spacer(Modifier.height(32.dp))

                        // Кольца макронутриентов
                        MacroRings(
                            proteins = MacroData(
                                current = viewModel.dailyIntake.protein,
                                target = viewModel.userProfile.dailyProteins.toFloat()
                            ),
                            fats = MacroData(
                                current = viewModel.dailyIntake.fat,
                                target = viewModel.userProfile.dailyFats.toFloat()
                            ),
                            carbs = MacroData(
                                current = viewModel.dailyIntake.carbs,
                                target = viewModel.userProfile.dailyCarbs.toFloat()
                            )
                        )

                        Spacer(Modifier.height(24.dp))

                        // Легенда макронутриентов
                        MacroLegend(
                            proteins = viewModel.dailyIntake.protein,
                            proteinsTarget = viewModel.userProfile.dailyProteins,
                            fats = viewModel.dailyIntake.fat,
                            fatsTarget = viewModel.userProfile.dailyFats,
                            carbs = viewModel.dailyIntake.carbs,
                            carbsTarget = viewModel.userProfile.dailyCarbs
                        )
                    }
                }
            }
        }
    }
}

// Основной прогресс-бар калорий
@Composable
private fun CaloriesProgressBar(
    current: Int,
    target: Int,
    color: Color
) {
    val progress = if (target > 0) current.toFloat() / target else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "calories_progress"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Большое число калорий
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = current.toString(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "ккал",
                fontSize = 20.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Цель
        Text(
            text = "из $target ккал",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(16.dp))

        // Прогресс-бар
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE5E7EB).copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Процент
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (progress > 1f) Color(0xFFE53935) else color
        )
    }
}

// Кольца макронутриентов в стиле Apple Watch
@Composable
private fun MacroRings(
    proteins: MacroData,
    fats: MacroData,
    carbs: MacroData
) {
    val proteinProgress by animateFloatAsState(
        targetValue = (proteins.current / proteins.target).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, delayMillis = 100),
        label = "protein_ring"
    )
    
    val fatProgress by animateFloatAsState(
        targetValue = (fats.current / fats.target).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, delayMillis = 200),
        label = "fat_ring"
    )
    
    val carbProgress by animateFloatAsState(
        targetValue = (carbs.current / carbs.target).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, delayMillis = 300),
        label = "carb_ring"
    )

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Внешнее кольцо - Углеводы
            drawRing(
                color = MacroColors.Carbs,
                progress = carbProgress,
                strokeWidth = 24.dp,
                radius = 95.dp
            )
            
            // Среднее кольцо - Жиры
            drawRing(
                color = MacroColors.Fats,
                progress = fatProgress,
                strokeWidth = 24.dp,
                radius = 65.dp
            )
            
            // Внутреннее кольцо - Белки
            drawRing(
                color = MacroColors.Proteins,
                progress = proteinProgress,
                strokeWidth = 24.dp,
                radius = 35.dp
            )
        }
        
        // Центральная метка БЖУ
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "БЖУ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            // Общий процент выполнения
            val totalProgress = (proteinProgress + fatProgress + carbProgress) / 3
            Text(
                text = "${(totalProgress * 100).toInt()}%",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

// Функция отрисовки кольца
private fun DrawScope.drawRing(
    color: Color,
    progress: Float,
    strokeWidth: androidx.compose.ui.unit.Dp,
    radius: androidx.compose.ui.unit.Dp
) {
    val strokePx = strokeWidth.toPx()
    val radiusPx = radius.toPx()
    val center = Offset(size.width / 2, size.height / 2)
    
    // Фоновое кольцо
    drawCircle(
        color = color.copy(alpha = 0.2f),
        radius = radiusPx,
        center = center,
        style = Stroke(strokePx)
    )
    
    // Прогресс
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
        size = Size(radiusPx * 2, radiusPx * 2),
        style = Stroke(strokePx, cap = StrokeCap.Round)
    )
}

// Легенда макронутриентов
@Composable
private fun MacroLegend(
    proteins: Float,
    proteinsTarget: Int,
    fats: Float,
    fatsTarget: Int,
    carbs: Float,
    carbsTarget: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MacroLegendItem(
            color = MacroColors.Proteins,
            label = "Белки",
            current = proteins,
            target = proteinsTarget
        )
        
        MacroLegendItem(
            color = MacroColors.Fats,
            label = "Жиры",
            current = fats,
            target = fatsTarget
        )
        
        MacroLegendItem(
            color = MacroColors.Carbs,
            label = "Углеводы",
            current = carbs,
            target = carbsTarget
        )
    }
}

@Composable
private fun MacroLegendItem(
    color: Color,
    label: String,
    current: Float,
    target: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Цветной индикатор
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        
        Text(
            text = "${NutritionFormatter.formatMacroInt(current)}/$target г",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
    }
}

// Навигатор дат
@Composable
private fun DateNavigator(
    currentDate: java.time.LocalDate,
    onDateChange: (java.time.LocalDate) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isToday = currentDate == java.time.LocalDate.now()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Кнопка назад
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDateChange(currentDate.minusDays(1))
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowLeft,
                contentDescription = "Предыдущий день",
                tint = Color.Black
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        // Текущая дата
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val dayOfWeek = when(currentDate.dayOfWeek.value) {
                1 -> "Понедельник"
                2 -> "Вторник"
                3 -> "Среда"
                4 -> "Четверг"
                5 -> "Пятница"
                6 -> "Суббота"
                7 -> "Воскресенье"
                else -> ""
            }
            
            Text(
                text = if (isToday) "Сегодня" else dayOfWeek,
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            val month = when(currentDate.monthValue) {
                1 -> "января"
                2 -> "февраля"
                3 -> "марта"
                4 -> "апреля"
                5 -> "мая"
                6 -> "июня"
                7 -> "июля"
                8 -> "августа"
                9 -> "сентября"
                10 -> "октября"
                11 -> "ноября"
                12 -> "декабря"
                else -> ""
            }
            
            Text(
                text = "${currentDate.dayOfMonth} $month",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        // Кнопка вперед (неактивна, если сегодня)
        IconButton(
            onClick = {
                if (!isToday) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDateChange(currentDate.plusDays(1))
                }
            },
            enabled = !isToday,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Следующий день",
                tint = if (isToday) Color.Gray.copy(alpha = 0.5f) else Color.Black
            )
        }
    }
}

// Модель данных для макронутриента
data class MacroData(
    val current: Float,
    val target: Float
)
