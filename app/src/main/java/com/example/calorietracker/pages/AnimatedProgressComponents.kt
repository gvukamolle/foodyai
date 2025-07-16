package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.FoodItem
import com.example.calorietracker.MealType
import com.example.calorietracker.ui.animations.StaggeredAnimatedList
import com.example.calorietracker.ui.animations.TypewriterText
import kotlinx.coroutines.delay
import kotlin.math.ceil
import com.example.calorietracker.utils.NutritionFormatter
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically


// Анимированные прогресс-бары с коллапсом
@Composable
fun AnimatedProgressBars(
    viewModel: CalorieTrackerViewModel,
    isVisible: Boolean // Новый параметр
) {
    AnimatedContent(
        targetState = isVisible,
        transitionSpec = {
            if (targetState) {
                (fadeIn() + expandVertically()) togetherWith (fadeOut() + shrinkVertically())
            } else {
                (fadeIn() + expandVertically()) togetherWith (fadeOut() + shrinkVertically())
            }
        },
        label = "progress_bars"
    ) { expanded ->
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // Фиксированная высота для развернутого вида
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                ExpandedProgressView(viewModel = viewModel)
            }
        } else {
            CollapsedProgressView(viewModel = viewModel)
            }
        }
    }


// Свернутый вид прогресса - отображает только калории
@Composable
private fun CollapsedProgressView(viewModel: CalorieTrackerViewModel) {
    val calorieData = NutrientData(
        label = "Калории",
        current = viewModel.dailyIntake.calories.toFloat(),
        target = viewModel.userProfile.dailyCalories,
        unit = "ккал",
        color = viewModel.getProgressColor(
            viewModel.dailyIntake.calories,
            viewModel.userProfile.dailyCalories
        )
    )
    LabelLessNutrientBar(
        nutrient = calorieData,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 4.dp)
    )
}

// Прогресс-бар без подписей, повторяет размеры полноразмерного бара
@Composable
private fun LabelLessNutrientBar(
    nutrient: NutrientData,
    modifier: Modifier = Modifier
) {
    val progress = if (nutrient.target > 0) {
        nutrient.current / nutrient.target.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFE5E7EB).copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(nutrient.color)
        )
    }
}

// Развернутый вид прогресса БЕЗ ИКОНОК ЕДЫ
@Composable
private fun ExpandedProgressView(viewModel: CalorieTrackerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // *** ИЗМЕНЕНИЕ: Отступы сделаны симметричными, так как стрелки больше нет ***
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceAround
    ) {
        val nutrients = listOf(
            NutrientData(
                label = "Калории",
                current = viewModel.dailyIntake.calories.toFloat(),
                target = viewModel.userProfile.dailyCalories,
                unit = "ккал",
                color = viewModel.getProgressColor(
                    viewModel.dailyIntake.calories,
                    viewModel.userProfile.dailyCalories
                )
            ),
            NutrientData(
                label = "Белки",
                current = viewModel.dailyIntake.protein,
                target = viewModel.userProfile.dailyProteins,
                unit = "г",
                color = viewModel.getProgressColor(
                    viewModel.dailyIntake.protein.toInt(),
                    viewModel.userProfile.dailyProteins
                )
            ),
            NutrientData(
                label = "Жиры",
                current = viewModel.dailyIntake.fat,
                target = viewModel.userProfile.dailyFats,
                unit = "г",
                color = viewModel.getProgressColor(
                    viewModel.dailyIntake.fat.toInt(),
                    viewModel.userProfile.dailyFats
                )
            ),
            NutrientData(
                label = "Углеводы",
                current = viewModel.dailyIntake.carbs,
                target = viewModel.userProfile.dailyCarbs,
                unit = "г",
                color = viewModel.getProgressColor(
                    viewModel.dailyIntake.carbs.toInt(),
                    viewModel.userProfile.dailyCarbs
                )
            )
        )

        nutrients.forEach { nutrient ->
            CompactNutrientBar(nutrient = nutrient)
        }
    }
}

// Компактный бар для нутриента
@Composable
private fun CompactNutrientBar(nutrient: NutrientData) {
    val progress = if (nutrient.target > 0) {
        nutrient.current / nutrient.target.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress"
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Верхний ряд с подписями
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = nutrient.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = "${NutritionFormatter.formatMacroInt(nutrient.current)} / ${nutrient.target}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Прогресс-бар
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE5E7EB).copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(nutrient.color)
            )
        }
    }
}

// Мини-бар для свернутого вида
@Composable
private fun MiniNutrientBar(
    nutrient: NutrientData,
    modifier: Modifier = Modifier
) {    val progress = if (nutrient.target > 0) {
        nutrient.current / nutrient.target.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = nutrient.label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = "${NutritionFormatter.formatMacroInt(nutrient.current)} / ${nutrient.target}",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFE5E7EB).copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(nutrient.color)
            )
        }
    }
}

// Улучшенный компонент для отображения нутриента БЕЗ ИКОНОК
@Composable
private fun AnimatedNutrientBar(nutrient: NutrientData) {
    val progress = if (nutrient.target > 0) nutrient.current / nutrient.target.toFloat() else 0f

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isVisible) progress else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val animatedColor by animateColorAsState(
        targetValue = nutrient.color,
        animationSpec = tween(durationMillis = 300),
        label = "color"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Название и значение
            Column {
                Text(
                    text = nutrient.label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = NutritionFormatter.formatMacro(nutrient.current),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = animatedColor
                    )
                    Text(
                        text = "/ ${nutrient.target} ${nutrient.unit}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            // Процент с анимацией
            Text(
                text = "${(animatedProgress.coerceIn(0f, 2f) * 100).toInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (progress > 1f) Color(0xFFE53935) else animatedColor
            )
        }

        // Прогресс-бар
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE5E7EB).copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(animatedColor)
            )

            // Индикатор переедания
            if (progress > 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .graphicsLayer { alpha = 0.3f }
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "overeating")
                    val animatedAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0.6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            color = Color.Red.copy(alpha = animatedAlpha),
                            size = size
                        )
                    }
                }
            }
        }
    }
}

// Модель данных для нутриента БЕЗ ИКОНКИ
data class NutrientData(
    val label: String,
    val current: Float,
    val target: Int,
    val unit: String,
    val color: Color
)

// Анимированная карточка подтверждения еды
@Composable
fun AnimatedPendingFoodCard(
    food: FoodItem,
    selectedMeal: MealType,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(food) {
        delay(100)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.95f),
        exit = fadeOut() + scaleOut(targetScale = 0.95f)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Заголовок с анимацией
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    TypewriterText(
                        text = "Подтвердите данные",
                        style = androidx.compose.ui.text.TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    )
                }

                // Данные о еде с анимацией
                AnimatedFoodDetails(food = food)

                // Автоматически выбранный прием пищи
                Text(
                    text = "Приём пищи: ${selectedMeal.displayName}",
                    fontSize = 14.sp,
                    color = Color.Black
                )

                // Кнопки действий
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCancel()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfirm()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Подтвердить")
                    }
                }
            }
        }
    }
}

// ИЗМЕНЕНО: StaggeredAnimatedList заменен на обычный Column
@Composable
private fun AnimatedFoodDetails(food: FoodItem) {
    val details = listOf(
        "Блюдо: ${food.name}",
        "Калории: ${food.calories}",
        "Белки: ${NutritionFormatter.formatMacro(food.protein.toFloat())} г",
        "Жиры: ${NutritionFormatter.formatMacro(food.fat.toFloat())} г",
        "Углеводы: ${NutritionFormatter.formatMacro(food.carbs.toFloat())} г",
        "Вес: ${food.weight} г"
    )

    // Используем Column и forEach для мгновенного отображения всех элементов
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { // Добавим небольшой отступ
        details.forEach { detail ->
            Text(
                text = detail,
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}