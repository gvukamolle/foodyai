package com.example.calorietracker.ui.animations

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateColorAsState as composeAnimateColorAsState


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedMessage(
    visible: Boolean,
    isUserMessage: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        initiallyVisible = false,
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
            transformOrigin = if (isUserMessage) {
                TransformOrigin(1f, 0.5f)
            } else {
                TransformOrigin(0f, 0.5f)
            }
        ),
        exit = fadeOut(animationSpec = tween(150))
    ) {
        content()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun AnimatedSendButton(
    modifier: Modifier = Modifier,
    hasText: Boolean,
    content: @Composable () -> Unit // или @Composable (Boolean) -> Unit
) {
    AnimatedContent(
        targetState = hasText,
        modifier = modifier,
        transitionSpec = {
            if (targetState) {
                (slideInVertically { height -> height } + fadeIn()) with
                        (slideOutVertically { height -> -height } + fadeOut())
            } else {
                (slideInVertically { height -> -height } + fadeIn()) with
                        (slideOutVertically { height -> height } + fadeOut())
            }.using(
                SizeTransform(clip = false)
            )
        }
    ) {
        content()
        // или content(it), если сигнатура с аргументом
    }
}

@Composable
fun Modifier.pulsate(): Modifier {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    return this.then(
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}
