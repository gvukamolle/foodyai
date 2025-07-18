package com.example.calorietracker.pages.settings

import android.R.attr.height
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.utils.BodyParametersValidator
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.example.calorietracker.components.AppTextField
import com.example.calorietracker.components.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodySettingsScreen(
    viewModel: CalorieTrackerViewModel,
    onBack: () -> Unit
) {
    val systemUiController = rememberSystemUiController()
    LaunchedEffect(Unit) {
        systemUiController.setSystemBarsColor(color = Color.White, darkIcons = true)
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    // Локальные состояния для редактирования
    var height by remember { mutableStateOf(if (viewModel.userProfile.height > 0) viewModel.userProfile.height.toString() else "") }
    var weight by remember { mutableStateOf(if (viewModel.userProfile.weight > 0) viewModel.userProfile.weight.toString() else "") }
    var gender by remember { mutableStateOf(viewModel.userProfile.gender) }
    var condition by remember { mutableStateOf(viewModel.userProfile.condition) }
    var goal by remember { mutableStateOf(viewModel.userProfile.goal) }

    val initialBirthdayParts = viewModel.userProfile.birthday.split("-").mapNotNull { it.toIntOrNull() }
    var year by remember { mutableStateOf(initialBirthdayParts.getOrNull(0)?.toString() ?: "") }
    var month by remember { mutableStateOf(initialBirthdayParts.getOrNull(1)?.toString() ?: "") }
    var day by remember { mutableStateOf(initialBirthdayParts.getOrNull(2)?.toString() ?: "") }

    var heightError by remember { mutableStateOf<String?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    var birthdayError by remember { mutableStateOf<String?>(null) }

    // Проверка валидности
    val isDataValid by remember(height, weight, year, month, day, gender, condition, goal) {
        derivedStateOf {
            height.toIntOrNull() ?: 0 > 0 &&
                    weight.toIntOrNull() ?: 0 > 0 &&
                    gender.isNotEmpty() &&
                    condition.isNotEmpty() &&
                    goal.isNotEmpty() &&
                    (year.toIntOrNull() ?: 0) > 1900 &&
                    (month.toIntOrNull() ?: 0) in 1..12 &&
                    (day.toIntOrNull() ?: 0) in 1..31
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки тела", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Секция основных параметров
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White), // Изменено с Color(0xFFF5F5F5)
                border = BorderStroke(1.dp, Color.Black), // Добавлена черная обводка
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Основные параметры",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AppTextField(
                            value = height,
                            onValueChange = {
                                height = it
                                val validation = BodyParametersValidator.validateHeight(it)
                                heightError = validation.errorMessage
                            },
                            label = { Text("Рост (см)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            isError = heightError != null,
                            supportingText = if (heightError != null) {{ Text(heightError!!) }} else null
                        )

                        AppTextField(
                            value = weight,
                            onValueChange = {
                                weight = it
                                val validation = BodyParametersValidator.validateWeight(it)
                                weightError = validation.errorMessage
                            },
                            label = { Text("Вес (кг)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            isError = weightError != null,
                            supportingText = if (weightError != null) {{ Text(weightError!!) }} else null
                        )
                    }
                }
            }

            // Секция даты рождения
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White), // Изменено
                border = BorderStroke(1.dp, Color.Black), // Добавлена черная обводка
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Дата рождения",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppTextField(
                            value = day,
                            onValueChange = {
                                day = it
                            },
                            label = { Text("День") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        AppTextField(
                            value = month,
                            onValueChange = {
                                month = it
                            },
                            label = { Text("Месяц") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        AppTextField(
                            value = year,
                            onValueChange = {
                                year = it
                            },
                            label = { Text("Год") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            }

            // Секция дополнительных параметров
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White), // Изменено
                border = BorderStroke(1.dp, Color.Black), // Добавлена черная обводка
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Дополнительные параметры",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Пол
                    var genderExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = !genderExpanded }
                    ) {
                        AppTextField(
                            value = when (gender) {
                                "male" -> "Мужской"
                                "female" -> "Женский"
                                else -> ""
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Пол") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Мужской") },
                                onClick = {
                                    gender = "male"
                                    genderExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Женский") },
                                onClick = {
                                    gender = "female"
                                    genderExpanded = false
                                }
                            )
                        }
                    }

                    // Активность
                    var conditionExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = conditionExpanded,
                        onExpandedChange = { conditionExpanded = !conditionExpanded }
                    ) {
                        AppTextField(
                            value = when (condition) {
                                "sedentary" -> "Малоподвижный"
                                "active" -> "Активный"
                                "very-active" -> "Очень активный"
                                else -> ""
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Активность") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = conditionExpanded,
                            onDismissRequest = { conditionExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Малоподвижный") },
                                onClick = {
                                    condition = "sedentary"
                                    conditionExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Активный") },
                                onClick = {
                                    condition = "active"
                                    conditionExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Очень активный") },
                                onClick = {
                                    condition = "very-active"
                                    conditionExpanded = false
                                }
                            )
                        }
                    }

                    // Цель
                    var goalExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = goalExpanded,
                        onExpandedChange = { goalExpanded = !goalExpanded }
                    ) {
                        AppTextField(
                            value = when (goal) {
                                "lose" -> "Худеем"
                                "maintain" -> "Питаемся лучше"
                                "gain" -> "Набор массы"
                                else -> ""
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Цель") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = goalExpanded,
                            onDismissRequest = { goalExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Худеем") },
                                onClick = {
                                    goal = "lose"
                                    goalExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Поддержание веса") },
                                onClick = {
                                    goal = "maintain"
                                    goalExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Набор массы") },
                                onClick = {
                                    goal = "gain"
                                    goalExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Кнопка сохранения
            Button(
                onClick = {
                    val formattedBirthday = String.format("%04d-%02d-%02d",
                        year.toIntOrNull() ?: 0,
                        month.toIntOrNull() ?: 0,
                        day.toIntOrNull() ?: 0
                    )
                    val finalProfile = viewModel.userProfile.copy(
                        height = height.toInt(),
                        weight = weight.toInt(),
                        birthday = formattedBirthday,
                        gender = gender,
                        condition = condition,
                        goal = goal
                    )
                    viewModel.updateUserProfile(finalProfile)
                    Toast.makeText(context, "Настройки обновлены", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                enabled = isDataValid,
                modifier = Modifier.fillMaxWidth().height(AppTheme.buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.Colors.primaryBlack,
                    disabledContainerColor = AppTheme.Colors.borderGray
                ),
                shape = RoundedCornerShape(AppTheme.cornerRadius)
            ) {
                Text("Сохранить", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
