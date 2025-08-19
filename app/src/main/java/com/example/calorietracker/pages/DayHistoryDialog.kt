package com.example.calorietracker.pages

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.calorietracker.data.DailyIntake
import com.example.calorietracker.data.DailyNutritionSummary
import com.example.calorietracker.data.FoodItem
import com.example.calorietracker.data.Meal
import com.example.calorietracker.data.MealType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.calorietracker.utils.NutritionFormatter
import kotlin.math.roundToInt
import com.example.calorietracker.extensions.fancyShadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.calorietracker.components.WarningDialog

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
    val density = LocalDensity.current
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Локальное состояние списка приемов пищи
    val meals = remember { mutableStateListOf<Meal>().apply { addAll(dailyIntake.meals) } }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
    var showFoodDetailScreen by remember { mutableStateOf(false) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var selectedFoodIndex by remember { mutableStateOf<Int?>(null) }

    // Группируем приемы пищи по типу, сохраняя индекс
    val groupedMeals = remember(meals.toList()) {
        meals.withIndex()
            .groupBy({ it.value.type }, { it })
            .toSortedMap(compareBy { mealType ->
                when (mealType) {
                    MealType.BREAKFAST -> 1
                    MealType.LUNCH -> 2
                    MealType.DINNER -> 3
                    MealType.SUPPER -> 4
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
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { animatedDismiss() }
                )
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .fancyShadow(
                            color = Color.Black,
                            borderRadius = 24.dp,
                            shadowRadius = 12.dp,
                            alpha = 0.25f
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                ) {
                    // Основной контейнер с заголовком и контентом
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                // Заголовок с датой и кнопкой назад - теперь часть общего контейнера
                Box(
                modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 16.dp)
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

                Divider(
                color = Color(0xFFF0F0F0),
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
                )

                // Контент с градиентом для размытия краев
                Box(
                modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                ) {
                LazyColumn(
                modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp) // Увеличили отступ для итоговой статистики
                ) {
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
                                                    onViewDetails = {
                                                        selectedFood = food
                                                        selectedFoodIndex = mealIndex
                                                        showFoodDetailScreen = true
                                                    }
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        }
                                    }
                                    item { Spacer(modifier = Modifier.height(16.dp)) }
                                }


                            }

                            // Градиенты для размытия краев сверху и снизу
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .align(Alignment.TopCenter)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White,
                                                Color.White.copy(alpha = 0f)
                                            )
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0f),
                                                Color.White
                                            )
                                        )
                                    )
                            )
                        }
                        
                        // Итоговая статистика закреплена внизу
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Divider(
                                color = Color(0xFFF0F0F0),
                                thickness = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(horizontal = 24.dp, vertical = 20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Итого за день",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = totals.calories.toString(),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = "Калории",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        
                                        MacroStat(
                                            value = NutritionFormatter.formatMacro(totals.proteins),
                                            label = "Белки",
                                            color = Color.Gray
                                        )
                                        MacroStat(
                                            value = NutritionFormatter.formatMacro(totals.fats),
                                            label = "Жиры",
                                            color = Color.Gray
                                        )
                                        MacroStat(
                                            value = NutritionFormatter.formatMacro(totals.carbs),
                                            label = "Углеводы",
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Диалоги редактирования и удаления
        editIndex?.let { idx ->
            val meal = meals[idx]
            val food = meal.foods.first()

            val weight = food.weight.toFloatOrNull() ?: 100f
            fun per100(value: Double): String {
                val per100 = (value * 100f / weight * 10f).roundToInt() / 10f
                return per100.toString()
            }

            com.example.calorietracker.components.ManualFoodInputDialog(
                initialFoodName = food.name,
                initialCalories = ((food.calories.toFloat() * 100f / weight).roundToInt()).toString(),
                initialProteins = per100(food.protein),
                initialFats = per100(food.fat),
                initialCarbs = per100(food.carbs),
                initialWeight = food.weight,
                onDismiss = { editIndex = null },
                onConfirm = { name, calories, proteins, fats, carbs, weight ->
                    val updatedFood = FoodItem(
                        name = name,
                        calories = calories.toFloatOrNull()?.roundToInt() ?: 0,
                        protein = proteins.toDoubleOrNull() ?: 0.0,
                        fat = fats.toDoubleOrNull() ?: 0.0,
                        carbs = carbs.toDoubleOrNull() ?: 0.0,
                        weight = weight,
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
            WarningDialog(
                title = "Удалить запись?",
                message = "Это действие нельзя отменить.",
                confirmText = "Удалить",
                dismissText = "Отмена",
                onConfirm = {
                    meals.removeAt(idx)
                    onMealDelete(idx)
                    deleteIndex = null
                },
                onDismiss = { deleteIndex = null }
            )
        }

        if (showFoodDetailScreen && selectedFood != null && selectedFoodIndex != null) {
            FoodDetailScreen(
                food = selectedFood!!,
                onDismiss = { 
                    showFoodDetailScreen = false
                    selectedFood = null
                    selectedFoodIndex = null
                },
                onEdit = {
                    showFoodDetailScreen = false
                    editIndex = selectedFoodIndex
                },
                onDelete = {
                    showFoodDetailScreen = false
                    deleteIndex = selectedFoodIndex
                }
            )
        }
    }
}

// Упрощенная карточка продукта
@Composable
private fun FoodItemCard(
    food: FoodItem,
    onViewDetails: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F8F8)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Левая часть с названием и весом
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = food.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${food.weight} г",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            
            // Калории по центру
            Text(
                text = "${food.calories} ккал",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            // Кнопка справа
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onViewDetails()
                },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFDFEBF4),
                modifier = Modifier.height(28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (food.aiOpinion != null) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF2196F3)
                        )
                        Text(
                            text = "Foody AI",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                    } else {
                        Text(
                            text = "Больше",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2196F3)
                        )
                    }
                }
            }
        }
    }
}

// Остальные компоненты остаются без изменений...
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

@Composable
fun MacroChip(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
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

@Composable
private fun MacroStat(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun AiInfoButton(
    hasAiOpinion: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            if (hasAiOpinion) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFDFEBF4),
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
                tint = Color(0xFF2196F3)
            )
            Text(
                text = "Foody AI",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
            if (hasAiOpinion) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = Color(0xFF2196F3).copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Информация",
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF2196F3)
                    )
                }
            }
        }
    }
}

data class Totals(
    val calories: Int,
    val proteins: Float,
    val fats: Float,
    val carbs: Float
)