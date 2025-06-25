package com.example.calorietracker.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.utils.DailyResetUtils
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.example.calorietracker.ui.animations.AnimatedMessage
import kotlinx.coroutines.launch
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp

@Composable
fun OnlineStatus(isOnline: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
            contentDescription = null,
            tint = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isOnline) "Online" else "Offline",
            fontSize = 12.sp,
            color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProgressSection(
    label: String,
    current: Int,
    target: Int,
    unit: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Black,
            modifier = Modifier.width(48.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(0xFFE5E7EB), RoundedCornerShape(4.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        if (target > 0) minOf(current.toFloat() / target.toFloat(), 1f) else 0f
                    )
                    .height(8.dp)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
        Text(
            text = "$current/$target$unit",
            fontSize = 12.sp,
            color = Color.Black,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun RingIndicator(label: String, current: Int, target: Int, color: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(60.dp)
    ) {
        CircularProgressIndicator(
            progress = 1f, // фоновая окружность
            color = Color(0xFFE5E7EB),
            strokeWidth = 8.dp,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxSize()
        )
        CircularProgressIndicator(
            progress = if (target > 0) current.toFloat() / target.toFloat() else 0f,
            color = color,
            strokeWidth = 8.dp,
            strokeCap = StrokeCap.Round, // Закругленные концы
            modifier = Modifier.fillMaxSize()
        )
        Text(text = label, fontSize = 16.sp, color = Color.Black)
    }
}

// Кастомный Divider с визуально закругленными краями
@Composable
fun RoundedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFE5E5E5),
    thickness: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal =0.dp) // отступы от краев экрана
            .height(thickness)
            .clip(RoundedCornerShape(thickness)) // закругление концов линии
            .background(color)
    )
}

@Composable
fun CollapsibleProgressBars(viewModel: CalorieTrackerViewModel) {
    var expanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable { expanded = !expanded }
    ) {
        if (expanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)) {
                ProgressSection(
                    label = "ККАЛ",
                    current = viewModel.dailyIntake.calories,
                    target = viewModel.userProfile.dailyCalories,
                    unit = "",
                    color = viewModel.getProgressColor(
                        viewModel.dailyIntake.calories,
                        viewModel.userProfile.dailyCalories
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                ProgressSection(
                    label = "Б",
                    current = viewModel.dailyIntake.proteins,
                    target = viewModel.userProfile.dailyProteins,
                    unit = "г",
                    color = viewModel.getProgressColor(
                        viewModel.dailyIntake.proteins,
                        viewModel.userProfile.dailyProteins
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                ProgressSection(
                    label = "Ж",
                    current = viewModel.dailyIntake.fats,
                    target = viewModel.userProfile.dailyFats,
                    unit = "г",
                    color = viewModel.getProgressColor(
                        viewModel.dailyIntake.fats,
                        viewModel.userProfile.dailyFats
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                ProgressSection(
                    label = "У",
                    current = viewModel.dailyIntake.carbs,
                    target = viewModel.userProfile.dailyCarbs,
                    unit = "г",
                    color = viewModel.getProgressColor(
                        viewModel.dailyIntake.carbs,
                        viewModel.userProfile.dailyCarbs
                    )
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RingIndicator(
                    label = "К",
                    current = viewModel.dailyIntake.calories,
                    target = viewModel.userProfile.dailyCalories,
                    color = viewModel.getProgressColor(
                        viewModel.dailyIntake.calories,
                        viewModel.userProfile.dailyCalories
                    )
                )
                RingIndicator(
                    label = "Б",
                    current = viewModel.dailyIntake.proteins,
                    target = viewModel.userProfile.dailyProteins,
                    color = viewModel.getProgressColor(
                        viewModel.dailyIntake.proteins,
                        viewModel.userProfile.dailyProteins
                    )
                )
                RingIndicator(
                    label = "Ж",
                    current = viewModel.dailyIntake.fats,
                    target = viewModel.userProfile.dailyFats,
                    color = viewModel.getProgressColor(
                        viewModel.dailyIntake.fats,
                        viewModel.userProfile.dailyFats
                    )
                )
                RingIndicator(
                    label = "У",
                    current = viewModel.dailyIntake.carbs,
                    target = viewModel.userProfile.dailyCarbs,
                    color = viewModel.getProgressColor(
                        viewModel.dailyIntake.carbs,
                        viewModel.userProfile.dailyCarbs
                    )
                )
            }
        }
    }
}

@Composable
fun ChatMessageCard(message: com.example.calorietracker.ChatMessage) {
    val alignment = if (message.type == com.example.calorietracker.MessageType.USER)
        Alignment.CenterEnd else Alignment.CenterStart
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.type == com.example.calorietracker.MessageType.USER)
                    Color.Black else Color(0xFFF3F4F6)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = message.content,
                color = if (message.type == com.example.calorietracker.MessageType.USER)
                    Color.White else Color.Black,
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun PendingFoodCard(
    food: com.example.calorietracker.FoodItem,
    selectedMeal: com.example.calorietracker.MealType,
    onMealChange: (com.example.calorietracker.MealType) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Подтвердите данные",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Блюдо: ${food.name}")
            Text(text = "Калории: ${food.calories}")
            Text(text = "Белки: ${food.proteins} г")
            Text(text = "Жиры: ${food.fats} г")
            Text(text = "Углеводы: ${food.carbs} г")
            Text(text = "Вес: ${food.weight} г")

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Приём пищи:",
                color = Color.Black,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            val firstRow = listOf(
                com.example.calorietracker.MealType.BREAKFAST,
                com.example.calorietracker.MealType.LUNCH,
                com.example.calorietracker.MealType.DINNER
            )
            val secondRow = listOf(
                com.example.calorietracker.MealType.LATE_BREAKFAST,
                com.example.calorietracker.MealType.SNACK,
                com.example.calorietracker.MealType.SUPPER
            )

            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    firstRow.forEach { meal ->
                        val isSelected = meal == selectedMeal
                        Button(
                            onClick = { onMealChange(meal) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color.Black else Color(0xFFE5E7EB),
                                contentColor = if (isSelected) Color.White else Color.Black
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(meal.displayName, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    secondRow.forEach { meal ->
                        val isSelected = meal == selectedMeal
                        Button(
                            onClick = { onMealChange(meal) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color.Black else Color(0xFFE5E7EB),
                                contentColor = if (isSelected) Color.White else Color.Black
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(meal.displayName, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Подтвердить")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Отмена")
                }
            }
        }
    }
}

@Composable
fun ManualFoodInputDialog(
    initialFoodName: String = "",
    initialCalories: String = "",
    initialProteins: String = "",
    initialFats: String = "",
    initialCarbs: String = "",
    initialWeight: String = "100",
    onDismiss: () -> Unit,
    onConfirm: (name: String, calories: String, proteins: String, fats: String, carbs: String, weight: String) -> Unit
) {
    var foodName by remember { mutableStateOf(initialFoodName) }
    var caloriesPer100g by remember { mutableStateOf(initialCalories) }
    var proteinsPer100g by remember { mutableStateOf(initialProteins) }
    var fatsPer100g by remember { mutableStateOf(initialFats) }
    var carbsPer100g by remember { mutableStateOf(initialCarbs) }
    var weight by remember { mutableStateOf(initialWeight) }

    val weightFloat = weight.toFloatOrNull() ?: 100f
    val totalCalories = ((caloriesPer100g.toFloatOrNull() ?: 0f) * weightFloat / 100).toInt()
    val totalProteins = ((proteinsPer100g.toFloatOrNull() ?: 0f) * weightFloat / 100).toInt()
    val totalFats = ((fatsPer100g.toFloatOrNull() ?: 0f) * weightFloat / 100).toInt()
    val totalCarbs = ((carbsPer100g.toFloatOrNull() ?: 0f) * weightFloat / 100).toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialFoodName.isNotEmpty()) "Проверьте данные" else "Добавить продукт",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Название продукта") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Вес порции (г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Пищевая ценность на 100г:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = caloriesPer100g,
                        onValueChange = { caloriesPer100g = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Ккал") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = proteinsPer100g,
                        onValueChange = { proteinsPer100g = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Белки") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fatsPer100g,
                        onValueChange = { fatsPer100g = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Жиры") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = carbsPer100g,
                        onValueChange = { carbsPer100g = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Углеводы") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (weight.isNotBlank() && weight != "100") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Итого на ${weight}г:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Калории: $totalCalories ккал | Белки: ${totalProteins}г | Жиры: ${totalFats}г | Углеводы: ${totalCarbs}г",
                                fontSize = 12.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (foodName.isNotBlank() && caloriesPer100g.isNotBlank()) {
                        onConfirm(
                            foodName,
                            totalCalories.toString(),
                            totalProteins.toString(),
                            totalFats.toString(),
                            totalCarbs.toString(),
                            weight
                        )
                        onDismiss()
                    }
                }
            ) {
                Text("Добавить", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color.Gray)
            }
        }
    )
}

@Composable
fun PhotoUploadDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Добавить фото продукта",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Рекомендуем загрузить фотографию этикетки с КБЖУ и составом",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCameraClick() },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3F4F6)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Сделать фото",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            Text(
                                text = "Сфотографируйте продукт или упаковку",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGalleryClick() },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3F4F6)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Выбрать из галереи",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            Text(
                                text = "Загрузите готовое фото",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color.Black)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatedMainScreen(
    viewModel: CalorieTrackerViewModel,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onManualClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.White,
            darkIcons = true
        )
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(viewModel.messages.size - 1)
            }
        }
    }

    // Используем Scaffold для правильной обработки клавиатуры
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(), // защита от системных баров
        containerColor = Color.White,
        bottomBar = {
            // ПОЛЕ ВВОДА В BOTTOM BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                var menuExpanded by remember { mutableStateOf(false) }

                // Серая линия сверху с закруглениями
                RoundedDivider(
                    color = Color(0xFFE5E5E5),
                    thickness = 1.dp
                )

                // Поле ввода
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(16.dp))

                    BasicTextField(
                        value = viewModel.inputMessage,
                        onValueChange = { viewModel.inputMessage = it },
                        singleLine = false,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences, // ← вот это включает автоматическую заглавную букву в начале предложения
                            keyboardType = KeyboardType.Text
                        ),
                        textStyle = TextStyle(
                            fontSize = 18.sp, // Увеличенный размер шрифта
                            color = Color.Black
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box {
                                if (viewModel.inputMessage.isEmpty()) {
                                    Text(
                                        text = if (viewModel.isOnline)
                                            "Спросите у AI-диетолога..."
                                        else
                                            "Задайте вопрос...",
                                        color = Color.Gray,
                                        fontSize = 18.sp // Тот же размер шрифта
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        AnimatedContent(
                            targetState = viewModel.inputMessage.isNotBlank(),
                            label = "SendPlus"
                        ) { hasText ->
                            if (hasText) {
                                IconButton(
                                    onClick = { viewModel.sendMessage() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Отправить",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .background(Color.Black, CircleShape)
                                            .padding(8.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Добавить",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            offset = DpOffset(x = (-8).dp, y = 0.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Отправить фото") },
                                onClick = {
                                    menuExpanded = false
                                    onCameraClick() // Используем переданный callback
                                },
                                leadingIcon = { Icon(Icons.Default.CameraAlt, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Загрузить фото") },
                                onClick = {
                                    menuExpanded = false
                                    onGalleryClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Photo, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Ввести вручную") },
                                onClick = {
                                    menuExpanded = false
                                    onManualClick() // Используем переданный callback
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        // ОСНОВНОЙ КОНТЕНТ
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // используем padding от Scaffold
        ) {
            // Заголовок с настройками
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Дневной прогресс",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = DailyResetUtils.getDisplayDate(DailyResetUtils.getFoodDate()),
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                OnlineStatus(isOnline = viewModel.isOnline)
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = Color.Black
                    )
                }
            }

            // Прогресс бары
            CollapsibleProgressBars(viewModel)

            // Подтверждение еды
            viewModel.pendingFood?.let { food ->
                Spacer(modifier = Modifier.height(10.dp))
                PendingFoodCard(
                    food = food,
                    selectedMeal = viewModel.selectedMeal,
                    onMealChange = { viewModel.selectedMeal = it },
                    onConfirm = { viewModel.confirmFood() },
                    onCancel = { viewModel.pendingFood = null }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Чат
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.messages) { message ->
                        AnimatedMessage(
                            visible = true,
                            isUserMessage = message.type == com.example.calorietracker.MessageType.USER
                        ) {
                            ChatMessageCard(message = message)
                        }
                    }
                }

                // Индикатор загрузки
                if (viewModel.isAnalyzing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = Color.Black)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (viewModel.isOnline) "AI анализирует фото..." else "Обрабатываем...",
                                    color = Color.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}