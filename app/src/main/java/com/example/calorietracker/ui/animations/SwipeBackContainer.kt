package com.example.calorietracker.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue      // для делегатов `by`
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer      // вместо layout-offset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeBackContainer(
    onSwipeBack: () -> Unit,
    previousContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val swipeOffset = remember { Animatable(0f) }
    var containerWidth by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { containerWidth = size.width.toFloat() },
                    onDragEnd = {
                        scope.launch {
                            if (swipeOffset.value > containerWidth * 0.3f) {
                                swipeOffset.animateTo(containerWidth, tween(200))
                                onSwipeBack()
                            }
                            swipeOffset.animateTo(0f, tween(200))
                        }
                    }
                ) { change, dragAmount ->
                    val newOffset =
                        (swipeOffset.value + dragAmount).coerceIn(0f, containerWidth)
                    scope.launch { swipeOffset.snapTo(newOffset) }
                    change.consume()
                }
            }
    ) {
        // нижний экран
        previousContent()

        // верхний экран, уезжает по оси X
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = swipeOffset.value } // px-трансляция
        ) {
            content()
        }
    }
}
