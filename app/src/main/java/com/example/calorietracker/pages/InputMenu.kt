package com.example.calorietracker.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.foundation.clickable
import java.time.LocalTime
import androidx.compose.ui.window.Popup
import androidx.compose.ui.platform.LocalDensity
import com.example.calorietracker.extensions.fancyShadow
import androidx.compose.ui.platform.LocalFocusManager


// Вариант 2: С градиентом и современными иконками (БЕЗ СЕРЫХ ОБЛАСТЕЙ)
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlusDropdownMenuV2(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDescribeClick: () -> Unit,
    onManualClick: () -> Unit,
    context: Context
) {
    // Получаем последнее действие из SharedPreferences
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val lastAction = prefs.getString("last_food_action", null)

    // Определяем контекстную подсказку по времени
    val hour = LocalTime.now().hour
    val contextualHint = when (hour) {
        in 6..10 -> "Время завтрака 🍳"
        in 11..15 -> "Время обеда 🍝"
        in 16..18 -> "Время перекуса 🍎"
        in 19..21 -> "Время ужина 🍽️"
        else -> "Поздний перекус 🌙"
    }

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(animationSpec = tween(150)) + scaleIn(
            transformOrigin = TransformOrigin(0.9f, 0.1f),
            animationSpec = tween(150)
        ),
        exit = fadeOut(animationSpec = tween(100)) + scaleOut(
            transformOrigin = TransformOrigin(0.9f, 0.1f),
            animationSpec = tween(100)
        )
    ) {
        val density = LocalDensity.current
        val focusManager = LocalFocusManager.current
        val ime = WindowInsets.ime
        val imeVisible by remember {
            derivedStateOf { ime.getBottom(density) > 0 }
        }
        Popup(
            alignment = Alignment.BottomEnd,
            offset = with(density) {
                IntOffset(
                    x = (30).dp.roundToPx(), // <-- Теперь смещение по X в dp
                    y = (-40).dp.roundToPx()   // <-- Теперь смещение по Y в dp
                )
            },
            onDismissRequest = {
                if (imeVisible) {
                    focusManager.clearFocus()
                } else {
                    onDismissRequest()
                }
            },            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (imeVisible) {
                                focusManager.clearFocus()
                            } else {
                                onDismissRequest()
                            }
                        }
                )
                Card(
                modifier = Modifier
                    .fancyShadow(
                        borderRadius = 20.dp,
                        shadowRadius = 8.dp,
                        alpha = 0.25f
                    )
                    .width(230.dp), // Остальные модификаторы оставляем

                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                // ВАЖНО: Убедитесь, что у самой карточки нет собственной тени/elevation
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ){}
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFFAFAFA)
                                )
                            )
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Контекстная подсказка
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contextualHint,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF424242)
                        )
                    }

                    // Если есть последнее действие, показываем его первым
                    lastAction?.let { action ->
                        val (text, subtitle, icon, color) = when (action) {
                            "camera" -> listOf("Сфоткать", "Использовано недавно", Icons.Default.PhotoCamera, Color(0xFF4CAF50))
                            "gallery" -> listOf("Выбрать фото", "Использовано недавно", Icons.Default.Image, Color(0xFF2196F3))
                            "describe" -> listOf("Расскажите", "Использовано недавно", Icons.Default.AutoAwesome, Color(0xFFFF9800))
                            "manual" -> listOf("Ввести данные", "Использовано недавно", Icons.Default.Keyboard, Color(0xFF9C27B0))
                            else -> return@let
                        }

                        CompactMenuItem(
                            text = text as String,
                            subtitle = subtitle as String,
                            icon = icon as ImageVector,
                            iconColor = color as Color,
                            onClick = {
                                saveLastAction(context, action)
                                onDismissRequest()
                                when (action) {
                                    "camera" -> onCameraClick()
                                    "gallery" -> onGalleryClick()
                                    "describe" -> onDescribeClick()
                                    "manual" -> onManualClick()
                                }
                            },
                            delay = 0,
                            isRecent = true
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            thickness = 1.dp,
                            color = Color(0xFFEEEEEE)
                        )
                    }

                    // Остальные пункты меню
                    val menuItems = listOf(
                        MenuItemData("camera", "Сфоткать", "Быстрый снимок", Icons.Default.PhotoCamera, Color(0xFF4CAF50), onCameraClick),
                        MenuItemData("gallery", "Выбрать фото", "Из вашей галереи", Icons.Default.Image, Color(0xFF2196F3), onGalleryClick),
                        MenuItemData("describe", "Расскажите", "А мы поймем", Icons.Default.AutoAwesome, Color(0xFFFF9800), onDescribeClick),
                        MenuItemData("manual", "Ввести данные", "Полный контроль", Icons.Default.Keyboard, Color(0xFF9C27B0), onManualClick)
                    )

                    menuItems.forEachIndexed { index, item ->
                        if (lastAction != item.id) {
                            CompactMenuItem(
                                text = item.text,
                                subtitle = item.subtitle,
                                icon = item.icon,
                                iconColor = item.color,
                                onClick = {
                                    saveLastAction(context, item.id)
                                    onDismissRequest()
                                    item.onClick()
                                },
                                delay = if (lastAction == null) index * 50 else (index + 1) * 50
                            )
                        }
                    }
                }
            }
        }
    }
}

// Вспомогательная data class для элементов меню
internal data class MenuItemData(
    val id: String,
    val text: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

// Компактный вариант элемента меню с ripple эффектом (ИСПРАВЛЕННАЯ ВЕРСИЯ)
@Composable
private fun CompactMenuItem(
    text: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    delay: Int = 0,
    isRecent: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            visible -> 1f
            else -> 0.9f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale_animation"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200),
        label = "alpha_animation"
    )

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    // ВАЖНОЕ ИЗМЕНЕНИЕ ЗДЕСЬ
    Surface(
        // Параметр onClick убран отсюда
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            // 1. Сначала обрезаем область по нужной форме
            .clip(RoundedCornerShape(12.dp))
            // 2. Затем делаем ее кликабельной с ripple-эффектом
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true, // Ripple не выходит за границы
                    color = iconColor
                ),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isPressed = true
                    coroutineScope.launch {
                        delay(100) // Задержка для визуального эффекта нажатия
                        onClick()
                    }
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
        // Параметры indication и interactionSource убраны, т.к. они теперь в .clickable()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Этот background теперь внутри кликабельной области
                .background(
                    color = if (isRecent) {
                        iconColor.copy(alpha = 0.12f)
                    } else {
                        iconColor.copy(alpha = 0.08f)
                    }
                    // Форма здесь не нужна, так как Surface уже имеет форму
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = iconColor.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text,
                    fontSize = 15.sp,
                    fontWeight = if (isRecent) FontWeight.SemiBold else FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = if (isRecent) iconColor else Color(0xFF757575),
                    fontWeight = if (isRecent) FontWeight.Medium else FontWeight.Normal
                )
            }
            if (isRecent) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Недавно использовано",
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Функция для сохранения последнего действия
private fun saveLastAction(context: Context, action: String) {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("last_food_action", action)
        .apply()
}


// Используй этот компонент в MainScreen вместо старого
@Composable
fun PlusDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDescribeClick: () -> Unit,
    onManualClick: () -> Unit,
) {
    val context = LocalContext.current

    PlusDropdownMenuV2( // Используем улучшенный вариант 2
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        onCameraClick = onCameraClick,
        onGalleryClick = onGalleryClick,
        onDescribeClick = onDescribeClick,
        onManualClick = onManualClick,
        context = context
    )
}

// Анимированная кнопка для MainScreen (того же размера что и Send)
@Composable
fun AnimatedPlusButton(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val scale by animateFloatAsState(
        targetValue = if (expanded) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    val haptic = LocalHapticFeedback.current

    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.size(40.dp) // Тот же размер что у Send
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Добавить",
            tint = Color.Black,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}