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
import androidx.compose.material.icons.filled.Photo
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Divider
import androidx.compose.runtime.SideEffect
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.lazy.rememberLazyListState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import com.example.calorietracker.utils.DailyResetUtils

@Composable
fun OnlineStatus(isOnline: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), // Более светлые цвета
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
            contentDescription = null,
            tint = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336), // Зеленый/красный
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isOnline) "Online" else "Offline",
            fontSize = 12.sp,
            color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828), // Темнее для лучшей читаемости
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

    // Расчет итоговых значений
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
                // Название продукта
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Название продукта") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Вес порции
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Вес порции (г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Заголовок для КБЖУ на 100г
                Text(
                    text = "Пищевая ценность на 100г:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )

                // КБЖУ на 100г
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

                // Итоговые значения
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
                        // Передаем итоговые значения
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

                // Кнопка камеры
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

                // Кнопка галереи
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatedMainScreen(
    viewModel: CalorieTrackerViewModel,
    onPhotoClick: () -> Unit,
    onManualClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // Управление системными UI элементами
    val systemUiController = rememberSystemUiController()

    // Устанавливаем белый цвет для статус бара и навигационного бара
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.White,
            darkIcons = true // черные иконки на белом фоне
        )

        // Отдельно для навигационного бара если нужно
        systemUiController.setNavigationBarColor(
            color = Color.White,
            darkIcons = true
        )
    }

    // Состояние для прокрутки чата
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Автоматическая прокрутка при новых сообщениях
    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(viewModel.messages.size - 1)
            }
        }
    }

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
            // Заголовок с настройками - убираем серый фон
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White) // Явно белый фон
                    .padding(WindowInsets.statusBars.asPaddingValues())
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

            // Чат с автопрокруткой
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState, // Используем состояние для прокрутки
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


            // Поле ввода сообщения
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White) // Белый фон
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
                        .height(54.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(54),
                    trailingIcon = {
                        IconButton(
                            onClick = { viewModel.sendMessage() },
                            modifier = Modifier.size(54.dp)
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