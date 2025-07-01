package com.example.calorietracker.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlusDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDescribeClick: () -> Unit,
    onManualClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(animationSpec = tween(150)) + scaleIn(
            transformOrigin = TransformOrigin(1f, 0f),
            animationSpec = tween(150)
        ),
        exit = fadeOut(animationSpec = tween(150)) + scaleOut(
            transformOrigin = TransformOrigin(1f, 0f),
            animationSpec = tween(150)
        )
    ) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = onDismissRequest,
            offset = DpOffset(x = (30).dp, y = 30.dp),
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFF6F6F6))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
        ) {
            FancyMenuItem("Отправить фото", Icons.Default.CameraAlt) {
                onDismissRequest(); onCameraClick()
            }
            FancyMenuItem("Загрузить фото", Icons.Default.Photo) {
                onDismissRequest(); onGalleryClick()
            }
            FancyMenuItem("Рассказать", Icons.Default.Chat) {
                onDismissRequest(); onDescribeClick()
            }
            FancyMenuItem("Ввести вручную", Icons.Default.Edit) {
                onDismissRequest(); onManualClick()
            }
        }
    }
}

@Composable
private fun FancyMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium) },
        onClick = onClick,
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}
