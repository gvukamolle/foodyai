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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
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
import com.example.calorietracker.extensions.fancyShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Крутая переливающаяся радужная обводка
@Composable
fun AnimatedRainbowBorder(
    modifier: Modifier = Modifier,
    borderWidth: Dp = 12.dp,
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rainbow")

    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hue"
    )

    val color = Color.hsv(hue, 1f, 1f)

    Box(
        modifier = modifier
            .fancyShadow(
                color = color,
                borderRadius = cornerRadius,
                shadowRadius = borderWidth,
                alpha = 0.9f
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color.White)
    ) {
        Box(modifier = Modifier.padding(borderWidth)) {
            content()
        }
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
                    borderWidth = 8.dp,
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
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = date.format(dateFormatter),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
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

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NutritionStat(
                        "Калории",
                        "$calories ккал",
                        Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    NutritionStat(
                        "Белки",
                        "%.1f г".format(protein),
                        Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NutritionStat(
                        "Жиры",
                        "%.1f г".format(fat),
                        Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                    NutritionStat(
                        "Углеводы",
                        "%.1f г".format(carbs),
                        Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Компонент для отображения значения КБЖУ
@Composable
private fun NutritionStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
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
                val totalWeight = meal.foods.sumOf { it.weight.toIntOrNull() ?: 0 }
                Text(
                    text = "Масса: ${totalWeight} г",
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
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroStat(
                    "Калории",
                    "${food.calories} ккал",
                    Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                MacroStat(
                    "Белки",
                    "${food.protein} г",
                    Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroStat(
                    "Жиры",
                    "${food.fat} г",
                    Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
                MacroStat(
                    "Углеводы",
                    "${food.carbs} г",
                    Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MacroStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

private fun parseWeight(weight: String): Int {
    return weight.filter { it.isDigit() }.toIntOrNull() ?: 0
    }