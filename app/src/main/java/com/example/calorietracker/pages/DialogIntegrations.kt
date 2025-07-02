package com.example.calorietracker.pages

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.FoodItem

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

    // Диалог ручного ввода
    if (viewModel.showManualInputDialog) {
        val prefill = viewModel.prefillFood
        val initialData = if (prefill != null) {
            ManualInputData(
                name = prefill.name,
                caloriesPer100g = prefill.calories.toString(),
                proteinsPer100g = prefill.proteins.toString(),
                fatsPer100g = prefill.fats.toString(),
                carbsPer100g = prefill.carbs.toString(),
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
                    calories = data.totalCalories.toString(),
                    proteins = data.totalProteins.toString(),
                    fats = data.totalFats.toString(),
                    carbs = data.totalCarbs.toString(),
                    weight = data.weight
                )
                viewModel.showManualInputDialog = false
            }
        )
    }

    // Диалог описания блюда
    if (viewModel.showDescriptionDialog) {
        EnhancedDescribeDialog(
            onDismiss = {
                viewModel.showDescriptionDialog = false
            },
            onAnalyze = { text ->
                viewModel.analyzeDescription(text)
            },
            isAnalyzing = viewModel.isAnalyzing
        )
    }

    // Диалог подтверждения фото
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
}

// Улучшенный диалог загрузки фото
@Composable
fun EnhancedPhotoUploadDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)
        showContent = true
    }

    AnimatedDialogContainer(
        onDismiss = onDismiss,
        accentColor = DialogColors.Photo
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = showContent,
            enter = androidx.compose.animation.fadeIn() +
                    androidx.compose.animation.scaleIn(initialScale = 0.95f)
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier.padding(24.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp)
            ) {
                DialogHeader(
                    icon = androidx.compose.material.icons.Icons.Default.CameraAlt,
                    title = "Добавить фото",
                    subtitle = "AI распознает продукт",
                    accentColor = DialogColors.Photo
                )

                androidx.compose.foundation.layout.Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    PhotoOptionCard(
                        icon = androidx.compose.material.icons.Icons.Default.PhotoCamera,
                        title = "Сделать фото",
                        subtitle = "Используйте камеру",
                        color = DialogColors.Photo,
                        onClick = onCameraClick,
                        delay = 100
                    )

                    PhotoOptionCard(
                        icon = androidx.compose.material.icons.Icons.Default.PhotoLibrary,
                        title = "Выбрать из галереи",
                        subtitle = "Загрузите готовое фото",
                        color = DialogColors.Gallery,
                        onClick = onGalleryClick,
                        delay = 200
                    )
                }

                HintCard(
                    text = "Совет: Сфотографируйте этикетку с составом для точного анализа",
                    icon = androidx.compose.material.icons.Icons.Default.Info,
                    backgroundColor = androidx.compose.ui.graphics.Color(0xFFE8F5E9),
                    textColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                )

                androidx.compose.material3.TextButton(
                    onClick = onDismiss,
                    modifier = androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally)
                ) {
                    androidx.compose.material3.Text(
                        "Отмена",
                        color = androidx.compose.ui.graphics.Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

// Улучшенный диалог подтверждения фото
@Composable
fun EnhancedPhotoConfirmDialog(
    bitmap: Bitmap,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedDialogContainer(
        onDismiss = onDismiss,
        accentColor = DialogColors.Photo
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.material3.Text(
                "Отправить фото",
                fontSize = 20.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.Black
            )

            // Анимированное изображение
            var imageVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(100)
                imageVisible = true
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = imageVisible,
                enter = androidx.compose.animation.fadeIn() +
                        androidx.compose.animation.scaleIn(initialScale = 0.9f)
            ) {
                androidx.compose.material3.Card(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    androidx.compose.foundation.Image(
                        bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            AnimatedTextField(
                value = caption,
                onValueChange = onCaptionChange,
                placeholder = "Добавить подпись (необязательно)",
                icon = androidx.compose.material.icons.Icons.Default.Edit,
                accentColor = DialogColors.Photo,
                singleLine = true
            )

            DialogActions(
                onCancel = onDismiss,
                onConfirm = onConfirm,
                confirmEnabled = true,
                confirmText = "Отправить",
                accentColor = DialogColors.Photo
            )
        }
    }
}

// Анимированная карточка опции фото
@Composable
private fun PhotoOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    delay: Int
) {
    var visible by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delay.toLong())
        visible = true
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn() +
                androidx.compose.animation.scaleIn(initialScale = 0.95f)
    ) {
        androidx.compose.material3.Surface(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = androidx.compose.material.ripple.rememberRipple(color = color)
                ) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onClick()
                },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.08f)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier
                        .size(48.dp)
                        .background(
                            color = color.copy(alpha = 0.15f),
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = androidx.compose.ui.Modifier.size(24.dp)
                    )
                }
                androidx.compose.foundation.layout.Spacer(
                    androidx.compose.ui.Modifier.width(16.dp)
                )
                androidx.compose.foundation.layout.Column(
                    modifier = androidx.compose.ui.Modifier.weight(1f)
                ) {
                    androidx.compose.material3.Text(
                        title,
                        fontSize = 16.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = androidx.compose.ui.graphics.Color.Black
                    )
                    androidx.compose.material3.Text(
                        subtitle,
                        fontSize = 14.sp,
                        color = androidx.compose.ui.graphics.Color.Gray
                    )
                }
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Default.ArrowForwardIos,
                    contentDescription = null,
                    tint = color,
                    modifier = androidx.compose.ui.Modifier.size(16.dp)
                )
            }
        }
    }
}
