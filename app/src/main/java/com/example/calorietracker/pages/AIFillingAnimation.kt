package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Enum для определения порядка заполнения полей
enum class FieldType {
    NAME, WEIGHT, CALORIES, PROTEINS, FATS, CARBS
}

// Основной компонент для полей ввода с AI анимацией
@Composable
fun AIAnimatedInputFields(
    data: ManualInputData,
    onDataChange: (ManualInputData) -> Unit,
    isFromAI: Boolean = false
) {
    var currentFillingField by remember { mutableStateOf<FieldType?>(null) }
    var filledFields by remember { mutableStateOf(setOf<FieldType>()) }
    var showGlobalAIBadge by remember { mutableStateOf(false) }

    // Запускаем анимацию заполнения только если данные от AI
    LaunchedEffect(isFromAI, data) {
        if (isFromAI && filledFields.isEmpty()) {
            // Заполняем поля по очереди
            val fieldsOrder = listOf(
                FieldType.NAME to data.name,
                FieldType.WEIGHT to data.weight,
                FieldType.CALORIES to data.caloriesPer100g,
                FieldType.PROTEINS to data.proteinsPer100g,
                FieldType.FATS to data.fatsPer100g,
                FieldType.CARBS to data.carbsPer100g
            )

            fieldsOrder.forEach { (fieldType, value) ->
                if (value.isNotEmpty()) {
                    currentFillingField = fieldType
                    delay(600) // Время для анимации заполнения поля
                    filledFields = filledFields + fieldType
                    currentFillingField = null
                    delay(200) // Пауза между полями
                }
            }

            // Показываем глобальный AI badge после завершения заполнения
            if (filledFields.isNotEmpty()) {
                showGlobalAIBadge = true
                delay(3000) // Показываем 3 секунды
                showGlobalAIBadge = false
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Название продукта
        AIAnimatedTextField(
            value = data.name,
            onValueChange = { onDataChange(data.copy(name = it)) },
            label = "Название продукта",
            icon = Icons.Default.Restaurant,
            accentColor = Color(0xFF4CAF50),
            fieldType = FieldType.NAME,
            currentFillingField = currentFillingField,
            isFieldFilled = filledFields.contains(FieldType.NAME),
            isFromAI = isFromAI
        )

        // Вес порции
        AIAnimatedTextField(
            value = data.weight,
            onValueChange = { onDataChange(data.copy(weight = it)) },
            label = "Вес порции (г)",
            icon = Icons.Default.MonitorWeight,
            accentColor = Color(0xFF2196F3),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            fieldType = FieldType.WEIGHT,
            currentFillingField = currentFillingField,
            isFieldFilled = filledFields.contains(FieldType.WEIGHT),
            isFromAI = isFromAI
        )

        // Калории на 100г
        AIAnimatedTextField(
            value = data.caloriesPer100g,
            onValueChange = { onDataChange(data.copy(caloriesPer100g = it)) },
            label = "Калории на 100г",
            icon = Icons.Default.LocalFireDepartment,
            accentColor = Color(0xFFFF5722),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            fieldType = FieldType.CALORIES,
            currentFillingField = currentFillingField,
            isFieldFilled = filledFields.contains(FieldType.CALORIES),
            isFromAI = isFromAI
        )

        // Белки на 100г
        AIAnimatedTextField(
            value = data.proteinsPer100g,
            onValueChange = { onDataChange(data.copy(proteinsPer100g = it)) },
            label = "Белки на 100г",
            icon = Icons.Default.FitnessCenter,
            accentColor = Color(0xFF9C27B0),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            fieldType = FieldType.PROTEINS,
            currentFillingField = currentFillingField,
            isFieldFilled = filledFields.contains(FieldType.PROTEINS),
            isFromAI = isFromAI
        )

        // Жиры на 100г
        AIAnimatedTextField(
            value = data.fatsPer100g,
            onValueChange = { onDataChange(data.copy(fatsPer100g = it)) },
            label = "Жиры на 100г",
            icon = Icons.Default.Opacity,
            accentColor = Color(0xFFFFC107),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            fieldType = FieldType.FATS,
            currentFillingField = currentFillingField,
            isFieldFilled = filledFields.contains(FieldType.FATS),
            isFromAI = isFromAI
        )

        // Углеводы на 100г
        AIAnimatedTextField(
            value = data.carbsPer100g,
            onValueChange = { onDataChange(data.copy(carbsPer100g = it)) },
            label = "Углеводы на 100г",
            icon = Icons.Default.Grain,
            accentColor = Color(0xFF795548),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            fieldType = FieldType.CARBS,
            currentFillingField = currentFillingField,
            isFieldFilled = filledFields.contains(FieldType.CARBS),
            isFromAI = isFromAI
        )

        // Глобальный AI Badge внизу
        AnimatedVisibility(
            visible = showGlobalAIBadge,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                AICompletionBadge()
            }
        }
    }
}

// Компонент текстового поля с AI анимацией
@Composable
fun AIAnimatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accentColor: Color = Color.Black,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    fieldType: FieldType,
    currentFillingField: FieldType?,
    isFieldFilled: Boolean,
    isFromAI: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    val isCurrentlyFilling = currentFillingField == fieldType
    var showLocalBadge by remember { mutableStateOf(false) }

    // ИЗМЕНЕНО: Локальное состояние только для анимации "печати"
    var animatedDisplayValue by remember { mutableStateOf(if (isFieldFilled) value else "") }

    // Анимация заполнения текста
    LaunchedEffect(isCurrentlyFilling) {
        if (isCurrentlyFilling && value.isNotEmpty()) {
            animatedDisplayValue = "" // Сбрасываем перед анимацией

            // Анимация печати
            value.forEachIndexed { index, _ ->
                delay(30) // Скорость печати
                animatedDisplayValue = value.substring(0, index + 1)
            }

            // Показываем локальный badge
            showLocalBadge = true
            delay(1500)
            showLocalBadge = false
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            // ИЗМЕНЕНО: Логика отображения значения
            // Если идет анимация - показываем анимированное значение.
            // Во всех остальных случаях - реальное значение из state.
            // Это исправляет баг, когда ручной ввод был невозможен.
            value = if (isCurrentlyFilling) animatedDisplayValue else value,

            // ИЗМЕНЕНО: onValueChange теперь всегда вызывает колбэк.
            // Блокировка ввода реализована через параметр `enabled`.
            onValueChange = onValueChange,
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = when {
                                isCurrentlyFilling -> accentColor
                                isFieldFilled || value.isNotEmpty() -> accentColor.copy(alpha = 0.7f) // Учитываем ручной ввод
                                else -> Color.Gray
                            }
                        )
                    }
                    Text(
                        text = label,
                        color = if (isCurrentlyFilling || isFieldFilled || value.isNotEmpty()) // Учитываем ручной ввод
                            accentColor.copy(alpha = 0.8f)
                        else
                            Color.Gray
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = if (isCurrentlyFilling)
                    accentColor.copy(alpha = 0.5f)
                else
                    Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = when {
                    isCurrentlyFilling -> accentColor.copy(alpha = 0.05f)
                    isFieldFilled && isFromAI -> Color(0xFFF5F5F5)
                    else -> Color.White
                }
            ),
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            // ИЗМЕНЕНО: Поле отключается только на время анимации "печати".
            // Это правильный способ предотвратить ввод во время анимации.
            enabled = enabled && !isCurrentlyFilling,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                when {
                    isCurrentlyFilling -> {
                        AITypingIndicator(color = accentColor)
                    }
                    showLocalBadge -> {
                        AISmallBadge(color = accentColor)
                    }
                }
            }
        )
    }
}

// Индикатор печати
@Composable
private fun AITypingIndicator(
    color: Color = Color(0xFFFF9800)
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 12.dp)
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing_$index")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 150,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(5.dp)
                    .scale(scale)
                    .background(
                        color = color.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
            )
        }
    }
}

// Маленький AI badge для отдельных полей
@Composable
private fun AISmallBadge(color: Color) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "badge_scale"
    )

    Box(
        modifier = Modifier
            .padding(end = 12.dp)
            .scale(scale)
            .background(
                color = color,
                shape = CircleShape
            )
            .padding(4.dp)
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = "AI filled",
            modifier = Modifier.size(12.dp),
            tint = Color.White
        )
    }
}

// Бейдж завершения заполнения AI
@Composable
private fun AICompletionBadge() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFF9800),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            Text(
                "Данные заполнены с помощью AI",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

// Компонент для отображения информации о нутриенте
@Composable
internal fun NutrientChip(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}