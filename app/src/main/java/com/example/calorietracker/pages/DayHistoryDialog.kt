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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.calorietracker.utils.NutritionFormatter
import kotlin.math.roundToInt


@Composable
fun AiInfoButton(
    hasAiOpinion: Boolean,
    onClick: () -> Unit
) {

    // Зеленая кнопка-бокс
    Surface(
        onClick = { if (hasAiOpinion) onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFDBF0E4), // Зеленый цвет
        modifier = Modifier.height(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF00BA65)
            )
            Text(
                text = "Foody AI",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00BA65)
            )
            if (hasAiOpinion) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = Color(0xFF00BA65).copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Информация",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF00BA65)
                    )
                }
            }
        }
    }
}

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
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
    var showAiOpinionDialog by remember { mutableStateOf(false) }
    var aiOpinionText by remember { mutableStateOf<String?>(null) }

    // Группируем приемы пищи по типу, сохраняя индекс
    val groupedMeals = remember(meals.toList()) {
        meals.withIndex()
            .groupBy({ it.value.type }, { it })
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
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(24.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        stickyHeader {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                            ) {
                                Text(
                                    text = date.format(dateFormatter),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                                IconButton(
                                    onClick = { animatedDismiss() },
                                    modifier = Modifier.align(Alignment.CenterStart)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        contentDescription = "Назад",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }

                        // Группированные приемы пищи
                        groupedMeals.forEach { (mealType, mealsOfType) ->
                            item {
                                MealTypeHeader(
                                    mealType = mealType,
                                    totalCalories = mealsOfType.sumOf { indexed ->
                                        indexed.value.foods.sumOf { it.calories }
                                    }
                                )
                            }

                            // Продукты в группе
                            mealsOfType.forEach { indexedMeal ->
                                val mealIndex = indexedMeal.index
                                val meal = indexedMeal.value
                                val food = meal.foods.firstOrNull()
                                if (food != null) {
                                    item {
                                        FoodItemCard(
                                            food = food,
                                            onEdit = { editIndex = mealIndex },
                                            onDelete = { deleteIndex = mealIndex },
                                            onAiOpinionClick = {
                                                aiOpinionText = it
                                                showAiOpinionDialog = true
                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }

                        // Итоговая статистика
                        item {
                            Divider(
                                color = Color(0xFFE0E0E0),
                                thickness = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }

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
                                        MacroChip("Калории", totals.calories.toString(), Color.Black)
                                        MacroChip("Белки", NutritionFormatter.formatMacro(totals.proteins), Color.Gray)
                                        MacroChip("Жиры", NutritionFormatter.formatMacro(totals.fats), Color.Gray)
                                        MacroChip("Углеводы", NutritionFormatter.formatMacro(totals.carbs), Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        editIndex?.let { idx ->
            val meal = meals[idx]
            val food = meal.foods.first()

            val weight = food.weight.toFloatOrNull() ?: 100f
            fun per100(value: Double): String {
                val per100 = (value * 100f / weight * 10f).roundToInt() / 10f
                return per100.toString()
            }

            EnhancedManualInputDialog(
                initialData = ManualInputData(
                    name = food.name,
                    caloriesPer100g = ((food.calories.toFloat() * 100f / weight).roundToInt()).toString(),
                    proteinsPer100g = per100(food.protein),
                    fatsPer100g = per100(food.fat),
                    carbsPer100g = per100(food.carbs),
                    weight = food.weight
                ),
                onDismiss = { editIndex = null },
                onConfirm = { data ->
                    val updatedFood = FoodItem(
                        name = data.name,
                        calories = data.totalCalories.roundToInt(),
                        protein = data.totalProteins.toDouble(),
                        fat = data.totalFats.toDouble(),
                        carbs = data.totalCarbs.toDouble(),
                        weight = data.weight,
                        aiOpinion = food.aiOpinion
                    )
                    val updatedMeal = meal.copy(foods = listOf(updatedFood))
                    meals[idx] = updatedMeal
                    onMealUpdate(idx, updatedMeal)
                    editIndex = null
                }
            )
        }

        deleteIndex?.let { idx ->
            AlertDialog(
                onDismissRequest = { deleteIndex = null },
                icon = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = { Text("Удалить запись?", textAlign = TextAlign.Center) },
                text = { Text("Это действие нельзя отменить.", textAlign = TextAlign.Center) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            meals.removeAt(idx)
                            onMealDelete(idx)
                            deleteIndex = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteIndex = null }) {
                        Text("Отмена")
                    }
                }
            )
        }
        if (showAiOpinionDialog && aiOpinionText != null) {
            AiOpinionDialog(
                opinion = aiOpinionText!!,
                onDismiss = { showAiOpinionDialog = false }
            )
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
private fun FoodItemCard(
    food: FoodItem,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onAiOpinionClick: ((String) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Название и кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = food.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                onEdit?.let {
                    IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Редактировать",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                onDelete?.let {
                    IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
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
                MacroChip(
                    "Калории",
                    food.calories.toString(),
                    Color.Black
                )
                MacroChip(
                    "Белки",
                    "${NutritionFormatter.formatMacro(food.protein.toFloat())}г",
                    Color.Gray
                )
                MacroChip(
                    "Жиры",
                    "${NutritionFormatter.formatMacro(food.fat.toFloat())}г",
                    Color.Gray
                )
                MacroChip(
                    "Углеводы",
                    "${NutritionFormatter.formatMacro(food.carbs.toFloat())}г",
                    Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Нижняя часть с весом и AI меткой
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Вес: ${food.weight}г",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                // AI индикатор и кнопка информации
                if (food.source != "manual" || food.aiOpinion != null) {
                    AiInfoButton(
                        hasAiOpinion = food.aiOpinion != null,
                        onClick = {
                            food.aiOpinion?.let { onAiOpinionClick?.invoke(it) }
                        }
                    )
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

// Data class для итогов
private data class Totals(
    val calories: Int,
    val proteins: Float,
    val fats: Float,
    val carbs: Float
)
