package com.example.calorietracker.pages.subscription

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.calorietracker.auth.SubscriptionPlan

// Расширенная модель с фичами
data class PlanFeature(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val isHighlighted: Boolean = false
)

data class PlanDetails(
    val plan: SubscriptionPlan,
    val price: String,
    val period: String,
    val features: List<PlanFeature>,
    val isPopular: Boolean = false,
    val savings: String? = null,
    val gradientColors: List<Color>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlansScreen(
    currentPlan: SubscriptionPlan,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    onBack: () -> Unit
) {

    BackHandler {
        onBack()
    }

    val planDetails = remember { getPlanDetails() }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var selectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Планы подписки",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заголовок
                Text(
                    text = "Раскройте полный потенциал",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "AI-помощника по питанию",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Карточки планов
                planDetails.forEach { details ->
                    PlanCard(
                        details = details,
                        isCurrentPlan = currentPlan == details.plan,
                        isSelected = selectedPlan == details.plan,
                        onSelect = {
                            selectedPlan = details.plan
                            showConfirmDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Дополнительная информация
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF0F8FF)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Все планы включают базовые функции отслеживания калорий",
                            fontSize = 14.sp,
                            color = Color(0xFF424242)
                        )
                    }
                }
            }
        }
    }
    if (showConfirmDialog && selectedPlan != null) {
        SubscriptionConfirmationDialog(  // новый красивый диалог
            plan = selectedPlan!!,
            onConfirm = {
                onSelectPlan(selectedPlan!!)
                showConfirmDialog = false
            },
            onDismiss = {
                showConfirmDialog = false
                selectedPlan = null
            }
        )
    }
}

@Composable
fun PlanCard(
    details: PlanDetails,
    isCurrentPlan: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(20.dp), // Увеличенное закругление как в EnhancedDialogs
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        border = if (isSelected) {
            BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4CAF50),
                        Color(0xFF45A049)
                    )
                )
            )
        } else {
            BorderStroke(1.dp, Color(0xFFE5E5E5))
        }
    ) {
         Column(
             modifier = Modifier.padding(20.dp) // Увеличенный паддинг
            ) {
                // Заголовок плана
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = details.plan.displayName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (isCurrentPlan) {
                            Surface(
                                color = Color(0xFF4CAF50),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Текущий план",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (details.plan != SubscriptionPlan.FREE) {
                            Text(
                                text = details.price,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (details.isPopular) details.gradientColors[0] else Color.Black
                            )
                            Text(
                                text = details.period,
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                            details.savings?.let {
                                Text(
                                    text = it,
                                    fontSize = 12.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                text = "Бесплатно",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Фичи
                details.features.forEach { feature ->
                    FeatureRow(feature = feature)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Кнопка выбора
                if (!isCurrentPlan) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onSelect,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (details.isPopular) {
                                details.gradientColors[0]
                            } else {
                                Color.Black
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (details.plan == SubscriptionPlan.FREE) {
                                "Понизить до бесплатного"
                            } else {
                                "Выбрать план"
                            },
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

@Composable
fun FeatureRow(feature: PlanFeature) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = feature.icon,
            contentDescription = null,
            tint = if (feature.isHighlighted) Color(0xFFFF9800) else Color(0xFF4CAF50),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = feature.title,
                fontSize = 16.sp,
                fontWeight = if (feature.isHighlighted) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = feature.description,
                fontSize = 13.sp,
                color = Color(0xFF666666),
                lineHeight = 18.sp
            )
        }
    }
}

// Функция для получения деталей планов
private fun getPlanDetails(): List<PlanDetails> {
    return listOf(
        PlanDetails(
            plan = SubscriptionPlan.FREE,
            price = "0₽",
            period = "навсегда",
            features = listOf(
                PlanFeature(
                    Icons.Default.Edit,
                    "Ручной ввод данных",
                    "Добавляйте продукты и блюда вручную"
                ),
                PlanFeature(
                    Icons.Default.CalendarToday,
                    "Календарь питания",
                    "Просматривайте историю за любой день"
                ),
                PlanFeature(
                    Icons.Default.TableChart,
                    "Экспорт данных",
                    "Выгружайте статистику в таблицы"
                )
            ),
            gradientColors = listOf(Color(0xFF757575), Color(0xFF9E9E9E))
        ),
        PlanDetails(
            plan = SubscriptionPlan.PRO,
            price = "399₽",
            period = "в месяц",
            features = listOf(
                PlanFeature(
                    Icons.Default.Star,
                    "Всё из бесплатного плана",
                    "Все предыдущие функции включены"
                ),
                PlanFeature(
                    Icons.Default.AllInclusive,
                    "Безлимитный AI",
                    "Неограниченный анализ фото и описаний",
                    isHighlighted = true
                ),
                PlanFeature(
                    Icons.Default.Analytics,
                    "Дневные сводки",
                    "AI анализ вашего рациона каждый день"
                ),
                PlanFeature(
                    Icons.Default.FitnessCenter,
                    "Персональные рекомендации",
                    "Советы по достижению ваших целей"
                ),
                PlanFeature(
                    Icons.Default.Restaurant,
                    "Планы питания",
                    "Генерация меню на неделю с рецептами"
                ),
                PlanFeature(
                    Icons.Default.TrendingUp,
                    "Продвинутая аналитика",
                    "Детальные графики и прогнозы"
                )
            ),
            // Этот план не помечаем как популярный
            isPopular = false,
            gradientColors = listOf(Color(0xFF2196F3), Color(0xFF03A9F4))
        )
    )
}
