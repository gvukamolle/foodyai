package com.example.calorietracker.pages

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Настройки приложения
@Composable
fun AppSettingsContent() {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var mealReminders by remember { mutableStateOf(true) }
    var darkTheme by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("Русский") }
    var showLanguageDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsSectionCard(title = "Уведомления") {
                SwitchSettingItem(title = "Push-уведомления", subtitle = "Получать уведомления о приемах пищи", checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                if (notificationsEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SwitchSettingItem(title = "Напоминания о еде", subtitle = "Завтрак, обед, ужин", checked = mealReminders, onCheckedChange = { mealReminders = it })
                }
            }
        }
        item {
            SettingsSectionCard(title = "Внешний вид") {
                SwitchSettingItem(title = "Темная тема", subtitle = "Включить ночной режим", checked = darkTheme, onCheckedChange = { darkTheme = it })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ClickableSettingItem(title = "Язык", subtitle = language, onClick = { showLanguageDialog = true })
            }
        }
        item {
            SettingsSectionCard(title = "Конфиденциальность") {
                ClickableSettingItem(title = "Политика конфиденциальности", onClick = { /* Open privacy policy */ })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ClickableSettingItem(title = "Условия использования", onClick = { /* Open terms */ })
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Выбрать язык") },
            text = {
                Column {
                    listOf("Русский", "English", "Español", "Deutsch").forEach { lang ->
                        Row(modifier = Modifier.fillMaxWidth().clickable { language = lang; showLanguageDialog = false }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = language == lang, onClick = { language = lang; showLanguageDialog = false })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(lang)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

// Выгрузка данных
@Composable
fun DataExportContent() {
    val scope = rememberCoroutineScope()
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var isExporting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { InfoCard(icon = Icons.Default.Info, text = "Экспортируйте ваши данные в удобном формате для анализа или резервного копирования") }
        item {
            SettingsSectionCard(title = "Форматы экспорта") {
                ExportFormatItem(title = "CSV файл", subtitle = "Таблица для Excel, Google Sheets", icon = Icons.Default.TableChart, onClick = {
                    scope.launch { isExporting = true; for (i in 0..100 step 10) { exportProgress = i / 100f; kotlinx.coroutines.delay(100) }; isExporting = false }
                })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ExportFormatItem(title = "PDF отчет", subtitle = "Полный отчет с графиками", icon = Icons.Default.PictureAsPdf, badge = "Pro", onClick = { /* Export PDF */ })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ExportFormatItem(title = "JSON данные", subtitle = "Для разработчиков и интеграций", icon = Icons.Default.Code, badge = "Premium", onClick = { /* Export JSON */ })
            }
        }
        if (isExporting) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Экспортируем данные...", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(progress = { exportProgress }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${(exportProgress * 100).toInt()}%", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// Управление данными
@Composable
fun DataManagementContent() {
    var showClearDialog by remember { mutableStateOf(false) }
    val lastBackupDate by remember { mutableStateOf("15 января 2025, 14:30") }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SettingsSectionCard(title = "Резервное копирование") {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Автоматическое копирование", fontWeight = FontWeight.Medium)
                        Text("Последнее: $lastBackupDate", fontSize = 14.sp, color = Color.Gray)
                    }
                    Switch(checked = true, onCheckedChange = {})
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                TextButton(onClick = { /* Manual backup */ }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Создать резервную копию сейчас")
                }
            }
        }
        item {
            SettingsSectionCard(title = "Очистка данных") {
                DataActionItem(title = "Очистить историю питания", subtitle = "Удалит все записи о приемах пищи", icon = Icons.Default.History, iconColor = Color(0xFFFF9800), onClick = { showClearDialog = true })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                DataActionItem(title = "Очистить кэш изображений", subtitle = "Освободит 124 МБ", icon = Icons.Default.Image, iconColor = Color(0xFF2196F3), onClick = { /* Clear cache */ })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                DataActionItem(title = "Сбросить все настройки", subtitle = "Вернуть к заводским установкам", icon = Icons.Default.RestartAlt, iconColor = Color(0xFFF44336), onClick = { /* Reset settings */ })
            }
        }
        item {
            SettingsSectionCard(title = "Использование памяти") {
                StorageUsageItem(title = "База данных", size = "45 МБ", percentage = 0.35f, color = Color(0xFF4CAF50))
                StorageUsageItem(title = "Изображения", size = "124 МБ", percentage = 0.85f, color = Color(0xFF2196F3))
                StorageUsageItem(title = "Кэш", size = "12 МБ", percentage = 0.15f, color = Color(0xFFFF9800))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Всего", fontWeight = FontWeight.Medium)
                    Text("181 МБ", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(48.dp)) },
            title = { Text("Очистить историю?") },
            text = { Text("Все записи о приемах пищи будут удалены. Это действие нельзя отменить.", textAlign = TextAlign.Center) },
            confirmButton = { TextButton(onClick = { showClearDialog = false }, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF9800))) { Text("Очистить") } },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Отмена") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackContent() {
    var feedbackType by remember { mutableStateOf("") }
    var feedbackText by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Мы ценим ваше мнение!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Помогите нам сделать приложение лучше", fontSize = 16.sp, color = Color.Gray)
        }
        item {
            Text("Тип обращения", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
            Row(modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeedbackTypeChip(text = "Ошибка", icon = Icons.Default.BugReport, selected = feedbackType == "bug", onClick = { feedbackType = "bug" })
                FeedbackTypeChip(text = "Идея", icon = Icons.Default.Lightbulb, selected = feedbackType == "suggestion", onClick = { feedbackType = "suggestion" })
            }
            Row(modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeedbackTypeChip(text = "Вопрос", icon = Icons.Default.Help, selected = feedbackType == "question", onClick = { feedbackType = "question" })
                FeedbackTypeChip(text = "Хвалю", icon = Icons.Default.Favorite, selected = feedbackType == "thanks", onClick = { feedbackType = "thanks" })
            }
        }
        item { OutlinedTextField(value = feedbackText, onValueChange = { feedbackText = it }, label = { Text("Ваше сообщение") }, placeholder = { Text("Расскажите подробнее...") }, modifier = Modifier.fillMaxWidth().height(150.dp), maxLines = 6) }
        item { OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email для ответа (необязательно)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
        item {
            Button(onClick = { /* Send feedback */ }, modifier = Modifier.fillMaxWidth(), enabled = feedbackType.isNotEmpty() && feedbackText.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Отправить")
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Или свяжитесь с нами напрямую:", fontWeight = FontWeight.Medium)
                    ContactItem(icon = Icons.Default.Email, text = "support@foodyai.com", onClick = { /* Open email */ })
                    ContactItem(icon = Icons.Default.Send, text = "@foodyai_support (Telegram)", onClick = { /* Open Telegram */ })
                }
            }
        }
    }
}

@Composable
fun AboutContent() {
    val uriHandler = LocalUriHandler.current
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                val infiniteTransition = rememberInfiniteTransition(label = "logo-transition")
                val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.1f, animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse), label = "logo-scale")
                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(80.dp).graphicsLayer { scaleX = scale; scaleY = scale }, tint = Color.Black)
            }
        }
        item {
            Text("Foody AI", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text("Ваш персональный AI-диетолог", fontSize = 16.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        item {
            SettingsSectionCard(title = "Наша команда") {
                TeamMemberItem(name = "Александр Иванов", role = "CEO & Founder", avatar = "АИ")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                TeamMemberItem(name = "Мария Петрова", role = "Head of Nutrition", avatar = "МП")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                TeamMemberItem(name = "Дмитрий Сидоров", role = "Lead AI Engineer", avatar = "ДС")
            }
        }
        item {
            SettingsSectionCard(title = "Наши достижения") {
                AchievementItem(icon = Icons.Default.People, title = "100K+ пользователей", subtitle = "По всему миру")
                AchievementItem(icon = Icons.Default.Analytics, title = "5M+ проанализировано", subtitle = "Продуктов и блюд")
                AchievementItem(icon = Icons.Default.Star, title = "4.8 рейтинг", subtitle = "В App Store и Google Play")
            }
        }
        item {
            SettingsSectionCard(title = "Мы в социальных сетях") {
                SocialMediaItem(platform = "Instagram", handle = "@foodyai", onClick = { uriHandler.openUri("https://instagram.com/foodyai") })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SocialMediaItem(platform = "Telegram", handle = "@foodyai_channel", onClick = { uriHandler.openUri("https://t.me/foodyai_channel") })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SocialMediaItem(platform = "YouTube", handle = "Foody AI", onClick = { uriHandler.openUri("https://youtube.com/@foodyai") })
            }
        }
    }
}

@Composable
fun MissionContent() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Black), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Наша миссия", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Сделать здоровое питание простым и доступным для каждого с помощью искусственного интеллекта", fontSize = 18.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 28.sp)
                }
            }
        }
        item {
            Text("Наши ценности", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            ValueCard(icon = Icons.Default.Accessibility, title = "Доступность", description = "Мы верим, что здоровое питание должно быть доступно каждому, независимо от уровня знаний и опыта")
            Spacer(modifier = Modifier.height(12.dp))
            ValueCard(icon = Icons.Default.Psychology, title = "Инновации", description = "Используем передовые технологии AI для персонализированных рекомендаций")
            Spacer(modifier = Modifier.height(12.dp))
            ValueCard(icon = Icons.Default.Favorite, title = "Забота", description = "Ваше здоровье - наш приоритет. Мы заботимся о каждом пользователе")
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Видение будущего", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("К 2030 году мы стремимся стать глобальной платформой для управления здоровьем, объединяющей питание, физическую активность и ментальное благополучие.", fontSize = 16.sp, lineHeight = 24.sp, color = Color.Black.copy(alpha = 0.8f))
                }
            }
        }
        item {
            Text("Наши обязательства", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            CommitmentItem("Конфиденциальность ваших данных")
            CommitmentItem("Научно обоснованные рекомендации")
            CommitmentItem("Постоянное улучшение сервиса")
            CommitmentItem("Прозрачность в работе AI")
            CommitmentItem("Поддержка 24/7")
        }
    }
}

@Composable
fun OtherAppsContent() {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Экосистема здоровья", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Скоро запустим новые приложения для комплексной заботы о вашем здоровье", fontSize = 16.sp, color = Color.Gray)
        }
        item { AppPreviewCard(title = "Sporty AI", subtitle = "Персональный AI-тренер", description = "Индивидуальные программы тренировок, анализ техники выполнения упражнений, отслеживание прогресса", icon = Icons.Default.FitnessCenter, accentColor = Color(0xFF4CAF50), launchDate = "Запуск: Март 2025") }
        item { AppPreviewCard(title = "Mind AI", subtitle = "Ментальное здоровье", description = "Медитации, дыхательные практики, дневник настроения, AI-психолог для поддержки", icon = Icons.Default.SelfImprovement, accentColor = Color(0xFF2196F3), launchDate = "Запуск: Май 2025") }
        item { AppPreviewCard(title = "Woman AI", subtitle = "Женское здоровье", description = "Отслеживание цикла, рекомендации по питанию и тренировкам с учетом фаз, поддержка беременности", icon = Icons.Default.Female, accentColor = Color(0xFFE91E63), launchDate = "Запуск: Июль 2025") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Black), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Будьте первыми!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Получите ранний доступ и специальные предложения", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { /* Subscribe */ }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
                        Text("Подписаться на новости")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(0.dp), shape = RoundedCornerShape(12.dp)) {
            Column(content = content)
        }
    }
}

@Composable
private fun SwitchSettingItem(title: String, subtitle: String? = null, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            subtitle?.let { Text(it, fontSize = 14.sp, color = Color.Gray) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ClickableSettingItem(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            subtitle?.let { Text(it, fontSize = 14.sp, color = Color.Gray) }
        }
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun InfoCard(icon: ImageVector, text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, fontSize = 14.sp, color = Color(0xFF1976D2))
        }
    }
}

@Composable
private fun ExportFormatItem(title: String, subtitle: String, icon: ImageVector, badge: String? = null, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Black)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                badge?.let {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = Color(0xFFE3F2FD), shape = RoundedCornerShape(4.dp)) {
                        Text(it, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = Color(0xFF2196F3))
                    }
                }
            }
            Text(subtitle, fontSize = 14.sp, color = Color.Gray)
        }
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
    }
}