package com.example.calorietracker.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
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
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.auth.SubscriptionPlan
import com.example.calorietracker.auth.UserData
import kotlinx.coroutines.launch
import com.google.accompanist.systemuicontroller.rememberSystemUiController


enum class SettingsSection {
    MAIN, PROFILE, BODY_SETTINGS, APP_SETTINGS, SUBSCRIPTION, CHANGE_PASSWORD,
    DATA_EXPORT, DATA_MANAGEMENT, FEEDBACK, ABOUT, MISSION, OTHER_APPS
}

data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val section: SettingsSection? = null,
    val badge: String? = null,
    val showArrow: Boolean = true,
    val onClick: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SettingsScreenV2(
    authManager: AuthManager,
    viewModel: CalorieTrackerViewModel,
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToBodySettings: () -> Unit,
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
    var currentSection by remember { mutableStateOf(SettingsSection.MAIN) }

    BackHandler(enabled = currentSection != SettingsSection.MAIN) {
        currentSection = SettingsSection.MAIN
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentSection) {
                            SettingsSection.MAIN -> "Настройки"
                            SettingsSection.APP_SETTINGS -> "Настройки приложения"
                            SettingsSection.DATA_EXPORT -> "Выгрузка данных"
                            SettingsSection.DATA_MANAGEMENT -> "Управление данными"
                            SettingsSection.FEEDBACK -> "Обратная связь"
                            SettingsSection.ABOUT -> "О нас"
                            SettingsSection.MISSION -> "Наша миссия"
                            SettingsSection.OTHER_APPS -> "Другие приложения"
                            SettingsSection.SUBSCRIPTION -> "Планы подписок"
                            else -> "Настройки"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (currentSection == SettingsSection.MAIN) onBack() else currentSection = SettingsSection.MAIN
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black,
                    actionIconContentColor = Color.White
                ))
        },
        containerColor = Color.White
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentSection,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    slideInHorizontally { it } + fadeIn() with slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() with slideOutHorizontally { it } + fadeOut()
                }.using(SizeTransform(clip = false))
            }
        ) { section ->
            when (section) {

                SettingsSection.MAIN -> MainSettingsContent(
                    currentUser = currentUser,
                    onSectionClick = { currentSection = it },
                    onSignOutClick = { showSignOutDialog = true },     // <-- Передаем реализацию
                    onDeleteAccountClick = { deleteDialogStep = 1 } // <-- И здесь
                )
                SettingsSection.PROFILE -> ProfileSettingsContent(authManager = authManager, onSave = { currentSection = SettingsSection.MAIN })
                SettingsSection.BODY_SETTINGS -> BodySettingsContent(viewModel = viewModel, onSave = { currentSection = SettingsSection.MAIN }) // Возвращаемся в главное меню после сохранения
                SettingsSection.APP_SETTINGS -> AppSettingsContent()
                SettingsSection.DATA_EXPORT -> DataExportContent()
                SettingsSection.DATA_MANAGEMENT -> DataManagementContent()
                SettingsSection.FEEDBACK -> FeedbackContent()
                SettingsSection.ABOUT -> AboutContent()
                SettingsSection.MISSION -> MissionContent()
                SettingsSection.OTHER_APPS -> OtherAppsContent()
                SettingsSection.CHANGE_PASSWORD -> ChangePasswordContent(authManager = authManager) { currentSection = SettingsSection.MAIN }
                SettingsSection.SUBSCRIPTION -> SubscriptionScreen(
                    currentPlan = currentUser?.subscriptionPlan ?: SubscriptionPlan.FREE,
                    onSelectPlan = { /* TODO: Handle plan selection */ }
                )
                else -> {}
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(48.dp)) },
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Вы сможете войти снова в любой момент.", textAlign = TextAlign.Center) },
            confirmButton = { TextButton(onClick = { showSignOutDialog = false; onSignOut() }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF9800))) { Text("Выйти") } },
            dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text("Отмена") } }
        )
    }

    if (deleteDialogStep == 1) {
        AlertDialog(
            onDismissRequest = { deleteDialogStep = 0 },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp)) },
            title = { Text("Удалить аккаунт?", textAlign = TextAlign.Center) },
            text = { Text("Все ваши данные будут удалены.", textAlign = TextAlign.Center) },
            confirmButton = { TextButton(onClick = { deleteDialogStep = 2 }, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) { Text("Далее") } },
            dismissButton = { TextButton(onClick = { deleteDialogStep = 0 }) { Text("Отмена") } }
        )
    }

    if (deleteDialogStep == 2) {
        AlertDialog(
            onDismissRequest = { deleteDialogStep = 0 },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp)) },
            title = { Text("Точно удалить?", textAlign = TextAlign.Center) },
            text = { Text("Это действие необратимо.", textAlign = TextAlign.Center) },            confirmButton = {
                TextButton(
                    onClick = { scope.launch { authManager.deleteAccount(); deleteDialogStep = 0; onSignOut() } },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { deleteDialogStep = 0 }) { Text("Отмена") } }
        )
    }
}

@Composable
fun MainSettingsContent(
    currentUser: UserData?,
    onSectionClick: (SettingsSection) -> Unit,
    onSignOutClick: () -> Unit,      // <-- Добавили это
    onDeleteAccountClick: () -> Unit // <-- И это
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { onSectionClick(SettingsSection.PROFILE) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (currentUser?.photoUrl != null) {
                        AsyncImage(model = currentUser.photoUrl, contentDescription = "Аватар", modifier = Modifier.size(64.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.Black), contentAlignment = Alignment.Center) {
                            Text(text = currentUser?.displayName?.firstOrNull()?.toString() ?: "?", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = currentUser?.displayName ?: "Пользователь", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = currentUser?.email ?: "", fontSize = 14.sp, color = Color.Gray)
                        currentUser?.subscriptionPlan?.let { plan ->
                            if (plan != SubscriptionPlan.FREE) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = when (plan) {
                                        SubscriptionPlan.PRO -> Color(0xFF2196F3)
                                        SubscriptionPlan.PREMIUM -> Color(0xFFFFD700)
                                        else -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(text = plan.displayName, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
        item {
            SettingsGroup(
                title = "Основные",
                items = listOf(
                    SettingsItem(icon = Icons.Default.Person, title = "Настройки тела", subtitle = "Рост, вес, цели", section = SettingsSection.BODY_SETTINGS),
                    SettingsItem(icon = Icons.Default.Settings, title = "Настройки приложения", subtitle = "Уведомления, тема, язык", section = SettingsSection.APP_SETTINGS),
                    SettingsItem(icon = Icons.Default.Star, title = "Планы подписок", subtitle = currentUser?.subscriptionPlan?.displayName ?: "Бесплатный", section = SettingsSection.SUBSCRIPTION, badge = if (currentUser?.subscriptionPlan == SubscriptionPlan.FREE) "Обновить" else null)
                ),
                onItemClick = { onSectionClick(it.section!!) }
            )
        }
        item {
            SettingsGroup(
                title = "Данные",
                items = listOf(
                    SettingsItem(icon = Icons.Default.Download, title = "Выгрузка данных", subtitle = "Экспорт в CSV, PDF", section = SettingsSection.DATA_EXPORT),
                    SettingsItem(icon = Icons.Default.Storage, title = "Управление данными", subtitle = "Очистка, резервное копирование", section = SettingsSection.DATA_MANAGEMENT)
                ),
                onItemClick = { onSectionClick(it.section!!) }
            )
        }
        item {
            SettingsGroup(
                title = "Информация",
                items = listOf(
                    SettingsItem(icon = Icons.Default.Feedback, title = "Обратная связь", section = SettingsSection.FEEDBACK),
                    SettingsItem(icon = Icons.Default.Info, title = "О нас", section = SettingsSection.ABOUT),
                    SettingsItem(icon = Icons.Default.Flag, title = "Наша миссия", section = SettingsSection.MISSION),
                    SettingsItem(icon = Icons.Default.Apps, title = "Другие приложения", subtitle = "Спорт, ментальное и женское здоровье", section = SettingsSection.OTHER_APPS, badge = "Скоро")
                ),
                onItemClick = { onSectionClick(it.section!!) }
            )
        }
        item {
            SettingsGroup(
                title = "Аккаунт",
                items = listOf(
                    SettingsItem(icon = Icons.Default.Lock, title = "Изменить пароль", section = SettingsSection.CHANGE_PASSWORD),
                    SettingsItem(icon = Icons.Default.Logout, title = "Выйти", showArrow = false, onClick = onSignOutClick), // <-- Используем лямбду
                    SettingsItem(icon = Icons.Default.DeleteForever, title = "Удалить аккаунт", showArrow = false, onClick = onDeleteAccountClick) // <-- И здесь тоже
                 ),
                onItemClick = { item -> item.section?.let { onSectionClick(it) } ?: item.onClick() }
            )
        }
        item {
            Text(text = "Версия 1.0.0 (Build 1)", modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun AsyncImage(
    model: String,
    contentDescription: String,
    modifier: Modifier,
    contentScale: ContentScale
) {
    coil.compose.AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}