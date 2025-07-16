package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: CalorieTrackerViewModel,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    
    // Получаем данные за последние 7 дней
    val todayData = viewModel.getTodayData()
    val weekData = remember {
        (0..6).map { daysAgo ->
            val date = LocalDate.now().minusDays(daysAgo.toLong())
            val data = viewModel.getDayData(date)
            date to data
        }.reversed()
    }
    
    // Расчет средних значений за неделю
    val weeklyAverages = remember(weekData) {
        val validDays = weekData.filter { it.second != null }
        if (validDays.isEmpty()) {
            WeeklyAverages(0f, 0f, 0f, 0f)
        } else {
            WeeklyAverages(
                calories = validDays.map { it.second!!.calories }.average().toFloat(),
                proteins = validDays.map { it.second!!.proteins }.average().toFloat(),
                fats = validDays.map { it.second!!.fats }.average().toFloat(),
                carbs = validDays.map { it.second!!.carbs }.average().toFloat()
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Аналитика", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Сегодняшний прогресс
            TodayProgressCard(
                todayData = todayData,
                userProfile = viewModel.userProfile
            )
            
            // Недельная статистика
            WeeklyStatsCard(weeklyAverages = weeklyAverages)
            
            // График за неделю
            WeeklyChartCard(weekData = weekData)
            
            // Достижения
            AchievementsCard(viewModel = viewModel)
        }
    }
}

@Composable
private fun TodayProgressCard(
    todayData: com.example.calorietracker.data.DayData?,
    userProfile: com.example.calorietracker.data.UserProfile
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    "Сегодня",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                val percentage = if (userProfile.dailyCalories > 0) {
                    ((todayData?.calories ?: 0f) / userProfile.dailyCalories * 100).toInt()
                } else 0
                
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                percentage < 50 -> Color(0xFFE3F2FD)
                                percentage < 90 -> Color(0xFFE8F5E9)
                                percentage <= 110 -> Color(0xFFF3E5F5)
                                else -> Color(0xFFFFEBEE)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$percentage%",
                        fontWeight = FontWeight.Bold,
                        color = when {
                            percentage < 50 -> Color(0xFF1976D2)
                            percentage < 90 -> Color(0xFF388E3C)
                            percentage <= 110 -> Color(0xFF7B1FA2)
                            else -> Color(0xFFD32F2F)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Прогресс по калориям
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${todayData?.calories?.toInt() ?: 0} ккал",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "из ${userProfile.dailyCalories} ккал",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
            
            LinearProgressIndicator(
                progress = {
                    ((todayData?.calories ?: 0f) / userProfile.dailyCalories)
                        .coerceIn(0f, 1.5f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
private fun WeeklyStatsCard(weeklyAverages: WeeklyAverages) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "Средние показатели за неделю",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = weeklyAverages.calories.toInt().toString(),
                    label = "Калории",
                    color = Color(0xFFFF6B6B)
                )
                StatItem(
                    value = "${weeklyAverages.proteins.toInt()}г",
                    label = "Белки",
                    color = Color(0xFF4ECDC4)
                )
                StatItem(
                    value = "${weeklyAverages.fats.toInt()}г",
                    label = "Жиры",
                    color = Color(0xFFFFD93D)
                )
                StatItem(
                    value = "${weeklyAverages.carbs.toInt()}г",
                    label = "Углеводы",
                    color = Color(0xFF6BCF7F)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun WeeklyChartCard(
    weekData: List<Pair<LocalDate, com.example.calorietracker.data.DayData?>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "Последние 7 дней",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val maxCalories = weekData.maxOfOrNull { it.second?.calories ?: 0f } ?: 1f
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weekData.forEach { (date, data) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .fillMaxHeight()
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val height = if (data != null && maxCalories > 0) {
                                (data.calories / maxCalories)
                            } else 0f
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(height)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (date == LocalDate.now()) Color(0xFF4CAF50)
                                        else Color(0xFFE0E0E0)
                                    )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            date.format(DateTimeFormatter.ofPattern("E", Locale("ru")))
                                .replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            color = if (date == LocalDate.now()) Color.Black else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementsCard(viewModel: CalorieTrackerViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "Достижения",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Пример достижений - можно расширить
            val totalDays = viewModel.getAllDaysData().size
            val daysWithinTarget = viewModel.getAllDaysData().count { dayData ->
                val diff = (dayData.calories - viewModel.userProfile.dailyCalories).absoluteValue
                diff <= viewModel.userProfile.dailyCalories * 0.1f // В пределах 10%
            }
            
            AchievementItem(
                icon = Icons.Default.CalendarMonth,
                title = "Дней записей",
                value = totalDays.toString(),
                color = Color(0xFF2196F3)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AchievementItem(
                icon = Icons.Default.Star,
                title = "Дней в целевой зоне",
                value = daysWithinTarget.toString(),
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
private fun AchievementItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class WeeklyAverages(
    val calories: Float,
    val proteins: Float,
    val fats: Float,
    val carbs: Float
)