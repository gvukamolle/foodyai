package com.example.calorietracker.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import com.example.calorietracker.CalorieTrackerViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import kotlin.Exception

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: CalorieTrackerViewModel,
    onFinish: () -> Unit
) {
    // Управление системными цветами
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.White,
            darkIcons = true // чёрные иконки
        )
    }

    // Дата рождения: подгружаем из viewModel если есть
    val initialBirthday = viewModel.userProfile.birthday
    val (initYear, initMonth, initDay) = if (initialBirthday.isNotBlank()) {
        val parts = initialBirthday.split("-")
        Triple(
            parts.getOrNull(0)?.toIntOrNull() ?: 0,
            parts.getOrNull(1)?.toIntOrNull() ?: 0,
            parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
    } else {
        Triple(0, 0, 0)
    }
    var year by remember { mutableStateOf(if (initYear != 0) initYear.toString() else "") }
    var month by remember { mutableStateOf(if (initMonth != 0) initMonth.toString() else "") }
    var day by remember { mutableStateOf(if (initDay != 0) initDay.toString() else "") }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "AI Калория Трекер",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Настройте свой профиль для персональных рекомендаций",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(18.dp))
        OutlinedTextField(
            value = if (viewModel.userProfile.height == 0) "" else viewModel.userProfile.height.toString(),
            onValueChange = {
                viewModel.userProfile = viewModel.userProfile.copy(
                    height = it.toIntOrNull() ?: 0
                )
            },
            label = { Text("Рост (см)") },
            placeholder = { Text("175") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = if (viewModel.userProfile.weight == 0) "" else viewModel.userProfile.weight.toString(),
            onValueChange = {
                viewModel.userProfile = viewModel.userProfile.copy(
                    weight = it.toIntOrNull() ?: 0
                )
            },
            label = { Text("Вес (кг)") },
            placeholder = { Text("70") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        // --- Блок выбора даты рождения (ручной ввод) ---
        Text(
            text = "Дата рождения",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = year,
                onValueChange = {
                    year = it.filter { ch -> ch.isDigit() }.take(4)
                    viewModel.userProfile = viewModel.userProfile.copy(
                        birthday = String.format(
                            "%04d-%02d-%02d",
                            year.toIntOrNull() ?: 0,
                            month.toIntOrNull() ?: 0,
                            day.toIntOrNull() ?: 0
                        )
                    )
                },
                label = { Text("Год") },
                placeholder = { Text("2000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(92.dp)
            )
            OutlinedTextField(
                value = month,
                onValueChange = {
                    month = it.filter { ch -> ch.isDigit() }.take(2)
                    viewModel.userProfile = viewModel.userProfile.copy(
                        birthday = String.format(
                            "%04d-%02d-%02d",
                            year.toIntOrNull() ?: 0,
                            month.toIntOrNull() ?: 0,
                            day.toIntOrNull() ?: 0
                        )
                    )
                },
                label = { Text("Месяц") },
                placeholder = { Text("1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(72.dp)
            )
            OutlinedTextField(
                value = day,
                onValueChange = {
                    day = it.filter { ch -> ch.isDigit() }.take(2)
                    viewModel.userProfile = viewModel.userProfile.copy(
                        birthday = String.format(
                            "%04d-%02d-%02d",
                            year.toIntOrNull() ?: 0,
                            month.toIntOrNull() ?: 0,
                            day.toIntOrNull() ?: 0
                        )
                    )
                },
                label = { Text("День") },
                placeholder = { Text("1") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(72.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Пол
        var genderExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded }
        ) {
            OutlinedTextField(
                value = when (viewModel.userProfile.gender) {
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
                        viewModel.userProfile = viewModel.userProfile.copy(gender = "male")
                        genderExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Женский") },
                    onClick = {
                        viewModel.userProfile = viewModel.userProfile.copy(gender = "female")
                        genderExpanded = false
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Активность
        var conditionExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = conditionExpanded,
            onExpandedChange = { conditionExpanded = !conditionExpanded }
        ) {
            OutlinedTextField(
                value = when (viewModel.userProfile.condition) {
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
                        viewModel.userProfile = viewModel.userProfile.copy(condition = "sedentary")
                        conditionExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Активный") },
                    onClick = {
                        viewModel.userProfile = viewModel.userProfile.copy(condition = "active")
                        conditionExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Очень активный") },
                    onClick = {
                        viewModel.userProfile = viewModel.userProfile.copy(condition = "very-active")
                        conditionExpanded = false
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Ощущение тела
        var bodyFeelingExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = bodyFeelingExpanded,
            onExpandedChange = { bodyFeelingExpanded = !bodyFeelingExpanded }
        ) {
            OutlinedTextField(
                value = when (viewModel.userProfile.bodyFeeling) {
                    "thin" -> "Худой"
                    "normal" -> "Обычный"
                    "chubby" -> "Плотный"
                    "fat" -> "Толстый"
                    else -> ""
                },
                onValueChange = {},
                readOnly = true,
                label = { Text("Ощущение тела") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bodyFeelingExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = bodyFeelingExpanded,
                onDismissRequest = { bodyFeelingExpanded = false }
            ) {
                DropdownMenuItem(text = { Text("Худой") }, onClick = {
                    viewModel.userProfile = viewModel.userProfile.copy(bodyFeeling = "thin")
                    bodyFeelingExpanded = false
                })
                DropdownMenuItem(text = { Text("Обычный") }, onClick = {
                    viewModel.userProfile = viewModel.userProfile.copy(bodyFeeling = "normal")
                    bodyFeelingExpanded = false
                })
                DropdownMenuItem(text = { Text("Плотный") }, onClick = {
                    viewModel.userProfile = viewModel.userProfile.copy(bodyFeeling = "chubby")
                    bodyFeelingExpanded = false
                })
                DropdownMenuItem(text = { Text("Толстый") }, onClick = {
                    viewModel.userProfile = viewModel.userProfile.copy(bodyFeeling = "fat")
                    bodyFeelingExpanded = false
                })
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Цель
        var goalExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = goalExpanded,
            onExpandedChange = { goalExpanded = !goalExpanded }
        ) {
            OutlinedTextField(
                value = when (viewModel.userProfile.goal) {
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
                        viewModel.userProfile = viewModel.userProfile.copy(goal = "lose")
                        goalExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Питаемся лучше") },
                    onClick = {
                        viewModel.userProfile = viewModel.userProfile.copy(goal = "maintain")
                        goalExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Набор массы") },
                    onClick = {
                        viewModel.userProfile = viewModel.userProfile.copy(goal = "gain")
                        goalExpanded = false
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        fun daysInMonth(y: Int, m: Int): Int = when (m) {
            2 -> if ((y % 4 == 0 && y % 100 != 0) || (y % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            in 1..12 -> 31
            else -> 0
        }
        val dateIsValid = try {
            val y = year.toInt()
            val m = month.toInt()
            val d = day.toInt()
            m in 1..12 && d in 1..daysInMonth(y, m)
        } catch (e: Exception) {
            false
        }
        Button(
            onClick = {
                viewModel.handleSetupSubmit()
                onFinish()
            },
            enabled = viewModel.userProfile.height > 0 &&
                    viewModel.userProfile.weight > 0 &&
                    viewModel.userProfile.birthday.isNotEmpty() &&
                    viewModel.userProfile.gender.isNotEmpty() &&
                    viewModel.userProfile.condition.isNotEmpty() &&
                    viewModel.userProfile.goal.isNotEmpty() &&
                    dateIsValid,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "Поехали!",
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}