package com.example.calorietracker.pages

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.calorietracker.extensions.fancyShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.calorietracker.ui.components.AnimatedRainbowBorder
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.calorietracker.utils.NutritionFormatter
import kotlin.math.roundToInt

// Основной диалог истории дня с группировкой
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayHistoryDialog(
    date: LocalDate,
    dailyIntake: DailyIntake,
    nutritionSummary: DailyNutritionSummary?,
    onDismiss: () -> Unit,
    onMealUpdate: (Int, Meal) -> Unit = { _, _ -> },
    onMealDelete: (Int) -> Unit = {}
) {
    val view = LocalView.current
    val density = LocalDensity.current
    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Локальное состояние списка приемов пищи
    val meals = remember { mutableStateListOf<Meal>().apply { addAll(dailyIntake.meals) } }
    var editIndex by remember { mutableStateOf<Int?>(null) }

    // Группируем приемы пищи по типу
    val groupedMeals = remember(meals.toList()) {
        meals.groupBy { it.type }
            .toSortedMap(compareBy { mealType ->
                when (mealType) {
                    MealType.BREAKFAST -> 1
                    MealType.LATE_BREAKFAST -> 2
                    MealType.LUNCH -> 3
                    MealType.SNACK -> 4
                    MealType.DINNER -> 5
                    MealType.SUPPER -> 6
                }
            })
    }

    val totals by remember {
        derivedStateOf {
            var cals = 0
            var prot = 0f
            var fat = 0f
            var carbs = 0f
            meals.forEach { meal ->
                meal.foods.forEach { food ->
                    cals += food.calories
                    prot += food.protein.toFloat()
                    fat += food.fat.toFloat()
                    carbs += food.carbs.toFloat()
                }
            }
            Totals(cals, prot, fat, carbs)
        }
    }

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
                        .fillMaxSize()
                        .padding(24.dp),
                    borderWidth = 8.dp,
                    cornerRadius = 24.dp
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color.White.copy(alpha = 0.95f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = date.format(dateFormatter),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { animatedDismiss() }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Закрыть",
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        // Группированные приемы пищи
                        groupedMeals.forEach { (mealType, mealsOfType) ->
                            item {
                                // Заголовок группы
                                MealTypeHeader(
                                    mealType = mealType,
                                    totalCalories = mealsOfType.sumOf { meal ->
                                        meal.foods.sumOf { it.calories }
                                    }
                                )
                            }

                            // Продукты в группе
                            mealsOfType.forEach { meal ->
                                meal.foods.forEach { food ->
                                    item {
                                        FoodItemCard(food = food)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Итоговая статистика
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF0F0F0)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Итого за день",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        NutritionInfo("Калории", totals.calories.toString(), Color(0xFF4CAF50))
                                        NutritionInfo("Белки", NutritionFormatter.formatMacro(totals.proteins), Color(0xFF2196F3))
                                        NutritionInfo("Жиры", NutritionFormatter.formatMacro(totals.fats), Color(0xFFFFC107))
                                        NutritionInfo("Углеводы", NutritionFormatter.formatMacro(totals.carbs), Color(0xFFFF5722))
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

// Заголовок группы приемов пищи
@Composable
private fun MealTypeHeader(
    mealType: MealType,
    totalCalories: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (mealType) {
                MealType.BREAKFAST -> Color(0xFFFFF3E0)
                MealType.LUNCH -> Color(0xFFE8F5E9)
                MealType.DINNER -> Color(0xFFE3F2FD)
                MealType.SNACK -> Color(0xFFFCE4EC)
                MealType.LATE_BREAKFAST -> Color(0xFFF3E5F5)
                MealType.SUPPER -> Color(0xFFE0F2F1)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = mealType.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$totalCalories ккал",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

// Карточка продукта с AI мнением (если есть)
@Composable
private fun FoodItemCard(food: FoodItem) {
    var showAiOpinion by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Название и AI кнопка
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = food.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                // Показываем кнопку AI только если есть мнение
                if (food.aiOpinion != null) {
                    IconButton(
                        onClick = { showAiOpinion = !showAiOpinion },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "AI мнение",
                            tint = if (showAiOpinion) Color(0xFF4CAF50) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Макронутриенты в ряд
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MacroChip("Калории", food.calories.toString(), Color.Black)
                MacroChip("Белки", "${food.protein}г", Color.Gray)
                MacroChip("Жиры", "${food.fat}г", Color.Gray)
                MacroChip("Углеводы", "${food.carbs}г", Color.Gray)
            }

            // Вес
            Text(
                text = "Вес: ${food.weight}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            // AI мнение (раскрывающееся)
            AnimatedVisibility(
                visible = showAiOpinion && food.aiOpinion != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                food.aiOpinion?.let { opinion ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = opinion,
                                fontSize = 13.sp,
                                color = Color(0xFF424242),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Чип для макронутриента
@Composable
private fun MacroChip(
    label: String,
    value: String,
    textColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

// Компонент для отображения макронутриента
@Composable
private fun NutritionInfo(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

// Data class для итогов
private data class Totals(
    val calories: Int,
    val proteins: Float,
    val fats: Float,
    val carbs: Float
)