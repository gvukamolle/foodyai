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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.derivedStateOf
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.drawToBitmap
import com.example.calorietracker.extensions.fancyShadow
import com.example.calorietracker.ui.animations.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

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

    // Обработка системной кнопки "назад" - работает правильно
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
                    // Этот код сработает ТОЛЬКО если кликнуть на фон,
                    // т.к. клик по карточке будет перехвачен и "съеден".
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
                Box(
                    // КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: pointerInput вместо clickable
                    // Он "съедает" событие нажатия, не давая ему "провалиться" на фон.
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                // Этот код сработает при клике на карточку.
                                // И событие на этом закончится.
                                if (imeVisible) {
                                    // Нужно запускать в корутине, т.к. мы в suspend-контексте
                                    coroutineScope.launch {
                                        focusManager.clearFocus()
                                    }
                                }
                            })
                        }
                ) {
                    Card(
                        modifier = Modifier
                            .padding(24.dp) // padding теперь внутри, чтобы область нажатия была больше
                            .widthIn(max = 360.dp)
                            .fancyShadow(borderRadius = 24.dp, shadowRadius = 12.dp, alpha = 0.35f, color = accentColor),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        content()
                    }
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InputFields(
                    data = data,
                    onDataChange = { data = it }
                )

                if (data.isValid() && data.weight.toFloatOrNull() != null) {
                    NutritionSummary(data = data)
                }
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

// Диалог "Расскажи"
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedDescribeDialog(
    onDismiss: () -> Unit,
    onAnalyze: (String) -> Unit,
    isAnalyzing: Boolean
) {
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current

    AnimatedDialogContainer(
        onDismiss = onDismiss,
        accentColor = DialogColors.AIAnalysis
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Заголовок
            DialogHeader(
                icon = Icons.Default.AutoAwesome,
                title = "Опишите блюдо",
                subtitle = "AI проанализирует состав",
                accentColor = DialogColors.AIAnalysis
            )

            Spacer(Modifier.height(16.dp))

            // Поле ввода текста
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("Описание блюда") },
                placeholder = {
                    Text(
                        "Например: Овсяная каша с бананом и орехами, примерно 300 грамм",
                        fontSize = 16.sp
                    )
                },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DialogColors.AIAnalysis,
                    focusedLabelColor = DialogColors.AIAnalysis,
                    cursorColor = DialogColors.AIAnalysis
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )

            // AI индикатор анализа
            if (isAnalyzing) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = DialogColors.AIAnalysis
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "AI анализирует...",
                        fontSize = 14.sp,
                        color = DialogColors.AIAnalysis
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Кнопки
            DialogActions(
                onCancel = onDismiss,
                onConfirm = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAnalyze(text)
                },
                confirmEnabled = text.isNotBlank() && !isAnalyzing,
                confirmText = if (isAnalyzing) "Анализ..." else "Отправить",
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Image(
                    bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(16.dp))

            // Поле для подписи
            OutlinedTextField(
                value = caption,
                onValueChange = onCaptionChange,
                label = { Text("Добавить подпись (необязательно)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DialogColors.Photo,
                    focusedLabelColor = DialogColors.Photo
                ),
                leadingIcon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = DialogColors.Photo
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                singleLine = true
            )

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
    val fields = listOf(
        Triple("Название", data.name) { value: String ->
            onDataChange(data.copy(name = value))
        },
        Triple("Калории на 100г", data.caloriesPer100g) { value: String ->
            onDataChange(data.copy(caloriesPer100g = value))
        },
        Triple("Белки на 100г", data.proteinsPer100g) { value: String ->
            onDataChange(data.copy(proteinsPer100g = value))
        },
        Triple("Жиры на 100г", data.fatsPer100g) { value: String ->
            onDataChange(data.copy(fatsPer100g = value))
        },
        Triple("Углеводы на 100г", data.carbsPer100g) { value: String ->
            onDataChange(data.copy(carbsPer100g = value))
        },
        Triple("Вес (грамм)", data.weight) { value: String ->
            onDataChange(data.copy(weight = value))
        }
    )

    fields.forEach { (label, value, onChange) ->
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DialogColors.ManualInput,
                focusedLabelColor = DialogColors.ManualInput
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (label == "Название") KeyboardType.Text else KeyboardType.Number,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Итого на ${data.weight} г:",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionItem("Калории", data.totalCalories)
                NutritionItem("Белки", data.totalProteins)
                NutritionItem("Жиры", data.totalFats)
                NutritionItem("Углеводы", data.totalCarbs)
            }
        }
    }
}

@Composable
private fun NutritionItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = DialogColors.ManualInput
        )
        Text(
            label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

// Модель данных для ручного ввода
data class ManualInputData(
    val name: String = "",
    val caloriesPer100g: String = "",
    val proteinsPer100g: String = "",
    val fatsPer100g: String = "",
    val carbsPer100g: String = "",
    val weight: String = "100"
) {
    val totalCalories: Int get() = ((caloriesPer100g.toFloatOrNull() ?: 0f) * (weight.toFloatOrNull() ?: 100f) / 100).toInt()
    val totalProteins: Int get() = ((proteinsPer100g.toFloatOrNull() ?: 0f) * (weight.toFloatOrNull() ?: 100f) / 100).toInt()
    val totalFats: Int get() = ((fatsPer100g.toFloatOrNull() ?: 0f) * (weight.toFloatOrNull() ?: 100f) / 100).toInt()
    val totalCarbs: Int get() = ((carbsPer100g.toFloatOrNull() ?: 0f) * (weight.toFloatOrNull() ?: 100f) / 100).toInt()

    fun isValid(): Boolean = name.isNotBlank() &&
            caloriesPer100g.toFloatOrNull() != null &&
            proteinsPer100g.toFloatOrNull() != null &&
            fatsPer100g.toFloatOrNull() != null &&
            carbsPer100g.toFloatOrNull() != null &&
            weight.toFloatOrNull() != null
}
