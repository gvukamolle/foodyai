package com.example.calorietracker.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.ChatMessage
import com.example.calorietracker.FoodItem
import com.example.calorietracker.MealType
import com.example.calorietracker.MessageType
import com.example.calorietracker.ui.animations.AnimatedMessage
import com.example.calorietracker.utils.DailyResetUtils
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

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

// Кастомный Divider с закругленными краями
@Composable
fun RoundedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFE5E5E5),
    thickness: Dp = 1.dp,
    curveDown: Boolean = true,
    curveHeight: Dp = 10.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(curveHeight)
    ) {
        val path = Path().apply {
            val curveHeightPx = curveHeight.toPx()
            val thicknessPx = thickness.toPx()
            val halfThickness = thicknessPx / 2f

            if (curveDown) {
                moveTo(0f, curveHeightPx)
                quadraticBezierTo(curveHeightPx * 0.7f, halfThickness, curveHeightPx * 1.5f, halfThickness)
                lineTo(size.width - curveHeightPx * 1.5f, halfThickness)
                quadraticBezierTo(size.width - curveHeightPx * 0.7f, halfThickness, size.width, curveHeightPx)
            } else {
                moveTo(0f, halfThickness)
                quadraticBezierTo(curveHeightPx * 0.7f, curveHeightPx, curveHeightPx * 1.5f, curveHeightPx)
                lineTo(size.width - curveHeightPx * 1.5f, curveHeightPx)
                quadraticBezierTo(size.width - curveHeightPx * 0.7f, curveHeightPx, size.width, halfThickness)
            }
        }
        drawPath(path = path, color = color, style = Stroke(width = thickness.toPx()))
    }
}

@Composable
fun CollapsibleProgressBars(viewModel: CalorieTrackerViewModel) {
    var expanded by remember { mutableStateOf(true) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(viewModel.dailyIntake) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val containerHeight by animateDpAsState(targetValue = if (expanded) 150.dp else 80.dp, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "")
    val progressAlpha by animateFloatAsState(targetValue = if (expanded) 1f else 0f, animationSpec = tween(durationMillis = 200), label = "")
    val ringsAlpha by animateFloatAsState(targetValue = if (expanded) 0f else 1f, animationSpec = tween(durationMillis = 200, delayMillis = if (expanded) 0 else 100), label = "")
    val indicatorRotation by animateFloatAsState(targetValue = if (expanded) 0f else 180f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(containerHeight)
            .background(Color.White)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                expanded = !expanded
            }
    ) {
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = progressAlpha }) {
            if (progressAlpha > 0.01f) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WaveProgressSection(label = "ККАЛ", current = viewModel.dailyIntake.calories, target = viewModel.userProfile.dailyCalories, unit = "", color = viewModel.getProgressColor(viewModel.dailyIntake.calories, viewModel.userProfile.dailyCalories), delay = 0, visible = expanded)
                    WaveProgressSection(label = "Б", current = viewModel.dailyIntake.proteins, target = viewModel.userProfile.dailyProteins, unit = "г", color = viewModel.getProgressColor(viewModel.dailyIntake.proteins, viewModel.userProfile.dailyProteins), delay = 50, visible = expanded)
                    WaveProgressSection(label = "Ж", current = viewModel.dailyIntake.fats, target = viewModel.userProfile.dailyFats, unit = "г", color = viewModel.getProgressColor(viewModel.dailyIntake.fats, viewModel.userProfile.dailyFats), delay = 100, visible = expanded)
                    WaveProgressSection(label = "У", current = viewModel.dailyIntake.carbs, target = viewModel.userProfile.dailyCarbs, unit = "г", color = viewModel.getProgressColor(viewModel.dailyIntake.carbs, viewModel.userProfile.dailyCarbs), delay = 150, visible = expanded)
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = ringsAlpha }) {
            if (ringsAlpha > 0.01f) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    AnimatedRingIndicator(label = "К", current = viewModel.dailyIntake.calories, target = viewModel.userProfile.dailyCalories, color = viewModel.getProgressColor(viewModel.dailyIntake.calories, viewModel.userProfile.dailyCalories), delay = 0, visible = !expanded)
                    AnimatedRingIndicator(label = "Б", current = viewModel.dailyIntake.proteins, target = viewModel.userProfile.dailyProteins, color = viewModel.getProgressColor(viewModel.dailyIntake.proteins, viewModel.userProfile.dailyProteins), delay = 75, visible = !expanded)
                    AnimatedRingIndicator(label = "Ж", current = viewModel.dailyIntake.fats, target = viewModel.userProfile.dailyFats, color = viewModel.getProgressColor(viewModel.dailyIntake.fats, viewModel.userProfile.dailyFats), delay = 150, visible = !expanded)
                    AnimatedRingIndicator(label = "У", current = viewModel.dailyIntake.carbs, target = viewModel.userProfile.dailyCarbs, color = viewModel.getProgressColor(viewModel.dailyIntake.carbs, viewModel.userProfile.dailyCarbs), delay = 225, visible = !expanded)
                }
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter).size(20.dp).graphicsLayer { rotationZ = indicatorRotation; alpha = 0.3f }) {
            Icon(imageVector = Icons.Default.ExpandLess, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun WaveProgressSection(label: String, current: Int, target: Int, unit: String, color: Color, delay: Int, visible: Boolean) {
    val progress = if (target > 0) minOf(current.toFloat() / target.toFloat(), 1f) else 0f
    val sectionAlpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, animationSpec = tween(durationMillis = 300, delayMillis = if (visible) delay else 0), label = "")
    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "")

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = sectionAlpha; scaleX = scale; scaleY = scale }) {
        Text(text = label, fontSize = 12.sp, color = Color.Black.copy(alpha = 0.8f), modifier = Modifier.width(48.dp), fontWeight = FontWeight.Medium)
        Box(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFE5E7EB).copy(alpha = 0.6f), RoundedCornerShape(4.dp)))
            val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(durationMillis = 800, delayMillis = delay), label = "")
            val animatedColor by animateColorAsState(targetValue = color, animationSpec = tween(durationMillis = 1000), label = "")
            Box(modifier = Modifier.fillMaxWidth(animatedProgress).height(8.dp).clip(RoundedCornerShape(4.dp)).background(animatedColor))
        }
        Text(text = "$current/$target$unit", fontSize = 12.sp, color = Color.Black.copy(alpha = 0.8f), modifier = Modifier.width(64.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AnimatedRingIndicator(label: String, current: Int, target: Int, color: Color, delay: Int, visible: Boolean) {
    val progress = if (target > 0) current.toFloat() / target.toFloat() else 0f
    val scale by animateFloatAsState(targetValue = if (visible) 1f else 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "")
    val rotation by animateFloatAsState(targetValue = if (visible) 0f else -90f, animationSpec = tween(durationMillis = 400, delayMillis = delay), label = "")

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp).graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation }) {
        CircularProgressIndicator(progress = { 1f }, color = Color(0xFFE5E7EB).copy(alpha = 0.5f), strokeWidth = 6.dp, strokeCap = StrokeCap.Round, modifier = Modifier.fillMaxSize()) // ИСПРАВЛЕНО
        val animatedProgress by animateFloatAsState(targetValue = if (visible) progress else 0f, animationSpec = tween(durationMillis = 1000, delayMillis = if (visible) delay else 0), label = "")
        CircularProgressIndicator(progress = { animatedProgress }, color = color, strokeWidth = 6.dp, strokeCap = StrokeCap.Round, modifier = Modifier.fillMaxSize()) // ИСПРАВЛЕНО
        Text(text = label, fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChatMessageCard(message: ChatMessage) {
    val alignment = if (message.type == MessageType.USER) Alignment.CenterEnd else Alignment.CenterStart
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(modifier = Modifier.widthIn(max = 280.dp), colors = CardDefaults.cardColors(containerColor = if (message.type == MessageType.USER) Color.Black else Color(0xFFF3F4F6)), shape = RoundedCornerShape(12.dp)) {
            Text(text = message.content, color = if (message.type == MessageType.USER) Color.White else Color.Black, fontSize = 14.sp, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
fun PendingFoodCard(food: FoodItem, selectedMeal: MealType, onMealChange: (MealType) -> Unit, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Подтвердите данные", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Блюдо: ${food.name}")
            Text("Калории: ${food.calories}")
            Text("Белки: ${food.proteins} г")
            Text("Жиры: ${food.fats} г")
            Text("Углеводы: ${food.carbs} г")
            Text("Вес: ${food.weight} г")
            Spacer(modifier = Modifier.height(8.dp))
            Text("Приём пищи:", color = Color.Black, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            val firstRow = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)
            val secondRow = listOf(MealType.LATE_BREAKFAST, MealType.SNACK, MealType.SUPPER)
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    firstRow.forEach { meal -> MealTypeButton(meal = meal, isSelected = meal == selectedMeal, onClick = { onMealChange(meal) }, modifier = Modifier.weight(1f)) }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    secondRow.forEach { meal -> MealTypeButton(meal = meal, isSelected = meal == selectedMeal, onClick = { onMealChange(meal) }, modifier = Modifier.weight(1f)) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HapticButton(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White), modifier = Modifier.weight(1f)) { Text("Подтвердить") }
                OutlinedButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onCancel() }, modifier = Modifier.weight(1f)) { Text("Отмена") }
            }
        }
    }
}

@Composable
fun MealTypeButton(meal: MealType, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() },
        colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) Color.Black else Color(0xFFE5E7EB), contentColor = if (isSelected) Color.White else Color.Black),
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(meal.displayName, fontSize = 13.sp)
    }
}

@Composable
fun HapticIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
    val haptic = LocalHapticFeedback.current
    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }, modifier = modifier, enabled = enabled) { content() }
}

@Composable
fun HapticButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, colors: ButtonColors = ButtonDefaults.buttonColors(), contentPadding: PaddingValues = ButtonDefaults.ContentPadding, content: @Composable RowScope.() -> Unit) {
    val haptic = LocalHapticFeedback.current
    Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }, modifier = modifier, enabled = enabled, colors = colors, contentPadding = contentPadding) { content() }
}

@Composable
fun ManualFoodInputDialog(initialFoodName: String = "", initialCalories: String = "", initialProteins: String = "", initialFats: String = "", initialCarbs: String = "", initialWeight: String = "100", onDismiss: () -> Unit, onConfirm: (name: String, calories: String, proteins: String, fats: String, carbs: String, weight: String) -> Unit) {
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
        title = { Text(if (initialFoodName.isNotEmpty()) "Проверьте данные" else "Добавить продукт", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = foodName, onValueChange = { foodName = it }, label = { Text("Название продукта") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = weight, onValueChange = { weight = it.filter { ch -> ch.isDigit() } }, label = { Text("Вес порции (г)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Пищевая ценность на 100г:", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = caloriesPer100g, onValueChange = { caloriesPer100g = it.filter { ch -> ch.isDigit() } }, label = { Text("Ккал") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = proteinsPer100g, onValueChange = { proteinsPer100g = it.filter { ch -> ch.isDigit() } }, label = { Text("Белки") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fatsPer100g, onValueChange = { fatsPer100g = it.filter { ch -> ch.isDigit() } }, label = { Text("Жиры") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = carbsPer100g, onValueChange = { carbsPer100g = it.filter { ch -> ch.isDigit() } }, label = { Text("Углеводы") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                if (weight.isNotBlank() && weight != "100") {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Итого на ${weight}г:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Калории: $totalCalories ккал | Белки: ${totalProteins}г | Жиры: ${totalFats}г | Углеводы: ${totalCarbs}г", fontSize = 12.sp, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (foodName.isNotBlank() && caloriesPer100g.isNotBlank()) { onConfirm(foodName, totalCalories.toString(), totalProteins.toString(), totalFats.toString(), totalCarbs.toString(), weight); onDismiss() } }) { Text("Добавить", color = Color.Black) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Color.Gray) } }
    )
}

@Composable
fun PhotoUploadDialog(onDismiss: () -> Unit, onCameraClick: () -> Unit, onGalleryClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CameraAlt, null, Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Text("Добавить фото продукта", fontSize = 18.sp, fontWeight = FontWeight.Medium) } },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Рекомендуем загрузить фотографию этикетки с КБЖУ и составом", fontSize = 14.sp, color = Color.Gray)
                Card(modifier = Modifier.fillMaxWidth().clickable { onCameraClick() }, colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.Black, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Column { Text("Сделать фото", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black); Text("Сфотографируйте продукт или упаковку", fontSize = 13.sp, color = Color.Gray) }
                    }
                }
                Card(modifier = Modifier.fillMaxWidth().clickable { onGalleryClick() }, colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Photo, null, tint = Color.Black, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(16.dp))
                        Column { Text("Выбрать из галереи", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black); Text("Загрузите готовое фото", fontSize = 13.sp, color = Color.Gray) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Color.Black) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatedMainScreen(viewModel: CalorieTrackerViewModel, onCameraClick: () -> Unit, onGalleryClick: () -> Unit, onManualClick: () -> Unit, onSettingsClick: () -> Unit) {
    val systemUiController = rememberSystemUiController()
    SideEffect { systemUiController.setSystemBarsColor(color = Color.White, darkIcons = true) }

    LaunchedEffect(key1 = Unit) {
        while (true) {
            val now = LocalDateTime.now()
            val nextResetTime = DailyResetUtils.getNextResetTime()
            val durationToReset = Duration.between(now, nextResetTime).toMillis()
            delay(durationToReset + 1000)
            viewModel.updateDateAndCheckForReset()
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            coroutineScope.launch { listState.animateScrollToItem(viewModel.messages.size - 1) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        containerColor = Color.White,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                var menuExpanded by remember { mutableStateOf(false) }
                RoundedDivider(color = Color(0xFFE5E5E5), thickness = 1.dp, curveDown = true, curveHeight = 10.dp)
                Row(modifier = Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.width(16.dp))
                    BasicTextField(
                        value = viewModel.inputMessage,
                        onValueChange = { viewModel.inputMessage = it },
                        singleLine = false,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text),
                        textStyle = TextStyle(fontSize = 18.sp, color = Color.Black),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box {
                                if (viewModel.inputMessage.isEmpty()) {
                                    Text(text = if (viewModel.isOnline) "Спросите у AI-диетолога..." else "Задайте вопрос...", color = Color.Gray, fontSize = 18.sp)
                                }
                                innerTextField()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        AnimatedContent(targetState = viewModel.inputMessage.isNotBlank(), label = "SendPlus") { hasText ->
                            if (hasText) {
                                IconButton(onClick = { viewModel.sendMessage() }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Send, "Отправить", tint = Color.White, modifier = Modifier.background(Color.Black, CircleShape).padding(8.dp))
                                }
                            } else {
                                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Add, "Добавить", tint = Color.Black, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, offset = DpOffset(x = (-8).dp, y = 0.dp)) {
                            DropdownMenuItem(text = { Text("Отправить фото") }, onClick = { menuExpanded = false; onCameraClick() }, leadingIcon = { Icon(Icons.Default.CameraAlt, null) })
                            DropdownMenuItem(text = { Text("Загрузить фото") }, onClick = { menuExpanded = false; onGalleryClick() }, leadingIcon = { Icon(Icons.Default.Photo, null) })
                            DropdownMenuItem(text = { Text("Ввести вручную") }, onClick = { menuExpanded = false; onManualClick() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Дневной прогресс", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(viewModel.displayDate, fontSize = 13.sp, color = Color.Gray)
                }
                OnlineStatus(isOnline = viewModel.isOnline)
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = onSettingsClick, modifier = Modifier.size(32.dp)) { // ИСПРАВЛЕНО
                    Icon(Icons.Default.Settings, "Настройки", tint = Color.Black)
                }
            }
            CollapsibleProgressBars(viewModel)
            RoundedDivider(color = Color(0xFFE5E5E5), thickness = 1.dp, curveDown = false, curveHeight = 8.dp, modifier = Modifier.padding(top = 4.dp))
            viewModel.pendingFood?.let { food ->
                Spacer(modifier = Modifier.height(8.dp))
                PendingFoodCard(food = food, selectedMeal = viewModel.selectedMeal, onMealChange = { viewModel.selectedMeal = it }, onConfirm = { viewModel.confirmFood() }, onCancel = { viewModel.pendingFood = null })
            }
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(viewModel.messages) { message ->
                        AnimatedMessage(visible = true, isUserMessage = message.type == MessageType.USER) {
                            ChatMessageCard(message = message)
                        }
                    }
                }
                if (viewModel.isAnalyzing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.Black)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(if (viewModel.isOnline) "AI анализирует фото..." else "Обрабатываем...", color = Color.Black, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}