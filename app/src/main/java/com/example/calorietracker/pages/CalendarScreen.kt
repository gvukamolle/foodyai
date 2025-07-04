package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.DailyIntake
import com.example.calorietracker.data.DailyNutritionSummary
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalorieTrackerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val systemUiController = rememberSystemUiController()
    val calendarData by viewModel.calendarData.collectAsState()
    val currentIntake = viewModel.dailyIntake
    val coroutineScope = rememberCoroutineScope()

    // Состояние календаря
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var showDayHistory by remember { mutableStateOf(false) }
    var dayHistoryData by remember { mutableStateOf<DailyIntake?>(null) }

    // Состояние масштабирования
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Анимация масштабирования
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Репозиторий для получения данных
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
            .background(Color(0xFFF5F5F5))
    ) {
        CalendarTopBar(
            currentMonth = selectedMonth,
            onBack = onBack,
            onPreviousMonth = {
                selectedMonth = selectedMonth.minusMonths(1)
                selectedDay = null
            },
            onNextMonth = {
                if (selectedMonth < YearMonth.now()) {
                    selectedMonth = selectedMonth.plusMonths(1)
                    selectedDay = null
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Трансформируемый календарь
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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

                            // Ограничиваем смещение при масштабировании
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
                        selectedDay = day

                        // Загружаем данные для выбранного дня
                        coroutineScope.launch {
                            val dateString = day.toString()
                            dayHistoryData = repository.getIntakeHistory(dateString)
                            showDayHistory = true
                        }
                    },
                    currentIntake = currentIntake,
                    calendarData = calendarData
                )
            }
        }
    }

    // Диалог истории дня
    if (showDayHistory && selectedDay != null) {
        DayHistoryDialog(
            date = selectedDay!!,
            dailyIntake = dayHistoryData ?: DailyIntake(),
            nutritionSummary = calendarData[selectedDay],
            onDismiss = { showDayHistory = false }
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
    val month = currentMonth.month.getDisplayName(DateTextStyle.FULL_STANDALONE, Locale("ru"))
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
    onDayClick: (LocalDate) -> Unit,
    currentIntake: DailyIntake,
    calendarData: Map<LocalDate, DailyNutritionSummary>
) {
    val daysOfWeek = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
    val firstDayOfMonth = selectedMonth.atDay(1)
    val lastDayOfMonth = selectedMonth.atEndOfMonth()
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
    val daysInMonth = selectedMonth.lengthOfMonth()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Дни недели
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Сетка дней
            val totalCells = (firstDayOfWeek - 1) + daysInMonth
            val rows = (totalCells + 6) / 7

            for (week in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayOfWeek in 0..6) {
                        val dayIndex = week * 7 + dayOfWeek - (firstDayOfWeek - 1) + 1

                        if (dayIndex in 1..daysInMonth) {
                            val date = selectedMonth.atDay(dayIndex)
                            val dayData = getDayData(date, currentIntake, calendarData)

                            DayCell(
                                date = date,
                                dayData = dayData,
                                isSelected = selectedDay == date,
                                isToday = date == LocalDate.now(),
                                isFuture = date > LocalDate.now(),
                                onClick = { onDayClick(date) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (week < rows - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    dayData: DayData,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        dayData.hasData -> Color(0xFFE8F5E9)
        else -> Color.Transparent
    }

    val textColor = when {
        isSelected -> Color.White
        isFuture -> Color.Gray.copy(alpha = 0.5f)
        dayData.hasData -> Color(0xFF2E7D32)
        else -> Color.Black
    }

    // Анимация при выборе
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cellScale"
    )

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .height(56.dp) // Увеличенная высота для "таблетки"
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(RoundedCornerShape(50)) // Полностью закругленные края
            .background(backgroundColor)
            .clickable(enabled = !isFuture) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 18.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )

            if (dayData.hasData && !isSelected) {
                Text(
                    text = "${dayData.calories} ккал",
                    fontSize = 10.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        // Индикатор для сегодняшнего дня
        if (isToday && !isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

private data class DayData(
    val hasData: Boolean,
    val calories: String,
    val protein: Float,
    val fat: Float,
    val carbs: Float
)

private fun getDayData(
    date: LocalDate,
    currentIntake: DailyIntake,
    calendarData: Map<LocalDate, DailyNutritionSummary>
): DayData {
    return if (date == LocalDate.now()) {
        DayData(
            hasData = currentIntake.calories > 0,
            calories = currentIntake.calories.toString(),
            protein = currentIntake.protein,
            fat = currentIntake.fat,
            carbs = currentIntake.carbs
        )
    } else {
        val summary = calendarData[date]
        DayData(
            hasData = summary != null,
            calories = (summary?.totalCalories ?: 0).toString(),
            protein = summary?.totalProtein ?: 0f,
            fat = summary?.totalFat ?: 0f,
            carbs = summary?.totalCarbs ?: 0f
        )
    }
}