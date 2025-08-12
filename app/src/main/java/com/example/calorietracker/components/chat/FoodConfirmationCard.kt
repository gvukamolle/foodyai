package com.example.calorietracker.components.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.data.FoodItem

@Composable
fun FoodConfirmationCard(
    foodItem: FoodItem,
    onEdit: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
            // Основная карточка с данными
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 6.dp,
                    bottomEnd = 6.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3F4F6)
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Заголовок с названием блюда
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = foodItem.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Text(
                            text = "${foodItem.weight}г",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                    
                    // КБЖУ в виде сетки
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NutrientItem(
                            value = foodItem.calories.toString(),
                            label = "Калории",
                            modifier = Modifier.weight(1f),
                            isMain = true
                        )
                        
                        NutrientItem(
                            value = String.format("%.1f", foodItem.protein),
                            label = "Белки",
                            modifier = Modifier.weight(1f)
                        )
                        
                        NutrientItem(
                            value = String.format("%.1f", foodItem.fat),
                            label = "Жиры",
                            modifier = Modifier.weight(1f)
                        )
                        
                        NutrientItem(
                            value = String.format("%.1f", foodItem.carbs),
                            label = "Углеводы",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Кнопки действий как отдельные элементы
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Кнопка "Изменить"
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEdit()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFF3F4F6),
                        contentColor = Color(0xFF868686)
                    ),
                    border = null
                ) {
                    Text(
                        text = "Изменить",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                
                // Кнопка "Записать"
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(
                        topStart = 6.dp,
                        topEnd = 6.dp,
                        bottomStart = 6.dp,
                        bottomEnd = 20.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDBF0E4),
                        contentColor = Color(0xFF00BA65)
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text(
                        text = "Записать",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }


@Composable
private fun NutrientItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    isMain: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isMain) Color.Black else Color(0xFF868686),
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF9E9E9E),
            fontWeight = FontWeight.Medium
        )
    }
}
