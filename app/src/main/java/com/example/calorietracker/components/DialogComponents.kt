package com.example.calorietracker.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.calorietracker.extensions.fancyShadow
import com.example.calorietracker.utils.capitalizeFirst
import com.example.calorietracker.utils.filterDecimal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Объединенный файл всех диалоговых компонентов
 * Заменяет: BeautifulDialogs.kt, DialogComponents.kt, EnhancedDialogs.kt, DialogIntegrations.kt
 */

// =========================================================================
// БАЗОВЫЕ КОМПОНЕНТЫ
// =========================================================================

@Composable
fun AnimatedPopup(
    onDismissRequest: () -> Unit,
    content: @Composable (onDismiss: () -> Unit) -> Unit
) {
    var isVisible by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val ime = WindowInsets.ime
    val imeVisible by remember {
        derivedStateOf { ime.getBottom(density) > 0 }
    }

    fun dismiss() {
        coroutineScope.launch {
            isVisible = false
            delay(150)
            onDismissRequest()
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isVisible) 150 else 100,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    Popup(
        onDismissRequest = { dismiss() },
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.White.copy(alpha = 0.6f * animatedAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (imeVisible) {
                            focusManager.clearFocus()
                        } else {
                            dismiss()
                        }
                    }
            )

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = animatedAlpha
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (imeVisible) {
                            focusManager.clearFocus()
                        }
                    }
            ) {
                content(::dismiss)
            }
        }
    }
}

@Composable
fun DialogHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    accentColor.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Column {
            Text(
                title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                subtitle,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun BeautifulTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconColor: Color = Color.Gray,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = icon?.let { 
            { Icon(it, contentDescription = null, tint = iconColor) }
        },
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = iconColor,
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFFAFAFA)
        ),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = keyboardOptions,
        singleLine = singleLine
    )
}

@Composable
fun NutrientChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                label,
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}
// =========================================================================
// ОСНОВНЫЕ ДИАЛОГИ
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualFoodInputDialog(
    initialFoodName: String = "",
    initialCalories: String = "",
    initialProteins: String = "",
    initialFats: String = "",
    initialCarbs: String = "",
    initialWeight: String = "100",
    onDismiss: () -> Unit,
    onConfirm: (name: String, calories: String, proteins: String, fats: String, carbs: String, weight: String) -> Unit
) {
    var foodName by remember { mutableStateOf(initialFoodName) }
    var caloriesPer100g by remember { mutableStateOf(initialCalories) }
    var proteinsPer100g by remember { mutableStateOf(initialProteins) }
    var fatsPer100g by remember { mutableStateOf(initialFats) }
    var carbsPer100g by remember { mutableStateOf(initialCarbs) }
    var weight by remember { mutableStateOf(initialWeight) }

    val weightFloat = weight.toFloatOrNull() ?: 100f
    fun calc(value: String): Float {
        val v = value.toFloatOrNull() ?: 0f
        val total = v * weightFloat / 100f
        return (total * 10).roundToInt() / 10f
    }
    val totalCalories = calc(caloriesPer100g)
    val totalProteins = calc(proteinsPer100g)
    val totalFats = calc(fatsPer100g)
    val totalCarbs = calc(carbsPer100g)

    val haptic = LocalHapticFeedback.current

    AnimatedPopup(onDismissRequest = onDismiss) { animatedDismiss ->
        Card(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fancyShadow(
                    borderRadius = 24.dp,
                    shadowRadius = 8.dp,
                    alpha = 0.25f
                )
                .graphicsLayer {
                    clip = true
                    shape = RoundedCornerShape(24.dp)
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFFAFAFA)
                            )
                        )
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DialogHeader(
                    icon = Icons.Default.Restaurant,
                    title = if (initialFoodName.isNotEmpty()) "Проверьте данные" else "Добавить продукт",
                    subtitle = "Заполните информацию о продукте",
                    accentColor = Color(0xFF9C27B0)
                )

                BeautifulTextField(
                    value = foodName,
                    onValueChange = { foodName = it.capitalizeFirst() },
                    label = "Название продукта",
                    icon = Icons.Default.FoodBank,
                    iconColor = Color(0xFF9C27B0),
                    modifier = Modifier.fillMaxWidth()
                )

                BeautifulTextField(
                    value = weight,
                    onValueChange = { weight = filterDecimal(it) },
                    label = "Вес порции (г)",
                    icon = Icons.Default.Scale,
                    iconColor = Color(0xFF2196F3),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFE0E0E0),
                                    Color.Transparent
                                )
                            )
                        )
                )

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

                BeautifulTextField(
                    value = caloriesPer100g,
                    onValueChange = { caloriesPer100g = filterDecimal(it) },
                    label = "Калории (ккал)",
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = Color(0xFFFF5722),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BeautifulTextField(
                        value = proteinsPer100g,
                        onValueChange = { proteinsPer100g = filterDecimal(it) },
                        label = "Белки",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    BeautifulTextField(
                        value = fatsPer100g,
                        onValueChange = { fatsPer100g = filterDecimal(it) },
                        label = "Жиры",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    BeautifulTextField(
                        value = carbsPer100g,
                        onValueChange = { carbsPer100g = filterDecimal(it) },
                        label = "Углеводы",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                AnimatedVisibility(
                    visible = weight.isNotBlank() && weight != "100",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Итого на ${weight}г:",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NutrientChip(
                                    icon = Icons.Default.LocalFireDepartment,
                                    label = "Ккал",
                                    value = String.format(Locale.US, "%.1f", totalCalories),
                                    color = Color(0xFFFF5722),
                                    modifier = Modifier.weight(1f)
                                )
                                NutrientChip(
                                    icon = Icons.Default.FitnessCenter,
                                    label = "Белки",
                                    value = String.format(Locale.US, "%.1fг", totalProteins),
                                    color = Color(0xFF2196F3),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NutrientChip(
                                    icon = Icons.Default.WaterDrop,
                                    label = "Жиры",
                                    value = String.format(Locale.US, "%.1fг", totalFats),
                                    color = Color(0xFFFFC107),
                                    modifier = Modifier.weight(1f)
                                )
                                NutrientChip(
                                    icon = Icons.Default.Grain,
                                    label = "Углеводы",
                                    value = String.format(Locale.US, "%.1fг", totalCarbs),
                                    color = Color(0xFF9C27B0),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            animatedDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Gray
                        )
                    ) {
                        Text("Отмена", fontSize = 16.sp)
                    }

                    Button(
                        onClick = {
                            if (foodName.isNotBlank() && caloriesPer100g.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onConfirm(
                                    foodName,
                                    String.format(Locale.US, "%.1f", totalCalories),
                                    String.format(Locale.US, "%.1f", totalProteins),
                                    String.format(Locale.US, "%.1f", totalFats),
                                    String.format(Locale.US, "%.1f", totalCarbs),
                                    weight
                                )
                                animatedDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = foodName.isNotBlank() && caloriesPer100g.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text("Добавить", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DescribeFoodDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current

    AnimatedPopup(onDismissRequest = { if (!isLoading) onDismiss() }) { animatedDismiss ->
        Card(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fancyShadow(
                    borderRadius = 24.dp,
                    shadowRadius = 8.dp,
                    alpha = 0.25f
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFFAFAFA)
                            )
                        )
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DialogHeader(
                    icon = Icons.Default.AutoAwesome,
                    title = "Расскажите",
                    subtitle = "AI распознает и посчитает КБЖУ",
                    accentColor = Color(0xFFFF9800)
                )

                AnimatedContent(
                    targetState = isLoading,
                    transitionSpec = {
                        fadeIn() with fadeOut()
                    }, label = ""
                ) { loading ->
                    if (loading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFFF9800)
                                )
                                Text(
                                    "AI анализирует описание...",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = text,
                            onValueChange = onTextChange,
                            placeholder = {
                                Text(
                                    "Например: Овсянка с бананом и орехами",
                                    color = Color.Gray
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF9800),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFFAFAFA)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    if (text.isNotBlank() && !isLoading) {
                                        onSend()
                                    }
                                }
                            ),
                            singleLine = true,
                            maxLines = 1
                        )
                    }
                }

                if (!isLoading && text.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFFFF6F00),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Опишите блюдо максимально подробно: ингредиенты, способ приготовления, размер порции",
                                fontSize = 12.sp,
                                color = Color(0xFFE65100),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                if (!isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                animatedDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Отмена", fontSize = 16.sp)
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSend()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = text.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black
                            )
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Отправить", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoUploadDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    AnimatedPopup(onDismissRequest = onDismiss) { animatedDismiss ->
        Card(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fancyShadow(
                    borderRadius = 24.dp,
                    shadowRadius = 8.dp,
                    alpha = 0.25f
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFFAFAFA)
                            )
                        )
                    )
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                DialogHeader(
                    icon = Icons.Default.PhotoCamera,
                    title = "Добавить фото",
                    subtitle = "Выберите источник изображения",
                    accentColor = Color(0xFF2196F3)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCameraClick()
                            animatedDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Сделать фото", fontSize = 16.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGalleryClick()
                            animatedDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF2196F3)
                        )
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Выбрать из галереи", fontSize = 16.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        animatedDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Отмена", fontSize = 16.sp)
                }
            }
        }
    }
}