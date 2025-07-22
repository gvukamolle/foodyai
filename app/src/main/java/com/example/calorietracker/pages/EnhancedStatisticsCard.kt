package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.drawToBitmap
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.extensions.fancyShadow
import com.example.calorietracker.utils.NutritionFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.example.calorietracker.extensions.toNetworkProfile
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import com.example.calorietracker.network.DayDataForAnalysis
import com.example.calorietracker.network.FoodItemData
import com.example.calorietracker.network.UserProfileData
import com.example.calorietracker.network.DailyAnalysisRequest
import com.example.calorietracker.network.TargetNutrients

// Цвета для макронутриентов
object MacroColors {
    val Proteins = Color(0xFF00BFA5) // Бирюзовый
    val Fats = Color(0xFFFFB74D) // Оранжевый
    val Carbs = Color(0xFF64B5F6) // Голубой
    val Calories = Color(0xFFFF4F8A) // Розовый
}

// Модель данных для кнопки вызова
data class ButtonPosition(val x: Float, val y: Float, val width: Int, val height: Int)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedStatisticsCard(
    viewModel: CalorieTrackerViewModel,
    onDismiss: () -> Unit,
    buttonPosition: ButtonPosition? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

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
            modifier = Modifier.fillMaxSize()
        ) {
            // Кликабельный фон для закрытия
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        animatedDismiss()
                    }
            )

            // Размытый фон с затемнением
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
                                .blur(
                                    radiusX = animateDpAsState(
                                        targetValue = if (isVisible) 20.dp else 0.dp,
                                        animationSpec = tween(200),
                                        label = "blur_x"
                                    ).value,
                                    radiusY = animateDpAsState(
                                        targetValue = if (isVisible) 20.dp else 0.dp,
                                        animationSpec = tween(200),
                                        label = "blur_y"
                                    ).value
                                ),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.7f))
                        )
                    }
                }
            }

            // Карточка статистики с позиционированием под кнопкой
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.9f,
                            transformOrigin = TransformOrigin(0.5f, 0f),
                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(animationSpec = tween(150)) +
                        scaleOut(
                            targetScale = 0.9f,
                            transformOrigin = TransformOrigin(0.5f, 0f),
                            animationSpec = tween(150)
                        ),
                modifier = Modifier.then(
                    if (buttonPosition != null) {
                        Modifier.offset {
                            IntOffset(
                                x = buttonPosition.x.toInt() - (360.dp.toPx() / 2).toInt() + (buttonPosition.width / 2),
                                y = (buttonPosition.y + buttonPosition.height + 8.dp.toPx()).toInt()
                            )
                        }
                    } else {
                        Modifier.align(Alignment.TopCenter).padding(top = 64.dp)
                    }
                )
            ) {
                // ВАЖНО: Box с padding для тени
                Box(modifier = Modifier.padding(16.dp)) {
                    Card(
                        modifier = Modifier
                            .width(360.dp)
                            .pointerInput(Unit) {
                                detectTapGestures { /* Блокируем клики внутри карточки */ }
                            }
                            .fancyShadow(
                                borderRadius = 24.dp,
                                shadowRadius = 12.dp,
                                alpha = 0.35f,
                                color = Color.Black
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            // Заголовок с кнопкой закрытия
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Статистика за сегодня",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF212121)
                                    )
                                    Text(
                                        text = "Подробная информация",
                                        fontSize = 14.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        animatedDismiss()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Закрыть",
                                        tint = Color(0xFF757575)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Основной контент статистики
                            StatisticsContent(viewModel = viewModel)

                            Spacer(modifier = Modifier.height(24.dp))

                            // AI анализ дня
                            AIAnalysisSection(
                                viewModel = viewModel,
                                onDismiss = { animatedDismiss() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsContent(viewModel: CalorieTrackerViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Секция калорий
        CaloriesSection(
            current = viewModel.dailyCalories,
            target = viewModel.userProfile.dailyCalories,
            color = MacroColors.Calories
        )

        // Макронутриенты
        MacronutrientsSection(
            proteins = MacroData(viewModel.dailyProtein, viewModel.userProfile.dailyProteins.toFloat()),
            fats = MacroData(viewModel.dailyFat, viewModel.userProfile.dailyFats.toFloat()),
            carbs = MacroData(viewModel.dailyCarbs, viewModel.userProfile.dailyCarbs.toFloat())
        )
    }
}

@Composable
private fun CaloriesSection(
    current: Int,
    target: Int,
    color: Color
) {
    val progress = if (target > 0) current.toFloat() / target else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "calories_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Калории",
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$current",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = " / $target",
                    fontSize = 18.sp,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Прогресс-бар
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE0E0E0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (current < target)
                    "Осталось ${target - current} ккал"
                else
                    "Превышено на ${current - target} ккал",
                fontSize = 13.sp,
                color = if (current <= target) Color(0xFF757575) else Color(0xFFE91E63)
            )
        }
    }
}

@Composable
private fun MacronutrientsSection(
    proteins: MacroData,
    fats: MacroData,
    carbs: MacroData
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MacroCard(
            title = "Белки",
            current = proteins.current,
            target = proteins.target,
            color = MacroColors.Proteins,
            modifier = Modifier.weight(1f)
        )

        MacroCard(
            title = "Жиры",
            current = fats.current,
            target = fats.target,
            color = MacroColors.Fats,
            modifier = Modifier.weight(1f)
        )

        MacroCard(
            title = "Углеводы",
            current = carbs.current,
            target = carbs.target,
            color = MacroColors.Carbs,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MacroCard(
    title: String,
    current: Float,
    target: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) current / target else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "${title}_progress"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF757575)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Круговой прогресс
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 6.dp.toPx()
                    val startAngle = -90f
                    val sweepAngle = 360f * animatedProgress

                    // Фоновый круг
                    drawArc(
                        color = Color(0xFFE0E0E0),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )

                    // Прогресс
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = "${current.toInt()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "из ${target.toInt()}г",
                fontSize = 11.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

@Composable
private fun AIAnalysisSection(
    viewModel: CalorieTrackerViewModel,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
    // Состояния для анализа - сохраняются до конца дня
    var isAnalyzing by remember { mutableStateOf(false) }
    
    // Получаем сохраненный анализ из ViewModel
    val todayKey = LocalDate.now().toString()
    val savedAnalysis = viewModel.getDailyAnalysis(todayKey)
    var analysisResult by remember(savedAnalysis) { mutableStateOf(savedAnalysis?.result) }
    var lastAnalysisTime by remember(savedAnalysis) { mutableStateOf(savedAnalysis?.timestamp) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Если есть результат анализа - показываем его
        if (analysisResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Text(
                            text = "AI Анализ дня",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF212121),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        
                        // Время анализа
                        lastAnalysisTime?.let { time ->
                            Text(
                                text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Результат анализа с прокруткой
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp) // Максимальная высота
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = analysisResult!!,
                            fontSize = 13.sp,
                            color = Color(0xFF424242),
                            lineHeight = 20.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Кнопка повторного анализа
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            analysisResult = null
                            lastAnalysisTime = null
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Запросить повторный анализ",
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            // Кнопка для запуска анализа
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    
                    coroutineScope.launch {
                        isAnalyzing = true
                        
                        try {
                            // Собираем информацию о каждом приеме пищи
                            val mealsData = viewModel.meals.flatMap { meal ->
                                meal.foods.map { food ->
                                    FoodItemData(
                                        name = food.name,
                                        calories = food.calories,
                                        protein = food.protein,
                                        fat = food.fat,
                                        carbs = food.carbs,
                                        weight = food.weight.toIntOrNull() ?: 100
                                    )
                                }
                            }
                            
                            // Создаем запрос для анализа
                            val request = DailyAnalysisRequest(
                                userId = viewModel.userId,
                                date = LocalDate.now().toString(),
                                userProfile = viewModel.userProfile.toNetworkProfile(),
                                targetNutrients = TargetNutrients(
                                    calories = viewModel.userProfile.dailyCalories,
                                    proteins = viewModel.userProfile.dailyProteins.toFloat(),
                                    fats = viewModel.userProfile.dailyFats.toFloat(),
                                    carbs = viewModel.userProfile.dailyCarbs.toFloat()
                                ),
                                meals = mealsData,
                                messageType = "daily_analysis" // Маркер для разделения сценариев
                            )
                            
                            // Отправляем запрос
                            val response = viewModel.sendDailyAnalysisRequest(request)
                            
                            if (response != null) {
                                analysisResult = response
                                lastAnalysisTime = LocalDateTime.now()
                                // Анализ уже сохранен в ViewModel через sendDailyAnalysisRequest
                            } else {
                                // Обработка ошибки
                                analysisResult = "Не удалось получить анализ. Проверьте подключение к интернету."
                            }
                            
                        } catch (e: Exception) {
                            analysisResult = "Произошла ошибка при анализе: ${e.message}"
                        } finally {
                            isAnalyzing = false
                        }
                    }
                },
                enabled = !isAnalyzing && viewModel.meals.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800),
                    disabledContainerColor = Color(0xFFE0E0E0)
                )
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Анализирую...",
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Проанализировать день с AI",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Подсказка если нет данных
            if (viewModel.meals.isEmpty()) {
                Text(
                    text = "Добавьте приемы пищи для анализа",
                    fontSize = 12.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Модель данных для макронутриента
data class MacroData(
    val current: Float,
    val target: Float
)