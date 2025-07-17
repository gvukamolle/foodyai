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

@OptIn(ExperimentalAnimationApi::class)
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
    if (!isOpen) return
    
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
            delay(200)
            onDismiss()
        }
    }
    
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
            // Фон с размытием - как в AnimatedDialogContainer
            AnimatedVisibility(
                visible = isVisible && backgroundBitmap != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(100))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            animatedDismiss()
                        }
                ) {
                    backgroundBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(
                                    radiusX = animateDpAsState(
                                        if (isVisible) 20.dp else 0.dp,
                                        tween(200),
                                        "blur_x"
                                    ).value,
                                    radiusY = animateDpAsState(
                                        if (isVisible) 20.dp else 0.dp,
                                        tween(200),
                                        "blur_y"
                                    ).value
                                ),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Затемнение
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.7f))
                    )
                }
            }
            
            // Выдвижная панель в стиле поп-апа
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(300)),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(200)
                ) + fadeOut(tween(200))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Card(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.85f) // Занимает 85% ширины экрана
                            .fancyShadow(
                                borderRadius = 0.dp,
                                shadowRadius = 12.dp,
                                alpha = 0.35f,
                                color = Color.Black
                            ),
                        shape = RoundedCornerShape(
                            topEnd = 0.dp,
                            bottomEnd = 0.dp,
                            topStart = 0.dp,
                            bottomStart = 0.dp
                        ),
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
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onProfileClick()
                                        animatedDismiss()
                                    }
                                )
                                
                                DrawerMenuItem(
                                    icon = Icons.Default.CalendarMonth,
                                    title = "Календарь",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onCalendarClick()
                                        animatedDismiss()
                                    }
                                )
                                
                                DrawerMenuItem(
                                    icon = Icons.Default.BarChart,
                                    title = "Вся аналитика",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onAnalyticsClick()
                                        animatedDismiss()
                                    }
                                )
                                
                                DrawerMenuItem(
                                    icon = Icons.Default.AutoAwesome,
                                    title = "Подписка",
                                    subtitle = if (userData?.subscriptionPlan == SubscriptionPlan.PRO) "PRO активен" else "Обновить план",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSubscriptionClick()
                                        animatedDismiss()
                                    }
                                )
                                
                                DrawerMenuItem(
                                    icon = Icons.Default.RateReview,
                                    title = "Обратная связь",
                                    subtitle = "Напишите нам",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onFeedbackClick()
                                        animatedDismiss()
                                    }
                                )
                                
                                DrawerMenuItem(
                                    icon = Icons.Default.Settings,
                                    title = "Настройки приложения",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSettingsClick()
                                        animatedDismiss()
                                    }
                                )
                            }
                            
                            // Версия приложения внизу
                            Divider(
                                color = Color(0xFFE5E5E5),
                                thickness = 1.dp
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Версия 1.0",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    
                    // Оставшаяся область для клика
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                animatedDismiss()
                            }
                    )
                }
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
        // Иконка пользователя
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFE3F2FD)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Имя пользователя
        Text(
            text = userData?.displayName ?: "Пользователь",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        // Email или статус
        userData?.email?.let { email ->
            Text(
                text = email,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
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
            Icon(
                icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                subtitle?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}