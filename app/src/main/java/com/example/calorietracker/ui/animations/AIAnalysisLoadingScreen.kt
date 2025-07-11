package com.example.calorietracker.ui.animations

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.delay

/**
 * Универсальный контейнер, который реализует эффект размытия фона в новом окне.
 */
@Composable
private fun FullscreenEffectContainer(
    onDismiss: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusManager.clearFocus()
        delay(100)
        try {
            backgroundBitmap = view.drawToBitmap()
        } catch (e: Exception) { /* Игнорируем */ }
        isVisible = true
    }

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                backgroundBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Blurred background",
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(radius = 20.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.7f))
                )
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(300, delayMillis = 50)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                content()
            }
        }
    }
}


/**
 * Полноэкранный компонент загрузки для AI анализа
 */
@Composable
fun AIAnalysisLoadingScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    showDismissButton: Boolean = false
) {
    FullscreenEffectContainer(onDismiss = onDismiss) {
        // Основной контент
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = 16.dp, top = 24.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Верхний отступ, чтобы центрировать кольцо
            Spacer(modifier = Modifier.weight(1f))

            // Центральная часть - кольцо загрузки
            AILoadingRing()

            // Нижняя часть - анимированный текст и кнопка
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom // Прижимаем все к низу
            ) {
                // Контейнер для фраз, чтобы они не прыгали по ширине
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp), // Даем высоту, чтобы текст не прыгал
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedPhrases()
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопка отмены
                if (showDismissButton) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 32.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            // Полупрозрачный фон для кнопки
                            containerColor = Color.Black.copy(alpha = 0.1f),
                            contentColor = Color.Black.copy(alpha = 0.8f)
                        )
                    ) {
                        Text("Отмена", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


/**
 * Анимированное кольцо загрузки (без изменений)
 */
@Composable
private fun AILoadingRing() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        CircularProgressIndicator(
            modifier = Modifier
                .size(80.dp)
                .rotate(rotation),
            color = Color.Black,
            strokeWidth = 4.dp,
            trackColor = Color.Black.copy(alpha = 0.2f)
        )

        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = Color.Black
        )
    }
}

/**
 * Компонент с анимированными забавными фразами
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedPhrases() {
    val phrases = remember {
        listOf(
            "Дайте подумать... 🤔",
            "Так, это похоже на еду... 🍽️",
            "Считаю калории по пикселям... 📸",
            "Мне кажется это съедобно... 🧐",
            "Активирую нейросети... 🧠",
            "Анализирую молекулярный состав... 🔬",
            "Это точно не торт? 🎂",
            "Проверяю базу данных вкусняшек... 📚",
            "Хм, выглядит аппетитно... 😋",
            "Применяю магию подсчета КБЖУ... ✨",
            "Сканирую на предмет белков... 🥩",
            "Ищу спрятанные углеводы... 🍞",
            "Жиры, покажитесь! 🧈",
            "Почти готово, еще чуть-чуть... ⏳",
            "AI в замешательстве... 🤖"
        )
    }

    var currentPhraseIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            currentPhraseIndex = (currentPhraseIndex + 1) % phrases.size
        }
    }

    // Анимация смены фраз
    AnimatedContent(
        targetState = currentPhraseIndex,
        transitionSpec = {
            // Анимация: въезд строго снизу и выезд вниз
            (fadeIn(animationSpec = tween(400)) +
                    slideInVertically(animationSpec = tween(400)) { fullHeight -> fullHeight }) with
                    (fadeOut(animationSpec = tween(400)) +
                            slideOutVertically(animationSpec = tween(400)) { fullHeight -> -fullHeight })
        },
        label = "phrase_animation"
    ) { index ->
        // Текст без фона, с жирным начертанием для читаемости
        Text(
            text = phrases[index],
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold // Делаем жирнее
            ),
            textAlign = TextAlign.Center,
            color = Color.Black.copy(alpha = 0.8f) // Темный цвет для контраста
        )
    }
}