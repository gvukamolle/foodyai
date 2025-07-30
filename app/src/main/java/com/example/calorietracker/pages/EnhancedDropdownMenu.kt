package com.example.calorietracker.pages

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ripple
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import com.example.calorietracker.extensions.fancyShadow
import androidx.compose.ui.platform.LocalFocusManager

// Улучшенное выпадающее меню с размытием фона
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedPlusDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onManualClick: () -> Unit,
) {
    val contextualHint = getContextualHint()

    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val ime = WindowInsets.ime
    val imeVisible by remember {
        derivedStateOf { ime.getBottom(density) > 0 }
    }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded) {
            delay(10)
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
            onDismissRequest = {
                if (imeVisible) {
                    focusManager.clearFocus()
                } else {
                    animatedDismiss()
                }
            },
            properties = PopupProperties(
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
                                animatedDismiss()
                            }
                        }
                )
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(100))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.6f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (imeVisible) {
                                    focusManager.clearFocus()
                                } else {
                                    animatedDismiss()
                                }
                            }
                    )
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
                            onCameraClick = {
                                animatedDismiss { onCameraClick() }
                            },
                            onGalleryClick = {
                                animatedDismiss { onGalleryClick() }
                            },
                            onManualClick = {
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
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
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
                val menuItems = getMenuItems(
                    onCameraClick = onCameraClick,
                    onGalleryClick = onGalleryClick,
                    onManualClick = onManualClick
                )
                menuItems.forEach { item ->
                    EnhancedMenuItem(item = item)
                }
            }
        }
    }
}

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
                indication = ripple(color = item.color)
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

private fun getMenuItems(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onManualClick: () -> Unit
): List<MenuItemData> {

    val allItems = listOf(
        MenuItemData("camera",   "Сфоткать",      "Быстрый снимок",  Icons.Default.PhotoCamera, Color(0xFF4CAF50), onCameraClick),
        MenuItemData("gallery",  "Выбрать фото",  "Из вашей галереи",Icons.Default.Image,       Color(0xFF2196F3), onGalleryClick),
        MenuItemData("manual",   "Вручную", "Полный контроль", Icons.Default.Keyboard,    Color(0xFF9C27B0), onManualClick)
    )
    return allItems
}

// Модель данных одного пункта меню
data class MenuItemData(
    val id: String,
    val text: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)