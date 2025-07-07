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
import com.example.calorietracker.ui.animations.TypewriterText
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import java.time.format.TextStyle as DateTextStyle


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedMainScreen(
    viewModel: CalorieTrackerViewModel,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onManualClick: () -> Unit,
    onDescribeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCalendarClick: () -> Unit
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

    // Все диалоги
    AnimatedDialogs(
        viewModel = viewModel,
        onCameraClick = onCameraClick,
        onGalleryClick = onGalleryClick
    )

    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        containerColor = Color.White,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Обновленный заголовок с кликом на календарь
            AnimatedHeader(
                viewModel = viewModel,
                onSettingsClick = onSettingsClick,
                onDateClick = onCalendarClick  // Передаем обработчик
            )

            // Прогресс-бары
            AnimatedProgressBars(viewModel = viewModel)

            // Разделитель с анимацией
            AnimatedContentDivider()

            // Карточка подтверждения еды
            viewModel.pendingFood?.let { food ->
                AnimatedPendingFoodCard(
                    food = food,
                    selectedMeal = viewModel.selectedMeal,
                    onMealChange = { viewModel.selectedMeal = it },
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
    }
}

// Обновленный AnimatedHeader с датой и стрелкой
@Composable
private fun AnimatedHeader(
    viewModel: CalorieTrackerViewModel,
    onSettingsClick: () -> Unit,
    onDateClick: () -> Unit // Новый параметр для перехода к календарю
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -30 })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кликабельная область с датой
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
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
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedOnlineStatus(isOnline = viewModel.isOnline)

            Spacer(modifier = Modifier.width(12.dp))

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

// Анимированный статус подключения
@Composable
private fun AnimatedOnlineStatus(isOnline: Boolean) {
    val color by animateColorAsState(
        targetValue = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336),
        animationSpec = tween(300),
        label = "status_color"
    )

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isOnline) "Online" else "Offline",
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
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

// Анимированный чат
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
            val lastIndex = viewModel.messages.lastIndex
            itemsIndexed(viewModel.messages) { index, message ->
                AnimatedMessage(
                    visible = true,
                    isUserMessage = message.type == MessageType.USER
                ) {
                    val animateText = index == lastIndex && message.type == MessageType.AI
                    AnimatedChatMessageCard(
                        message = message,
                        animateText = animateText,
                        onAiOpinionClick = { text ->
                            viewModel.aiOpinionText = text
                            viewModel.showAiOpinionDialog = true
                        }
                    )
                }
            }
        }

        // Индикатор анализа AI
        AnimatedVisibility(
            visible = viewModel.isAnalyzing,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                EnhancedAILoadingIndicator(
                    modifier = Modifier.padding(24.dp),
                    text = if (viewModel.isOnline) "AI анализирует" else "Обрабатываем",
                    accentColor = Color(0xFFFF9800)
                )
            }
        }
    }
}

// Анимированная карточка сообщения
@Composable
private fun AnimatedChatMessageCard(
    message: com.example.calorietracker.ChatMessage,
    animateText: Boolean,
    onAiOpinionClick: (String) -> Unit
) {
    // no expandable content

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Основное сообщение
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (message.type == MessageType.USER) {
                Alignment.CenterEnd
            } else {
                Alignment.CenterStart
            }
        ) {
            Card(
                modifier = Modifier.widthIn(max = 280.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (message.type == MessageType.USER) {
                        Color.Black
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Текст сообщения
                        Box(modifier = Modifier.weight(1f)) {
                            if (message.type == MessageType.AI) {
                                if (animateText) {
                                    TypewriterText(
                                        text = message.content,
                                        style = TextStyle(
                                            color = Color.Black,
                                            fontSize = 14.sp
                                        )
                                    )
                                } else {
                                    Text(
                                        text = message.content,
                                        color = Color.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = message.content,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    if (message.isExpandable && message.foodItem?.aiOpinion != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            AnimatedAiChip(onClick = { onAiOpinionClick(message.foodItem.aiOpinion!!) })
                        }
                    } }
            }
        }
    }
}

@Composable
fun PendingDescriptionCard(
    description: String,
    isAnalyzing: Boolean,
    onAnalyze: () -> Unit,
    onEdit: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = description.isNotBlank(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFF8E1)  // Светло-желтый фон
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Заголовок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Описание блюда",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }

                    // Кнопка очистки
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Очистить",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Текст описания
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFF424242),
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Кнопки действий
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Кнопка редактирования
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF9800)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF9800))
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Изменить")
                    }

                    // Кнопка анализа
                    Button(
                        onClick = onAnalyze,
                        modifier = Modifier.weight(1f),
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800),
                            disabledContainerColor = Color(0xFFFFCC80)
                        )
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Анализ...")
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Анализировать")
                        }
                    }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
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
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF4CAF50).copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Что думает Foody?",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
