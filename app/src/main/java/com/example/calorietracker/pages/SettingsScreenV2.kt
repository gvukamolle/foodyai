package com.example.calorietracker.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.auth.SubscriptionPlan
import kotlinx.coroutines.launch
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.ui.platform.LocalContext


data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val badge: String? = null,
    val showArrow: Boolean = true,
    val onClick: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenV2(
    authManager: AuthManager,
    viewModel: CalorieTrackerViewModel,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBodySettings: () -> Unit,
    onNavigateToAppSettings: () -> Unit,
    onNavigateToSubscription: () -> Unit, // НОВОЕ
    onSignOut: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(Unit) {
        systemUiController.setSystemBarsColor(color = Color.White, darkIcons = true)
    }
    val currentUser by authManager.currentUser.collectAsState()
    var showSignOutDialog by remember { mutableStateOf(false) }
    var deleteDialogStep by remember { mutableStateOf(0) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                // Профиль пользователя
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { onNavigateToProfile() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser?.displayName?.firstOrNull()?.toString() ?: "?",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.displayName ?: "Пользователь",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser?.email ?: "",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                            currentUser?.subscriptionPlan?.let { plan ->
                                if (plan != SubscriptionPlan.FREE) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = when (plan) {
                                            SubscriptionPlan.PLUS -> Color(0xFFFF9800)
                                            SubscriptionPlan.PRO -> Color(0xFF2196F3)
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = plan.displayName,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        Icon(
                            Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            item {
                SettingsGroup(
                    title = "Основные",
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.Settings,
                            title = "Настройки приложения",
                            subtitle = "Уведомления, тема, язык",
                            onClick = onNavigateToAppSettings
                        ),
                        SettingsItem(
                            icon = Icons.Default.Star,
                            title = "Планы подписок",
                            subtitle = currentUser?.subscriptionPlan?.displayName ?: "Бесплатный",
                            onClick = onNavigateToSubscription // Изменено
                        )
                    ),
                    onItemClick = { item -> item.onClick() }
                )
            }

            item {
                SettingsGroup(
                    title = "Информация",
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.Feedback,
                            title = "Обратная связь",
                            onClick = { /* TODO: Feedback */ }
                        ),
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "О нас",
                            onClick = { /* TODO: About */ }
                        ),
                        SettingsItem(
                            icon = Icons.Default.Flag,
                            title = "Наша миссия",
                            onClick = { /* TODO: Mission */ }
                        ),
                        SettingsItem(
                            icon = Icons.Default.Apps,
                            title = "Другие приложения",
                            subtitle = "Спорт, ментальное и женское здоровье",
                            badge = "Скоро",
                            onClick = { /* TODO: Other apps */ }
                        )
                    ),
                    onItemClick = { item -> item.onClick() }
                )
            }

            item {
                SettingsGroup(
                    title = "Аккаунт",
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Default.Logout,
                            title = "Выйти",
                            showArrow = false,
                            onClick = { showSignOutDialog = true }
                        ),
                        SettingsItem(
                            icon = Icons.Default.DeleteForever,
                            title = "Удалить аккаунт",
                            showArrow = false,
                            onClick = { deleteDialogStep = 1 }
                        )
                    ),
                    onItemClick = { item -> item.onClick() }
                )
            }

            item {
                Text(
                    text = "Версия 1.0.0 (Build 1)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Вы сможете войти снова в любой момент.", textAlign = TextAlign.Center) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF9800))
                ) {
                    Text("Выйти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (deleteDialogStep == 1) {
        AlertDialog(
            onDismissRequest = { deleteDialogStep = 0 },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Удалить аккаунт?", textAlign = TextAlign.Center) },
            text = { Text("Все ваши данные будут удалены. Это действие нельзя отменить.", textAlign = TextAlign.Center) },
            confirmButton = {
                TextButton(
                    onClick = { deleteDialogStep = 2 },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogStep = 0 }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (deleteDialogStep == 2) {
        AlertDialog(
            onDismissRequest = { deleteDialogStep = 0 },
            icon = {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Последнее предупреждение", textAlign = TextAlign.Center) },
            text = { Text("Вы действительно хотите удалить аккаунт? Все данные будут потеряны навсегда.", textAlign = TextAlign.Center) },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteDialogStep = 0
                        scope.launch {
                            authManager.deleteAccount()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Да, удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogStep = 0 }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    items: List<SettingsItem>,
    onItemClick: (SettingsItem) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.title,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            item.badge?.let { badge ->
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFF000000),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = badge,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        item.subtitle?.let {
                            Text(
                                text = it,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    if (item.showArrow) {
                        Icon(
                            Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 56.dp),
                        color = Color(0xFFE0E0E0)
                    )
                }
            }
        }
    }
}