package com.example.calorietracker.pages

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.DailyIntake
import com.example.calorietracker.data.DailyNutritionSummary
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.example.calorietracker.extensions.fancyShadow
import kotlin.math.abs

@Composable
fun CalendarScreen(
    viewModel: CalorieTrackerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val systemUiController = rememberSystemUiController()
    val calendarData by viewModel.calendarData.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Состояние календаря
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var showDayHistory by remember { mutableStateOf(false) }
    var dayHistoryData by remember { mutableStateOf<DailyIntake?>(null) }

    // Состояние масштабирования
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Состояние для свайпа
    var swipeOffset by remember { mutableStateOf(0f) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val repository = remember { DataRepository(context) }

    DisposableEffect(systemUiController) {
        systemUiController.setSystemBarsColor(
            color = Color.White,
            darkIcons = true
        )
        onDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        CalendarTopBar(
            currentMonth = selectedMonth,
            onBack = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onBack()
            },
            onPreviousMonth = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                selectedMonth = selectedMonth.minusMonths(1)
                selectedDay = null
            },
            onNextMonth = {
                if (selectedMonth < YearMonth.now()) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selectedMonth = selectedMonth.plusMonths(1)
                    selectedDay = null
                }
            }
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    swipeOffset > 100 -> {
                                        // Свайп вправо - предыдущий месяц
                                        selectedMonth = selectedMonth.minusMonths(1)
                                        selectedDay = null
                                    }

                                    swipeOffset < -100 -> {
                                        // Свайп влево - следующий месяц
                                        if (selectedMonth < YearMonth.now()) {
                                            selectedMonth = selectedMonth.plusMonths(1)
                                            selectedDay = null
                                        }
                                    }
                                }
                                swipeOffset = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                swipeOffset += dragAmount
                            }
                        )
                    }
                    .graphicsLayer(
                        scaleX = animatedScale,
                        scaleY = animatedScale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .transformable(
                        state = rememberTransformableState { zoomChange, offsetChange, _ ->
                            scale = (scale * zoomChange).coerceIn(0.8f, 2f)
                            offsetX += offsetChange.x
                            offsetY += offsetChange.y

                            val maxOffset = 200f * (scale - 1f)
                            offsetX = offsetX.coerceIn(-maxOffset, maxOffset)
                            offsetY = offsetY.coerceIn(-maxOffset, maxOffset)
                        }
                    )
            ) {
                CalendarGrid(
                    selectedMonth = selectedMonth,
                    selectedDay = selectedDay,
                    onDayClick = { day ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (selectedDay == day) {
                            coroutineScope.launch {
                                val dateString = day.toString()
                                dayHistoryData = repository.getIntakeHistory(dateString)
                                showDayHistory = true
                            }
                        } else {
                            selectedDay = day
                        }
                    }
                )
            }
        }
    }


        if (showDayHistory && selectedDay != null) {
            DayHistoryDialog(
                date = selectedDay!!,
                dailyIntake = dayHistoryData ?: DailyIntake(),
                nutritionSummary = calendarData[selectedDay],
                onDismiss = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showDayHistory = false
                },
                onMealUpdate = { idx, meal ->
                    viewModel.updateMealInHistory(selectedDay!!.toString(), idx, meal)
                },
                onMealDelete = { idx ->
                    viewModel.deleteMealFromHistory(selectedDay!!.toString(), idx)
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CalendarTopBar(
        currentMonth: YearMonth,
        onBack: () -> Unit,
        onPreviousMonth: () -> Unit,
        onNextMonth: () -> Unit
    ) {
        val month = currentMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru"))
            .replaceFirstChar { it.uppercase() }
        val year = currentMonth.year

        CenterAlignedTopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = onPreviousMonth) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Предыдущий месяц"
                        )
                    }

                    Text(
                        text = "$month $year",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    IconButton(
                        onClick = onNextMonth,
                        enabled = currentMonth < YearMonth.now()
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "Следующий месяц",
                            tint = if (currentMonth < YearMonth.now())
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.White
            )
        )
    }

    @Composable
    private fun CalendarGrid(
        selectedMonth: YearMonth,
        selectedDay: LocalDate?,
        onDayClick: (LocalDate) -> Unit
    ) {
        val daysOfWeek = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
        val firstDayOfMonth = selectedMonth.atDay(1)
        val lastDayOfMonth = selectedMonth.atEndOfMonth()
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
        val daysInMonth = selectedMonth.lengthOfMonth()
        val today = LocalDate.now()

        // Вычисляем даты для заполнения из предыдущего месяца
        val previousMonth = selectedMonth.minusMonths(1)
        val daysInPreviousMonth = previousMonth.lengthOfMonth()
        val daysFromPreviousMonth = firstDayOfWeek - 1

        // Фиксируем количество строк в календаре
        val CALENDAR_ROWS = 6

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 12.sp, // Уменьшили размер
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp)) // Уменьшили отступ

            // Всегда отображаем 6 строк
            for (week in 0 until CALENDAR_ROWS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayOfWeek in 0..6) {
                        val cellIndex = week * 7 + dayOfWeek

                        when {
                            // Дни из предыдущего месяца
                            cellIndex < daysFromPreviousMonth -> {
                                val dayNumber =
                                    daysInPreviousMonth - (daysFromPreviousMonth - cellIndex - 1)
                                val date = previousMonth.atDay(dayNumber)

                                DayCell(
                                    date = date,
                                    isSelected = false,
                                    isToday = false,
                                    isFuture = false,
                                    isOtherMonth = true,
                                    onClick = { /* Некликабельно */ },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Дни текущего месяца
                            cellIndex - daysFromPreviousMonth < daysInMonth -> {
                                val dayNumber = cellIndex - daysFromPreviousMonth + 1
                                val date = selectedMonth.atDay(dayNumber)

                                DayCell(
                                    date = date,
                                    isSelected = selectedDay == date,
                                    isToday = date == today,
                                    isFuture = date > today,
                                    isOtherMonth = false,
                                    onClick = { onDayClick(date) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Дни из следующего месяца
                            else -> {
                                val nextMonth = selectedMonth.plusMonths(1)
                                val dayNumber = cellIndex - daysFromPreviousMonth - daysInMonth + 1
                                val date = nextMonth.atDay(dayNumber)

                                DayCell(
                                    date = date,
                                    isSelected = false,
                                    isToday = false,
                                    isFuture = false,
                                    isOtherMonth = true,
                                    onClick = { /* Некликабельно */ },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (week < CALENDAR_ROWS - 1) {
                    Spacer(modifier = Modifier.height(8.dp)) // Уменьшили отступ
                }
            }
        }
    }

    @Composable
    private fun DayCell(
        date: LocalDate,
        isSelected: Boolean,
        isToday: Boolean,
        isFuture: Boolean,
        isOtherMonth: Boolean = false,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val backgroundColor = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isToday && !isOtherMonth -> Color.Gray.copy(alpha = 0.2f)
            else -> Color.Transparent
        }

        val textColor = when {
            isOtherMonth -> Color.Gray.copy(alpha = 0.3f) // Тускло-серый для дат из других месяцев
            isSelected -> Color.White
            isToday -> Color.Black
            isFuture -> Color.DarkGray.copy(alpha = 0.5f)
            else -> Color.DarkGray
        }

        val animatedScale by animateFloatAsState(
            targetValue = if (isSelected && !isOtherMonth) 0.95f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "cellScale"
        )

        // Анимация для выбранного дня
        val numberOffset by animateDpAsState(
            targetValue = if (isSelected && !isOtherMonth) (-8).dp else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "numberOffset"
        )

        val dotAlpha by animateFloatAsState(
            targetValue = if (isSelected && !isOtherMonth) 1f else 0f,
            animationSpec = tween(300),
            label = "dotAlpha"
        )

        Box(
            modifier = modifier
                .padding(horizontal = 4.dp, vertical = 4.dp) // Уменьшили padding
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
                .clip(RoundedCornerShape(24.dp)) // Уменьшили радиус
                .background(backgroundColor)
                .clickable(enabled = !isFuture && !isOtherMonth) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxHeight()
            ) {
                // Число с анимацией смещения
                Text(
                    text = date.dayOfMonth.toString(),
                    fontSize = 18.sp, // Уменьшили размер
                    fontWeight = if ((isSelected || isToday) && !isOtherMonth) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    modifier = Modifier.offset(y = numberOffset)
                )

                // Точка, появляющаяся при выборе
                if (isSelected && !isOtherMonth) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .graphicsLayer { alpha = dotAlpha }
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
