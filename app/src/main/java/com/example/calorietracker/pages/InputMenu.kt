package com.example.calorietracker.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import java.time.LocalTime
import androidx.compose.ui.window.Popup
import androidx.compose.ui.platform.LocalDensity
import com.example.calorietracker.extensions.fancyShadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime

// Основная функция меню
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlusDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onManualClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val ime = WindowInsets.ime
    val imeVisible by remember {
        derivedStateOf { ime.getBottom(density) > 0 }
    }

    LaunchedEffect(expanded) {
        if (expanded) {
            delay(5)
            isVisible = true
        } else {
            isVisible = false
        }
    }

    fun animatedDismiss() {
        coroutineScope.launch {
            isVisible = false
            delay(150)
            onDismissRequest()
        }
    }

    if (expanded) {
        Popup(
            alignment = Alignment.BottomEnd,
            offset = with(density) {
                IntOffset(x = (-16).dp.roundToPx(), y = (-56).dp.roundToPx())
            },
            onDismissRequest = { 
                if (imeVisible) {
                    focusManager.clearFocus()
                } else {
                    animatedDismiss()
                }
            },
            properties = PopupProperties(focusable = true)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Полупрозрачный фон для закрытия
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(100)),
                    exit = fadeOut(tween(100))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.05f))
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

                // Карточка меню
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(150, easing = FastOutSlowInEasing)) +
                            scaleIn(
                                initialScale = 0.85f,
                                transformOrigin = TransformOrigin(1f, 0f),
                                animationSpec = tween(150, easing = FastOutSlowInEasing)
                            ),
                    exit = fadeOut(animationSpec = tween(100)) +
                            scaleOut(
                                targetScale = 0.85f,
                                transformOrigin = TransformOrigin(1f, 0f),
                                animationSpec = tween(100)
                            ),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .width(220.dp)
                            .fancyShadow(
                                borderRadius = 20.dp,
                                shadowRadius = 12.dp,
                                alpha = 0.25f,
                                color = Color.Black
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Заголовок с временем приема пищи
                            val hour = LocalTime.now().hour
                            val mealTime = when (hour) {
                                in 6..10 -> "🌅 Время завтрака"
                                in 11..15 -> "☀️ Время обеда"
                                in 16..18 -> "🍎 Время перекуса"
                                in 19..21 -> "🌙 Время ужина"
                                else -> "🌟 Поздний перекус"
                            }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF5F5F5))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = mealTime,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF616161)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Пункт "Сфоткать"
                            MenuItem(
                                text = "Сфоткать",
                                subtitle = "Недавнее",
                                icon = Icons.Default.PhotoCamera,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCameraClick()
                                    animatedDismiss()
                                },
                                delay = 0
                            )

                            // Пункт "Выбрать фото"
                            MenuItem(
                                text = "Выбрать фото",
                                subtitle = "Из вашей галереи",
                                icon = Icons.Default.Image,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onGalleryClick()
                                    animatedDismiss()
                                },
                                delay = 50
                            )

                            // Пункт "Вручную"
                            MenuItem(
                                text = "Вручную",
                                subtitle = "Полный контроль",
                                icon = Icons.Default.Keyboard,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onManualClick()
                                    animatedDismiss()
                                },
                                delay = 100
                            )
                        }
                    }
                }
            }
        }
    }
}

// Элемент меню
@Composable
private fun MenuItem(
    text: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    delay: Int = 0
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200),
        label = "alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Иконка с серым фоном
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF616161),
                    modifier = Modifier.size(22.dp)
                )
            }
            
            // Текст
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
        }
    }
}

// Анимированная кнопка для MainScreen
@Composable
fun AnimatedPlusButton(
    expanded: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
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
        enabled = enabled,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Добавить",
            tint = if (enabled) Color.Black else Color.LightGray,
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