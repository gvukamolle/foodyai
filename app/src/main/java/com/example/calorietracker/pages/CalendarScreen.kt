package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as DateTextStyle
import java.util.*

data class DayData(
    val date: LocalDate,
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val hasData: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalorieTrackerViewModel,
    onBack: () -> Unit
) {
    val systemUiController = rememberSystemUiController()
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var visible by remember { mutableStateOf(false) }

    // Данные из viewModel
    val calendarData by viewModel.calendarData.collectAsState()
    val currentIntake = viewModel.dailyIntake

    LaunchedEffect(Unit) {
        systemUiController.setSystemBarsColor(color = Color.White, darkIcons = true)
        delay(50)
        visible = true
    }

    Scaffold(
        topBar = {
            CalendarTopBar(
                currentMonth = selectedMonth,
                onBack = onBack,
                onPreviousMonth = {
                    selectedMonth = selectedMonth.minusMonths(1)
                },
                onNextMonth = {
                    if (selectedMonth < YearMonth.now()) {
                        selectedMonth = selectedMonth.plusMonths(1)
                    }
                }
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically { -20 }
            ) {
                CalendarGrid(
                    selectedMonth = selectedMonth,
                    selectedDay = selectedDay,
                    onDayClick = { date ->
                        selectedDay = if (selectedDay == date) null else date
                    },
                    currentIntake = currentIntake,
                    calendarData = calendarData
                )
            }

            selectedDay?.let { day ->
                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    DayDetailsCard(
                        dayData = getDayData(day, currentIntake, calendarData),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
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

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
private fun CalendarGrid(
    selectedMonth: YearMonth,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    currentIntake: com.example.calorietracker.data.DailyIntake,
    calendarData: Map<LocalDate, com.example.calorietracker.data.DailyNutritionSummary>
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

            Spacer(modifier = Modifier.height(12.dp))

            // Сетка дней
            val totalCells = (firstDayOfWeek - 1) + daysInMonth
            val rows = (totalCells + 6) / 7

            for (week in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayOfWeek in 0..6) {
                        val dayIndex = week * 7 + dayOfWeek - (firstDayOfWeek - 1)

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
                    Spacer(modifier = Modifier.height(8.dp))
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

    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
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
                fontSize = 16.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )

            if (dayData.hasData && !isSelected) {
                Text(
                    text = "${dayData.calories}",
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DayDetailsCard(
    dayData: DayData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            val dayOfWeek = dayData.date.dayOfWeek.getDisplayName(DateTextStyle.FULL, Locale("ru"))
            val month = dayData.date.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("ru"))

            Text(
                text = "${dayData.date.dayOfMonth} $month, $dayOfWeek",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (dayData.hasData) {
                MacroRow(
                    label = "Калории",
                    value = "${dayData.calories} ккал",
                    color = MaterialTheme.colorScheme.primary
                )
                MacroRow(
                    label = "Белки",
                    value = "${dayData.protein} г",
                    color = Color(0xFF4CAF50)
                )
                MacroRow(
                    label = "Углеводы",
                    value = "${dayData.carbs} г",
                    color = Color(0xFFFF9800)
                )
                MacroRow(
                    label = "Жиры",
                    value = "${dayData.fat} г",
                    color = Color(0xFFF44336)
                )
            } else {
                Text(
                    text = "Нет данных за этот день",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MacroRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

// Вспомогательная функция для получения данных дня
private fun getDayData(
    date: LocalDate,
    currentIntake: com.example.calorietracker.data.DailyIntake,
    calendarData: Map<LocalDate, com.example.calorietracker.data.DailyNutritionSummary>
): DayData {

    return when {
        date == LocalDate.now() -> DayData(
            date = date,
            calories = currentIntake.calories,
            protein = currentIntake.protein.toInt(),
            carbs = currentIntake.carbs.toInt(),
            fat = currentIntake.fat.toInt(),
            hasData = currentIntake.calories > 0
        )
        calendarData[date] != null -> {
            val summary = calendarData[date]!!
            DayData(
                date = date,
                calories = summary.totalCalories,
                protein = summary.totalProtein.toInt(),
                carbs = summary.totalCarbs.toInt(),
                fat = summary.totalFat.toInt(),
                hasData = true
            )
        }
        else -> DayData(date = date)
    }
}