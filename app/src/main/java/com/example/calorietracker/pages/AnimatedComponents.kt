package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.pages.NutrientChip
import com.example.calorietracker.ui.animations.StaggeredAnimatedList
import com.example.calorietracker.ui.animations.TypewriterText
import kotlinx.coroutines.delay

// Анимированное текстовое поле
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
    animatePrefill: Boolean = false
    ) {
    var isFocused by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var internalText by remember(value) { mutableStateOf(if (animatePrefill) "" else value) }

    LaunchedEffect(Unit) {
        delay(appearDelay.toLong())
        isVisible = true
    }

    LaunchedEffect(value) {
        if (animatePrefill) {
            internalText = ""
            value.forEachIndexed { index, _ ->
                delay(30)
                internalText = value.substring(0, index + 1)
                onValueChange(internalText)
            }
        } else {
            internalText = value
        }
    }


    val borderColor by animateColorAsState(
        targetValue = if (isFocused) accentColor else Color(0xFFE0E0E0),
        animationSpec = tween(200),
        label = "border"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 1.dp,
        animationSpec = tween(200),
        label = "width"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.95f)
    ) {
        BasicTextField(
            value = internalText,
            onValueChange = {
                internalText = it
                onValueChange(it)
            },
            modifier = modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = Color.Black
            ),
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            maxLines = maxLines,
            cursorBrush = SolidColor(accentColor),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isFocused) Color.White else Color(0xFFFAFAFA)
                        )
                        .border(
                            width = borderWidth,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        icon?.let {
                            Icon(
                                it,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isFocused) accentColor else Color.Gray
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            }
        )
    }
}

// Анимированные поля ввода для данных о еде
@Composable
fun AnimatedInputFields(
    data: ManualInputData,
    onDataChange: (ManualInputData) -> Unit,
    animatePrefill: Boolean = false
    ) {
    StaggeredAnimatedList(
        items = listOf(
            InputFieldData(
                value = data.name,
                label = "Название продукта",
                icon = Icons.Default.FoodBank,
                onChange = { onDataChange(data.copy(name = it)) }
            ),
            InputFieldData(
                value = data.weight,
                label = "Вес порции (г)",
                icon = Icons.Default.Scale,
                keyboardType = KeyboardType.Number,
                onChange = { onDataChange(data.copy(weight = it.filter { ch -> ch.isDigit() })) }
            )
        ),
        delayBetweenItems = 0
    ) { field, _ ->
        AnimatedTextField(
            value = field.value,
            onValueChange = field.onChange,
            placeholder = field.label,
            icon = field.icon,
            keyboardOptions = KeyboardOptions(keyboardType = field.keyboardType),
            accentColor = DialogColors.ManualInput,
            singleLine = true,
            appearDelay = 0,
            animatePrefill = animatePrefill
        )
    }

    Spacer(Modifier.height(16.dp))

    // Заголовок для нутриентов с анимацией
    var showNutrients by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        showNutrients = true
    }

    AnimatedVisibility(
        visible = showNutrients,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
    ) {
        Column {
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
            }

            Spacer(Modifier.height(12.dp))

            // Калории
            AnimatedTextField(
                value = data.caloriesPer100g,
                onValueChange = { onDataChange(data.copy(caloriesPer100g = it.filter { ch -> ch.isDigit() })) },
                placeholder = "Калории (ккал)",
                icon = Icons.Default.LocalFireDepartment,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                accentColor = Color(0xFFFF5722),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            // БЖУ в строку
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactNutrientField(
                    value = data.proteinsPer100g,
                    label = "Белки",
                    color = Color(0xFF2196F3),
                    onChange = { onDataChange(data.copy(proteinsPer100g = it.filter { ch -> ch.isDigit() })) },
                    modifier = Modifier.weight(1f)
                )
                CompactNutrientField(
                    value = data.fatsPer100g,
                    label = "Жиры",
                    color = Color(0xFFFFC107),
                    onChange = { onDataChange(data.copy(fatsPer100g = it.filter { ch -> ch.isDigit() })) },
                    modifier = Modifier.weight(1f)
                )
                CompactNutrientField(
                    value = data.carbsPer100g,
                    label = "Углеводы",
                    color = Color(0xFF9C27B0),
                    onChange = { onDataChange(data.copy(carbsPer100g = it.filter { ch -> ch.isDigit() })) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Компактное поле для нутриентов
@Composable
private fun CompactNutrientField(
    value: String,
    label: String,
    color: Color,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(400)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        modifier = modifier
    ) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = 16.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = SolidColor(color),
            decorationBox = { innerTextField ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.1f))
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
                        if (value.isEmpty()) {
                            Text("0", color = Color.Gray, fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
fun AnimatedNutritionSummary(data: ManualInputData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            // ИЗМЕНЕНО: Используем тот самый серый цвет
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Итого на ${data.weight}г:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutrientChip("Калории", "${data.totalCalories} ккал", Color(0xFFFF5722))
                NutrientChip("Белки", "${data.totalProteins}г", Color(0xFF9C27B0))
                NutrientChip("Жиры", "${data.totalFats}г", Color(0xFFFFC107))
                NutrientChip("Углеводы", "${data.totalCarbs}г", Color(0xFF795548))
            }
        }
    }
}

// Анимированный чип для нутриентов
@Composable
private fun AnimatedNutrientChip(nutrient: NutrientInfo) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = nutrient.color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                nutrient.label,
                fontSize = 12.sp,
                color = nutrient.color,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(4.dp))
            Text(
                nutrient.value,
                fontSize = 14.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Кнопки действий с анимацией
@Composable
fun AnimatedDialogActions(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean,
    confirmText: String = "Добавить",
    accentColor: Color
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Gray
                )
            ) {
                Text("Отмена", fontSize = 16.sp)
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = confirmEnabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(confirmText, fontSize = 16.sp)
            }
        }
    }
}

// Карточка подсказки
@Composable
fun AnimatedHintCard(
    text: String,
    icon: ImageVector,
    backgroundColor: Color,
    textColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                fontSize = 12.sp,
                color = textColor,
                lineHeight = 18.sp
            )
        }
    }
}

// Вспомогательные data классы
data class InputFieldData(
    val value: String,
    val label: String,
    val icon: ImageVector? = null,
    val keyboardType: KeyboardType = KeyboardType.Text,
    val onChange: (String) -> Unit
)

data class NutrientInfo(
    val label: String,
    val value: String,
    val color: Color
)