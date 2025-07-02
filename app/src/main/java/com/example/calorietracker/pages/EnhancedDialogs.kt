package com.example.calorietracker.pages

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.drawToBitmap
import com.example.calorietracker.ui.animations.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// Цветовая схема для диалогов
object DialogColors {
    val ManualInput = Color(0xFF9C27B0) // Фиолетовый
    val AIAnalysis = Color(0xFFFF9800) // Оранжевый
    val Photo = Color(0xFF4CAF50) // Зеленый
    val Gallery = Color(0xFF2196F3) // Синий
}

// Улучшенный диалог ручного ввода с анимациями
@Composable
fun EnhancedManualInputDialog(
    initialData: ManualInputData? = null,
    onDismiss: () -> Unit,
    onConfirm: (ManualInputData) -> Unit
) {
    var data by remember { mutableStateOf(initialData ?: ManualInputData()) }
    val isFromAI = initialData != null

    AnimatedDialogContainer(
        onDismiss = onDismiss,
        accentColor = DialogColors.ManualInput
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок с иконкой
            DialogHeader(
                icon = Icons.Default.Restaurant,
                title = if (isFromAI) "Проверьте данные от AI" else "Добавить продукт",
                subtitle = if (isFromAI) "AI распознал продукт" else "Заполните информацию",
                accentColor = DialogColors.ManualInput
            )

            // Поля ввода с AI анимацией заполнения
            AIAnimatedInputFields(
                data = data,
                onDataChange = { data = it },
                isFromAI = isFromAI
            )

            // Анимированная сводка
            AnimatedVisibility(
                visible = data.weight.isNotBlank() && data.weight != "100",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AnimatedNutritionSummary(data = data)
            }

            // Кнопки действий
            DialogActions(
                onCancel = onDismiss,
                onConfirm = { onConfirm(data) },
                confirmEnabled = data.isValid(),
                confirmText = if (isFromAI) "Подтвердить" else "Добавить",
                accentColor = DialogColors.ManualInput
            )
        }
    }
}

// Улучшенный диалог описания с AI анимацией
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun EnhancedDescribeDialog(
    onDismiss: () -> Unit,
    onAnalyze: (String) -> Unit,
    isAnalyzing: Boolean
) {
    var text by remember { mutableStateOf("") }
    var showHint by remember { mutableStateOf(true) }

    AnimatedDialogContainer(
        onDismiss = { if (!isAnalyzing) onDismiss() },
        accentColor = DialogColors.AIAnalysis
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DialogHeader(
                icon = Icons.Default.AutoAwesome,
                title = "Расскажите о блюде",
                subtitle = "AI распознает и посчитает КБЖУ",
                accentColor = DialogColors.AIAnalysis
            )

            AnimatedContent(
                targetState = isAnalyzing,
                transitionSpec = {
                    fadeIn() with fadeOut()
                }
            ) { analyzing ->
                if (analyzing) {
                    AIAnalyzingView()
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AnimatedTextField(
                            value = text,
                            onValueChange = {
                                text = it
                                if (it.isNotEmpty()) showHint = false
                            },
                            placeholder = "Например: Овсянка с бананом и орехами",
                            accentColor = DialogColors.AIAnalysis
                        )

                        AnimatedVisibility(
                            visible = showHint && text.isEmpty(),
                            enter = fadeIn() + scaleIn(initialScale = 0.95f),
                            exit = fadeOut() + scaleOut(targetScale = 0.95f)
                        ) {
                            HintCard(
                                text = "Опишите подробно: ингредиенты, способ приготовления, размер порции",
                                icon = Icons.Default.Lightbulb,
                                backgroundColor = Color(0xFFFFF3E0),
                                textColor = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }

            if (!isAnalyzing) {
                DialogActions(
                    onCancel = onDismiss,
                    onConfirm = { onAnalyze(text) },
                    confirmEnabled = text.isNotBlank(),
                    confirmText = "Отправить AI",
                    accentColor = DialogColors.AIAnalysis
                )
            }
        }
    }
}

// Компонент для AI анализа с красивой анимацией
@Composable
private fun AIAnalyzingView() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Анимированный логотип AI
        AIAnimatedLogo()

        // Анимированный текст состояния
        EnhancedAILoadingIndicator(
            text = "AI анализирует",
            accentColor = DialogColors.AIAnalysis
        )
        // Прогресс-индикатор
        AnimatedLoadingDots(color = DialogColors.AIAnalysis)
    }
}

// Анимированный логотип AI
@Composable
internal fun AIAnimatedLogo() {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_logo")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
            .background(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        DialogColors.AIAnalysis.copy(alpha = 0.3f),
                        DialogColors.AIAnalysis,
                        DialogColors.AIAnalysis.copy(alpha = 0.3f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = Color.White
        )
    }
}

// Базовые компоненты для диалогов
@Composable
internal fun AnimatedDialogContainer(
    onDismiss: () -> Unit,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    AnimatedPopup(onDismissRequest = onDismiss) { animatedDismiss ->
        Card(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fancyShadow(
                    shadowRadius = 8.dp,
                    alpha = 0.3f,
                    color = accentColor
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                accentColor.copy(alpha = 0.03f)
                            )
                        )
                    )
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun DialogHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.95f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// Модель данных для ручного ввода
data class ManualInputData(
    val name: String = "",
    val caloriesPer100g: String = "",
    val proteinsPer100g: String = "",
    val fatsPer100g: String = "",
    val carbsPer100g: String = "",
    val weight: String = "100"
) {
    fun isValid() = name.isNotBlank() && caloriesPer100g.isNotBlank()

    val totalCalories: Int
        get() = ((caloriesPer100g.toFloatOrNull() ?: 0f) * (weight.toFloatOrNull() ?: 100f) / 100).toInt()

    val totalProteins: Int
        get() = ((proteinsPer100g.toFloatOrNull() ?: 0f) * (weight.toFloatOrNull() ?: 100f) / 100).toInt()

    val totalFats: Int
        get() = ((fatsPer100g.toFloatOrNull() ?: 0f) * (weight.toFloatOrNull() ?: 100f) / 100).toInt()

    val totalCarbs: Int
        get() = ((carbsPer100g.toFloatOrNull() ?: 0f) * (weight.toFloatOrNull() ?: 100f) / 100).toInt()
}
