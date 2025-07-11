package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.calorietracker.ChatMessage
import com.example.calorietracker.MessageType
import com.example.calorietracker.ui.animations.*
import com.example.calorietracker.utils.DailyResetUtils
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import java.time.format.TextStyle as DateTextStyle
import com.example.calorietracker.pages.subscription.AILimitDialog
import com.example.calorietracker.auth.UserData
import com.example.calorietracker.ui.components.AIUsageToolbarIndicator
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import java.time.format.DateTimeFormatter
import com.example.calorietracker.ui.animations.AIProcessingMessage
import com.example.calorietracker.ui.animations.SimpleChatTypingIndicator
import com.example.calorietracker.ui.animations.AnimatedMessageRemoval
import com.example.calorietracker.ui.animations.AnimatedMessageWithBlur
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedMainScreen(
    viewModel: CalorieTrackerViewModel,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onManualClick: () -> Unit,
    onDescribeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onNavigateToSubscription: () -> Unit, // НОВОЕ
    modifier: Modifier = Modifier
) {
    val systemUiController = rememberSystemUiController()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

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
    var isStatusBarVisible by remember { mutableStateOf(false) } // Новое состояние

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets.systemBars, // <-- Добавьте это
        bottomBar = {
            AnimatedBottomBar(
                viewModel = viewModel,
                menuExpanded = menuExpanded,
                onMenuToggle = { menuExpanded = it },
                onCameraClick = onCameraClick,
                onGalleryClick = onGalleryClick,
                onDescribeClick = onDescribeClick,
                onManualClick = onManualClick
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
            // Обновленный заголовок с кликом на календарь
            AnimatedHeader(
                viewModel = viewModel,
                onSettingsClick = onSettingsClick,
                onDateClick = onCalendarClick,
                onNavigateToSubscription = onNavigateToSubscription,
                isStatusBarVisible = isStatusBarVisible,
                onToggleStatusBar = { isStatusBarVisible = !isStatusBarVisible }
                )

            // Прогресс-бары
            AnimatedProgressBars(
                viewModel = viewModel,
                isVisible = isStatusBarVisible
            )

            // Разделитель с анимацией
            AnimatedContentDivider()

            // Карточка подтверждения еды
            viewModel.pendingFood?.let { food ->
                AnimatedPendingFoodCard(
                    food = food,
                    selectedMeal = viewModel.selectedMeal,
                    onConfirm = { viewModel.confirmFood() },
                    onCancel = { viewModel.pendingFood = null }
                )
            }

            // Анимированный чат
            AnimatedChatContent(
                viewModel = viewModel,
                listState = listState,
                modifier = Modifier.weight(1f)
            )
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

            // Новый экран загрузки AI поверх всего остального
            AnimatedVisibility(
                visible = viewModel.showAILoadingScreen,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                AIAnalysisLoadingScreen(
                    onDismiss = {
                        viewModel.cancelAIAnalysis()
                    },
                    showDismissButton = true,
                    inputMethod = viewModel.inputMethod // Передаем метод ввода
                )
            }
        }
    }
    }


@Composable
private fun AnimatedHeader(
    viewModel: CalorieTrackerViewModel,
    onSettingsClick: () -> Unit,
    onDateClick: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    isStatusBarVisible: Boolean,
    onToggleStatusBar: () -> Unit
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Левая часть - Кликабельная область с датой
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onDateClick() }
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Форматируем дату: "3 июля, четверг"
                    val currentDate = LocalDate.now()
                    val dayOfWeek = currentDate.dayOfWeek.getDisplayName(DateTextStyle.FULL, Locale("ru"))
                    val month = currentDate.month.getDisplayName(java.time.format.TextStyle.FULL, Locale("ru"))
                    val day = currentDate.dayOfMonth

                    Text(
                        text = "$day $month",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = dayOfWeek.replaceFirstChar { it.uppercase() },
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Стрелка
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Открыть календарь",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Центральная часть - Кнопка Foody Stat
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleStatusBar()
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Foody",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            if (isStatusBarVisible) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Black
                        )
                    }
                }
            }

            // Правая часть - AI индикатор и настройки
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AIUsageToolbarIndicator(
                    userData = viewModel.currentUser,
                    onClick = onNavigateToSubscription
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = Color.Gray
                    )
                }
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
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
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

// В AnimatedMainScreen.kt обновите AnimatedChatContent:

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimatedChatContent(
    viewModel: CalorieTrackerViewModel,
    listState: androidx.compose.foundation.lazy.LazyListState,
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
                        onAiOpinionClick = { text ->
                            viewModel.aiOpinionText = text
                            viewModel.showAiOpinionDialog = true
                        }
                    )
                }
            }
        }
    }
}

// В AnimatedMainScreen.kt, обновите AnimatedChatMessageCard:

@Composable
private fun AnimatedChatMessageCard(
    message: com.example.calorietracker.ChatMessage,
    onAiOpinionClick: (String) -> Unit
) {
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
            Card(
                modifier = Modifier
                    .widthIn(max = maxMessageWidth)
                    .wrapContentWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.type == MessageType.USER) {
                        Color(0xFFDADADA)
                    } else {
                        Color(0xFFF3F4F6)
                    }
                ),
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (message.type == MessageType.USER) 12.dp else 4.dp,
                    bottomEnd = if (message.type == MessageType.USER) 4.dp else 12.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Проверяем, нужно ли показывать анимированные точки
                    when {
                        message.isProcessing -> {
                            // Показываем только точки без текста
                            Box(
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                )
                            ) {
                                SimpleChatTypingIndicator()
                            }
                        }
                        else -> {
                            // Обычный текст
                            val textStyle = MaterialTheme.typography.bodyMedium
                            Text(
                                text = message.content,
                                style = textStyle.copy(
                                    lineHeight = textStyle.lineHeight * 1.2f
                                ),
                                color = Color.Black
                            )

                            if (message.isExpandable && message.foodItem?.aiOpinion != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    AnimatedAiChip(
                                        onClick = { onAiOpinionClick(message.foodItem.aiOpinion!!) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Информация о продукте (если есть)
        message.foodItem?.let { food ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = food.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Калории: ${food.calories}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Вес: ${food.weight}",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Б: ${food.protein}г",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Ж: ${food.fat}г",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "У: ${food.carbs}г",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        // Время сообщения - только если не processing
        if (!message.isProcessing) {
            Text(
                text = message.timestamp.format(
                    DateTimeFormatter.ofPattern("HH:mm")
                ),
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(
                        if (message.type == MessageType.USER) {
                            Alignment.End
                        } else {
                            Alignment.Start
                        }
                    )
            )
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
    onDescribeClick: () -> Unit,
    onManualClick: () -> Unit
) {

    val density = LocalDensity.current
    val imeHeight = WindowInsets.ime.getBottom(density)
    val animatedBottomPadding by animateDpAsState(
        targetValue = with(density) { imeHeight.toDp() },
        animationSpec = tween(
            durationMillis = 1,
            easing = FastOutSlowInEasing
        ),
        label = "keyboard_animation"
    )


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding() // Для навбара
            .imePadding() // Для клавиатуры (системная анимация)
    ) {
        RoundedDivider(
            color = Color(0xFFE5E5E5),
            thickness = 1.dp,
            curveDown = true,
            curveHeight = 10.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Анимированное поле ввода
            AnimatedInputField(
                value = viewModel.inputMessage,
                onValueChange = { viewModel.inputMessage = it },
                modifier = Modifier.weight(1f),
                isOnline = viewModel.isOnline
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box {
                AnimatedContent(
                    targetState = viewModel.inputMessage.isNotBlank(),
                    transitionSpec = {
                        fadeIn() + scaleIn() with fadeOut() + scaleOut()
                    },
                    label = "send_button"
                ) { hasText ->
                    if (hasText) {
                        AnimatedSendButton(
                            onClick = { viewModel.sendMessage() }
                        )
                    } else {
                        AnimatedPlusButton(
                            expanded = menuExpanded,
                            onClick = { onMenuToggle(true) }
                        )
                    }
                }

                EnhancedPlusDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { onMenuToggle(false) },
                    onCameraClick = onCameraClick,
                    onGalleryClick = onGalleryClick,
                    onDescribeClick = onDescribeClick,
                    onManualClick = onManualClick
                )
            }
        }
    }
}

// Анимированное поле ввода
@Composable
private fun AnimatedInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isOnline: Boolean
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = false,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text
        ),
        textStyle = TextStyle(
            fontSize = 18.sp,
            color = Color.Black
        ),
        cursorBrush = SolidColor(Color.Black),
        modifier = modifier,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = if (isOnline) {
                            "Спросите у AI-диетолога..."
                        } else {
                            "Задайте вопрос..."
                        },
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 18.sp
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}

// Анимированная кнопка отправки
@Composable
private fun AnimatedSendButton(onClick: () -> Unit) {
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
@Composable
fun AnimatedAiChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFDBF0E4).copy(alpha = 1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Что думает Foody?",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF00BA65)
            )
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF00BA65),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
