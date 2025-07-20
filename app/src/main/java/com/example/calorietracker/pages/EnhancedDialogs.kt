package com.example.calorietracker.pages

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.derivedStateOf
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.drawToBitmap
import com.example.calorietracker.extensions.fancyShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalFocusManager
import com.example.calorietracker.utils.capitalizeFirst
import com.example.calorietracker.utils.filterDecimal
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
import java.util.Locale
import com.example.calorietracker.components.AppTextField

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}


// Цветовая схема для диалогов
object DialogColors {
    val ManualInput = Color(0xFF9C27B0) // Фиолетовый
    val AIAnalysis = Color(0xFFFF9800) // Оранжевый
    val Photo = Color(0xFF4CAF50) // Зеленый
    val Gallery = Color(0xFF2196F3) // Синий
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AnimatedDialogContainer(
    onDismiss: () -> Unit,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val ime = WindowInsets.ime
    val imeVisible by remember {
        derivedStateOf { ime.getBottom(density) > 0 }
    }
    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isVisible by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        delay(5)
        try {
            backgroundBitmap = view.drawToBitmap()
        } catch (e: Exception) { /* ignore */ }
        isVisible = true
    }

    fun animatedDismiss() {
        coroutineScope.launch {
            focusManager.clearFocus() // Скрываем клавиатуру перед выходом
            isVisible = false
            delay(200)
            onDismiss()
        }
    }

    BackHandler {
        if (imeVisible) {
            focusManager.clearFocus()
        } else {
            animatedDismiss()
        }
    }

    Popup(
        onDismissRequest = { animatedDismiss() },
        properties = PopupProperties(focusable = true)
    ) {
        // Слой 1: Внешний контейнер. Реагирует на клики ЗА пределами диалога.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (imeVisible) {
                        focusManager.clearFocus()
                    } else {
                        animatedDismiss()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Визуальная часть фона (размытие, затемнение)
            AnimatedVisibility(
                visible = isVisible && backgroundBitmap != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(100))
            ) {
                backgroundBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(
                                radiusX = animateDpAsState(if (isVisible) 20.dp else 0.dp, tween(200), "blur").value,
                                radiusY = animateDpAsState(if (isVisible) 20.dp else 0.dp, tween(200), "blur").value
                            ),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.7f)))
                }
            }

            // Слой 2: Контейнер для контента диалога
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.9f, transformOrigin = TransformOrigin.Center, animationSpec = tween(200, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.9f, transformOrigin = TransformOrigin.Center)
            ) {
                // ВАЖНО: Просто Card без всяких оберток, чтобы она не мешала фокусу
                Card(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 360.dp)
                        .fancyShadow(borderRadius = 24.dp, shadowRadius = 12.dp, alpha = 0.35f, color = accentColor),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    // Мы просто вызываем content(), позволяя ему самому обрабатывать нажатия
                    content()
                }
            }
        }
    }
}

// Диалог ручного ввода
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedManualInputDialog(
    initialData: ManualInputData? = null,
    onDismiss: () -> Unit,
    onConfirm: (ManualInputData) -> Unit
) {
    var data by remember { mutableStateOf(initialData ?: ManualInputData()) }
    val isFromAI = initialData != null

    AnimatedDialogContainer(
        onDismiss = onDismiss,
        accentColor = DialogColors.ManualInput
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Заголовок
            DialogHeader(
                icon = Icons.Default.Restaurant,
                title = if (isFromAI) "Проверьте данные от AI" else "Добавить продукт",
                subtitle = if (isFromAI) "AI распознал продукт" else "Заполните информацию",
                accentColor = DialogColors.ManualInput
            )

            Spacer(Modifier.height(16.dp))

            // Поля ввода
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InputFields(
                    data = data,
                    onDataChange = { data = it }
                )

                NutritionSummary(data = data)
            }

            Spacer(Modifier.height(16.dp))

            // Кнопки
            DialogActions(
                onCancel = onDismiss,
                onConfirm = { onConfirm(data) },
                confirmEnabled = data.isValid(),
                confirmText = if (isFromAI) "Подтвердить" else "Добавить",
                accentColor = DialogColors.ManualInput
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun EnhancedDescribeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    initialText: String = ""
) {

    AnimatedDialogContainer(
        onDismiss = onDismiss,
        accentColor = DialogColors.AIAnalysis
    ) {
        var text by remember { mutableStateOf(initialText) }
        val keyboardController = LocalSoftwareKeyboardController.current
        val haptic = LocalHapticFeedback.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            /* ── Заголовок ── */
            DialogHeader(
                icon = Icons.Default.AutoAwesome,
                title = "Опишите блюдо",
                subtitle = "AI проанализирует состав",
                accentColor = DialogColors.AIAnalysis
            )

            Spacer(Modifier.height(16.dp))

            // Поле для описания
            Column {
                FieldLabel("Описание блюда")
                AppTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Например: роллы, 350г", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    singleLine = true
                )
            }

            /* ── Подсказка ── */
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(8.dp)
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
                        "Укажите ингредиенты, способ приготовления и примерный вес порции",
                        fontSize = 12.sp,
                        color = Color(0xFFE65100),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            /* ── Кнопки ── */
            DialogActions(
                onCancel = onDismiss,
                onConfirm = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    keyboardController?.hide()
                    onConfirm(text)          // передаём строку вверх
                },
                confirmEnabled = true,
                confirmText = "Отправить",
                accentColor = DialogColors.AIAnalysis
            )
                }
            }
        }

// Диалог подтверждения фото
@Composable
fun EnhancedPhotoConfirmDialog(
    bitmap: Bitmap,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedDialogContainer(
        onDismiss = onDismiss,
        accentColor = DialogColors.Photo
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Заголовок
            Text(
                "Отправить фото",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(Modifier.height(16.dp))

            // Изображение
            val aspectRatio = remember(bitmap) {
                if (bitmap.height != 0) bitmap.width.toFloat() / bitmap.height else 1f
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Image(
                    bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(16.dp))

            Column {
                FieldLabel("Подпись к фото")
                AppTextField(
                    value = caption,
                    onValueChange = onCaptionChange,
                    placeholder = { Text("Добавить подпись (необязательно)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    singleLine = true
                )
            }

            Spacer(Modifier.height(16.dp))

            // Кнопки
            DialogActions(
                onCancel = onDismiss,
                onConfirm = onConfirm,
                confirmEnabled = true,
                confirmText = "Отправить",
                accentColor = DialogColors.Photo
            )
        }
    }
}

// Компоненты-помощники (private в этом файле)

@Composable
private fun InputFields(
    data: ManualInputData,
    onDataChange: (ManualInputData) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Column {
        FieldLabel("Название")
        AppTextField(
            value = data.name,
            onValueChange = { onDataChange(data.copy(name = it.capitalizeFirst())) },
            placeholder = { Text("Салат Цезарь") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            singleLine = true
        )
    }

    Spacer(Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                FieldLabel("Калории на 100г")
                AppTextField(
                    value = data.caloriesPer100g,
                    onValueChange = { onDataChange(data.copy(caloriesPer100g = filterDecimal(it))) },
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    singleLine = true
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                FieldLabel("Белки на 100г")
                AppTextField(
                    value = data.proteinsPer100g,
                    onValueChange = { onDataChange(data.copy(proteinsPer100g = filterDecimal(it))) },
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    singleLine = true
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                FieldLabel("Жиры на 100г")
                AppTextField(
                    value = data.fatsPer100g,
                    onValueChange = { onDataChange(data.copy(fatsPer100g = filterDecimal(it))) },
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    singleLine = true
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                FieldLabel("Углеводы на 100г")
                AppTextField(
                    value = data.carbsPer100g,
                    onValueChange = { onDataChange(data.copy(carbsPer100g = filterDecimal(it))) },
                    placeholder = { Text("0") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    singleLine = true
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))

    Column {
        FieldLabel("Вес (грамм)")
        AppTextField(
            value = data.weight,
            onValueChange = { onDataChange(data.copy(weight = filterDecimal(it))) },
            placeholder = { Text("0") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
            singleLine = true
        )
    }
}

@Composable
private fun NutritionSummary(data: ManualInputData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Итого на ${data.weight} г:",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black // Убираем фиолетовый цвет
            )

            // Сетка 2x2 для итогов
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Первая строка: Калории и Белки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    NutritionItem(
                        label = "Калории",
                        value = data.totalCalories,
                        unit = "ккал",
                        modifier = Modifier.weight(1f)
                    )
                    NutritionItem(
                        label = "Белки",
                        value = data.totalProteins,
                        unit = "г",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Вторая строка: Жиры и Углеводы
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    NutritionItem(
                        label = "Жиры",
                        value = data.totalFats,
                        unit = "г",
                        modifier = Modifier.weight(1f)
                    )
                    NutritionItem(
                        label = "Углеводы",
                        value = data.totalCarbs,
                        unit = "г",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NutritionItem(
    label: String,
    value: Float,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF)
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                // Убираем verticalAlignment отсюда
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f", value),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black,
                    modifier = Modifier.alignByBaseline() // <-- Добавляем здесь
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.alignByBaseline() // <-- И здесь
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

// Модель данных для ручного ввода
data class ManualInputData(
    val name: String = "",
    val caloriesPer100g: String = "",
    val proteinsPer100g: String = "",
    val fatsPer100g: String = "",
    val carbsPer100g: String = "",
    val weight: String = ""
) {
    private fun calc(valuePer100g: String): Float {
        val v = valuePer100g.toFloatOrNull() ?: 0f
        val w = weight.toFloatOrNull() ?: 100f
        val total = v * w / 100f
        return (total * 10).roundToInt() / 10f
    }

    val totalCalories: Float get() = calc(caloriesPer100g)
    val totalProteins: Float get() = calc(proteinsPer100g)
    val totalFats: Float get() = calc(fatsPer100g)
    val totalCarbs: Float get() = calc(carbsPer100g)

    fun isValid(): Boolean = name.isNotBlank() &&
            caloriesPer100g.toFloatOrNull() != null &&
            proteinsPer100g.toFloatOrNull() != null &&
            fatsPer100g.toFloatOrNull() != null &&
            carbsPer100g.toFloatOrNull() != null &&
            weight.toFloatOrNull() != null
}
