package com.example.calorietracker.pages

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

// Data class для пунктов меню
private data class AnimatedDropdowmMenu(
    val id: String,
    val text: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

// Анимированное выпадающее меню с эффектами
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedPlusDropdownMenu(
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

    // Контекстная подсказка по времени
    val contextualHint = getContextualHint()

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(animationSpec = tween(150)) +
                scaleIn(
                    transformOrigin = TransformOrigin(0.9f, 0.1f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
        exit = fadeOut(animationSpec = tween(100)) +
                scaleOut(
                    transformOrigin = TransformOrigin(0.9f, 0.1f),
                    animationSpec = tween(100)
                )
    ) {
        val density = LocalDensity.current
        Popup(
            alignment = Alignment.BottomEnd,
            offset = with(density) {
                IntOffset(
                    x = (30).dp.roundToPx(),
                    y = (-40).dp.roundToPx()
                )
            },
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            AnimatedMenuContent(
                contextualHint = contextualHint,
                lastAction = lastAction,
                onCameraClick = {
                    saveLastAction(context, "camera")
                    onDismissRequest()
                    onCameraClick()
                },
                onGalleryClick = {
                    saveLastAction(context, "gallery")
                    onDismissRequest()
                    onGalleryClick()
                },
                onDescribeClick = {
                    saveLastAction(context, "describe")
                    onDismissRequest()
                    onDescribeClick()
                },
                onManualClick = {
                    saveLastAction(context, "manual")
                    onDismissRequest()
                    onManualClick()
                }
            )
        }
    }
}

@Composable
private fun AnimatedMenuContent(
    contextualHint: String,
    lastAction: String?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDescribeClick: () -> Unit,
    onManualClick: () -> Unit
) {
    var showItems by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        showItems = true
    }

    Card(
        modifier = Modifier
            .width(250.dp)
            .fancyShadow(
                borderRadius = 20.dp,
                shadowRadius = 20.dp,
                offsetY = 8.dp,
                alpha = 0.25f
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF8F9FA)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Контекстная подсказка с анимацией
            AnimatedVisibility(
                visible = showItems,
                enter = fadeIn() + expandVertically()
            ) {
                ContextualHintCard(hint = contextualHint)
            }

            // Недавно использованное действие
            lastAction?.let { action ->
                AnimatedVisibility(
                    visible = showItems,
                    enter = fadeIn(tween(200)) + slideInVertically(
                        initialOffsetY = { -20 },
                        animationSpec = tween(200)
                    )
                ) {
                    Column {
                        RecentActionItem(
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
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
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
                AnimatedMenuItem(
                    item = item,
                    delay = 100 + (index * 50),
                    visible = showItems
                )
            }
        }
    }
}

// Контекстная подсказка
@Composable
private fun ContextualHintCard(hint: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF5F5F5)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "hint")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Text(
                text = hint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242),
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            )
        }
    }
}

// Недавно использованное действие
@Composable
private fun RecentActionItem(
    action: String,
    onClick: () -> Unit
) {
    val (text, icon, color) = when (action) {
        "camera" -> Triple("Сфоткать", Icons.Default.PhotoCamera, Color(0xFF4CAF50))
        "gallery" -> Triple("Выбрать фото", Icons.Default.Image, Color(0xFF2196F3))
        "describe" -> Triple("Расскажите", Icons.Default.AutoAwesome, Color(0xFFFF9800))
        "manual" -> Triple("Ввести данные", Icons.Default.Keyboard, Color(0xFF9C27B0))
        else -> return
    }

    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = color)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = color.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Недавно",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = color,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    "Использовано недавно",
                    fontSize = 12.sp,
                    color = color.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Анимированный пункт меню
@Composable
private fun AnimatedMenuItem(
    item: AnimatedDropdowmMenu,
    delay: Int,
    visible: Boolean
) {
    var itemVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(delay.toLong())
            itemVisible = true
        } else {
            itemVisible = false
        }
    }

    AnimatedVisibility(
        visible = itemVisible,
        enter = fadeIn() + slideInHorizontally(
            initialOffsetX = { 50 },
            animationSpec = tween(200)
        ),
        exit = fadeOut()
    ) {
        var isPressed by remember { mutableStateOf(false) }

        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "scale"
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = item.color)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isPressed = true
                    coroutineScope.launch {
                        delay(100)
                        item.onClick()
                    }
                },
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(item.color.copy(alpha = 0.08f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = item.color.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = item.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        item.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        item.subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
        }
    }
}

// Анимированный разделитель
@Composable
private fun AnimatedDivider() {
    var width by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        width = 1f
    }

    val animatedWidth by animateFloatAsState(
        targetValue = width,
        animationSpec = tween(300),
        label = "divider"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(animatedWidth)
            .height(1.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
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
        in 6..10 -> "Время завтрака 🍳"
        in 11..15 -> "Время обеда 🍝"
        in 16..18 -> "Время перекуса 🍎"
        in 19..21 -> "Время ужина 🍽️"
        else -> "Поздний перекус 🌙"
    }
}

private fun getMenuItems(
    lastAction: String?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDescribeClick: () -> Unit,
    onManualClick: () -> Unit
): List<AnimatedDropdowmMenu> {
    val allItems = listOf(
        AnimatedDropdowmMenu("camera", "Сфоткать", "Быстрый снимок", Icons.Default.PhotoCamera, Color(0xFF4CAF50), onCameraClick),
        AnimatedDropdowmMenu("gallery", "Выбрать фото", "Из вашей галереи", Icons.Default.Image, Color(0xFF2196F3), onGalleryClick),
        AnimatedDropdowmMenu("describe", "Расскажите", "А мы поймем", Icons.Default.AutoAwesome, Color(0xFFFF9800), onDescribeClick),
        AnimatedDropdowmMenu("manual", "Ввести данные", "Полный контроль", Icons.Default.Keyboard, Color(0xFF9C27B0), onManualClick)
    )

    return allItems.filter { it.id != lastAction }
}

private fun saveLastAction(context: Context, action: String) {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("last_food_action", action)
        .apply()
}
