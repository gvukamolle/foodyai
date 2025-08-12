package com.example.calorietracker.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.presentation.viewmodels.CalorieTrackerViewModel
import com.example.calorietracker.data.FoodItem
import com.example.calorietracker.data.MealType
import com.example.calorietracker.ui.animations.StaggeredAnimatedList
import com.example.calorietracker.ui.animations.TypewriterText
import com.example.calorietracker.utils.NutritionFormatter
import com.example.calorietracker.utils.capitalizeFirst
import com.example.calorietracker.utils.filterDecimal
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.ceil

/**
 * Объединенный файл всех анимационных компонентов
 * Заменяет: AnimatedComponents.kt, AIFillingAnimation.kt, AnimatedProgressComponents.kt
 */

// =========================================================================
// АНИМИРОВАННЫЕ ТЕКСТОВЫЕ ПОЛЯ
// =========================================================================

@Composable
fun AnimatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accentColor: Color = Color.Black,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else 5,
    appearDelay: Int = 150,
    enabled: Boolean = true
) {
    var isVisible by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(appearDelay.toLong())
        isVisible = true
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
            }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray) },
            leadingIcon = icon?.let { 
                { Icon(it, contentDescription = null, tint = if (isFocused) accentColor else Color.Gray) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFFAFAFA)
            ),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled
        )
    }
}

// =========================================================================
// AI ЗАПОЛНЕНИЕ ПОЛЕЙ
// =========================================================================

enum class FieldType {
    NAME, WEIGHT, CALORIES, PROTEINS, FATS, CARBS
}

data class ManualInputData(
    val name: String = "",
    val weight: String = "100",
    val calories: String = "",
    val proteins: String = "",
    val fats: String = "",
    val carbs: String = ""
)

@Composable
fun AIAnimatedInputFields(
    data: ManualInputData,
    onDataChange: (ManualInputData) -> Unit,
    isFromAI: Boolean = false
) {
    var currentFillingField by remember { mutableStateOf<FieldType?>(null) }
    var filledFields by remember { mutableStateOf(setOf<FieldType>()) }
    var showGlobalAIBadge by remember { mutableStateOf(false) }

    LaunchedEffect(isFromAI, data) {
        if (isFromAI && filledFields.isEmpty()) {
            val fieldsOrder = listOf(
                FieldType.NAME to data.name,
                FieldType.WEIGHT to data.weight,
                FieldType.CALORIES to data.calories,
                FieldType.PROTEINS to data.proteins,
                FieldType.FATS to data.fats,
                FieldType.CARBS to data.carbs
            )

            showGlobalAIBadge = true
            delay(500)

            for ((fieldType, value) in fieldsOrder) {
                if (value.isNotBlank()) {
                    currentFillingField = fieldType
                    delay(800)
                    filledFields = filledFields + fieldType
                    currentFillingField = null
                    delay(200)
                }
            }

            delay(1000)
            showGlobalAIBadge = false
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showGlobalAIBadge) {
            AIFillingBadge()
        }

        AIInputField(
            value = data.name,
            onValueChange = { onDataChange(data.copy(name = it.capitalizeFirst())) },
            label = "Название продукта",
            icon = Icons.Default.FoodBank,
            fieldType = FieldType.NAME,
            currentFillingField = currentFillingField,
            isFieldFilled = FieldType.NAME in filledFields,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        AIInputField(
            value = data.weight,
            onValueChange = { onDataChange(data.copy(weight = filterDecimal(it))) },
            label = "Вес (г)",
            icon = Icons.Default.Scale,
            fieldType = FieldType.WEIGHT,
            currentFillingField = currentFillingField,
            isFieldFilled = FieldType.WEIGHT in filledFields,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        AIInputField(
            value = data.calories,
            onValueChange = { onDataChange(data.copy(calories = filterDecimal(it))) },
            label = "Калории",
            icon = Icons.Default.LocalFireDepartment,
            fieldType = FieldType.CALORIES,
            currentFillingField = currentFillingField,
            isFieldFilled = FieldType.CALORIES in filledFields,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AIInputField(
                value = data.proteins,
                onValueChange = { onDataChange(data.copy(proteins = filterDecimal(it))) },
                label = "Белки",
                icon = Icons.Default.FitnessCenter,
                fieldType = FieldType.PROTEINS,
                currentFillingField = currentFillingField,
                isFieldFilled = FieldType.PROTEINS in filledFields,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )

            AIInputField(
                value = data.fats,
                onValueChange = { onDataChange(data.copy(fats = filterDecimal(it))) },
                label = "Жиры",
                icon = Icons.Default.WaterDrop,
                fieldType = FieldType.FATS,
                currentFillingField = currentFillingField,
                isFieldFilled = FieldType.FATS in filledFields,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )

            AIInputField(
                value = data.carbs,
                onValueChange = { onDataChange(data.copy(carbs = filterDecimal(it))) },
                label = "Углеводы",
                icon = Icons.Default.Grain,
                fieldType = FieldType.CARBS,
                currentFillingField = currentFillingField,
                isFieldFilled = FieldType.CARBS in filledFields,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AIInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    fieldType: FieldType,
    currentFillingField: FieldType?,
    isFieldFilled: Boolean,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    val isCurrentlyFilling = currentFillingField == fieldType
    val focusRequester = remember { FocusRequester() }

    val borderColor by animateColorAsState(
        targetValue = when {
            isCurrentlyFilling -> Color(0xFF4CAF50)
            isFieldFilled -> Color(0xFF2196F3)
            else -> Color(0xFFE0E0E0)
        },
        animationSpec = tween(300),
        label = "border_color"
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            isCurrentlyFilling -> Color(0xFF4CAF50)
            isFieldFilled -> Color(0xFF2196F3)
            else -> Color.Gray
        },
        animationSpec = tween(300),
        label = "icon_color"
    )

    LaunchedEffect(isCurrentlyFilling) {
        if (isCurrentlyFilling) {
            focusRequester.requestFocus()
        }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor
                )
            },
            trailingIcon = if (isFieldFilled) {
                {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .border(
                    width = if (isCurrentlyFilling) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = if (isCurrentlyFilling) Color(0xFF4CAF50).copy(alpha = 0.05f) else Color.White,
                unfocusedContainerColor = if (isFieldFilled) Color(0xFF2196F3).copy(alpha = 0.05f) else Color(0xFFFAFAFA)
            ),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = keyboardOptions,
            singleLine = true
        )

        if (isCurrentlyFilling) {
            AITypingIndicator(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 48.dp)
            )
        }
    }
}

@Composable
private fun AIFillingBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_badge")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF4CAF50).copy(alpha = alpha),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "AI заполняет поля...",
                color = Color(0xFF4CAF50).copy(alpha = alpha),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AITypingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(16.dp)
            .scale(scale)
            .background(
                Color(0xFF4CAF50),
                CircleShape
            )
    )
}

// =========================================================================
// АНИМИРОВАННЫЕ ПРОГРЕСС КОМПОНЕНТЫ
// =========================================================================

@Composable
fun AnimatedNutritionProgress(
    viewModel: CalorieTrackerViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Калории
        AnimatedProgressCard(
            current = viewModel.dailyCalories,
            target = viewModel.userProfile.dailyCalories,
            label = "Калории",
            unit = "ккал",
            color = Color(0xFFFF5722),
            icon = Icons.Default.LocalFireDepartment,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        )

        // БЖУ в ряд
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedProgressCard(
                current = viewModel.formattedProtein.toFloatOrNull()?.toInt() ?: 0,
                target = viewModel.userProfile.dailyProteins,
                label = "Белки",
                unit = "г",
                color = Color(0xFF2196F3),
                icon = Icons.Default.FitnessCenter,
                modifier = Modifier.weight(1f),
                compact = true,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )

            AnimatedProgressCard(
                current = viewModel.formattedFat.toFloatOrNull()?.toInt() ?: 0,
                target = viewModel.userProfile.dailyFats,
                label = "Жиры",
                unit = "г",
                color = Color(0xFFFFC107),
                icon = Icons.Default.WaterDrop,
                modifier = Modifier.weight(1f),
                compact = true,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )

            AnimatedProgressCard(
                current = viewModel.formattedCarbs.toFloatOrNull()?.toInt() ?: 0,
                target = viewModel.userProfile.dailyCarbs,
                label = "Углеводы",
                unit = "г",
                color = Color(0xFF9C27B0),
                icon = Icons.Default.Grain,
                modifier = Modifier.weight(1f),
                compact = true,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        }
    }
}

@Composable
private fun AnimatedProgressCard(
    current: Int,
    target: Int,
    label: String,
    unit: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val progress = if (target > 0) (current.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (isVisible) progress else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(if (compact) 16.dp else 20.dp)
                    )
                    Text(
                        label,
                        fontSize = if (compact) 12.sp else 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }

                Text(
                    "$current/$target $unit",
                    fontSize = if (compact) 11.sp else 13.sp,
                    color = Color.Gray
                )
            }

            // Прогресс бар
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 6.dp else 8.dp)
                    .background(
                        color.copy(alpha = 0.1f),
                        RoundedCornerShape(if (compact) 3.dp else 4.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(
                            color,
                            RoundedCornerShape(if (compact) 3.dp else 4.dp)
                        )
                )
            }

            // Процент
            if (!compact) {
                Text(
                    "${(animatedProgress * 100).toInt()}%",
                    fontSize = 12.sp,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun AnimatedMealsList(
    foods: List<FoodItem>,
    onFoodClick: (FoodItem) -> Unit,
    modifier: Modifier = Modifier
) {
    StaggeredAnimatedList(
        items = foods,
        modifier = modifier
    ) { food, index ->
        AnimatedFoodCard(
            food = food,
            onClick = { onFoodClick(food) },
            delay = index * 100
        )
    }
}

@Composable
private fun AnimatedFoodCard(
    food: FoodItem,
    onClick: () -> Unit,
    delay: Int = 0
) {
    var isVisible by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        isVisible = true
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    val animatedTranslationY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 20.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "translation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedTranslationY.toPx()
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    food.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    "${food.weight}г • ${food.calories} ккал",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NutrientBadge(
                    value = NutritionFormatter.formatMacro(food.protein.toFloat()),
                    label = "Б",
                    color = Color(0xFF2196F3)
                )
                NutrientBadge(
                    value = NutritionFormatter.formatMacro(food.fat.toFloat()),
                    label = "Ж",
                    color = Color(0xFFFFC107)
                )
                NutrientBadge(
                    value = NutritionFormatter.formatMacro(food.carbs.toFloat()),
                    label = "У",
                    color = Color(0xFF9C27B0)
                )
            }
        }
    }
}

@Composable
private fun NutrientBadge(
    value: String,
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .background(
                color.copy(alpha = 0.1f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$label: $value",
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// =========================================================================
// АНИМИРОВАННЫЕ СТАТИСТИКИ
// =========================================================================

@Composable
fun AnimatedStatsRow(
    stats: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        stats.forEachIndexed { index, (label, value) ->
            AnimatedStatItem(
                label = label,
                value = value,
                delay = index * 100
            )
        }
    }
}

@Composable
private fun AnimatedStatItem(
    label: String,
    value: String,
    delay: Int = 0
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay.toLong())
        isVisible = true
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
    ) {
        Text(
            value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}