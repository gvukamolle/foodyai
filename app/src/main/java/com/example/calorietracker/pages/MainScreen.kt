package com.example.calorietracker.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import android.app.Activity
import android.os.Build
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.CameraAlt

@Composable
fun OnlineStatus(isOnline: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = if (isOnline) Color(0x1A82FFAE) else Color(0x1AFF9292),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
            contentDescription = null,
            tint = if (isOnline) Color(0xFF4EFF8A) else Color(0xFFFF9292),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isOnline) "Online" else "Offline",
            fontSize = 13.sp,
            color = if (isOnline) Color(0xFF4EFF8A) else Color(0xFFFF9292),
            fontWeight = FontWeight.Medium
        )
    }
}

// ProgressSection - существующий компонент
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

// ChatMessageCard - существующий компонент
@Composable
fun ChatMessageCard(message: com.example.calorietracker.ChatMessage) {
    val alignment = if (message.type == com.example.calorietracker.MessageType.USER) Alignment.CenterEnd else Alignment.CenterStart
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.type == com.example.calorietracker.MessageType.USER) Color.Black else Color(0xFFF3F4F6)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = message.content,
                color = if (message.type == com.example.calorietracker.MessageType.USER) Color.White else Color.Black,
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

// PendingFoodCard - существующий компонент
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

            // Два ряда кнопок
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
                ) { Text("Подтвердить") }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) { Text("Отмена") }
            }
        }
    }
}

// SetStatusBarColorToWhite - существующий компонент
@Composable
fun SetStatusBarColorToWhite() {
    val context = LocalContext.current
    val view = LocalView.current
    SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = android.graphics.Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = window.decorView
            decorView.systemUiVisibility =
                decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
}

// НОВЫЕ КОМПОНЕНТЫ ДЛЯ AI

// Диалог для ручного ввода продукта
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
    var calories by remember { mutableStateOf(initialCalories) }
    var proteins by remember { mutableStateOf(initialProteins) }
    var fats by remember { mutableStateOf(initialFats) }
    var carbs by remember { mutableStateOf(initialCarbs) }
    var weight by remember { mutableStateOf(initialWeight) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить продукт вручную") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    label = { Text("Вес (г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Ккал") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = proteins,
                        onValueChange = { proteins = it.filter { ch -> ch.isDigit() } },
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
                        value = fats,
                        onValueChange = { fats = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Жиры") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Углеводы") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Введите данные с упаковки продукта",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (foodName.isNotBlank() && calories.isNotBlank()) {
                        onConfirm(foodName, calories, proteins, fats, carbs, weight)
                        onDismiss()
                    }
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

// Простой индикатор статуса AI
@Composable
fun SimpleAIStatus(isOnline: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (isOnline) Color(0xFF4CAF50) else Color(0xFFFF9800),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isOnline)
                    "AI анализ доступен - сфотографируйте продукт"
                else
                    "Нет интернета - введите данные вручную",
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}

// Кнопки для фото/ручного ввода
@Composable
fun AddFoodButton(
    isOnline: Boolean,
    onPhotoClick: () -> Unit,
    onManualClick: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Кнопка камеры
        Button(
            onClick = onPhotoClick,
            modifier = Modifier.weight(1f),
            enabled = isOnline,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isOnline) Color.Black else Color.Gray,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Фото")
        }

        // Кнопка ручного ввода
        OutlinedButton(
            onClick = onManualClick,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Вручную")
        }
    }
}

// СТАРЫЙ MainScreen для обратной совместимости
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: CalorieTrackerViewModel,
    onPhotoClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    UpdatedMainScreen(
        viewModel = viewModel,
        onPhotoClick = onPhotoClick,
        onManualClick = { viewModel.showManualInputDialog = true },
        onSettingsClick = onSettingsClick
    )
}

// ОБНОВЛЕННЫЙ MainScreen с AI функциями
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatedMainScreen(
    viewModel: CalorieTrackerViewModel,
    onPhotoClick: () -> Unit,
    onManualClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    SetStatusBarColorToWhite()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Заголовок с настройками
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Дневной прогресс",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                // ВСТАВЬ вот эту строку прямо перед IconButton:
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp)
            ) {
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

            Spacer(modifier = Modifier.height(12.dp))

            // Кнопки добавления еды
            AddFoodButton(
                isOnline = viewModel.isOnline,
                onPhotoClick = onPhotoClick,
                onManualClick = onManualClick
            )

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

            // Чат и загрузка
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.messages) { message ->
                        ChatMessageCard(message = message)
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

            // Подсказка
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF9FAFB)
            ) {
                Text(
                    text = if (viewModel.isOnline)
                        "💡 AI распознает продукты по фото автоматически"
                    else
                        "💡 Введите данные с упаковки продукта вручную",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }

            // Поле ввода сообщения
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.inputMessage,
                    onValueChange = { viewModel.inputMessage = it },
                    placeholder = {
                        Text(
                            if (viewModel.isOnline)
                                "Спросите у AI диетолога..."
                            else
                                "Задайте вопрос..."
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    trailingIcon = {
                        IconButton(
                            onClick = { viewModel.sendMessage() },
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Отправить",
                                tint = if (viewModel.inputMessage.isNotBlank())
                                    Color.White else Color.Gray,
                                modifier = Modifier
                                    .background(
                                        if (viewModel.inputMessage.isNotBlank())
                                            Color.Black else Color.Transparent,
                                        CircleShape
                                    )
                                    .padding(if (viewModel.inputMessage.isNotBlank()) 9.dp else 0.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray,
                        disabledBorderColor = Color.Gray
                    )
                )
            }
        }
    }
}