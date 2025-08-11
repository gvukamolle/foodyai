package com.example.calorietracker.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp

/* -------------------------------------------------------------------------- */
/*                              Сообщение в чате                              */
/* -------------------------------------------------------------------------- */

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedMessage(
    id: Any,
    playAnimation: Boolean,
    startDelay: Long = 0L,
    modifier: Modifier = Modifier,
    onAnimationStart: () -> Unit = {},
    content: @Composable () -> Unit
) {
    // 1. Запоминаем, нужно ли анимировать именно ЭТОТ экземпляр компонента.
    //    Это значение захватится при первой композиции и больше не изменится.
    val isAnimationTriggered by remember { mutableStateOf(playAnimation) }

    // 2. Состояние видимости. Изначально невидимо, если анимация запускается.
    var show by remember { mutableStateOf(!isAnimationTriggered) }

    // 3. LaunchedEffect запускается ОДИН РАЗ при входе компонента в композицию.
    LaunchedEffect(Unit) {
        // Если была команда на анимацию, выполняем ее.
        if (isAnimationTriggered) {
            delay(100) // Небольшая задержка для плавности
            if (startDelay > 0) delay(startDelay)
            show = true // Запускаем саму анимацию (меняем целевое значение)
            onAnimationStart() // Сообщаем ViewModel, что анимация для этого сообщения проиграна
        }
    }

    // 4. Спецификация анимации зависит от того, была ли она запущена изначально.
    val animationSpec = if (isAnimationTriggered) {
        tween<Float>(durationMillis = 400, easing = FastOutSlowInEasing)
    } else {
        snap()
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = animationSpec,
        label = "alpha"
    )

    val animatedBlur by animateDpAsState(
        targetValue = if (show) 0.dp else 8.dp,
        animationSpec = if (isAnimationTriggered) {
            tween(durationMillis = 400, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "blur"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = animatedAlpha
            }
            .blur(radius = animatedBlur)
    ) {
        content()
    }
}

// Добавьте эту функцию в AnimationUtils.kt

/**
 * Компонент для анимированного удаления сообщения
 * Анимация обратная появлению - исчезновение с размытием
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedMessageRemoval(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            )
        ) + expandVertically(
            animationSpec = tween(
                durationMillis = 400,
                easing = FastOutSlowInEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        ) + shrinkVertically(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        ) + scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        ),
        modifier = modifier
    ) {
        content()
    }
}

// Альтернативный вариант с blur эффектом
@Composable
fun AnimatedMessageWithBlur(
    id: Any,
    isVisible: Boolean,
    playAnimation: Boolean,
    startDelay: Long = 0L,
    modifier: Modifier = Modifier,
    onAnimationStart: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var show by remember(id) { mutableStateOf(!playAnimation) }
    var blur by remember(id) { mutableStateOf(if (playAnimation) 8.dp else 0.dp) }

    // Анимация появления
    LaunchedEffect(id, isVisible) {
        if (playAnimation && isVisible) {
            delay(100)
            if (startDelay > 0) delay(startDelay)
            show = true
            blur = 0.dp
            onAnimationStart()
        } else if (!isVisible) {
            // Анимация исчезновения
            blur = 8.dp
            delay(300)
            show = false
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (show && isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )

    val animatedBlur by animateDpAsState(
        targetValue = blur,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "blur"
    )

    if (show || animatedAlpha > 0.01f) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    alpha = animatedAlpha
                }
                .blur(radius = animatedBlur)
        ) {
            content()
        }
    }
}

@Composable
fun Modifier.pulsate(): Modifier {
    val infinite = rememberInfiniteTransition(label = "pulsate-transition")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue  = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsate-scale"
    )
    return this.then(
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}