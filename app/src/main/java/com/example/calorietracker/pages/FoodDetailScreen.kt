package com.example.calorietracker.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.calorietracker.FoodItem
import com.example.calorietracker.utils.NutritionFormatter

@Composable
fun FoodDetailScreen(
    food: FoodItem,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    AnimatedDialogContainer(
        onDismiss = onDismiss,
        accentColor = DialogColors.Photo
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Название продукта
            Text(
                text = food.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.Black,
                textAlign = TextAlign.Center
            )

            // Вес
            Text(
                text = "${food.weight} г",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // КБЖУ
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MacroInfo(
                        value = food.calories.toString(),
                        label = "ккал",
                        color = Color.Black,
                        isMain = true
                    )
                    
                    VerticalDivider(
                        modifier = Modifier.height(40.dp),
                        color = Color(0xFFE0E0E0)
                    )
                    
                    MacroInfo(
                        value = NutritionFormatter.formatMacro(food.protein.toFloat()),
                        label = "белки",
                        color = Color.Gray
                    )
                    
                    VerticalDivider(
                        modifier = Modifier.height(40.dp),
                        color = Color(0xFFE0E0E0)
                    )
                    
                    MacroInfo(
                        value = NutritionFormatter.formatMacro(food.fat.toFloat()),
                        label = "жиры",
                        color = Color.Gray
                    )
                    
                    VerticalDivider(
                        modifier = Modifier.height(40.dp),
                        color = Color(0xFFE0E0E0)
                    )
                    
                    MacroInfo(
                        value = NutritionFormatter.formatMacro(food.carbs.toFloat()),
                        label = "углеводы",
                        color = Color.Gray
                    )
                }
            }

            // AI мнение, если есть
            if (food.aiOpinion != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = DialogColors.Photo.copy(alpha = 0.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = DialogColors.Photo,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Анализ от Foody AI",
                                style = MaterialTheme.typography.titleMedium,
                                color = DialogColors.Photo,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = food.aiOpinion,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp
                            ),
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Кнопка редактирования
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEdit()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Редактировать",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Кнопка удаления
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Удалить",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroInfo(
    value: String,
    label: String,
    color: Color,
    isMain: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = if (isMain) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun VerticalDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray
) {
    Box(
        modifier = modifier
            .width(1.dp)
            .background(color)
    )
}