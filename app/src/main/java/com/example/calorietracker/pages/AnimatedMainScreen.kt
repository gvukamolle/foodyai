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
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandHorizontally
import com.example.calorietracker.ui.animations.SimpleChatTypingIndicator
import com.example.calorietracker.ui.animations.AnimatedMessageWithBlur
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.material.icons.filled.Menu

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
    onNavigateToSubscription: () -> Unit,
    onProfileClick: () -> Unit = {}, // Новое
    onAnalyticsClick: () -> Unit = {}, // Новое
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
    var isStatusBarVisible by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) } // Состояние для выдвижного меню

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
                    onMenuClick = { isDrawerOpen = true },
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

// Выдвижное меню поверх контента
NavigationDrawer(
isOpen = isDrawerOpen,
onDismiss = { isDrawerOpen = false },
userData = viewModel.currentUser,
onProfileClick = onProfileClick,
onCalendarClick = onCalendarClick,
onAnalyticsClick = onAnalyticsClick,
onSubscriptionClick = onNavigateToSubscription,
onSettingsClick = onSettingsClick
)
}


@Composable
private fun AnimatedHeader(
    viewModel: CalorieTrackerViewModel,
    onMenuClick: () -> Unit,
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
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Левая часть - Кнопка меню
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onMenuClick()
            }
        ) {
            Icon(
                Icons.Default.Menu,
                contentDescription = "Меню",
                tint = Color.Black
            )
        }
        
        // Центральная часть - Дата с функцией развертывания
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleStatusBar()
                },
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
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
        
        // Правая часть - AI индикатор
        AIUsageToolbarIndicator(
            userData = viewModel.currentUser,
            onClick = onNavigateToSubscription
        )
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

// В AnimatedMainScreen.kt обновите функцию AnimatedChatMessageCard:

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

            val showCard = !(message.isProcessing ||
                    (message.content.isEmpty() && message.isExpandable && message.foodItem?.aiOpinion != null))

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
                                SimpleChatTypingIndicator()
                            }
                        } else {
                            if (message.imagePath != null) {
                                AsyncImage(
                                    model = File(message.imagePath),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
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
                } else if (message.content.isEmpty() && message.isExpandable && message.foodItem?.aiOpinion != null) {
                    AnimatedAiChip(
                        onClick = { onAiOpinionClick(message.foodItem.aiOpinion!!) }
                    )
                }
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
            lineHeight = style.lineHeight * 1.2f
        ),
        color = color,
        modifier = modifier
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
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFDBF0E4),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF00BA65),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Что думает Foody?",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00BA65)
            )
        }
    }
}
