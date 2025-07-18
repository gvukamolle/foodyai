package com.example.calorietracker.pages

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.drawToBitmap
import com.example.calorietracker.auth.UserData
import com.example.calorietracker.extensions.fancyShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.calorietracker.auth.SubscriptionPlan
import androidx.compose.ui.graphics.TransformOrigin

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun DrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    isPremium: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPremium)
                            Color(0xFFFFF3E0)  // Светло-оранжевый фон для Premium
                        else
                            Color(0xFFF5F5F5)  // Серый фон для обычных пунктов
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isPremium) Color(0xFFFF9800) else Color(0xFF757575),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121)
                )

                subtitle?.let {
                    Text(
                        text = it,
                        fontSize = 13.sp,
                        color = if (isPremium) Color(0xFFFF9800) else Color(0xFF757575),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (isPremium) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader(userData: UserData?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Имя пользователя
        Text(
            text = userData?.displayName ?: "Пользователь",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )

        // Email или статус
        userData?.email?.let { email ->
            Text(
                text = email,
                fontSize = 14.sp,
                color = Color(0xFF757575),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun NavigationDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    userData: UserData? = null,
    onProfileClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFeedbackClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current

    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    // Запоминаем состояние открытия
    LaunchedEffect(isOpen) {
        if (isOpen) {
            delay(5)
            try {
                backgroundBitmap = view.drawToBitmap()
            } catch (e: Exception) { /* ignore */ }
            isVisible = true
        }
    }

    fun animatedDismiss() {
        coroutineScope.launch {
            isVisible = false
            delay(150) // Синхронизировано с длительностью анимации выхода
            onDismiss()
        }
    }

    if (isOpen) {
        BackHandler {
            animatedDismiss()
        }

        Popup(
            onDismissRequest = { animatedDismiss() },
            properties = PopupProperties(focusable = true),
            alignment = Alignment.CenterStart
        ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Кликабельный фон для закрытия
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        animatedDismiss()
                    }
            )

            // Фон с размытием и затемнением
            val blurRadius by animateDpAsState(
                targetValue = if (isVisible) 20.dp else 0.dp,
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                label = "blur_animation"
            )
            
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
                                .blur(radius = blurRadius),
                            contentScale = ContentScale.Crop
                        )
                        // Затемнение
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.7f))
                        )
                    }
                }
            }

            // Выдвижная панель в стиле левитирующего блока
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.9f,
                            transformOrigin = TransformOrigin(0f, 0.5f),
                            animationSpec = tween(200, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(animationSpec = tween(150)) +
                        scaleOut(
                            targetScale = 0.9f,
                            transformOrigin = TransformOrigin(0f, 0.5f),
                            animationSpec = tween(150)
                        )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, top = 32.dp, bottom = 32.dp, end = 48.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.85f) // Занимает 85% ширины экрана
                            .fancyShadow(
                                borderRadius = 24.dp,
                                shadowRadius = 16.dp,
                                alpha = 0.4f,
                                color = Color.Black
                            ),
                        shape = RoundedCornerShape(24.dp), // Полностью округлая форма
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .systemBarsPadding()
                        ) {
                            // Заголовок с информацией о пользователе
                            DrawerHeader(userData = userData)

                            Divider(
                                color = Color(0xFFE5E5E5),
                                thickness = 1.dp
                            )

                            // Пункты меню
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 8.dp)
                            ) {
                                DrawerMenuItem(
                                    icon = Icons.Default.Person,
                                    title = "Настройки профиля",
                                    subtitle = "Имя, вес, цели",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        animatedDismiss()
                                        onProfileClick()
                                    }
                                )

                                DrawerMenuItem(
                                    icon = Icons.Default.CalendarMonth,
                                    title = "Календарь",
                                    subtitle = "История питания",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        animatedDismiss()
                                        onCalendarClick()
                                    }
                                )

                                DrawerMenuItem(
                                    icon = Icons.Default.Analytics,
                                    title = "Аналитика",
                                    subtitle = "Графики и тренды",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        animatedDismiss()
                                        onAnalyticsClick()
                                    }
                                )

                                DrawerMenuItem(
                                    icon = Icons.Default.Diamond,
                                    title = "Подписка",
                                    subtitle = if (userData?.subscriptionPlan == SubscriptionPlan.PRO)
                                        "Premium активен" else "Перейти на Premium",
                                    isPremium = userData?.subscriptionPlan != SubscriptionPlan.PRO,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        animatedDismiss()
                                        onSubscriptionClick()
                                    }
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                HorizontalDivider(
                                    color = Color(0xFFE5E5E5),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                DrawerMenuItem(
                                    icon = Icons.Default.Settings,
                                    title = "Настройки",
                                    subtitle = "Уведомления, язык",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        animatedDismiss()
                                        onSettingsClick()
                                    }
                                )

                                DrawerMenuItem(
                                    icon = Icons.Default.Feedback,
                                    title = "Обратная связь",
                                    subtitle = "Помогите нам стать лучше",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        animatedDismiss()
                                        onFeedbackClick()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}