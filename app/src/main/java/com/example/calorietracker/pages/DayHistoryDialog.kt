package com.example.calorietracker.pages

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.drawToBitmap
import com.example.calorietracker.data.DailyIntake
import com.example.calorietracker.data.DailyNutritionSummary
import com.example.calorietracker.FoodItem
import com.example.calorietracker.Meal
import com.example.calorietracker.MealType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

// Крутая переливающаяся радужная обводка
@Composable
fun AnimatedRainbowBorder(
    modifier: Modifier = Modifier,
    borderWidth: Dp = 3.dp,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rainbow")

    // Анимация вращения градиента
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Анимация пульсации
    val pulsate by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsate"
    )

    Box(modifier = modifier) {
        // Внешний контейнер с анимированным градиентом
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationZ = rotation
                }
                .drawBehind {
                    val strokeWidth = borderWidth.toPx()
                    val halfStroke = strokeWidth / 2
                    val radius = cornerRadius.toPx()

                    // Создаем радужный градиент
                    val colors = listOf(
                        Color(0xFFFF0080), // Розовый
                        Color(0xFFFF0040), // Красный
                        Color(0xFFFF8C00), // Оранжевый
                        Color(0xFFFFD700), // Золотой
                        Color(0xFF00FF00), // Зеленый
                        Color(0xFF00CED1), // Бирюзовый
                        Color(0xFF0080FF), // Синий
                        Color(0xFF8A2BE2), // Фиолетовый
                        Color(0xFFFF0080)  // Розовый (замыкаем круг)
                    )

                    val brush = Brush.sweepGradient(
                        colors = colors,
                        center = center
                    )

                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(halfStroke, halfStroke),
                        size = Size(
                            size.width - strokeWidth,
                            size.height - strokeWidth
                        ),
                        cornerRadius = CornerRadius(radius),
                        style = Stroke(width = strokeWidth * pulsate)
                    )
                }
        )

        // Внутренний белый контейнер
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(borderWidth)
                .clip(RoundedCornerShape(cornerRadius - borderWidth))
                .background(Color.White),
            content = content
        )
    }
}

// Основной диалог истории дня
@Composable
fun DayHistoryDialog(
    date: LocalDate,
    dailyIntake: DailyIntake,
    nutritionSummary: DailyNutritionSummary?,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    val density = LocalDensity.current
    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Форматтер для даты
    val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("ru"))

    LaunchedEffect(Unit) {
        delay(10)
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
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Размытый фон
            AnimatedVisibility(
                visible = isVisible && backgroundBitmap != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(100))
            ) {
                backgroundBitmap?.let { bitmap ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(20.dp),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.7f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { animatedDismiss() }
                        )
                    }
                }
            }

            // Контент диалога
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(200)) + scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                ),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.9f)
            ) {
                AnimatedRainbowBorder(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.8f)
                        .padding(16.dp),
                    borderWidth = 4.dp,
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        // Заголовок с датой и кнопкой закрытия
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = date.format(dateFormatter),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            IconButton(onClick = { animatedDismiss() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Суммарный КБЖУ
                        TotalNutritionCard(
                            calories = nutritionSummary?.totalCalories ?: dailyIntake.calories,
                            protein = nutritionSummary?.totalProtein ?: dailyIntake.protein,
                            fat = nutritionSummary?.totalFat ?: dailyIntake.fat,
                            carbs = nutritionSummary?.totalCarbs ?: dailyIntake.carbs
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Заголовок истории
                        Text(
                            text = "История питания",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Прокручиваемый список приемов пищи
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (dailyIntake.meals.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Нет записей о приемах пищи",
                                        color = Color.Gray,
                                        fontSize = 16.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    items(dailyIntake.meals) { meal ->
                                        MealCard(meal)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Карточка суммарного КБЖУ с новым дизайном
@Composable
private fun TotalNutritionCard(
    calories: Int,
    protein: Float,
    fat: Float,
    carbs: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Суммарный КБЖУ",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionValue(
                    label = "Калории",
                    value = calories.toString(),
                    color = Color(0xFF4CAF50),
                    icon = "🔥"
                )
                NutritionValue(
                    label = "Белки",
                    value = "%.1f г".format(protein),
                    color = Color(0xFF2196F3),
                    icon = "💪"
                )
                NutritionValue(
                    label = "Жиры",
                    value = "%.1f г".format(fat),
                    color = Color(0xFFFF9800),
                    icon = "🥑"
                )
                NutritionValue(
                    label = "Углеводы",
                    value = "%.1f г".format(carbs),
                    color = Color(0xFF9C27B0),
                    icon = "🍞"
                )
            }
        }
    }
}

// Компонент для отображения значения КБЖУ с иконкой
@Composable
private fun NutritionValue(
    label: String,
    value: String,
    color: Color,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            fontSize = 24.sp
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}

// Обновленная карточка приема пищи
@Composable
private fun MealCard(meal: Meal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFAFBFC)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = meal.type.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = "Всего: ${meal.foods.sumOf { it.calories }} ккал",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            meal.foods.forEach { food ->
                FoodItemRow(food)
                if (food != meal.foods.last()) {
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = Color.LightGray.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

// Строка с продуктом
@Composable
private fun FoodItemRow(food: FoodItem) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = food.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = food.weight,
                fontSize = 15.sp,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "${food.calories} ккал",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4CAF50)
            )
            Text(
                text = "Б: ${food.protein}г",
                fontSize = 13.sp,
                color = Color(0xFF2196F3)
            )
            Text(
                text = "Ж: ${food.fat}г",
                fontSize = 13.sp,
                color = Color(0xFFFF9800)
            )
            Text(
                text = "У: ${food.carbs}г",
                fontSize = 13.sp,
                color = Color(0xFF9C27B0)
            )
        }
    }
}