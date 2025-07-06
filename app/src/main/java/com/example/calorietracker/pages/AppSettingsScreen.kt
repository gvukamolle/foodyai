package com.example.calorietracker.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onBack: () -> Unit
) {
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(Unit) {
        systemUiController.setSystemBarsColor(color = Color.White, darkIcons = true)
    }

    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    // Состояния настроек
    var notificationsEnabled by remember { mutableStateOf(true) }
    var mealReminders by remember { mutableStateOf(true) }
    var waterReminders by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var language by remember { mutableStateOf("Русский") }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки приложения", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Секция уведомлений
            SettingsSectionCard(
                title = "Уведомления",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SwitchSettingItem(
                    title = "Push-уведомления",
                    subtitle = "Получать уведомления о приемах пищи",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
                if (notificationsEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingItem(
                        title = "Напоминания о еде",
                        subtitle = "Завтрак, обед, ужин",
                        checked = mealReminders,
                        onCheckedChange = { mealReminders = it }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingItem(
                        title = "Напоминания о воде",
                        subtitle = "Пить воду каждые 2 часа",
                        checked = waterReminders,
                        onCheckedChange = { waterReminders = it }
                    )
                }
            }

            // Секция звуков и вибрации
            SettingsSectionCard(
                title = "Звук и вибрация",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SwitchSettingItem(
                    title = "Звуки",
                    subtitle = "Звуковые эффекты в приложении",
                    checked = soundEnabled,
                    onCheckedChange = { soundEnabled = it }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SwitchSettingItem(
                    title = "Вибрация",
                    subtitle = "Тактильная обратная связь",
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it }
                )
            }

            // Секция языка
            SettingsSectionCard(
                title = "Язык и регион",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ClickableSettingItem(
                    title = "Язык приложения",
                    subtitle = language,
                    onClick = { showLanguageDialog = true }
                )
            }

            // Секция управления данными
            SettingsSectionCard(
                title = "Управление данными",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ClickableSettingItem(
                    title = "Очистить кэш",
                    subtitle = "Освободить место на устройстве",
                    onClick = { /* TODO: Clear cache */ }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ClickableSettingItem(
                    title = "Экспорт данных",
                    subtitle = "Сохранить данные в файл",
                    onClick = { /* TODO: Export data */ }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ClickableSettingItem(
                    title = "Импорт данных",
                    subtitle = "Загрузить данные из файла",
                    onClick = { /* TODO: Import data */ }
                )
            }

            // Секция конфиденциальности
            SettingsSectionCard(
                title = "Конфиденциальность",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ClickableSettingItem(
                    title = "Политика конфиденциальности",
                    onClick = { /* TODO: Open privacy policy */ }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ClickableSettingItem(
                    title = "Условия использования",
                    onClick = { /* TODO: Open terms */ }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = language,
            onLanguageSelected = {
                language = it
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            elevation = CardDefaults.cardElevation(0.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Color.Black)
            subtitle?.let {
                Text(it, fontSize = 14.sp, color = Color.Gray)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF6200EE),
                checkedTrackColor = Color(0xFF6200EE).copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun ClickableSettingItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = Color.Black)
            subtitle?.let {
                Text(it, fontSize = 14.sp, color = Color.Gray)
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

@Composable
private fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf("Русский", "English", "Español", "Deutsch", "Français", "中文")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите язык") },
        text = {
            Column {
                languages.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(lang) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == lang,
                            onClick = { onLanguageSelected(lang) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFF6200EE)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(lang)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
