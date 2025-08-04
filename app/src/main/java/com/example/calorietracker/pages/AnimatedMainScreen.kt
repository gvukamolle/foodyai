package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.MessageType
import com.example.calorietracker.ui.animations.*
import com.example.calorietracker.utils.DailyResetUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import com.example.calorietracker.pages.subscription.AILimitDialog
import com.example.calorietracker.auth.UserData
import com.example.calorietracker.ui.components.AIUsageToolbarIndicator
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.animation.core.FastOutSlowInEasing
import com.example.calorietracker.ui.animations.AnimatedMessageWithBlur
import com.example.calorietracker.components.chat.FoodConfirmationCard
import com.example.calorietracker.components.chat.AnimatedRetryChip
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.material.icons.filled.Menu
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import com.example.calorietracker.FoodItem
import com.example.calorietracker.managers.AppMode
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedMainScreen(
    viewModel: CalorieTrackerViewModel,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onManualClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onProfileClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val systemUiController = rememberSystemUiController()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appMode by viewModel.appMode.collectAsState()

    LaunchedEffect(Unit) {
        systemUiController.setSystemBarsColor(color = Color.White, darkIcons = true)
    }

    // Периодическая проверка сброса данных
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            val nextResetTime = DailyResetUtils.getNextResetTime()
            val durationToReset = Duration.between(now, nextResetTime).toMillis()
            delay(durationToReset + 1000)
            viewModel.updateDateAndCheckForReset()
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Автопрокрутка к новым сообщениям
    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(viewModel.messages.size - 1)
            }
        }
    }

    val showWelcome =
        viewModel.messages.size == 1 &&
                viewModel.messages.first().isWelcome &&
                !viewModel.isAnalyzing

    // Все диалоги
    AnimatedDialogs(
        viewModel = viewModel,
        onCameraClick = onCameraClick,
        onGalleryClick = onGalleryClick
    )

    var menuExpanded by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) } // Состояние для выдвижного меню
    var showStatisticsCard by remember { mutableStateOf(false) } // Состояние для карточки статистики
    var isRecordMode by remember { mutableStateOf(false) } // Состояние для режима записи
    
    // Состояние для FoodDetailScreen
    var showFoodDetailScreen by remember { mutableStateOf(false) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }

    val isAnalysisMode = viewModel.isDailyAnalysisEnabled

// Проверяем наличие блюд за сегодня
    val hasTodayMeals = viewModel.meals.isNotEmpty()

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            AnimatedBottomBar(
                viewModel = viewModel,
                menuExpanded = menuExpanded,
                onMenuToggle = { menuExpanded = it },
                onCameraClick = onCameraClick,
                onGalleryClick = onGalleryClick,
                onManualClick = onManualClick,
                isRecordMode = isRecordMode,
                onRecordModeToggle = { isRecordMode = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Заголовок с кнопкой статистики
                AnimatedHeader(
                    viewModel = viewModel,
                    appMode = appMode,
                    onMenuClick = { isDrawerOpen = true },
                    onShowStatistics = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showStatisticsCard = true
                    },
                    onNavigateToSubscription = onNavigateToSubscription
                )

                ThinCaloriesBar(
                    current = viewModel.dailyIntake.calories,
                    target = viewModel.userProfile.dailyCalories,
                    color = viewModel.getProgressColor(
                        viewModel.dailyIntake.calories,
                        viewModel.userProfile.dailyCalories
                    ),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Разделитель с анимацией
                AnimatedContentDivider()

                // Отображение подтверждения еды теперь происходит в чате

                // Анимированный чат
                AnimatedChatContent(
                    viewModel = viewModel,
                    listState = listState,
                    onAiOpinionClick = { food ->
                        selectedFood = food
                        showFoodDetailScreen = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }


            AnimatedVisibility(
                visible = showWelcome,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = viewModel.messages.first().content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        textAlign = TextAlign.Center,
                        color = Color.Black,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            val density = LocalDensity.current
            val ime = WindowInsets.ime
            val imeOffset by remember {
                derivedStateOf { ime.getBottom(density) / 4 }
            }
            val animatedImeOffset by animateIntAsState(
                targetValue = imeOffset,
                animationSpec = tween(durationMillis = 250),
                label = "ime_offset"
            )
        }
    }

    // Карточка статистики
    if (showStatisticsCard) {
        EnhancedStatisticsCard(
            viewModel = viewModel,
            onDismiss = {
                showStatisticsCard = false
            }
        )
    }

    // Выдвижное меню поверх контента
    NavigationDrawer(
        isOpen = isDrawerOpen,
        onDismiss = { isDrawerOpen = false },
        userData = viewModel.currentUser,
        onProfileClick = onProfileClick,
        onCalendarClick = onCalendarClick,
        onAnalyticsClick = onAnalyticsClick,
        onSubscriptionClick = onNavigateToSubscription,
        onSettingsClick = onSettingsClick,
        onFeedbackClick = onFeedbackClick
    )
    
    // Диалог с деталями продукта
    if (showFoodDetailScreen && selectedFood != null) {
        FoodDetailScreen(
            food = selectedFood!!,
            onDismiss = {
                showFoodDetailScreen = false
                selectedFood = null
            },
            onEdit = {
                showFoodDetailScreen = false
                viewModel.prefillFood = selectedFood
                viewModel.showManualInputDialog = true
                selectedFood = null
            },
            onDelete = {
                showFoodDetailScreen = false
                // Находим индекс продукта в списке приемов пищи
                val mealIndex = viewModel.meals.indexOfFirst { meal ->
                    meal.foods.contains(selectedFood)
                }
                if (mealIndex != -1) {
                    viewModel.deleteMealFromHistory(
                        DailyResetUtils.getFoodDate(),
                        mealIndex
                    )
                }
                selectedFood = null
            }
        )
    }
}


@Composable
private fun AnimatedHeader(
    viewModel: CalorieTrackerViewModel,
    appMode: AppMode,
    onMenuClick: () -> Unit,
    onShowStatistics: () -> Unit,
    onNavigateToSubscription: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    if (viewModel.showAILimitDialog) {
        AILimitDialog(
            userData = viewModel.currentUser ?: UserData(),
            onDismiss = {
                viewModel.showAILimitDialog = false
                viewModel.pendingAIAction = null
            },
            onUpgrade = {
                viewModel.showAILimitDialog = false
                onNavigateToSubscription()
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Левая часть - Кнопка меню
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onMenuClick()
            },
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Меню",
                tint = Color.Black
            )
        }

        // Центральная часть - Дата со стрелкой и индикатор сети
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Кликабельная область для экрана статистики
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShowStatistics()
                },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Форматируем дату: "3 июля"
                    val currentDate = LocalDate.now()
                    val month = when(currentDate.monthValue) {
                        1 -> "января"
                        2 -> "февраля"
                        3 -> "марта"
                        4 -> "апреля"
                        5 -> "мая"
                        6 -> "июня"
                        7 -> "июля"
                        8 -> "августа"
                        9 -> "сентября"
                        10 -> "октября"
                        11 -> "ноября"
                        12 -> "декабря"
                        else -> ""
                    }
                    val day = currentDate.dayOfMonth

                    Text(
                        text = "$day $month",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.Black
                    )
                }
            }
        }

        // Правая часть - AI индикатор
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            AIUsageToolbarIndicator(
                userData = viewModel.currentUser,
                onClick = onNavigateToSubscription
            )
        }
    }
}

// Кастомный Divider с закругленными краями
@Composable
fun RoundedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFE5E5E5),
    thickness: Dp = 1.dp,
    curveDown: Boolean = true,
    curveHeight: Dp = 10.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(curveHeight)
    ) {
        val path = Path().apply {
            val curveHeightPx = curveHeight.toPx()
            val thicknessPx = thickness.toPx()
            val halfThickness = thicknessPx / 2f

            if (curveDown) {
                moveTo(0f, curveHeightPx)
                quadraticBezierTo(curveHeightPx * 0.7f, halfThickness, curveHeightPx * 1.5f, halfThickness)
                lineTo(size.width - curveHeightPx * 1.5f, halfThickness)
                quadraticBezierTo(size.width - curveHeightPx * 0.7f, halfThickness, size.width, curveHeightPx)
            } else {
                moveTo(0f, halfThickness)
                quadraticBezierTo(curveHeightPx * 0.7f, curveHeightPx, curveHeightPx * 1.5f, curveHeightPx)
                lineTo(size.width - curveHeightPx * 1.5f, curveHeightPx)
                quadraticBezierTo(size.width - curveHeightPx * 0.7f, curveHeightPx, size.width, halfThickness)
            }
        }
        drawPath(path = path, color = color, style = Stroke(width = thickness.toPx()))
    }
}

// Анимированный разделитель контента
@Composable
private fun AnimatedContentDivider() {
    // Плавное появление без ожидания
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandHorizontally()
    ) {
        RoundedDivider(
            color = Color(0xFFE5E5E5),
            thickness = 1.dp,
            curveDown = false,
            curveHeight = 8.dp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimatedChatContent(
    viewModel: CalorieTrackerViewModel,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onAiOpinionClick: (FoodItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            val messagesToDisplay = if (viewModel.messages.firstOrNull()?.isWelcome == true) {
                viewModel.messages.drop(1)
            } else {
                viewModel.messages
            }

            items(
                items = messagesToDisplay,
                key = { msg -> msg.id }
            ) { message ->
                // Используем AnimatedMessageRemoval для плавного исчезновения
                AnimatedMessageWithBlur(
                    id = message.id,
                    isVisible = message.isVisible,
                    playAnimation = message.animate,
                    startDelay = if (message.animate && message.type == MessageType.AI) 750L else 0L,
                    modifier = Modifier.animateItemPlacement(
                        animationSpec = tween(300)
                    ),
                    onAnimationStart = {
                        viewModel.markMessageAnimated(message)
                    }
                ) {
                    AnimatedChatMessageCard(
                        message = message,
                        onAiOpinionClick = onAiOpinionClick,
                        onFoodEdit = { food ->
                            viewModel.prefillFood = food
                            viewModel.showManualInputDialog = true
                        },
                        onFoodConfirm = { food ->
                            viewModel.pendingFood = food
                            viewModel.confirmFood()
                        }
                    )
                }
            }
        }
        
        // Показываем кнопку "Повторить" для сообщений об ошибке
        if (message.isError && message.retryAction != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (message.type == MessageType.USER) {
                    Arrangement.End
                } else {
                    Arrangement.Start
                }
            ) {
                AnimatedRetryChip(
                    onClick = { message.retryAction.invoke() }
                )
            }
        }
    }
}

// В AnimatedMainScreen.kt обновите функцию AnimatedChatMessageCard:

@Composable
private fun AnimatedChatMessageCard(
    message: com.example.calorietracker.ChatMessage,
    onAiOpinionClick: (FoodItem) -> Unit,
    onFoodEdit: (FoodItem) -> Unit = {},
    onFoodConfirm: (FoodItem) -> Unit = {}
) {
    // Если это карточка подтверждения еды - показываем специальный компонент
    if (message.type == MessageType.FOOD_CONFIRMATION && message.foodItem != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            val configuration = LocalConfiguration.current
            val maxMessageWidth = (configuration.screenWidthDp * 2 / 3).dp
            
            FoodConfirmationCard(
                foodItem = message.foodItem,
                onEdit = { onFoodEdit(message.foodItem) },
                onConfirm = { onFoodConfirm(message.foodItem) },
                modifier = Modifier.widthIn(max = maxMessageWidth)
            )
        }
        return
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Основное сообщение
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.type == MessageType.USER) {
                Arrangement.End
            } else {
                Arrangement.Start
            }
        ) {
            val configuration = LocalConfiguration.current
            val maxMessageWidth = (configuration.screenWidthDp * 2 / 3).dp

            val showCard = !(
                    (message.isProcessing && message.inputMethod == null) ||
                            (message.content.isEmpty() && message.isExpandable && message.foodItem != null)
                    )

            if (showCard) {
                Card(
                    modifier = Modifier
                        .widthIn(max = maxMessageWidth)
                        .wrapContentWidth()
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            message.type == MessageType.USER -> Color(0xFFDADADA)
                            else -> Color(0xFFF3F4F6)
                        }
                    ),
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (message.type == MessageType.USER) 20.dp else 6.dp,
                        bottomEnd = if (message.type == MessageType.USER) 6.dp else 20.dp
                    )
                ) {
                    // Проверяем, нужно ли показывать анимированные точки
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        // Проверяем, нужно ли показывать анимированные точки
                        if (message.isProcessing) {
                            Box(
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                )
                            ) {
                                // Определяем метод ввода для AnimatedPhrases
                                val method = when {
                                    message.inputMethod != null -> message.inputMethod
                                    message.content.startsWith("[АНАЛИЗ]") -> "analysis"
                                    message.content.startsWith("[РЕЦЕПТ]") -> "recipe"
                                    else -> "chat"
                                }
                                AnimatedPhrases(inputMethod = method)
                            }
                        } else {
                            if (message.imagePath != null) {
                                AsyncImage(
                                    model = File(message.imagePath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.FillWidth
                                )
                                if (message.content.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                            if (message.content.isNotEmpty()) {
                                MarkdownText(
                                    text = message.content,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            } else {
                // Отображаем индикатор загрузки или кнопку без карточки
                if (message.isProcessing) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        SimpleChatTypingIndicator()
                    }
                } else if (message.content.isEmpty() && message.isExpandable && message.foodItem != null) {
                    if (message.foodItem.aiOpinion != null) {
                        AnimatedAiChip(
                            onClick = { onAiOpinionClick(message.foodItem!!) }
                        )
                    } else {
                        AnimatedMoreChip(
                            onClick = { onAiOpinionClick(message.foodItem!!) }
                        )
                    }
                }
            }
        }
        
        // Показываем кнопку "Повторить" для сообщений об ошибке
        if (message.isError && message.retryAction != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (message.type == MessageType.USER) {
                    Arrangement.End
                } else {
                    Arrangement.Start
                }
            ) {
                AnimatedRetryChip(
                    onClick = { message.retryAction.invoke() }
                )
            }
        }
    }
}

// Анимированная нижняя панель
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedBottomBar(
    viewModel: CalorieTrackerViewModel,
    menuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onManualClick: () -> Unit,
    isRecordMode: Boolean,
    onRecordModeToggle: (Boolean) -> Unit
) {
    val hasTodayMeals = viewModel.meals.isNotEmpty()
    val isAnalysisMode = viewModel.isDailyAnalysisEnabled
    val isRecipeMode = viewModel.isRecipeMode
    val coroutineScope = rememberCoroutineScope()
    val appMode by viewModel.appMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .imePadding()
    ) {
        RoundedDivider(
            color = Color(0xFFE5E5E5),
            thickness = 1.dp,
            curveDown = true,
            curveHeight = 10.dp
        )

        // Прикреплённое фото (если есть)
        AnimatedVisibility(
        visible = viewModel.attachedPhotoPath != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
        ) {
        viewModel.attachedPhotoPath?.let { path ->
        AttachedPhotoPreview(
        photoPath = path,
            onRemove = { viewModel.removeAttachedPhoto() },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        }
        }

        // Поле ввода
        Box(
        modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
        AnimatedInputField(
        value = if (isAnalysisMode) {
        viewModel.inputMessage.removePrefix("[АНАЛИЗ] ")
        } else {
            viewModel.inputMessage
            },
                onValueChange = { newValue ->
                        viewModel.inputMessage = if (isAnalysisMode) {
                            "[АНАЛИЗ] $newValue"
                        } else {
                            newValue
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isOnline = viewModel.isOnline,
                    placeholder = when {
                        viewModel.isRecordMode -> "Опишите ваш прием пищи..."
                        isRecipeMode -> "Опишите блюдо..."
                        isAnalysisMode -> "Задайте вопрос о питании..."
                        else -> "Сообщение..."
                    },
                    hasAttachment = viewModel.attachedPhoto != null
                )
            }

        // Кнопки под полем ввода
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопки слева
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка анализа
                AnimatedAnalysisToggle(
                    isEnabled = isAnalysisMode,
                    onClick = {
                        if (hasTodayMeals) {
                            val currentText = if (isAnalysisMode) {
                                viewModel.inputMessage.removePrefix("[АНАЛИЗ] ")
                            } else {
                                viewModel.inputMessage
                            }

                            // Если включаем режим анализа, отключаем режим записи
                            if (!isAnalysisMode && isRecordMode) {
                                onRecordModeToggle(false)
                                // Небольшая задержка для плавного перехода
                                coroutineScope.launch {
                                    delay(100)
                                    viewModel.toggleDailyAnalysis()
                                    viewModel.inputMessage = "[АНАЛИЗ] $currentText"
                                }
                            } else {
                                viewModel.toggleDailyAnalysis()
                                viewModel.inputMessage = if (isAnalysisMode) {
                                    currentText
                                } else {
                                    "[АНАЛИЗ] $currentText"
                                }
                            }
                        }
                    }
                )

                // Кнопка записи
                AnimatedRecordToggle(
                    isEnabled = viewModel.isRecordMode,
                    onClick = {
                        // Если включаем режим записи, отключаем режим анализа
                        if (!viewModel.isRecordMode && isAnalysisMode) {
                            // Сначала убираем префикс анализа из сообщения
                            val currentText = viewModel.inputMessage.removePrefix("[АНАЛИЗ] ")
                            viewModel.inputMessage = currentText
                            viewModel.toggleDailyAnalysis()
                            // Включаем режим записи сразу, чтобы placeholder сменился без задержки
                            viewModel.toggleRecordMode()
                        } else {
                            viewModel.toggleRecordMode()
                        }
                    }
                )

                // Кнопка рецептов
                AnimatedRecipeToggle(
                    isEnabled = isRecipeMode,
                    onClick = {
                        if (!isRecipeMode && isAnalysisMode) {
                            val currentText = viewModel.inputMessage.removePrefix("[АНАЛИЗ] ")
                            viewModel.inputMessage = currentText
                            viewModel.toggleDailyAnalysis()
                        }
                        viewModel.toggleRecipeMode()
                    }
                )
            }

            // Кнопка отправки / меню СПРАВА
            Box {
                AnimatedContent(
                    targetState = if (isAnalysisMode) {
                        viewModel.inputMessage.removePrefix("[АНАЛИЗ] ").isNotBlank()
                    } else {
                        viewModel.inputMessage.isNotBlank() || viewModel.attachedPhoto != null
                    },
                    transitionSpec = {
                        fadeIn() + scaleIn() with fadeOut() + scaleOut()
                    },
                    label = "send_button"
                ) { hasContent ->
                    if (hasContent) {
                        AnimatedSendButton(
                            onClick = {
                                viewModel.sendMessage()
                            }
                        )
                    } else {
                        AnimatedPlusButton(
                            expanded = menuExpanded,
                            onClick = { if (!isAnalysisMode) onMenuToggle(true) },
                            enabled = !isAnalysisMode
                        )
                    }
                }

                EnhancedPlusDropdownMenu(
                    expanded = menuExpanded && !isAnalysisMode,
                    onDismissRequest = { onMenuToggle(false) },
                    onCameraClick = onCameraClick,
                    onGalleryClick = onGalleryClick,
                    onManualClick = onManualClick
                )
            }
        }
    }
}

@Composable
private fun AnimatedInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isOnline: Boolean,
    placeholder: String = "Сообщение...",
    hasAttachment: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused },
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = Color.Black
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }

                if (!isOnline) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.WifiOff,
                        contentDescription = "Offline",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                }
            }
        }
    )
}

// Компонент для превью прикреплённого фото
@Composable
private fun AttachedPhotoPreview(
    photoPath: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Фото
            AsyncImage(
                model = File(photoPath),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            
            // Кнопка удаления
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRemove()
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Удалить фото",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Добавьте эту функцию для парсинга markdown-подобного текста
@Composable
fun MarkdownText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        val boldPattern = "\\*\\*(.*?)\\*\\*".toRegex()

        boldPattern.findAll(text).forEach { matchResult ->
            val startIndex = matchResult.range.first
            val endIndex = matchResult.range.last + 1
            val boldText = matchResult.groupValues[1]

            // Добавляем обычный текст до жирного
            if (startIndex > currentIndex) {
                append(text.substring(currentIndex, startIndex))
            }

            // Добавляем жирный текст
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(boldText)
            }

            currentIndex = endIndex
        }

        // Добавляем оставшийся текст
        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }

    Text(
        text = annotatedString,
        style = style.copy(
            fontSize = style.fontSize * 1.05f,
            lineHeight = style.lineHeight * 1.1f,
            fontWeight = FontWeight.Normal
        ),
        color = color,
        modifier = modifier
    )
}

// Анимированная кнопка отправки
@Composable
private fun AnimatedSendButton(
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { isPressed = false },
        label = "send_scale"
    )

    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isPressed = true
            onClick()
        },
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Icon(
            Icons.Default.Send,
            contentDescription = "Отправить",
            tint = Color.White,
            modifier = Modifier
                .background(Color.Black, CircleShape)
                .padding(8.dp)
        )
    }
}

// Добавить этот компонент в файл AnimatedMainScreen.kt

@Composable
private fun AnimatedAnalysisToggle(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    // Анимации
    val animatedWidth by animateDpAsState(
        targetValue = if (isEnabled) 115.dp else 40.dp,
        animationSpec = spring(
            dampingRatio = 0.60f, // Между LowBouncy (0.75) и NoBouncy (1.0)
            stiffness = 600f // Между MediumLow (200) и Medium (400)
        ),
        label = "toggle_width"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isEnabled) Color(0xFF2196F3) else Color.White,
        animationSpec = tween(300),
        label = "toggle_bg_color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isEnabled) Color(0xFF1976D2) else Color(0xFFE0E0E0),
        animationSpec = tween(300),
        label = "toggle_border_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isEnabled) Color.White else Color(0xFF757575),
        animationSpec = tween(300),
        label = "toggle_content_color"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isEnabled) 300 else 150,
            delayMillis = if (isEnabled) 100 else 0
        ),
        label = "toggle_text_alpha"
    )

    Box(
        modifier = modifier
            .height(40.dp) // Уменьшили высоту с 48 до 40
            .width(animatedWidth)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null // Убираем ripple эффект
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp), // Уменьшили padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = if (isEnabled) "Отключить режим анализа" else "Включить режим анализа",
                tint = contentColor,
                modifier = Modifier.size(20.dp) // Уменьшили иконку
            )

            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn(animationSpec = tween(200, delayMillis = 100)) +
                        expandHorizontally(animationSpec = tween(200)),
                exit = shrinkHorizontally(animationSpec = tween(150)) +
                        fadeOut(animationSpec = tween(150))
            ) {
                Row {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Спросить",
                        color = contentColor.copy(alpha = textAlpha),
                        fontSize = 14.sp, // Уменьшили шрифт
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Анимированная кнопка записи
@Composable
private fun AnimatedRecordToggle(
    isEnabled: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    // Анимации
    val animatedWidth by animateDpAsState(
        targetValue = if (isEnabled) 110.dp else 40.dp,
        animationSpec = spring(
            dampingRatio = 0.60f, // Между LowBouncy (0.75) и NoBouncy (1.0)
            stiffness = 600f // Между MediumLow (200) и Medium (400)
        ),
        label = "record_toggle_width"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isEnabled) Color(0xFF2196F3) else Color.White,
        animationSpec = tween(300),
        label = "record_toggle_bg_color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isEnabled) Color(0xFF1976D2) else Color(0xFFE0E0E0),
        animationSpec = tween(300),
        label = "record_toggle_border_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isEnabled) Color.White else Color(0xFF757575),
        animationSpec = tween(300),
        label = "record_toggle_content_color"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isEnabled) 300 else 150,
            delayMillis = if (isEnabled) 100 else 0
        ),
        label = "record_toggle_text_alpha"
    )

    Box(
        modifier = modifier
            .height(40.dp)
            .width(animatedWidth)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) {
                if (enabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = if (isEnabled) "Отключить режим записи" else "Включить режим записи",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn(animationSpec = tween(200, delayMillis = 100)) +
                        expandHorizontally(animationSpec = tween(200)),
                exit = shrinkHorizontally(animationSpec = tween(150)) +
                        fadeOut(animationSpec = tween(150))
            ) {
                Row {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Записать",
                        color = contentColor.copy(alpha = textAlpha),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedRecipeToggle(
    isEnabled: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    val animatedWidth by animateDpAsState(
        targetValue = if (isEnabled) 135.dp else 40.dp,
        animationSpec = spring(
            dampingRatio = 0.60f, // Между LowBouncy (0.75) и NoBouncy (1.0)
            stiffness = 600f // Между MediumLow (200) и Medium (400)
        ),
        label = "recipe_toggle_width"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isEnabled) Color(0xFF2196F3) else Color.White,
        animationSpec = tween(300),
        label = "recipe_toggle_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isEnabled) Color(0xFF1976D2) else Color(0xFFE0E0E0),
        animationSpec = tween(300),
        label = "recipe_toggle_border"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isEnabled) Color.White else Color(0xFF757575),
        animationSpec = tween(300),
        label = "recipe_toggle_content"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isEnabled) 300 else 150,
            delayMillis = if (isEnabled) 100 else 0
        ),
        label = "recipe_toggle_text_alpha"
    )

    Box(
        modifier = modifier
            .height(40.dp)
            .width(animatedWidth)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) {
                if (enabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = if (isEnabled) "Отключить рецепты" else "Включить рецепты",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            AnimatedVisibility(
                visible = isEnabled,
                enter = fadeIn(animationSpec = tween(200, delayMillis = 100)) +
                        expandHorizontally(animationSpec = tween(200)),
                exit = shrinkHorizontally(animationSpec = tween(150)) +
                        fadeOut(animationSpec = tween(150))
            ) {
                Row {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Приготовить",
                        color = contentColor.copy(alpha = textAlpha),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


// Тонкая полоска прогресса калорий
@Composable
private fun ThinCaloriesBar(
    current: Int,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) current.toFloat() / target else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "thin_calories_bar"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFFE5E7EB).copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
    }
}


@Composable
fun AnimatedAiChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFDFEBF4),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Что думает Foody?",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
        }
    }
}

@Composable
fun AnimatedMoreChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFE0E0E0)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Больше",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
    }
}
