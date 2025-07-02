package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Улучшенное поле с анимацией заполнения от AI
@Composable
fun AIAnimatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    accentColor: Color = Color.Black,
    singleLine: Boolean = true,
    isFromAI: Boolean = false,
    enabled: Boolean = true
) {
    var internalValue by remember(value) { mutableStateOf(if (isFromAI) "" else value) }
    var isAnimating by remember { mutableStateOf(isFromAI && value.isNotEmpty()) }
    var showAIBadge by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Анимация заполнения
    LaunchedEffect(value, isFromAI) {
        if (isFromAI && value.isNotEmpty() && internalValue != value) {
            isAnimating = true
            showAIBadge = true

            // Быстрая анимация печати
            val totalDuration = minOf(value.length * 20L, 800L) // Макс 0.8 сек
            val delayPerChar = totalDuration / value.length

            value.forEachIndexed { index, _ ->
                delay(delayPerChar)
                internalValue = value.substring(0, index + 1)
                onValueChange(internalValue)
            }

            isAnimating = false

            // Показываем AI бейдж еще немного
            delay(2000)
            showAIBadge = false
        } else if (!isFromAI) {
            internalValue = value
        }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = internalValue,
            onValueChange = {
                if (!isAnimating) {
                    internalValue = it
                    onValueChange(it)
                }
            },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon?.let {
                        Icon(
                            it,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isAnimating) accentColor else Color.Gray
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(label)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = if (isAnimating) Color(0xFFFFFBF0) else Color(0xFFFAFAFA)
            ),
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            enabled = enabled && !isAnimating,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                AnimatedVisibility(
                    visible = isAnimating,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    AITypingIndicator(color = accentColor)
                }
            }
        )

        // AI Badge
        AnimatedVisibility(
            visible = showAIBadge && !isAnimating,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 }),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-8).dp, y = (-8).dp)
        ) {
            AIFilledBadge()
        }
    }
}

// Индикатор печати для поля
@Composable
private fun AITypingIndicator(
    color: Color = Color(0xFFFF9800)
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "typing_$index")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, delayMillis = index * 100),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(4.dp)
                    .scale(scale)
                    .background(color, CircleShape)
            )
        }
    }
}

// Бейдж "Заполнено AI"
@Composable
private fun AIFilledBadge() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFF9800),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
            Text(
                "AI",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// Обновленный компонент для полей ввода с AI анимацией
@Composable
fun AIAnimatedInputFields(
    data: ManualInputData,
    onDataChange: (ManualInputData) -> Unit,
    isFromAI: Boolean = false
) {
    var fieldsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Поля появляются быстро
        delay(100)
        fieldsVisible = true
    }

    AnimatedVisibility(
        visible = fieldsVisible,
        enter = fadeIn(tween(200)) + expandVertically(tween(200))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Название продукта
            AIAnimatedTextField(
                value = data.name,
                onValueChange = { onDataChange(data.copy(name = it)) },
                label = "Название продукта",
                icon = Icons.Default.FoodBank,
                accentColor = DialogColors.ManualInput,
                isFromAI = isFromAI
            )

            // Вес порции
            AIAnimatedTextField(
                value = data.weight,
                onValueChange = { onDataChange(data.copy(weight = it.filter { ch -> ch.isDigit() })) },
                label = "Вес порции (г)",
                icon = Icons.Default.Scale,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                accentColor = Color(0xFF2196F3),
                isFromAI = isFromAI
            )

            // Заголовок для нутриентов
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300, delayMillis = 200))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Пищевая ценность на 100г",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    if (isFromAI) {
                        Spacer(Modifier.width(8.dp))
                        AIProcessingBadge()
                    }
                }
            }

            // Калории
            AIAnimatedTextField(
                value = data.caloriesPer100g,
                onValueChange = { onDataChange(data.copy(caloriesPer100g = it.filter { ch -> ch.isDigit() })) },
                label = "Калории (ккал)",
                icon = Icons.Default.LocalFireDepartment,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                accentColor = Color(0xFFFF5722),
                isFromAI = isFromAI
            )

            // БЖУ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    AICompactNutrientField(
                        value = data.proteinsPer100g,
                        label = "Белки",
                        color = Color(0xFF2196F3),
                        onChange = { onDataChange(data.copy(proteinsPer100g = it.filter { ch -> ch.isDigit() })) },
                        isFromAI = isFromAI
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    AICompactNutrientField(
                        value = data.fatsPer100g,
                        label = "Жиры",
                        color = Color(0xFFFFC107),
                        onChange = { onDataChange(data.copy(fatsPer100g = it.filter { ch -> ch.isDigit() })) },
                        isFromAI = isFromAI,
                        delayMs = 100
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    AICompactNutrientField(
                        value = data.carbsPer100g,
                        label = "Углеводы",
                        color = Color(0xFF9C27B0),
                        onChange = { onDataChange(data.copy(carbsPer100g = it.filter { ch -> ch.isDigit() })) },
                        isFromAI = isFromAI,
                        delayMs = 200
                    )
                }
            }
        }
    }
}

// Компактное поле для БЖУ с AI анимацией
@Composable
private fun AICompactNutrientField(
    value: String,
    label: String,
    color: Color,
    onChange: (String) -> Unit,
    isFromAI: Boolean = false,
    delayMs: Int = 0
) {
    var internalValue by remember(value) { mutableStateOf(if (isFromAI) "" else value) }
    var isAnimating by remember { mutableStateOf(isFromAI && value.isNotEmpty()) }

    LaunchedEffect(value, isFromAI) {
        if (isFromAI && value.isNotEmpty() && internalValue != value) {
            delay(delayMs.toLong())
            isAnimating = true

            // Числовая анимация
            val targetValue = value.toIntOrNull() ?: 0
            val duration = 500L
            val steps = 20
            val stepDelay = duration / steps

            repeat(steps) { step ->
                val progress = (step + 1) / steps.toFloat()
                val currentValue = (targetValue * progress).toInt()
                internalValue = currentValue.toString()
                onChange(internalValue)
                delay(stepDelay)
            }

            isAnimating = false
        } else if (!isFromAI) {
            internalValue = value
        }
    }

    BasicTextField(
        value = internalValue,
        onValueChange = {
            if (!isAnimating) {
                internalValue = it
                onChange(it)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = Color.Black,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        enabled = !isAnimating,
        cursorBrush = SolidColor(color),
        decorationBox = { innerTextField ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isAnimating)
                            color.copy(alpha = 0.15f)
                        else
                            color.copy(alpha = 0.1f)
                    )
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (internalValue.isEmpty()) {
                        Text("0", color = Color.Gray, fontSize = 16.sp)
                    }
                    innerTextField()
                }

                // Индикатор анимации
                AnimatedVisibility(
                    visible = isAnimating,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .padding(top = 4.dp),
                        color = color,
                        trackColor = color.copy(alpha = 0.2f)
                    )
                }
            }
        }
    )
}

// Бейдж обработки AI
@Composable
private fun AIProcessingBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "processing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFF9800).copy(alpha = alpha * 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color(0xFFFF9800).copy(alpha = alpha)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "AI",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800).copy(alpha = alpha)
            )
        }
    }
}
