package com.example.calorietracker.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.data.UserProfile
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: CalorieTrackerViewModel,
    onFinish: (UserProfile) -> Unit
) {
    val systemUiController = rememberSystemUiController()

    val systemBarColor = MaterialTheme.colorScheme.background

    LaunchedEffect(Unit) {
        systemUiController.setSystemBarsColor(
            color = systemBarColor,
            darkIcons = true
        )
    }

    // Локальные состояния для всех полей ввода
    var height by remember { mutableStateOf(if (viewModel.userProfile.height > 0) viewModel.userProfile.height.toString() else "") }
    var weight by remember { mutableStateOf(if (viewModel.userProfile.weight > 0) viewModel.userProfile.weight.toString() else "") }
    var gender by remember { mutableStateOf(viewModel.userProfile.gender) }
    var condition by remember { mutableStateOf(viewModel.userProfile.condition) }
    var bodyFeeling by remember { mutableStateOf(viewModel.userProfile.bodyFeeling) }
    var goal by remember { mutableStateOf(viewModel.userProfile.goal) }

    val initialBirthdayParts = viewModel.userProfile.birthday.split("-").mapNotNull { it.toIntOrNull() }
    var year by remember { mutableStateOf(initialBirthdayParts.getOrNull(0)?.toString() ?: "") }
    var month by remember { mutableStateOf(initialBirthdayParts.getOrNull(1)?.toString() ?: "") }
    var day by remember { mutableStateOf(initialBirthdayParts.getOrNull(2)?.toString() ?: "") }

    // Функция для проверки валидности даты
    fun isDateValid(y: String, m: String, d: String): Boolean {
        val yearInt = y.toIntOrNull() ?: return false
        val monthInt = m.toIntOrNull() ?: return false
        val dayInt = d.toIntOrNull() ?: return false
        if (yearInt < 1900 || yearInt > Calendar.getInstance().get(Calendar.YEAR)) return false
        if (monthInt !in 1..12) return false
        val maxDay = when (monthInt) {
            2 -> if (yearInt % 4 == 0 && (yearInt % 100 != 0 || yearInt % 400 == 0)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return dayInt in 1..maxDay
    }

    // --- УЛУЧШЕНИЕ КОДА ---
    // Убрали избыточный derivedStateOf. remember с ключами уже делает то, что нужно:
    // пересчитывает значение, когда один из ключей изменяется.
    val isButtonEnabled = remember(height, weight, year, month, day, gender, condition, bodyFeeling, goal) {
        (height.toIntOrNull() ?: 0 > 0) &&
                (weight.toIntOrNull() ?: 0 > 0) &&
                isDateValid(year, month, day) &&
                gender.isNotEmpty() &&
                condition.isNotEmpty() &&
                bodyFeeling.isNotEmpty() &&
                goal.isNotEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.systemBars.asPaddingValues())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "AI Калория Трекер",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Настройте свой профиль для персональных рекомендаций",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(value = height, onValueChange = { height = it.filter { ch -> ch.isDigit() } }, label = { Text("Рост (см)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = weight, onValueChange = { weight = it.filter { ch -> ch.isDigit() } }, label = { Text("Вес (кг)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

        Text(
            "Дата рождения",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = year, onValueChange = { if (it.length <= 4) year = it.filter(Char::isDigit) }, label = { Text("Год") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = month, onValueChange = { if (it.length <= 2) month = it.filter(Char::isDigit) }, label = { Text("Месяц") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = day, onValueChange = { if (it.length <= 2) day = it.filter(Char::isDigit) }, label = { Text("День") }, modifier = Modifier.weight(1f))
        }

        var genderExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = !genderExpanded }) {
            OutlinedTextField(value = when (gender) { "male" -> "Мужской"; "female" -> "Женский"; else -> "" }, onValueChange = {}, readOnly = true, label = { Text("Пол") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
            ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                DropdownMenuItem(text = { Text("Мужской") }, onClick = { gender = "male"; genderExpanded = false })
                DropdownMenuItem(text = { Text("Женский") }, onClick = { gender = "female"; genderExpanded = false })
            }
        }

        var conditionExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = conditionExpanded, onExpandedChange = { conditionExpanded = !conditionExpanded }) {
            OutlinedTextField(value = when (condition) { "sedentary" -> "Малоподвижный"; "active" -> "Активный"; "very-active" -> "Очень активный"; else -> "" }, onValueChange = {}, readOnly = true, label = { Text("Активность") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
            ExposedDropdownMenu(expanded = conditionExpanded, onDismissRequest = { conditionExpanded = false }) {
                DropdownMenuItem(text = { Text("Малоподвижный") }, onClick = { condition = "sedentary"; conditionExpanded = false })
                DropdownMenuItem(text = { Text("Активный") }, onClick = { condition = "active"; conditionExpanded = false })
                DropdownMenuItem(text = { Text("Очень активный") }, onClick = { condition = "very-active"; conditionExpanded = false })
            }
        }

        var bodyFeelingExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = bodyFeelingExpanded, onExpandedChange = { bodyFeelingExpanded = !bodyFeelingExpanded }) {
            OutlinedTextField(value = when (bodyFeeling) { "thin" -> "Худой"; "normal" -> "Обычный"; "chubby" -> "Плотный"; "fat" -> "Толстый"; else -> "" }, onValueChange = {}, readOnly = true, label = { Text("Ощущение тела") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bodyFeelingExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
            ExposedDropdownMenu(expanded = bodyFeelingExpanded, onDismissRequest = { bodyFeelingExpanded = false }) {
                DropdownMenuItem(text = { Text("Худой") }, onClick = { bodyFeeling = "thin"; bodyFeelingExpanded = false })
                DropdownMenuItem(text = { Text("Обычный") }, onClick = { bodyFeeling = "normal"; bodyFeelingExpanded = false })
                DropdownMenuItem(text = { Text("Плотный") }, onClick = { bodyFeeling = "chubby"; bodyFeelingExpanded = false })
                DropdownMenuItem(text = { Text("Толстый") }, onClick = { bodyFeeling = "fat"; bodyFeelingExpanded = false })
            }
        }

        var goalExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = goalExpanded, onExpandedChange = { goalExpanded = !goalExpanded }) {
            OutlinedTextField(value = when (goal) { "lose" -> "Худеем"; "maintain" -> "Питаемся лучше"; "gain" -> "Набор массы"; else -> "" }, onValueChange = {}, readOnly = true, label = { Text("Цель") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
            ExposedDropdownMenu(expanded = goalExpanded, onDismissRequest = { goalExpanded = false }) {
                DropdownMenuItem(text = { Text("Худеем") }, onClick = { goal = "lose"; goalExpanded = false })
                DropdownMenuItem(text = { Text("Питаемся лучше") }, onClick = { goal = "maintain"; goalExpanded = false })
                DropdownMenuItem(text = { Text("Набор массы") }, onClick = { goal = "gain"; goalExpanded = false })
            }
        }

        Spacer(modifier = Modifier.weight(1f)) // Заполнитель, чтобы кнопка была внизу

        Button(
            onClick = {
                val formattedBirthday = String.format(
                    "%04d-%02d-%02d",
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
                    bodyFeeling = bodyFeeling,
                    goal = goal,
                    isSetupComplete = true
                )
                onFinish(finalProfile)
            },
            enabled = isButtonEnabled,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Поехали!", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}