package com.example.calorietracker.pages

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ripple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale
import kotlin.math.roundToInt

// Обертка для всех диалогов с анимациями
@Composable
fun AnimatedDialogs(
    viewModel: CalorieTrackerViewModel,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    val context = LocalContext.current

    // Диалог выбора фото
    if (viewModel.showPhotoDialog) {
        EnhancedPhotoUploadDialog(
            onDismiss = { viewModel.showPhotoDialog = false },
            onCameraClick = {
                viewModel.showPhotoDialog = false
                onCameraClick()
            },
            onGalleryClick = {
                viewModel.showPhotoDialog = false
                onGalleryClick()
            }
        )
    }

    // Диалог ручного ввода с AI анимацией заполнения
    if (viewModel.showManualInputDialog) {
        val prefill = viewModel.prefillFood
        val initialData = if (prefill != null) {
            val weight = prefill.weight.toFloatOrNull() ?: 100f
            fun per100(value: Double): String {
                val per = (value * 100f / weight * 10f).roundToInt() / 10f
                return per.toString()
            }

            ManualInputData(
                name = prefill.name,
                caloriesPer100g = ((prefill.calories.toFloat() * 100f / weight).roundToInt()).toString(),
                proteinsPer100g = per100(prefill.protein),
                fatsPer100g = per100(prefill.fat),
                carbsPer100g = per100(prefill.carbs),
                weight = prefill.weight
            )
        } else null

        EnhancedManualInputDialog(
            initialData = initialData,
            onDismiss = {
                viewModel.showManualInputDialog = false
                viewModel.prefillFood = null
            },
            onConfirm = { data ->
                viewModel.handleManualInput(
                    name = data.name,
                    calories = String.format(Locale.US, "%.1f", data.totalCalories),
                    proteins = String.format(Locale.US, "%.1f", data.totalProteins),
                    fats = String.format(Locale.US, "%.1f", data.totalFats),
                    carbs = String.format(Locale.US, "%.1f", data.totalCarbs),
                    weight = data.weight
                )
                viewModel.showManualInputDialog = false
            }
        )
    }

    // Диалог описания блюда с улучшенным AI индикатором
    if (viewModel.showDescriptionDialog) {
        EnhancedDescribeDialog(
            onDismiss = {
                viewModel.showDescriptionDialog = false
                viewModel.pendingDescription = ""  // Очищаем сохраненное описание
            },
            onConfirm = { text ->
                viewModel.pendingDescription = text  // Сохраняем текст
                viewModel.showDescriptionDialog = false
                viewModel.analyzeDescription()  // Отправляем запрос на анализ
            },
            initialText = viewModel.pendingDescription  // Восстанавливаем текст при повторном открытии
        )
    }

    // Отключаем диалог подтверждения фото - теперь фото прикрепляются к сообщению
    /*
    if (viewModel.showPhotoConfirmDialog) {
        viewModel.pendingPhoto?.let { bitmap ->
            EnhancedPhotoConfirmDialog(
                bitmap = bitmap,
                caption = viewModel.photoCaption,
                onCaptionChange = { viewModel.photoCaption = it },
                onConfirm = {
                    viewModel.confirmPhoto()
                },
                onDismiss = {
                    viewModel.showPhotoConfirmDialog = false
                    viewModel.pendingPhoto = null
                    viewModel.photoCaption = ""
                }
            )
        }
    }
    */
}

@Composable
fun EnhancedPhotoUploadDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    AnimatedDialogContainer(
        onDismiss = onDismiss,
        accentColor = DialogColors.Photo
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            DialogHeader(
                icon = Icons.Default.CameraAlt,
                title = "Добавить фото",
                subtitle = "AI распознает продукт",
                accentColor = DialogColors.Photo
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PhotoOptionCard(
                    icon = Icons.Default.PhotoCamera,
                    title = "Сделать фото",
                    subtitle = "Используйте камеру",
                    color = DialogColors.Photo,
                    onClick = onCameraClick
                )

                PhotoOptionCard(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Выбрать из галереи",
                    subtitle = "Загрузите готовое фото",
                    color = DialogColors.Gallery,
                    onClick = onGalleryClick
                )
            }

            HintCard(
                text = "Совет: Сфотографируйте этикетку с составом для точного анализа",
                icon = Icons.Default.Info,
                backgroundColor = Color(0xFFE8F5E9),
                textColor = Color(0xFF4CAF50)
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "Отмена",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// Карточка опции фото (private, так как используется только здесь)
@Composable
private fun PhotoOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = color)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = color.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            Icon(
                Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}