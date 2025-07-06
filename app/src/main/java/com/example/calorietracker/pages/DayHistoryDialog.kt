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
import com.example.calorietracker.ui.components.AnimatedRainbowBorder
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import com.example.calorietracker.utils.NutritionFormatter
import kotlin.math.roundToInt


// Основной диалог истории дня
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
                            calories = totals.calories,
                            protein = totals.protein,
                            fat = totals.fat,
                            carbs = totals.carbs
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
                            if (meals.isEmpty()) {
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
                                    itemsIndexed(meals) { idx, meal ->
                                        MealCard(
                                            meal = meal,
                                            index = idx,
                                            onEdit = { editIndex = it },
                                            onUpdate = { i, m ->
                                                meals[i] = m
                                                onMealUpdate(i, m)
                                            },
                                            onDelete = { i ->
                                                meals.removeAt(i)
                                                onMealDelete(i)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editIndex != null) {
        val meal = meals[editIndex!!]
        val food = meal.foods.firstOrNull()
        val w = parseWeight(food?.weight ?: "0")
        val initialData = ManualInputData(
            name = food?.name ?: "",
            caloriesPer100g = if (food != null && w > 0) ((food.calories.toFloat() / w) * 100).toInt().toString() else "0",
            proteinsPer100g = if (food != null && w > 0) ((food.protein.toFloat() / w) * 100).toInt().toString() else "0",
            fatsPer100g = if (food != null && w > 0) ((food.fat.toFloat() / w) * 100).toInt().toString() else "0",
            carbsPer100g = if (food != null && w > 0) ((food.carbs.toFloat() / w) * 100).toInt().toString() else "0",
            weight = food?.weight ?: "100"
        )

        EnhancedManualInputDialog(
            initialData = initialData,
            onDismiss = { editIndex = null },
            onConfirm = { data ->
                val updatedFood = FoodItem(
                    name = data.name,
                    calories = data.totalCalories.roundToInt(),
                    protein = data.totalProteins.toDouble(),
                    fat = data.totalFats.toDouble(),
                    carbs = data.totalCarbs.toDouble(),
                    weight = data.weight
                )
                val updatedMeal = meal.copy(foods = listOf(updatedFood))
                meals[editIndex!!] = updatedMeal
                onMealUpdate(editIndex!!, updatedMeal)
                editIndex = null
            }
        )
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
                        NutritionFormatter.formatMacroWithUnit(protein),
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
                        NutritionFormatter.formatMacroWithUnit(fat),
                        Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                    NutritionStat(
                        "Углеводы",
                        NutritionFormatter.formatMacroWithUnit(carbs),
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
private fun MealCard(
    meal: Meal,
    index: Int,
    onEdit: (Int) -> Unit,
    onUpdate: (Int, Meal) -> Unit,
    onDelete: (Int) -> Unit
) {    Card(
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
            var typeExpanded by remember { mutableStateOf(false) }
            var menuExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.clickable { typeExpanded = true }) {
                    Text(
                        text = meal.type.displayName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    val totalWeight = meal.foods.sumOf { parseWeight(it.weight) }
                    Text(
                        text = "Масса: ${totalWeight} г",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "edit"
                        )
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = {
                                menuExpanded = false
                                onEdit(index)
                        })
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = {
                                menuExpanded = false
                                onDelete(index)
                        })
                    }
                }

                DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                    val options = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)
                    options.forEach { option ->
                        DropdownMenuItem(text = { Text(option.displayName) }, onClick = {
                            typeExpanded = false
                            if (option != meal.type) {
                                onUpdate(index, meal.copy(type = option))
                            }
                        })
                    }
                }
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

private data class Totals(
    val calories: Int,
    val protein: Float,
    val fat: Float,
    val carbs: Float
)