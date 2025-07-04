package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.calorietracker.ui.animations.StaggeredAnimatedList
import com.example.calorietracker.ui.animations.TypewriterText
import kotlinx.coroutines.delay

// Анимированные прогресс-бары с коллапсом
@Composable
fun AnimatedProgressBars(viewModel: CalorieTrackerViewModel) {
    var expanded by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    // Анимация при изменении значений
    LaunchedEffect(viewModel.dailyIntake) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val containerHeight by animateDpAsState(
        targetValue = if (expanded) 200.dp else 100.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "height"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(containerHeight)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = !expanded
            }
    ) {
        // Развернутый вид
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ExpandedProgressView(viewModel = viewModel)
        }

        // Свернутый вид
        AnimatedVisibility(
            visible = !expanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CollapsedProgressView(viewModel = viewModel)
        }

        // *** ИЗМЕНЕНИЕ: СТРЕЛКА-ИНДИКАТОР ПОЛНОСТЬЮ УДАЛЕНА ***
    }
}

// Развернутый вид прогресса БЕЗ ИКОНОК ЕДЫ
@Composable
private fun ExpandedProgressView(viewModel: CalorieTrackerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // *** ИЗМЕНЕНИЕ: Отступы сделаны симметричными, так как стрелки больше нет ***
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceAround
    ) {
        val nutrients = listOf(
            NutrientData(
                label = "Калории",
                current = viewModel.dailyIntake.calories,
                target = viewModel.userProfile.dailyCalories,
                unit = "ккал",
                color = viewModel.getProgressColor(
                    viewModel.dailyIntake.calories,
                    viewModel.userProfile.dailyCalories
                )
            ),
            NutrientData(
                label = "Белки",
                current = viewModel.dailyIntake.protein.toInt(),
                target = viewModel.userProfile.dailyProteins,
                unit = "г",
                color = viewModel.getProgressColor(
                    viewModel.dailyIntake.protein.toInt(),
                    viewModel.userProfile.dailyProteins
                )
            ),
            NutrientData(
                label = "Жиры",
                current = viewModel.dailyIntake.fat.toInt(),
                target = viewModel.userProfile.dailyFats,
                unit = "г",
                color = viewModel.getProgressColor(
                    viewModel.dailyIntake.fat.toInt(),
                    viewModel.userProfile.dailyFats
                )
            ),
            NutrientData(
                label = "Углеводы",
                current = viewModel.dailyIntake.carbs.toInt(),
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
        nutrient.current.toFloat() / nutrient.target.toFloat()
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
                text = "${nutrient.current}/${nutrient.target}",
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

// Улучшенный компонент для отображения нутриента БЕЗ ИКОНОК
@Composable
private fun AnimatedNutrientBar(nutrient: NutrientData) {
    val progress = if (nutrient.target > 0) nutrient.current.toFloat() / nutrient.target.toFloat() else 0f

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
                        text = "${nutrient.current}",
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

// Свернутый вид прогресса
@Composable
private fun CollapsedProgressView(viewModel: CalorieTrackerViewModel) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp), // Добавили вертикальные отступы
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val nutrients = listOf(
            Triple("К", viewModel.dailyIntake.calories, viewModel.userProfile.dailyCalories),
            Triple("Б", viewModel.dailyIntake.protein.toInt(), viewModel.userProfile.dailyProteins),
            Triple("Ж", viewModel.dailyIntake.fat.toInt(), viewModel.userProfile.dailyFats),
            Triple("У", viewModel.dailyIntake.carbs.toInt(), viewModel.userProfile.dailyCarbs)
        )

        nutrients.forEachIndexed { index, (label, current, target) ->
            Box( // Используем Box для фиксированного размера
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp), // Фиксированная высота
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Кольцо БЕЗ анимации масштаба
                    SimpleRingIndicator(
                        label = label,
                        current = current,
                        target = target,
                        color = viewModel.getProgressColor(current, target)
                    )
                }
            }
        }
    }
}

// Простое кольцо без анимации масштабирования
@Composable
fun SimpleRingIndicator(
    label: String,
    current: Int,
    target: Int,
    color: Color
) {
    val progress = if (target > 0) current.toFloat() / target.toFloat() else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "ring"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(60.dp) // Уменьшили размер с 60dp
    ) {
        // Фоновое кольцо
        CircularProgressIndicator(
            progress = { 1f },
            color = Color(0xFFE5E7EB).copy(alpha = 0.3f),
            strokeWidth = 6.dp, // Уменьшили толщину
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxSize()
        )

        // Прогресс с плавной анимацией
        CircularProgressIndicator(
            progress = { animatedProgress.coerceIn(0f, 1f) },
            color = color,
            strokeWidth = 6.dp,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxSize()
        )

        // Буква в центре
        Text(
            text = label,
            fontSize = 18.sp, // Уменьшили размер
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )
    }
}

// Модель данных для нутриента БЕЗ ИКОНКИ
data class NutrientData(
    val label: String,
    val current: Int,
    val target: Int,
    val unit: String,
    val color: Color
)

// Анимированная карточка подтверждения еды
@Composable
fun AnimatedPendingFoodCard(
    food: FoodItem,
    selectedMeal: MealType,
    onMealChange: (MealType) -> Unit,
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

                // Выбор приема пищи
                AnimatedMealSelector(
                    selectedMeal = selectedMeal,
                    onMealChange = onMealChange
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
        "Белки: ${food.protein} г",
        "Жиры: ${food.fat} г",
        "Углеводы: ${food.carbs} г",
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


// Анимированный селектор приема пищи
@Composable
private fun AnimatedMealSelector(
    selectedMeal: MealType,
    onMealChange: (MealType) -> Unit
) {
    var showMeals by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        showMeals = true
    }

    AnimatedVisibility(
        visible = showMeals,
        enter = fadeIn() + expandVertically()
    ) {
        Column {
            Text(
                "Приём пищи:",
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            val meals = MealType.values().toList()
            val firstRow = meals.take(3)
            val secondRow = meals.drop(3)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    firstRow.forEach { meal ->
                        AnimatedMealButton(
                            meal = meal,
                            isSelected = meal == selectedMeal,
                            onClick = { onMealChange(meal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    secondRow.forEach { meal ->
                        AnimatedMealButton(
                            meal = meal,
                            isSelected = meal == selectedMeal,
                            onClick = { onMealChange(meal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// Анимированная кнопка приема пищи
@Composable
private fun AnimatedMealButton(
    meal: MealType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color.Black else Color(0xFFE5E7EB),
        animationSpec = tween(200),
        label = "bg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.Black,
        animationSpec = tween(200),
        label = "content"
    )

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(0),
        label = "scale"
    )

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        modifier = modifier
            .height(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentPadding = PaddingValues(horizontal = 0.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Text(
            meal.displayName,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}