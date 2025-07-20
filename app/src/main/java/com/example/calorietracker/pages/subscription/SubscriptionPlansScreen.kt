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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.auth.SubscriptionPlan
import com.example.calorietracker.extensions.fancyShadow

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
                    color = Color(0xFFFFFFFF)
                )
        )
        {
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
                        onSelect = {
                            selectedPlan = details.plan
                            showConfirmDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Дополнительная информация
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fancyShadow(
                            borderRadius = 16.dp,
                            shadowRadius = 8.dp,
                            alpha = 0.25f,
                            color = Color(0xFF2196F3)
                        ),
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
                        Spacer(modifier = Modifier.width(8.dp))
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
        SubscriptionConfirmationDialog(
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
    onSelect: () -> Unit
) {
    val isPro = details.plan == SubscriptionPlan.PRO

    // Цвета для разных планов
    val cardBackgroundColor = if (isPro) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2196F3).copy(alpha = 0.05f),
                Color(0xFFFFFFFF)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF5F5F5),
                Color(0xFFF5F5F5)
            )
        )
    }

    val shadowColor = if (isPro) Color(0xFF2196F3) else Color(0xFF9E9E9E)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fancyShadow(
                borderRadius = 20.dp,
                shadowRadius = if (isPro) 10.dp else 6.dp,
                alpha = if (isPro) 0.3f else 0.2f,
                color = shadowColor
            )
            .clickable { onSelect() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(0.dp) // Убираем стандартную тень
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = cardBackgroundColor)
        ) {
            // Декоративный элемент для PRO плана
            if (isPro) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 20.dp, y = (-10).dp)
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF2196F3).copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(50)
                        )
                )
            }

            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Заголовок плана
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = details.plan.displayName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPro) Color(0xFF1976D2) else Color(0xFF424242)
                        )

                        if (isCurrentPlan) {
                            Surface(
                                color = if (isPro) Color(0xFF2196F3) else Color(0xFF757575),
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
                                color = Color(0xFF1976D2)
                            )
                            Text(
                                text = details.period,
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        } else {
                            Text(
                                text = "Бесплатно",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Фичи
                details.features.forEach { feature ->
                    FeatureRow(
                        feature = feature,
                        isPro = isPro
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Кнопка выбора
                if (!isCurrentPlan) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onSelect,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPro) {
                                Color(0xFF2196F3)
                            } else {
                                Color(0xFF424242)
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
}

@Composable
fun FeatureRow(
    feature: PlanFeature,
    isPro: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = feature.icon,
            contentDescription = null,
            tint = if (isPro) {
                if (feature.isHighlighted) Color(0xFFFF9800) else Color(0xFF2196F3)
            } else {
                Color(0xFF9E9E9E)
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = feature.title,
                fontSize = 16.sp,
                fontWeight = if (feature.isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (isPro) Color(0xFF212121) else Color(0xFF616161)
            )
            Text(
                text = feature.description,
                fontSize = 13.sp,
                color = if (isPro) Color(0xFF666666) else Color(0xFF9E9E9E),
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
            isPopular = false,
            gradientColors = listOf(Color(0xFF2196F3), Color(0xFF03A9F4))
        )
    )
}