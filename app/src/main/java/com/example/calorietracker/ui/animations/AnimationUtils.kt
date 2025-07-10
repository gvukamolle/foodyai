package com.example.calorietracker.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/* -------------------------------------------------------------------------- */
/*                              Сообщение в чате                              */
/* -------------------------------------------------------------------------- */

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedMessage(
    visible: Boolean,
    isUserMessage: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit  // Упрощаем - убираем AnimatedVisibilityScope
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInHorizontally(
            initialOffsetX = { if (isUserMessage) it else -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn() + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            transformOrigin = if (isUserMessage)
                TransformOrigin(1f, 0.5f)
            else
                TransformOrigin(0f, 0.5f)
        ),
        exit = fadeOut(animationSpec = tween(150))
    ) {
        content()
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