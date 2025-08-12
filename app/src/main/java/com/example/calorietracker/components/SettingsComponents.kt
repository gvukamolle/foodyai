package com.example.calorietracker.components

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.presentation.viewmodels.CalorieTrackerViewModel
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.auth.SubscriptionPlan
import com.example.calorietracker.utils.capitalizeFirst
import kotlinx.coroutines.launch

/**
 * Объединенный файл всех компонентов настроек
 * Заменяет: SettingsComponents.kt, SettingsSections.kt
 */

// =========================================================================
// БАЗОВЫЕ КОМПОНЕНТЫ НАСТРОЕК
// =========================================================================

data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val badge: String? = null,
    val showArrow: Boolean = true,
    val onClick: () -> Unit = {}
)

@Composable
fun SettingsItemRow(
    item: SettingsItem,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontSize = 16.sp, color = Color.Black)
            item.subtitle?.let {
                Text(it, fontSize = 14.sp, color = Color.Gray)
            }
        }
        item.badge?.let { badge ->
            Surface(
                color = if (badge == "Скоро") Color(0xFFF5F5F5) else Color(0xFFE3F2FD),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color = if (badge == "Скоро") Color.Gray else Color(0xFF2196F3),
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (item.showArrow) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun DataActionItem(
    title: String, 
    subtitle: String, 
    icon: ImageVector, 
    iconColor: Color, 
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, 
            contentDescription = null, 
            tint = iconColor, 
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(subtitle, fontSize = 14.sp, color = Color.Gray)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos, 
            contentDescription = null, 
            tint = Color.Gray, 
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun StorageUsageItem(
    title: String, 
    size: String, 
    percentage: Float, 
    color: Color
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 16.sp)
            Text(size, fontSize = 16.sp, color = Color.Gray)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { percentage }, 
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape), 
            color = color
        )
    }
}

// =========================================================================
// НАСТРОЙКИ ПРИЛОЖЕНИЯ
// =========================================================================

@Composable
fun AppSettingsContent() {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var mealReminders by remember { mutableStateOf(true) }
    var language by remember { mutableStateOf("Русский") }
    var showLanguageDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SettingsSection(title = "Уведомления") {
                SettingsToggleItem(
                    title = "Push-уведомления",
                    subtitle = "Получать уведомления о приемах пищи",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
                
                SettingsToggleItem(
                    title = "Напоминания о еде",
                    subtitle = "Напоминать о времени приема пищи",
                    checked = mealReminders,
                    onCheckedChange = { mealReminders = it },
                    enabled = notificationsEnabled
                )
            }
        }

        item {
            SettingsSection(title = "Язык и регион") {
                SettingsClickableItem(
                    title = "Язык приложения",
                    subtitle = language,
                    onClick = { showLanguageDialog = true }
                )
            }
        }

        item {
            SettingsSection(title = "Данные") {
                DataActionItem(
                    title = "Экспорт данных",
                    subtitle = "Сохранить данные в файл",
                    icon = Icons.Default.Download,
                    iconColor = Color(0xFF2196F3),
                    onClick = { /* TODO */ }
                )
                
                DataActionItem(
                    title = "Импорт данных",
                    subtitle = "Загрузить данные из файла",
                    icon = Icons.Default.Upload,
                    iconColor = Color(0xFF4CAF50),
                    onClick = { /* TODO */ }
                )
                
                DataActionItem(
                    title = "Очистить кэш",
                    subtitle = "Освободить место на устройстве",
                    icon = Icons.Default.CleaningServices,
                    iconColor = Color(0xFFFF9800),
                    onClick = { /* TODO */ }
                )
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = language,
            onLanguageSelected = { language = it },
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                fontSize = 16.sp, 
                color = if (enabled) Color.Black else Color.Gray
            )
            Text(
                subtitle, 
                fontSize = 14.sp, 
                color = Color.Gray
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsClickableItem(
    title: String,
    subtitle: String,
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
            Text(subtitle, fontSize = 14.sp, color = Color.Gray)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
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
    val languages = listOf("Русский", "English", "Español", "Français")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите язык") },
        text = {
            LazyColumn {
                items(languages) { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLanguageSelected(language)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == currentLanguage,
                            onClick = {
                                onLanguageSelected(language)
                                onDismiss()
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(language)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

// =========================================================================
// КОМПОНЕНТЫ ПОДПИСОК
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    currentPlan: SubscriptionPlan,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPlan by remember { mutableStateOf(currentPlan) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(), 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Выберите подходящий план", 
                    fontSize = 24.sp, 
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Разблокируйте все возможности AI", 
                    fontSize = 16.sp, 
                    color = Color.Gray
                )
            }
        }
        
        items(SubscriptionPlan.entries.toTypedArray()) { plan ->
            SubscriptionPlanCard(
                plan = plan,
                isSelected = selectedPlan == plan,
                isCurrent = currentPlan == plan,
                onSelect = { selectedPlan = plan }
            )
        }
        
        item {
            Button(
                onClick = { onSelectPlan(selectedPlan) },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedPlan != currentPlan,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Text(
                    if (selectedPlan.ordinal > currentPlan.ordinal) "Обновить план" else "Изменить план",
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun SubscriptionPlanCard(
    plan: SubscriptionPlan, 
    isSelected: Boolean, 
    isCurrent: Boolean, 
    onSelect: () -> Unit
) {
    val (price, period) = when (plan) {
        SubscriptionPlan.FREE -> "0₽" to "навсегда"
        SubscriptionPlan.PRO -> "399₽" to "в месяц"
    }

    val borderColor = when {
        isCurrent -> Color(0xFF4CAF50)
        isSelected -> Color.Black
        else -> Color(0xFFE0E0E0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFF5F5F5) else Color.White
        ),
        border = BorderStroke(2.dp, borderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        plan.displayName, 
                        fontSize = 24.sp, 
                        fontWeight = FontWeight.Bold
                    )
                    if (isCurrent) {
                        Surface(
                            color = Color(0xFF4CAF50), 
                            shape = RoundedCornerShape(4.dp), 
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                "Текущий план", 
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), 
                                fontSize = 12.sp, 
                                color = Color.White
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(price, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text(period, fontSize = 14.sp, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            plan.features.forEach { feature ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp), 
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check, 
                        contentDescription = null, 
                        tint = Color(0xFF4CAF50), 
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(feature, fontSize = 15.sp)
                }
            }
        }
    }
}

// =========================================================================
// ДОПОЛНИТЕЛЬНЫЕ КОМПОНЕНТЫ
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.FeedbackTypeChip(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        modifier = Modifier.weight(1f),
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = { 
            Icon(
                icon, 
                contentDescription = null, 
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            ) 
        }
    )
}

@Composable
fun ContactItem(
    icon: ImageVector, 
    text: String, 
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray)
        Spacer(Modifier.width(16.dp))
        Text(text, color = Color.Black)
    }
}

@Composable
fun TeamMemberItem(
    name: String, 
    role: String, 
    avatar: String
) {
    Row(
        modifier = Modifier.padding(16.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.LightGray), 
            contentAlignment = Alignment.Center
        ) {
            Text(avatar, fontWeight = FontWeight.Bold, color = Color.Black)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(name, fontWeight = FontWeight.Bold)
            Text(role, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun AchievementItem(
    icon: ImageVector, 
    title: String, 
    subtitle: String
) {
    Row(
        modifier = Modifier.padding(16.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, 
            contentDescription = null, 
            modifier = Modifier.size(24.dp), 
            tint = Color.Gray
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun SocialMediaItem(
    platform: String, 
    handle: String, 
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (platform) {
            "Instagram" -> Icons.Default.PhotoCamera
            "Telegram" -> Icons.AutoMirrored.Filled.Send
            "YouTube" -> Icons.Default.PlayArrow
            else -> Icons.AutoMirrored.Filled.Launch
        }
        Icon(
            icon, 
            contentDescription = platform, 
            tint = Color.Black, 
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(platform, fontWeight = FontWeight.Medium)
            Text(handle, color = Color(0xFF2196F3))
        }
        Icon(
            Icons.AutoMirrored.Filled.Launch, 
            contentDescription = "Open", 
            tint = Color.Gray, 
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun ValueCard(
    icon: ImageVector, 
    title: String, 
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon, 
            contentDescription = null, 
            modifier = Modifier
                .padding(top = 4.dp)
                .size(24.dp), 
            tint = Color.Black
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                description, 
                color = Color.Gray, 
                fontSize = 16.sp, 
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun CommitmentItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle, 
            contentDescription = null, 
            tint = Color(0xFF4CAF50)
        )
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 16.sp)
    }
}

@Composable
fun AppPreviewCard(
    title: String, 
    subtitle: String, 
    description: String, 
    icon: ImageVector, 
    accentColor: Color, 
    launchDate: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = accentColor, 
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(subtitle, color = Color.Gray, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(description, fontSize = 16.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp), 
                color = accentColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = launchDate,
                    color = accentColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}