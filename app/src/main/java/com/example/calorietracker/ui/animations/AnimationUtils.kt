package com.example.calorietracker.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay

/* -------------------------------------------------------------------------- */
/*                              Сообщение в чате                              */
/* -------------------------------------------------------------------------- */

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedMessage(
    visible: Boolean,
    isUserMessage: Boolean,
    startDelay: Long = 0L,
    modifier: Modifier = Modifier,
    onDisplayed: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            if (startDelay > 0) delay(startDelay)
            show = true
            onDisplayed()
        } else {
            show = false
        }
    }
    AnimatedVisibility(
        visible = show,
        modifier = modifier,
        enter =
            slideInHorizontally(
                initialOffsetX = { if (isUserMessage) it else -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) +
                    expandHorizontally(
                        expandFrom = if (isUserMessage) Alignment.End else Alignment.Start,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) +
                    fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(150)) +
                shrinkHorizontally(
                    shrinkTowards = if (isUserMessage) Alignment.End else Alignment.Start,
                    animationSpec = tween(150)
                )
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