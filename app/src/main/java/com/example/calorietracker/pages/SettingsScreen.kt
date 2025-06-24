package com.example.calorietracker.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeParseException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: CalorieTrackerViewModel,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isBirthdayValid = try {
        LocalDate.parse(viewModel.userProfile.birthday)
        true
    } catch (e: DateTimeParseException) {
        false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(WindowInsets.statusBars.asPaddingValues()) // <-- вот сюда
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.Black
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Настройки профиля",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(24.dp))
        // Рост
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
        Spacer(modifier = Modifier.height(16.dp))
        // Вес
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
// --- Блок выбора даты рождения (ручной ввод) ---
        Text(
            text = "Дата рождения",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        val initialBirthday = viewModel.userProfile.birthday
        val (initYear, initMonth, initDay) = if (initialBirthday.isNotBlank()) {
            val parts = initialBirthday.split("-")
            Triple(parts.getOrNull(0)?.toIntOrNull() ?: 0, parts.getOrNull(1)?.toIntOrNull() ?: 0, parts.getOrNull(2)?.toIntOrNull() ?: 0)
        } else {
            Triple(0, 0, 0)
        }

        var year by remember { mutableStateOf(if (initYear != 0) initYear.toString() else "") }
        var month by remember { mutableStateOf(if (initMonth != 0) initMonth.toString() else "") }
        var day by remember { mutableStateOf(if (initDay != 0) initDay.toString() else "") }

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
        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.height(16.dp))
        // Пол (Dropdown)
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
        Spacer(modifier = Modifier.height(16.dp))
        // Активность (Dropdown)
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
                label = { Text("Оценка состояния") },
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
        Spacer(modifier = Modifier.height(16.dp))
        // Ощущение тела (Dropdown)
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
        Spacer(modifier = Modifier.height(16.dp))
        // Цель (Dropdown)
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
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.handleSetupSubmit()
                Toast.makeText(context, "Настройки обновлены", Toast.LENGTH_SHORT).show()
                onSave()
            },
            enabled = viewModel.userProfile.height > 0 &&
                    viewModel.userProfile.weight > 0 &&
                    viewModel.userProfile.birthday.isNotEmpty() &&
                    isBirthdayValid &&
                    viewModel.userProfile.gender.isNotEmpty() &&
                    viewModel.userProfile.condition.isNotEmpty() &&
                    viewModel.userProfile.goal.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text("Сохранить", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}