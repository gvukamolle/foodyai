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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.drawToBitmap
import com.example.calorietracker.auth.UserData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    onSettingsClick: () -> Unit
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
        } else {
            isVisible = false
        }
    }
    
    fun animatedDismiss() {
        coroutineScope.launch {
            isVisible = false
            delay(300)
            onDismiss()
        }
    }
    
    if (isOpen) {
        BackHandler {
            animatedDismiss()
        }
        
        Popup(
            onDismissRequest = { animatedDismiss() },
            properties = PopupProperties(focusable = true)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Фон с размытием
                AnimatedVisibility(
                    visible = isVisible && backgroundBitmap != null,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200))
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
                                .background(Color.Black.copy(alpha = 0.4f))
                        )
                    }
                }
                
                // Выдвижная панель
                val drawerWidth = LocalDensity.current.run { 
                    (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp * 0.8f).dp 
                }
                
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
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(drawerWidth),
                        color = Color.White,
                        shadowElevation = 16.dp,
                        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
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
                                    subtitle = if (userData?.hasPremium == true) "Premium активна" else "Обновить план",
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSubscriptionClick()
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
                        }
                    }
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
                .clip(androidx.compose.foundation.shape.CircleShape)
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
            text = userData?.name ?: "Пользователь",
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