package com.example.calorietracker.pages

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.platform.LocalView
import androidx.core.view.drawToBitmap
import androidx.compose.ui.draw.blur
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.calorietracker.extensions.fancyShadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import com.example.calorietracker.utils.capitalizeFirst
import com.example.calorietracker.utils.filterDecimal
import kotlin.math.roundToInt
import java.util.Locale

// =========================================================================
// НОВАЯ УНИВЕРСАЛЬНАЯ ОБЕРТКА ДЛЯ ПРАВИЛЬНОЙ АНИМАЦИИ
// Она заменяет старый BeautifulDialogWrapper.
// =========================================================================
@Composable
fun AnimatedPopup(
    onDismissRequest: () -> Unit,
    content: @Composable (onDismiss: () -> Unit) -> Unit
) {
    // Сразу начинаем с видимого состояния для мгновенной анимации
    var isVisible by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Снимаем скриншот текущего экрана, чтобы размыть его под диалогом
    val view = LocalView.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val ime = WindowInsets.ime
    val imeVisible by remember {
        derivedStateOf { ime.getBottom(density) > 0 }
    }
    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(view) {
        // Даем вью отрисоваться перед захватом
        kotlinx.coroutines.delay(10)
        backgroundBitmap = view.drawToBitmap()
    }

    // Функция, которая запускает анимацию закрытия и только потом вызывает onDismissRequest
    fun dismiss() {
        coroutineScope.launch {
            isVisible = false
            delay(150) // Сократили время анимации исчезновения
            onDismissRequest()
        }
    }

    // Более быстрая и плавная анимация
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isVisible) 150 else 100, // Быстрее появление
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.95f, // Меньший диапазон для более тонкой анимации
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy, // Убираем отскок для скорости
            stiffness = Spring.StiffnessHigh // Высокая жесткость для быстрой анимации
        ),
        label = "scale"
    )

    // Радиус размытия фона анимируем синхронно с появлением
    val blurRadius by animateDpAsState(
        targetValue = if (isVisible) 16.dp else 0.dp,
        animationSpec = tween(
            durationMillis = if (isVisible) 150 else 100,
            easing = FastOutSlowInEasing
        ),
        label = "blur"
    )

    Popup(
        onDismissRequest = { dismiss() },
        properties = PopupProperties(focusable = true)
    ) {
        // Контейнер для затемнения и контента
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Размытый фон из скриншота
            backgroundBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .blur(blurRadius),
                    contentScale = ContentScale.Crop
                )
            }
            // Светлый оверлей, по нажатию закрывающий диалог
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

            // Анимированный контейнер для содержимого

            // Анимированный контейнер для содержимого
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = animatedAlpha
                    }
                    // Этот clickable нужен, чтобы клики по карточке не закрывали диалог
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


// Улучшенный диалог ручного ввода продукта
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeautifulManualFoodInputDialog(
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
                // Заголовок с иконкой
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color(0xFF9C27B0).copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            if (initialFoodName.isNotEmpty()) "Проверьте данные" else "Добавить продукт",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            "Заполните информацию о продукте",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Название продукта
                BeautifulTextField(
                    value = foodName,
                    onValueChange = { foodName = it.capitalizeFirst() },
                    label = "Название продукта",
                    icon = Icons.Default.FoodBank,
                    iconColor = Color(0xFF9C27B0),
                    modifier = Modifier.fillMaxWidth()
                )

                // Вес порции
                BeautifulTextField(
                    value = weight,
                    onValueChange = { weight = filterDecimal(it) },
                    label = "Вес порции (г)",
                    icon = Icons.Default.Scale,
                    iconColor = Color(0xFF2196F3),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Разделитель
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

                // Заголовок для нутриентов
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

                // Калории
                BeautifulTextField(
                    value = caloriesPer100g,
                    onValueChange = { caloriesPer100g = filterDecimal(it) },
                    label = "Калории (ккал)",
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = Color(0xFFFF5722),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // БЖУ в строку
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

                // Итоговая информация (если вес не 100г)
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

                // Кнопки действий
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


// Улучшенный диалог описания блюда
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BeautifulDescribeFoodDialog(
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
                // Заголовок
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color(0xFFFF9800).copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "Расскажите",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            "AI распознает и посчитает КБЖУ",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Поле ввода или индикатор загрузки
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

                // Подсказки
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

                // Кнопки
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


// Улучшенный диалог выбора фото
@Composable
fun BeautifulPhotoUploadDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    AnimatedPopup(onDismissRequest = onDismiss) { animatedDismiss ->
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Заголовок
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "Добавить фото",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            "AI распознает продукт",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Опции
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PhotoOptionCard(
                        icon = Icons.Default.PhotoCamera,
                        title = "Сделать фото",
                        subtitle = "Используйте камеру",
                        color = Color(0xFF4CAF50),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCameraClick()
                        }
                    )

                    PhotoOptionCard(
                        icon = Icons.Default.PhotoLibrary,
                        title = "Выбрать из галереи",
                        subtitle = "Загрузите готовое фото",
                        color = Color(0xFF2196F3),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGalleryClick()
                        }
                    )
                }

                // Подсказка
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
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
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Совет: Сфотографируйте этикетку с составом для точного анализа",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32),
                            lineHeight = 18.sp
                        )
                    }
                }

                // Кнопка отмены
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        animatedDismiss()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        "Отмена",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}


// Улучшенный диалог подтверждения фото
@Composable
fun BeautifulPhotoConfirmDialog(
    bitmap: Bitmap,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    AnimatedPopup(onDismissRequest = onDismiss) { animatedDismiss ->
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
                    .background(Color.White)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок
                Text(
                    "Отправить фото",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

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

                // Поле для подписи
                BeautifulTextField(
                    value = caption,
                    onValueChange = onCaptionChange,
                    label = "Добавить подпись (необязательно)",
                    modifier = Modifier.fillMaxWidth()
                )

                // Кнопки
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
                            onConfirm()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black
                        )
                    ) {
                        Icon(
                            Icons.Default.Send,
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


// =========================================================================
// Вспомогательные компоненты (без изменений)
// =========================================================================

@Composable
fun NutrientChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    label,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    value,
                    fontSize = 14.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BeautifulTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector? = null,
    iconColor: Color = Color.Black,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) iconColor else Color(0xFFE0E0E0),
        animationSpec = tween(200), label = ""
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Icon(
                        it,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isFocused) iconColor else Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(label)
            }
        },
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = borderColor,
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFFAFAFA)
        ),
        keyboardOptions = keyboardOptions,
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun PhotoOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = color)
            ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = color.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            Icon(
                Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
