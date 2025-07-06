package com.example.calorietracker.pages.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.utils.CacheManager
import com.example.calorietracker.utils.ExportManager
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    viewModel: CalorieTrackerViewModel,
    onBack: () -> Unit
) {
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(Unit) {
        systemUiController.setSystemBarsColor(color = Color.White, darkIcons = true)
    }

    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showExportDialog by remember { mutableStateOf(false) }

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
                    onClick = {
                        scope.launch {
                            val cacheInfo = CacheManager.getCacheDetails(context)
                            val totalSizeStr = CacheManager.formatFileSize(cacheInfo.totalSize)

                            val success = CacheManager.clearCache(context)
                            if (success) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(
                                    context,
                                    "Кэш очищен! Освобождено: $totalSizeStr",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "Ошибка очистки кэша", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ClickableSettingItem(
                    title = "Экспорт данных",
                    subtitle = "Сохранить данные в файл",
                    onClick = { showExportDialog = true }
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

    if (showExportDialog) {
        ExportDataDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                scope.launch {
                    try {
                        val file = when (format) {
                            "CSV" -> ExportManager.exportToCSV(viewModel, context)
                            "JSON" -> ExportManager.exportToJSON(viewModel, context)
                            else -> null
                        }
                        file?.let {
                            ExportManager.shareFile(context, it)
                            Toast.makeText(context, "Данные экспортированы", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                showExportDialog = false
            }
        )
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
private fun ExportDataDialog(
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Экспорт данных") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Выберите формат для экспорта:")

                Card(
                    onClick = { onExport("CSV") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("CSV", fontWeight = FontWeight.Medium)
                            Text("Таблица для Excel", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                Card(
                    onClick = { onExport("JSON") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("JSON", fontWeight = FontWeight.Medium)
                            Text("Для разработчиков", fontSize = 12.sp, color = Color.Gray)
                        }
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
                checkedThumbColor = Color(0xFF000000),
                checkedTrackColor = Color(0xFF000000).copy(alpha = 0.5f)
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
                                selectedColor = Color(0xFF000000)
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
