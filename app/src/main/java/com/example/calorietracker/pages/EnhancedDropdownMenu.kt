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
import com.example.calorietracker.extensions.fancyShadow

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

    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded) {
            delay(10)
            try {
                backgroundBitmap = view.drawToBitmap()
            } catch (e: Exception) { /* ignore */ }
            isVisible = true
        }
    }

    fun animatedDismiss(onFinished: (() -> Unit)? = null) {
        coroutineScope.launch {
            isVisible = false
            delay(200)
            onDismissRequest()
            onFinished?.invoke()
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
                AnimatedVisibility(
                    visible = isVisible && backgroundBitmap != null,
                    enter = fadeIn(tween(200)),
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
                                        radiusX = animateDpAsState(if (isVisible) 20.dp else 0.dp, tween(200), "blur").value,
                                        radiusY = animateDpAsState(if (isVisible) 20.dp else 0.dp, tween(200), "blur").value
                                    ),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.6f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { animatedDismiss() }
                                    )
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                            scaleIn(
                                initialScale = 0.9f,
                                transformOrigin = TransformOrigin(0.9f, 0.9f),
                                animationSpec = tween(200, easing = FastOutSlowInEasing)
                            ),
                    exit = fadeOut(tween(150)) + scaleOut(
                        targetScale = 0.9f,
                        transformOrigin = TransformOrigin(0.9f, 0.9f)
                    ),
                    modifier = Modifier.padding(bottom = 80.dp, end = 16.dp)
                ) {
                    // *** ВОТ КЛЮЧЕВОЕ ИЗМЕНЕНИЕ ***
                    // Этот Box создает "воздушную подушку" для тени, чтобы она не обрезалась
                    Box(modifier = Modifier.padding(16.dp)) {
                        EnhancedMenuContent(
                            contextualHint = contextualHint,
                            lastAction = lastAction,
                            onCameraClick = {
                                saveLastAction(context, "camera")
                                animatedDismiss { onCameraClick() }
                            },
                            onGalleryClick = {
                                saveLastAction(context, "gallery")
                                animatedDismiss { onGalleryClick() }
                            },
                            onDescribeClick = {
                                saveLastAction(context, "describe")
                                animatedDismiss { onDescribeClick() }
                            },
                            onManualClick = {
                                saveLastAction(context, "manual")
                                animatedDismiss { onManualClick() }
                            }
                        )
                    }
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
        // Тень снова здесь, на самой карточке, где ей и место
        modifier = Modifier
            .width(260.dp)
            .fancyShadow(
                borderRadius = 24.dp,
                shadowRadius = 14.dp, // Радиус тени меньше отступа в Box (16.dp), это важно
                alpha = 0.25f
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color(0xFFFDFDFD),
                                Color(0xFFFAFAFA)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedHintCard(hint = contextualHint)

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

                val menuItems = getMenuItems(
                    lastAction = lastAction,
                    onCameraClick = onCameraClick,
                    onGalleryClick = onGalleryClick,
                    onDescribeClick = onDescribeClick,
                    onManualClick = onManualClick
                )
                menuItems.forEach { item ->
                    EnhancedMenuItem(item = item)
                }
            }
        }
    }
}

// ... остальной код без изменений ...
@Composable
private fun AnimatedHintCard(hint: String) {
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
            Text(
                text = hint,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1976D2)
            )
        }
    }
}

@Composable
private fun AnimatedRecentAction(
    action: String,
    onClick: () -> Unit
) {
    val (text, icon, color) = getActionDetails(action)
    val haptic = LocalHapticFeedback.current

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
            Box(
                modifier = Modifier
                    .matchParentSize()
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

@Composable
private fun EnhancedMenuItem(
    item: MenuItemData,
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = item.color)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                item.onClick()
            },
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
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
            }
        }
    }
}

@Composable
private fun AnimatedDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
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
        "manual" -> Triple("Вручную", Icons.Default.Keyboard, Color(0xFF9C27B0))
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
        MenuItemData("camera",   "Сфоткать",      "Быстрый снимок",  Icons.Default.PhotoCamera, Color(0xFF4CAF50), onCameraClick),
        MenuItemData("gallery",  "Выбрать фото",  "Из вашей галереи",Icons.Default.Image,       Color(0xFF2196F3), onGalleryClick),
        MenuItemData("describe", "Рассказать",    "А мы поймём",     Icons.Default.AutoAwesome, Color(0xFFFF9800), onDescribeClick),
        MenuItemData("manual",   "Вручную", "Полный контроль", Icons.Default.Keyboard,    Color(0xFF9C27B0), onManualClick)
    )

    return if (lastAction == null) allItems
    else allItems.filter { it.id != lastAction }
}

private fun saveLastAction(context: Context, action: String) {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("last_food_action", action)
        .apply()
}