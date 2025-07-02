package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.FoodItem
import com.example.calorietracker.MealType
import com.example.calorietracker.ui.animations.*
import kotlinx.coroutines.delay

// Улучшенные анимированные прогресс-бары
@Composable
fun FixedAnimatedProgressBars(viewModel: CalorieTrackerViewModel) {
    var expanded by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    val containerHeight by animateDpAsState(
        targetValue = if (expanded) 240.dp else 120.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "height"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(containerHeight)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = !expanded
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Градиентный фон
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFFAFBFC)
                            )
                        )
                    )
            )

            // Контент
            Column(modifier = Modifier.fillMaxSize()) {
                // Развернутый вид
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    FixedExpandedProgressView(viewModel = viewModel)
                }

                // Свернутый вид
                AnimatedVisibility(
                    visible = !expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    FixedCollapsedProgressView(viewModel = viewModel)
                }
            }

            // Индикатор состояния внизу
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE0E0E0))
            ) {
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 0f else 180f,
                    animationSpec = tween(300),
                    label = "rotation"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = rotation }
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFBDBDBD),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

// Исправленный развернутый вид
@Composable
private fun FixedExpandedProgressView(viewModel: CalorieTrackerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Сегодняшний прогресс",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF0F0F0)
            ) {
                Text(
                    "${viewModel.dailyIntake.calories} / ${viewModel.userProfile.dailyCalories} ккал",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Прогресс-бары
        val nutrients = listOf(
            NutrientProgressData(
                label = "Калории",
                current = viewModel.dailyIntake.calories,
                target = viewModel.userProfile.dailyCalories,
                unit = "ккал",
                icon = Icons.Default.LocalFireDepartment,
                color = viewModel.getProgressColor(
                    viewModel.dailyIntake.calories,
                    viewModel.userProfile.dailyCalories
                )
            ),
            NutrientProgressData(
                label = "Белки",
                current = viewModel.dailyIntake.proteins,
                target = viewModel.userProfile.dailyProteins,
                unit = "г",
                icon = Icons.Default.FitnessCenter,
                color = viewModel.getProgressColor(
                    viewModel.dailyIntake.proteins,
                    viewModel.userProfile.dailyProteins
                )
            ),
            NutrientProgressData(
                label = "Жиры",
                current = viewModel.dailyIntake.fats,
                target = viewModel.userProfile.dailyFats,
                unit = "г",
                icon = Icons.Default.WaterDrop,
                color = viewModel.getProgressColor(
                    viewModel.dailyIntake.fats,
                    viewModel.userProfile.dailyFats
                )
            ),
            NutrientProgressData(
                label = "Углеводы",
                current = viewModel.dailyIntake.carbs,
                target = viewModel.userProfile.dailyCarbs,
                unit = "г",
                icon = Icons.Default.Grain,
                color = viewModel.getProgressColor(
                    viewModel.dailyIntake.carbs,
                    viewModel.userProfile.dailyCarbs
                )
            )
        )

        nutrients.forEachIndexed { index, nutrient ->
            FixedProgressBar(
                nutrient = nutrient,
                delay = index * 100
            )
        }
    }
}

// Исправленная полоса прогресса
@Composable
private fun FixedProgressBar(
    nutrient: NutrientProgressData,
    delay: Int = 0
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }

    val progress = if (nutrient.target > 0) {
        (nutrient.current.toFloat() / nutrient.target.toFloat()).coerceIn(0f, 1.5f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = if (visible) progress else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "progress"
    )

    val animatedColor by animateColorAsState(
        targetValue = nutrient.color,
        animationSpec = tween(500),
        label = "color"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { -50 })
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Иконка в круге
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = animatedColor.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    nutrient.icon,
                    contentDescription = null,
                    tint = animatedColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        nutrient.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    Text(
                        "${nutrient.current} / ${nutrient.target} ${nutrient.unit}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Прогресс-бар
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    // Фоновая дорожка
                    Canvas(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        drawRoundRect(
                            color = Color(0xFFEEEEEE),
                            size = size,
                            cornerRadius = CornerRadius(5.dp.toPx())
                        )
                    }

                    // Прогресс
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                    ) {
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    animatedColor.copy(alpha = 0.8f),
                                    animatedColor
                                )
                            ),
                            size = size,
                            cornerRadius = CornerRadius(5.dp.toPx())
                        )
                    }

                    // Индикатор переедания
                    if (progress > 1f) {
                        val overeatingProgress = (progress - 1f) / 0.5f
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth(overeatingProgress.coerceIn(0f, 1f))
                                .fillMaxHeight()
                        ) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Red.copy(alpha = 0.3f),
                                        Color.Red.copy(alpha = 0.5f)
                                    )
                                ),
                                size = size,
                                cornerRadius = CornerRadius(5.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

// Исправленный свернутый вид
@Composable
private fun FixedCollapsedProgressView(viewModel: CalorieTrackerViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val nutrients = listOf(
                CompactNutrientData("К", viewModel.dailyIntake.calories, viewModel.userProfile.dailyCalories, Color(0xFFFF5722)),
                CompactNutrientData("Б", viewModel.dailyIntake.proteins, viewModel.userProfile.dailyProteins, Color(0xFF2196F3)),
                CompactNutrientData("Ж", viewModel.dailyIntake.fats, viewModel.userProfile.dailyFats, Color(0xFFFFC107)),
                CompactNutrientData("У", viewModel.dailyIntake.carbs, viewModel.userProfile.dailyCarbs, Color(0xFF9C27B0))
            )

            nutrients.forEachIndexed { index, nutrient ->
                FixedCompactIndicator(
                    nutrient = nutrient,
                    delay = index * 100
                )
            }
        }
    }
}

// Исправленный компактный индикатор
@Composable
private fun FixedCompactIndicator(
    nutrient: CompactNutrientData,
    delay: Int
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }

    val progress = if (nutrient.target > 0) {
        nutrient.current.toFloat() / nutrient.target.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = if (visible) progress else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            delayMillis = delay,
            easing = FastOutSlowInEasing
        ),
        label = "progress"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Кольцевой индикатор
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(64.dp)
            ) {
                // Фоновое кольцо
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFFEEEEEE),
                        radius = size.minDimension / 2,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }

                // Прогресс
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = nutrient.color,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(
                            width = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                // Текст в центре
                Text(
                    text = nutrient.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // Процент под кольцом
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = nutrient.color
            )
        }
    }
}

// Вспомогательные data классы
private data class NutrientProgressData(
    val label: String,
    val current: Int,
    val target: Int,
    val unit: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

private data class CompactNutrientData(
    val label: String,
    val current: Int,
    val target: Int,
    val color: Color
)
