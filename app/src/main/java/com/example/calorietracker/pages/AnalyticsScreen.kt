package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.calorietracker.presentation.viewmodels.CalorieTrackerViewModel
import com.example.calorietracker.data.DayData
import com.example.calorietracker.extensions.fancyShadow
import com.example.calorietracker.network.*
import com.example.calorietracker.utils.NutritionFormatter
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.calorietracker.utils.DailyResetUtils
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.*

// Обновленные цвета для более современного вида
object AnalyticsColors {
    val Primary = Color(0xFF2196F3) // Фирменный синий
    val Secondary = Color(0xFF1976D2) // Темно-синий
    val Success = Color(0xFF4CAF50) // Зеленый
    val Warning = Color(0xFFF59E0B) // Amber
    val Error = Color(0xFFEF4444) // Red
    val Info = Color(0xFF03A9F4) // Light Blue

    val CardBackground = Color(0xFFFFFFFF)
    val Background = Color(0xFFF9FAFB)
    val TextPrimary = Color(0xFF111827)
    val TextSecondary = Color(0xFF6B7280)
    val Border = Color(0xFFE5E7EB)

    // Градиенты
    val PrimaryGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF2196F3), Color(0xFF1976D2))
    )

    val SuccessGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
    )

    val WarningGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
    )
}

// Модели данных для аналитики
data class WeeklyTrend(
    val weekData: List<Pair<LocalDate, DayData?>>,
    val averageCalories: Float,
    val trend: TrendType,
    val changePercent: Float
)

enum class TrendType {
    UP, DOWN, STABLE
}

data class NutritionBalance(
    val proteinsPercent: Float,
    val fatsPercent: Float,
    val carbsPercent: Float,
    val isBalanced: Boolean
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val progress: Float,
    val isUnlocked: Boolean,
    val unlockedDate: LocalDate? = null,
    val color: Color
)

data class Streak(
    val type: StreakType,
    val count: Int,
    val startDate: LocalDate,
    val isActive: Boolean
)

enum class StreakType(val label: String, val icon: ImageVector) {
    DAILY_GOAL("Дневная цель", Icons.Default.LocalFireDepartment),
    BALANCED_NUTRITION("Сбалансированное питание", Icons.Default.Balance),
    LOGGING("Ведение дневника", Icons.Default.EditCalendar)
}

// Модель для AI анализа
data class AIAnalysisResponse(
    val general_analysis: String,  // Общий анализ
    val trends: String,           // Тренды
    val recommendations: String   // Рекомендации на следующую неделю
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: CalorieTrackerViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Состояния для анимаций
    var isVisible by remember { mutableStateOf(false) }
    var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.WEEK) }
    var showAchievementDialog by remember { mutableStateOf<Achievement?>(null) }

    // Загрузка данных
    val todayData = viewModel.getTodayData()
    val weekData = remember { calculateWeekData(viewModel) }
    val monthData = remember { calculateMonthData(viewModel) }
    val yearData = remember { calculateYearData(viewModel) }

    // Расчет трендов и статистики
    val weeklyTrend = remember(weekData) { calculateWeeklyTrend(weekData) }
    val nutritionBalance = remember(todayData) { calculateNutritionBalance(todayData, viewModel.userProfile) }
    val achievements = remember { generateAchievements(viewModel) }
    val streaks = remember { calculateStreaks(viewModel) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        topBar = {
            AnalyticsTopBar(
                onBack = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                },
                onExport = { /* Data export functionality not implemented yet */ }
            )
        },
        containerColor = AnalyticsColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Период выбора
            PeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodChange = { selectedPeriod = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Основная статистика с анимацией
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + slideInVertically { -20 }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Карточка общего прогресса
                    OverviewCard(
                        todayData = todayData,
            userProfile = viewModel.userProfile,
                        weeklyTrend = weeklyTrend
                    )

                    // AI Insights - перенесен сюда
                    AIInsightsCard(
                        viewModel = viewModel,
                        weekData = weekData,
                        coroutineScope = coroutineScope
                    )

                    // График калорий
                    CaloriesChartCard(
                        period = selectedPeriod,
                        weekData = weekData,
                        monthData = monthData,
                        yearData = yearData,
                        userProfile = viewModel.userProfile
                    )

                    // Баланс макронутриентов
                    MacroBalanceCard(
                        nutritionBalance = nutritionBalance,
                        todayData = todayData,
                        userProfile = viewModel.userProfile
                    )

                    // Серии - отдельным блоком
                    StreaksCard(streaks = streaks)

                    // Постоянство - отдельным блоком
                    ConsistencyCard(weekData = weekData)

                    // Достижения в белом блоке
                    AchievementsCard(
                        achievements = achievements,
                        onAchievementClick = { achievement ->
                            showAchievementDialog = achievement
                        }
                    )
                }
            }
        }
    }

    // Диалог достижения
    showAchievementDialog?.let { achievement ->
        AchievementDialog(
            achievement = achievement,
            onDismiss = { showAchievementDialog = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsTopBar(
    onBack: () -> Unit,
    onExport: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "Аналитика",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Детальная статистика питания",
                    fontSize = 12.sp,
                    color = AnalyticsColors.TextSecondary
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
        },
        actions = {
            IconButton(onClick = onExport) {
                Icon(Icons.Default.Share, contentDescription = "Экспорт")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AnalyticsColors.CardBackground
        )
    )
}

enum class AnalyticsPeriod(val label: String) {
    WEEK("Неделя"),
    MONTH("Месяц"),
    YEAR("Год")
}

@Composable
private fun PeriodSelector(
    selectedPeriod: AnalyticsPeriod,
    onPeriodChange: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AnalyticsColors.CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnalyticsPeriod.values().forEach { period ->
                val isSelected = period == selectedPeriod

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) AnalyticsColors.Primary
                            else Color.Transparent
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPeriodChange(period)
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.label,
                        color = if (isSelected) Color.White else AnalyticsColors.TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    todayData: DayData?,
    userProfile: com.example.calorietracker.data.UserProfile,
    weeklyTrend: WeeklyTrend
) {
    val animatedCalories by animateFloatAsState(
        targetValue = todayData?.calories ?: 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "calories"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fancyShadow(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AnalyticsColors.CardBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // Градиентный фон
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRoundRect(
                            brush = AnalyticsColors.PrimaryGradient,
                            cornerRadius = CornerRadius(20.dp.toPx())
                        )
                    }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            "Сегодня",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Text(
                            "${animatedCalories.toInt()} ккал",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "из ${userProfile.dailyCalories} ккал",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }

                    // Круговой прогресс
                    CircularProgressIndicator(
                        progress = (todayData?.calories ?: 0f) / userProfile.dailyCalories,
                        color = Color.White,
                        size = 80.dp
                    )
                }

                // Тренд за неделю
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (weeklyTrend.trend) {
                                TrendType.UP -> Icons.Default.TrendingUp
                                TrendType.DOWN -> Icons.Default.TrendingDown
                                TrendType.STABLE -> Icons.Default.TrendingFlat
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Недельный тренд: ${weeklyTrend.changePercent.toInt()}%",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        "Ø ${weeklyTrend.averageCalories.toInt()} ккал",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AIInsightsCard(
    viewModel: CalorieTrackerViewModel,
    weekData: List<Pair<LocalDate, DayData?>>,
    coroutineScope: CoroutineScope
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var aiAnalysis by remember { mutableStateOf<AIAnalysisResponse?>(null) }
    var analysisDate by remember { mutableStateOf<LocalDate?>(null) }
    val haptic = LocalHapticFeedback.current
    val today = runCatching { DailyResetUtils.getFoodLocalDate() }.getOrElse { LocalDate.now() }
    
    // Проверяем, был ли анализ выполнен сегодня
    val isAnalysisFromToday = analysisDate == today

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3F4F6)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = AnalyticsColors.Secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "AI Рекомендации",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AnalyticsColors.TextSecondary
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                if (aiAnalysis != null) {
                    // Показываем результаты анализа
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            AnalysisSection(
                                "📊 Общий анализ", 
                                aiAnalysis!!.general_analysis
                            )
                            Divider(color = AnalyticsColors.Border)
                            AnalysisSection(
                                "📈 Тренды", 
                                aiAnalysis!!.trends
                            )
                            Divider(color = AnalyticsColors.Border)
                            AnalysisSection(
                                "🎯 Рекомендации на следующую неделю", 
                                aiAnalysis!!.recommendations
                            )
                        }
                    }
                }

                // Показываем кнопку только если анализ не выполнен сегодня
                if (!isAnalysisFromToday) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                performAIAnalysis(viewModel, weekData) { result ->
                                    aiAnalysis = result
                                    analysisDate = today
                                    isLoading = false
                                }
                            }
                            isLoading = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && viewModel.isOnline,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AnalyticsColors.Primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Персональная рекомендация")
                        }
                    }
                } else if (aiAnalysis != null) {
                    // Если анализ уже выполнен сегодня
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AnalyticsColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Анализ выполнен сегодня",
                            fontSize = 14.sp,
                            color = AnalyticsColors.Success
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Нажмите для анализа вашего прогресса за неделю",
                    fontSize = 14.sp,
                    color = AnalyticsColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AnalysisSection(title: String, content: String) {
    Column {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AnalyticsColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            content,
            fontSize = 14.sp,
            color = AnalyticsColors.TextSecondary,
            lineHeight = 20.sp
        )
    }
}

private suspend fun performAIAnalysis(
    viewModel: CalorieTrackerViewModel,
    weekData: List<Pair<LocalDate, DayData?>>,
    onResult: (AIAnalysisResponse?) -> Unit
) {
    try {
        // Подготавливаем данные для анализа
        val weekDataForAnalysis = weekData.map { (date, data) ->
            DayDataForAnalysis(
                date = date.toString(),
                calories = data?.calories?.toInt() ?: 0,
                proteins = data?.proteins ?: 0f,
                fats = data?.fats ?: 0f,
                carbs = data?.carbs ?: 0f,
                mealsCount = data?.mealsCount ?: 0
            )
        }

        val request = WeeklyDataForAnalysis(
            userId = viewModel.userId,
            weekData = weekDataForAnalysis,
            userProfile = viewModel.userProfile.toNetworkProfile(),
            userTargets = TargetNutrients(
                calories = viewModel.userProfile.dailyCalories,
                proteins = viewModel.userProfile.dailyProteins.toFloat(),
                fats = viewModel.userProfile.dailyFats.toFloat(),
                carbs = viewModel.userProfile.dailyCarbs.toFloat()
            )
        )

        // Отправляем запрос на сервер
        val tempRetrofit = Retrofit.Builder()
            .baseUrl(MakeService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val response = safeApiCall {
            tempRetrofit.create(MakeService::class.java).analyzeWeeklyData(
                webhookId = MakeService.WEBHOOK_ID,
                request = request
            )
        }

        val answer = response.getOrNull()?.answer
        if (answer != null) {
            val gson = Gson()
            val analysisResponse = gson.fromJson(answer, AIAnalysisResponse::class.java) as AIAnalysisResponse
            onResult(analysisResponse)
        } else {
            onResult(null)
        }
    } catch (e: Exception) {
        onResult(null)
    }
}

@Composable
private fun CircularProgressIndicator(
    progress: Float,
    color: Color,
    size: Dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val startAngle = -90f
            val sweepAngle = 360f * animatedProgress

            // Фоновый круг
            drawArc(
                color = color.copy(alpha = 0.3f),
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
            "${(progress * 100).toInt()}%",
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun CaloriesChartCard(
    period: AnalyticsPeriod,
    weekData: List<Pair<LocalDate, DayData?>>,
    monthData: List<Pair<LocalDate, DayData?>>,
    yearData: List<Pair<LocalDate, DayData?>>,
    userProfile: com.example.calorietracker.data.UserProfile
) {
    val data = when (period) {
        AnalyticsPeriod.WEEK -> weekData
        AnalyticsPeriod.MONTH -> monthData
        AnalyticsPeriod.YEAR -> yearData
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AnalyticsColors.CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                "График калорий",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AnalyticsColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (period) {
                    AnalyticsPeriod.WEEK -> WeeklyBarChart(data, userProfile)
                    AnalyticsPeriod.MONTH -> MonthlyLineChart(data, userProfile)
                    AnalyticsPeriod.YEAR -> YearlyHeatMap(data, userProfile)
                }
            }
        }
    }
}

@Composable
private fun WeeklyBarChart(
    data: List<Pair<LocalDate, DayData?>>,
    userProfile: com.example.calorietracker.data.UserProfile
) {
    val maxCalories = data.maxOfOrNull { it.second?.calories ?: 0f } ?: 1f
    val targetHeight = userProfile.dailyCalories / maxCalories

    Box(modifier = Modifier.fillMaxSize()) {
        // Подпись для линии нормы
        Text(
            "Норма: ${userProfile.dailyCalories} ккал",
            fontSize = 12.sp,
            color = AnalyticsColors.Success,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = ((1f - targetHeight) * 150).dp - 10.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
        data.forEach { (date, dayData) ->
            val progress = (dayData?.calories ?: 0f) / maxCalories
            val animatedHeight by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(
                    durationMillis = 800,
                    delayMillis = data.indexOf(date to dayData) * 50,
                    easing = FastOutSlowInEasing
                ),
                label = "bar_height"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .fillMaxHeight()
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Целевая линия
                    val targetHeight = userProfile.dailyCalories / maxCalories
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(targetHeight)
                            .drawBehind {
                                drawLine(
                                    color = AnalyticsColors.Success,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 2.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        floatArrayOf(10f, 5f)
                                    )
                                )
                            }
                    )

                    // Столбец
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .fillMaxHeight(animatedHeight)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (date == LocalDate.now()) AnalyticsColors.Primary
                                else AnalyticsColors.Primary.copy(alpha = 0.6f)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    date.format(DateTimeFormatter.ofPattern("E", Locale("ru")))
                        .replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    color = if (date == LocalDate.now())
                        AnalyticsColors.TextPrimary
                    else
                        AnalyticsColors.TextSecondary
                )
            }
        }
        }
    }
}

@Composable
private fun MonthlyLineChart(
    data: List<Pair<LocalDate, DayData?>>,
    userProfile: com.example.calorietracker.data.UserProfile
) {
    // Monthly linear chart implementation placeholder
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "График за месяц",
            color = AnalyticsColors.TextSecondary
        )
    }
}

@Composable
private fun YearlyHeatMap(
    data: List<Pair<LocalDate, DayData?>>,
    userProfile: com.example.calorietracker.data.UserProfile
) {
    // Yearly heatmap implementation placeholder
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Тепловая карта за год",
            color = AnalyticsColors.TextSecondary
        )
    }
}

@Composable
private fun MacroBalanceCard(
    nutritionBalance: NutritionBalance,
    todayData: DayData?,
    userProfile: com.example.calorietracker.data.UserProfile
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AnalyticsColors.CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Баланс макронутриентов",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (nutritionBalance.isBalanced) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AnalyticsColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Сбалансировано",
                            color = AnalyticsColors.Success,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Круговая диаграмма
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                MacroPieChart(
                    proteins = nutritionBalance.proteinsPercent,
                    fats = nutritionBalance.fatsPercent,
                    carbs = nutritionBalance.carbsPercent
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Легенда
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroLegendItem(
                    label = "Белки",
                    value = "${todayData?.proteins?.toInt() ?: 0}г",
                    percentage = nutritionBalance.proteinsPercent,
                    color = MacroColors.Proteins
                )

                MacroLegendItem(
                    label = "Жиры",
                    value = "${todayData?.fats?.toInt() ?: 0}г",
                    percentage = nutritionBalance.fatsPercent,
                    color = MacroColors.Fats
                )

                MacroLegendItem(
                    label = "Углеводы",
                    value = "${todayData?.carbs?.toInt() ?: 0}г",
                    percentage = nutritionBalance.carbsPercent,
                    color = MacroColors.Carbs
                )
            }
        }
    }
}

@Composable
private fun MacroPieChart(
    proteins: Float,
    fats: Float,
    carbs: Float
) {
    val animatedProteins by animateFloatAsState(
        targetValue = proteins,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "proteins"
    )
    val animatedFats by animateFloatAsState(
        targetValue = fats,
        animationSpec = tween(1000, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "fats"
    )
    val animatedCarbs by animateFloatAsState(
        targetValue = carbs,
        animationSpec = tween(1000, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "carbs"
    )

    Canvas(modifier = Modifier.size(180.dp)) {
        val radius = size.minDimension / 2f * 0.8f
        val center = Offset(size.width / 2f, size.height / 2f)

        var startAngle = -90f

        // Белки
        if (animatedProteins > 0) {
            drawArc(
                color = MacroColors.Proteins,
                startAngle = startAngle,
                sweepAngle = animatedProteins * 3.6f,
                useCenter = true,
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
            startAngle += animatedProteins * 3.6f
        }

        // Жиры
        if (animatedFats > 0) {
            drawArc(
                color = MacroColors.Fats,
                startAngle = startAngle,
                sweepAngle = animatedFats * 3.6f,
                useCenter = true,
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
            startAngle += animatedFats * 3.6f
        }

        // Углеводы
        if (animatedCarbs > 0) {
            drawArc(
                color = MacroColors.Carbs,
                startAngle = startAngle,
                sweepAngle = animatedCarbs * 3.6f,
                useCenter = true,
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
        }

        // Центральный круг
        drawCircle(
            color = AnalyticsColors.CardBackground,
            radius = radius * 0.6f,
            center = center
        )
    }
}

@Composable
private fun MacroLegendItem(
    label: String,
    value: String,
    percentage: Float,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                label,
                fontSize = 12.sp,
                color = AnalyticsColors.TextSecondary
            )
        }
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AnalyticsColors.TextPrimary
        )
        Text(
            "${percentage.toInt()}%",
            fontSize = 12.sp,
            color = AnalyticsColors.TextSecondary
        )
    }
}

@Composable
private fun StreaksCard(
    streaks: List<Streak>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AnalyticsColors.CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "Серии",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            streaks.forEach { streak ->
                StreakItem(streak)
                if (streak != streaks.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun StreakItem(streak: Streak) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (streak.isActive) AnalyticsColors.Warning.copy(alpha = 0.2f)
                    else AnalyticsColors.Border
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                streak.type.icon,
                contentDescription = null,
                tint = if (streak.isActive) AnalyticsColors.Warning else AnalyticsColors.TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                streak.type.label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = AnalyticsColors.TextPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "${streak.count} ${getDaysWord(streak.count)}",
                    fontSize = 14.sp,
                    color = if (streak.isActive) AnalyticsColors.TextPrimary else AnalyticsColors.TextSecondary
                )
                if (streak.isActive && streak.count > 0) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = AnalyticsColors.Warning,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsistencyCard(
    weekData: List<Pair<LocalDate, DayData?>>
) {
    val consistency = weekData.count { it.second != null } / 7f * 100

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AnalyticsColors.CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Постоянство",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${consistency.toInt()}%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        consistency >= 80 -> AnalyticsColors.Success
                        consistency >= 60 -> AnalyticsColors.Warning
                        else -> AnalyticsColors.Error
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Визуализация дней недели
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weekData.forEach { (date, data) ->
                    DayIndicator(
                        dayOfWeek = date.dayOfWeek.name.take(2),
                        isLogged = data != null,
                        isToday = date == LocalDate.now()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Записей за последнюю неделю: ${weekData.count { it.second != null }} из 7",
                fontSize = 14.sp,
                color = AnalyticsColors.TextSecondary
            )
        }
    }
}

@Composable
private fun DayIndicator(
    dayOfWeek: String,
    isLogged: Boolean,
    isToday: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isToday && isLogged -> AnalyticsColors.Primary
                        isToday -> AnalyticsColors.Primary.copy(alpha = 0.3f)
                        isLogged -> AnalyticsColors.Success
                        else -> AnalyticsColors.Border
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLogged) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            dayOfWeek,
            fontSize = 12.sp,
            color = if (isToday) AnalyticsColors.Primary else AnalyticsColors.TextSecondary,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun AchievementsCard(
    achievements: List<Achievement>,
    onAchievementClick: (Achievement) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AnalyticsColors.CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Достижения",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "${achievements.count { it.isUnlocked }} из ${achievements.size}",
                    fontSize = 14.sp,
                    color = AnalyticsColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                achievements.chunked(2).forEach { rowAchievements ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowAchievements.forEach { achievement ->
                            AchievementItem(
                                achievement = achievement,
                                onClick = { onAchievementClick(achievement) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Добавляем пустой элемент для выравнивания, если в строке только одно достижение
                        if (rowAchievements.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementItem(
    achievement: Achievement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (achievement.isUnlocked) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (achievement.isUnlocked)
                    achievement.color.copy(alpha = 0.1f)
                else
                    AnalyticsColors.Background
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked)
                            achievement.color.copy(alpha = 0.2f)
                        else
                            AnalyticsColors.Border
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    achievement.icon,
                    contentDescription = null,
                    tint = if (achievement.isUnlocked)
                        achievement.color
                    else
                        AnalyticsColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    achievement.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (achievement.isUnlocked)
                        AnalyticsColors.TextPrimary
                    else
                        AnalyticsColors.TextSecondary
                )

                if (!achievement.isUnlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { achievement.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = achievement.color,
                        trackColor = AnalyticsColors.Border
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementDialog(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = AnalyticsColors.CardBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Иконка достижения
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (achievement.isUnlocked)
                                achievement.color.copy(alpha = 0.2f)
                            else
                                AnalyticsColors.Border
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        achievement.icon,
                        contentDescription = null,
                        tint = if (achievement.isUnlocked)
                            achievement.color
                        else
                            AnalyticsColors.TextSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Название
                Text(
                    achievement.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Описание
                Text(
                    achievement.description,
                    fontSize = 14.sp,
                    color = AnalyticsColors.TextSecondary,
                    textAlign = TextAlign.Center
                )

                if (achievement.isUnlocked && achievement.unlockedDate != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Получено: ${achievement.unlockedDate.format(
                            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
                        )}",
                        fontSize = 12.sp,
                        color = AnalyticsColors.TextSecondary
                    )
                } else if (!achievement.isUnlocked) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Прогресс: ${(achievement.progress * 100).toInt()}%",
                            fontSize = 14.sp,
                            color = AnalyticsColors.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { achievement.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = achievement.color,
                            trackColor = AnalyticsColors.Border
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = achievement.color
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Отлично!")
                }
            }
        }
    }
}

// Вспомогательные функции
private fun calculateWeekData(viewModel: CalorieTrackerViewModel): List<Pair<LocalDate, DayData?>> {
    return (0..6).map { daysAgo ->
        val date = LocalDate.now().minusDays(daysAgo.toLong())
        date to viewModel.getDayData(date)
    }.reversed()
}

private fun calculateMonthData(viewModel: CalorieTrackerViewModel): List<Pair<LocalDate, DayData?>> {
    return (0..29).map { daysAgo ->
        val date = LocalDate.now().minusDays(daysAgo.toLong())
        date to viewModel.getDayData(date)
    }.reversed()
}

private fun calculateYearData(viewModel: CalorieTrackerViewModel): List<Pair<LocalDate, DayData?>> {
    return (0..364).map { daysAgo ->
        val date = LocalDate.now().minusDays(daysAgo.toLong())
        date to viewModel.getDayData(date)
    }.reversed()
}

private fun calculateWeeklyTrend(weekData: List<Pair<LocalDate, DayData?>>): WeeklyTrend {
    val validDays = weekData.filter { it.second != null }
    val averageCalories = if (validDays.isNotEmpty()) {
        validDays.map { it.second!!.calories }.average().toFloat()
    } else 0f

    val previousWeek = weekData.take(3).filter { it.second != null }
    val currentWeek = weekData.takeLast(3).filter { it.second != null }

    val previousAvg = if (previousWeek.isNotEmpty()) {
        previousWeek.map { it.second!!.calories }.average().toFloat()
    } else 0f

    val currentAvg = if (currentWeek.isNotEmpty()) {
        currentWeek.map { it.second!!.calories }.average().toFloat()
    } else 0f

    val changePercent = if (previousAvg > 0) {
        ((currentAvg - previousAvg) / previousAvg * 100)
    } else 0f

    val trend = when {
        changePercent > 5 -> TrendType.UP
        changePercent < -5 -> TrendType.DOWN
        else -> TrendType.STABLE
    }

    return WeeklyTrend(weekData, averageCalories, trend, changePercent)
}

private fun calculateNutritionBalance(
    dayData: DayData?,
    userProfile: com.example.calorietracker.data.UserProfile
): NutritionBalance {
    if (dayData == null) {
        return NutritionBalance(0f, 0f, 0f, false)
    }

    val totalMacros = dayData.proteins + dayData.fats + dayData.carbs
    if (totalMacros == 0f) {
        return NutritionBalance(0f, 0f, 0f, false)
    }

    val proteinsPercent = (dayData.proteins / totalMacros * 100)
    val fatsPercent = (dayData.fats / totalMacros * 100)
    val carbsPercent = (dayData.carbs / totalMacros * 100)

    // Проверка баланса (белки: 20-35%, жиры: 20-35%, углеводы: 40-60%)
    val isBalanced = proteinsPercent in 20f..35f &&
            fatsPercent in 20f..35f &&
            carbsPercent in 40f..60f

    return NutritionBalance(proteinsPercent, fatsPercent, carbsPercent, isBalanced)
}

private fun generateAchievements(viewModel: CalorieTrackerViewModel): List<Achievement> {
    val allDays = viewModel.getAllDaysData()

    return listOf(
        Achievement(
            id = "first_week",
            title = "Первая неделя",
            description = "Ведите дневник питания 7 дней подряд",
            icon = Icons.Default.EmojiEvents,
            progress = minOf(allDays.size / 7f, 1f),
            isUnlocked = allDays.size >= 7,
            unlockedDate = if (allDays.size >= 7) allDays[6].date else null,
            color = AnalyticsColors.Success
        ),
        Achievement(
            id = "perfect_day",
            title = "Идеальный день",
            description = "Достигните целевых показателей по всем макронутриентам за один день",
            icon = Icons.Default.Stars,
            progress = 0.7f,
            isUnlocked = false,
            color = AnalyticsColors.Warning
        ),
        Achievement(
            id = "consistency_master",
            title = "Мастер постоянства",
            description = "Ведите дневник питания 30 дней",
            icon = Icons.Default.WorkspacePremium,
            progress = minOf(allDays.size / 30f, 1f),
            isUnlocked = allDays.size >= 30,
            unlockedDate = if (allDays.size >= 30) allDays[29].date else null,
            color = AnalyticsColors.Primary
        ),
        Achievement(
            id = "balanced_week",
            title = "Сбалансированная неделя",
            description = "Поддерживайте баланс макронутриентов 7 дней подряд",
            icon = Icons.Default.Balance,
            progress = 0.3f,
            isUnlocked = false,
            color = AnalyticsColors.Info
        )
    )
}

private fun calculateStreaks(viewModel: CalorieTrackerViewModel): List<Streak> {
    val allDays = viewModel.getAllDaysData().sortedBy { it.date }

    // Расчет серии ведения дневника
    var currentStreak = 0
    var streakStart = LocalDate.now()

    for (i in allDays.indices.reversed()) {
        if (i == allDays.lastIndex ||
            ChronoUnit.DAYS.between(allDays[i].date, allDays[i + 1].date) == 1L) {
            currentStreak++
            if (i == allDays.indices.first) {
                streakStart = allDays[i].date
            }
        } else {
            break
        }
    }

    return listOf(
        Streak(
            type = StreakType.LOGGING,
            count = currentStreak,
            startDate = streakStart,
            isActive = currentStreak > 0 && allDays.lastOrNull()?.date == LocalDate.now()
        ),
        Streak(
            type = StreakType.DAILY_GOAL,
            count = calculateGoalStreak(viewModel),
            startDate = LocalDate.now().minusDays(calculateGoalStreak(viewModel).toLong()),
            isActive = true
        ),
        Streak(
            type = StreakType.BALANCED_NUTRITION,
            count = 0,
            startDate = LocalDate.now(),
            isActive = false
        )
    )
}

private fun calculateGoalStreak(viewModel: CalorieTrackerViewModel): Int {
    val allDays = viewModel.getAllDaysData().sortedByDescending { it.date }
    var streak = 0

    for (day in allDays) {
        val targetCalories = viewModel.userProfile.dailyCalories
        val difference = abs(day.calories - targetCalories)
        val percentDiff = (difference / targetCalories) * 100

        if (percentDiff <= 10) {
            streak++
        } else {
            break
        }
    }

    return streak
}

private fun getDaysWord(count: Int): String {
    return when {
        count % 10 == 1 && count % 100 != 11 -> "день"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "дня"
        else -> "дней"
    }
}

// Расширение для преобразования UserProfile в UserProfileData
private fun com.example.calorietracker.data.UserProfile.toNetworkProfile(): UserProfileData {
    val age = com.example.calorietracker.utils.calculateAge(birthday)
    return UserProfileData(
        age = age,
        weight = weight,
        height = height,
        gender = gender,
        activityLevel = condition,
        goal = goal
    )
}