package com.example.calorietracker.pages

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

// Улучшенное выпадающее меню с размытием фона
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedPlusDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDescribeClick: () -> Unit,
    onManualClick: () -> Unit,
    context: Context = LocalContext.current
) {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val lastAction = prefs.getString("last_food_action", null)
    val contextualHint = getContextualHint()

    // Состояние для анимации
    var isVisible by remember(expanded) { mutableStateOf(expanded) }
    val coroutineScope = rememberCoroutineScope()

    // Снимаем скриншот для размытия
    val view = LocalView.current
    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(expanded) {
        if (expanded) {
            delay(10)
            try {
                backgroundBitmap = view.drawToBitmap()
            } catch (e: Exception) {
                // Игнорируем ошибки скриншота
            }
            isVisible = true
        }
    }

    fun animatedDismiss() {
        coroutineScope.launch {
            isVisible = false
            delay(200)
            onDismissRequest()
        }
    }

    if (expanded) {
        Popup(
            onDismissRequest = { animatedDismiss() },
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                // Размытый фон
                AnimatedVisibility(
                    visible = isVisible && backgroundBitmap != null,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(100))
                ) {
                    backgroundBitmap?.let { bitmap ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(
                                        radiusX = animateDpAsState(
                                            targetValue = if (isVisible) 20.dp else 0.dp,
                                            animationSpec = tween(200),
                                            label = "blur"
                                        ).value,
                                        radiusY = animateDpAsState(
                                            targetValue = if (isVisible) 20.dp else 0.dp,
                                            animationSpec = tween(200),
                                            label = "blur"
                                        ).value
                                    ),
                                contentScale = ContentScale.Crop
                            )
                            // Темный оверлей
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { animatedDismiss() }
                                    )
                            )
                        }
                    }
                }

                // Меню
                val density = LocalDensity.current
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(150)) + scaleIn(
                        initialScale = 0.8f,
                        transformOrigin = TransformOrigin(0.9f, 0.9f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + slideInVertically(
                        initialOffsetY = { with(density) { 20.dp.roundToPx() } },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                    exit = fadeOut(tween(100)) + scaleOut(
                        targetScale = 0.9f,
                        transformOrigin = TransformOrigin(0.9f, 0.9f)
                    ),
                    modifier = Modifier
                        .padding(bottom = 80.dp, end = 16.dp)
                ) {
                    EnhancedMenuContent(
                        contextualHint = contextualHint,
                        lastAction = lastAction,
                        onCameraClick = {
                            saveLastAction(context, "camera")
                            animatedDismiss()
                            onCameraClick()
                        },
                        onGalleryClick = {
                            saveLastAction(context, "gallery")
                            animatedDismiss()
                            onGalleryClick()
                        },
                        onDescribeClick = {
                            saveLastAction(context, "describe")
                            animatedDismiss()
                            onDescribeClick()
                        },
                        onManualClick = {
                            saveLastAction(context, "manual")
                            animatedDismiss()
                            onManualClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedMenuContent(
    contextualHint: String,
    lastAction: String?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDescribeClick: () -> Unit,
    onManualClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .graphicsLayer {
                shadowElevation = 24.dp.toPx()
                shape = RoundedCornerShape(24.dp)
                clip = true
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            // Градиентный фон
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFFBFBFB),
                                Color(0xFFF8F8F8)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Контекстная подсказка с анимацией
                AnimatedHintCard(hint = contextualHint)

                // Недавнее действие
                lastAction?.let { action ->
                    AnimatedRecentAction(
                        action = action,
                        onClick = {
                            when (action) {
                                "camera" -> onCameraClick()
                                "gallery" -> onGalleryClick()
                                "describe" -> onDescribeClick()
                                "manual" -> onManualClick()
                            }
                        }
                    )

                    AnimatedDivider()
                }

                // Основные пункты меню
                val menuItems = getMenuItems(
                    lastAction = lastAction,
                    onCameraClick = onCameraClick,
                    onGalleryClick = onGalleryClick,
                    onDescribeClick = onDescribeClick,
                    onManualClick = onManualClick
                )

                menuItems.forEachIndexed { index, item ->
                    EnhancedMenuItem(
                        item = item,
                        delay = (index + 1) * 50
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedHintCard(hint: String) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + expandVertically()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF0F4FF)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "hint")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Text(
                    text = hint,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                )
            }
        }
    }
}

@Composable
private fun AnimatedRecentAction(
    action: String,
    onClick: () -> Unit
) {
    val (text, icon, color) = getActionDetails(action)
    var visible by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f) + slideInHorizontally(initialOffsetX = { -20 })
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = color)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.15f)
        ) {
            Box {
                // Градиент для красоты
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    color.copy(alpha = 0.05f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        color.copy(alpha = 0.3f),
                                        color.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = color.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "Недавнее",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedMenuItem(
    item: MenuItemData,
    delay: Int
) {
    var visible by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            visible -> 1f
            else -> 0.8f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f) + slideInHorizontally(initialOffsetX = { -30 })
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = item.color)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isPressed = true
                    item.onClick()
                },
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent
        ) {
            Box {
                // Фоновый градиент
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    item.color.copy(alpha = 0.08f),
                                    item.color.copy(alpha = 0.04f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = item.color.copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = item.color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            item.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            item.subtitle,
                            fontSize = 13.sp,
                            color = Color(0xFF757575)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = item.color.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedDivider() {
    var width by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        delay(200)
        width = 1f
    }

    val animatedWidth by animateFloatAsState(
        targetValue = width,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "divider"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(animatedWidth)
            .height(1.dp)
            .padding(vertical = 4.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFE0E0E0),
                        Color(0xFFE0E0E0),
                        Color.Transparent
                    )
                )
            )
    )
}

// Вспомогательные функции
private fun getContextualHint(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 6..10 -> "🌅 Время завтрака"
        in 11..15 -> "☀️ Время обеда"
        in 16..18 -> "🍎 Время перекуса"
        in 19..21 -> "🌆 Время ужина"
        else -> "🌙 Поздний перекус"
    }
}

private fun getActionDetails(action: String): Triple<String, ImageVector, Color> {
    return when (action) {
        "camera" -> Triple("Сфоткать", Icons.Default.PhotoCamera, Color(0xFF4CAF50))
        "gallery" -> Triple("Выбрать фото", Icons.Default.Image, Color(0xFF2196F3))
        "describe" -> Triple("Расскажите", Icons.Default.AutoAwesome, Color(0xFFFF9800))
        "manual" -> Triple("Ввести данные", Icons.Default.Keyboard, Color(0xFF9C27B0))
        else -> Triple("", Icons.Default.Add, Color.Black)
    }
}

private fun getMenuItems(
    lastAction: String?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDescribeClick: () -> Unit,
    onManualClick: () -> Unit
): List<MenuItemData> {
    val allItems = listOf(
        MenuItemData("camera", "Сфоткать", "Быстрый снимок", Icons.Default.PhotoCamera, Color(0xFF4CAF50), onCameraClick),
        MenuItemData("gallery", "Выбрать фото", "Из вашей галереи", Icons.Default.Image, Color(0xFF2196F3), onGalleryClick),
        MenuItemData("describe", "Расскажите", "А мы поймем", Icons.Default.AutoAwesome, Color(0xFFFF9800), onDescribeClick),
        MenuItemData("manual", "Ввести данные", "Полный контроль", Icons.Default.Keyboard, Color(0xFF9C27B0), onManualClick)
    )
    return allItems.filter { it.id != lastAction }
}

private fun saveLastAction(context: Context, action: String) {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("last_food_action", action)
        .apply()
}

