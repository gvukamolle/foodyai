@file:Suppress("DEPRECATION")

package com.example.calorietracker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.pages.SetupScreen
import com.example.calorietracker.pages.SettingsScreen
import com.example.calorietracker.pages.UpdatedMainScreen
import com.example.calorietracker.pages.ManualFoodInputDialog
import com.example.calorietracker.pages.PhotoUploadScreen
import kotlinx.coroutines.launch
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.create
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalorieTrackerTheme {
                val repository = remember { DataRepository(this@MainActivity) }
                CalorieTrackerApp(repository, this@MainActivity)
            }
        }
    }
}

@Composable
fun CalorieTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color.Black,
            background = Color.White,
            surface = Color.White
        ),
        content = content
    )
}

@Composable
fun CalorieTrackerApp(repository: DataRepository, context: android.content.Context) {
    // Создаем ViewModel с контекстом
    val viewModel: CalorieTrackerViewModel = remember {
        CalorieTrackerViewModel(repository, context)
    }
    val coroutineScope = rememberCoroutineScope()

// Диалог ручного ввода
    if (viewModel.showManualInputDialog) {
        ManualFoodInputDialog(
            initialFoodName = viewModel.prefillFood?.name ?: "",
            initialCalories = viewModel.prefillFood?.calories?.toString() ?: "",
            initialProteins = viewModel.prefillFood?.proteins?.toString() ?: "",
            initialFats = viewModel.prefillFood?.fats?.toString() ?: "",
            initialCarbs = viewModel.prefillFood?.carbs?.toString() ?: "",
            initialWeight = viewModel.prefillFood?.weight?.toString() ?: "100",
            onDismiss = {
                viewModel.showManualInputDialog = false
                viewModel.prefillFood = null
            },
            onConfirm = { name, calories, proteins, fats, carbs, weight ->
                viewModel.handleManualInput(name, calories, proteins, fats, carbs, weight)
                viewModel.prefillFood = null
            }
        )
    }

// Лаунчер для камеры
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { bitmap ->
                coroutineScope.launch {
                    viewModel.analyzePhotoWithAI(bitmap)
                }
            }
        }
    }

    // Лаунчер для разрешений
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            photoLauncher.launch(intent)
        } else {
            Toast.makeText(context, "Необходимо разрешение на использование камеры", Toast.LENGTH_SHORT).show()
        }
    }

    // Навигация между экранами
    Log.d("CalorieTracker", "currentStep = ${viewModel.currentStep}, showSettings = ${viewModel.showSettings}")

    when {
        viewModel.currentStep == "setup" -> {
            Log.d("CalorieTracker", "Запускается SetupScreen")
            SetupScreen(
                viewModel = viewModel,
                onFinish = {
                    Log.d("CalorieTracker", "Завершён SetupScreen, стартуем checkInternetConnection()")
                    viewModel.checkInternetConnection()
                }
            )
            // Маркер на экране для дебага
            Text("SetupScreen loaded!")
        }

        viewModel.showSettings -> {
            Log.d("CalorieTracker", "Показывается экран настроек")
            SettingsScreen(
                viewModel = viewModel,
                onSave = {
                    viewModel.showSettings = false
                    viewModel.checkInternetConnection()
                },
                onBack = { viewModel.showSettings = false }
            )
        }

        viewModel.showPhotoUploadScreen -> {
            PhotoUploadScreen(
                viewModel = viewModel,
                onBack = { viewModel.showPhotoUploadScreen = false }
            )
        }

        else -> {
            Log.d("CalorieTracker", "Показывается главный экран")
            UpdatedMainScreen(
                viewModel = viewModel,
                onPhotoClick = {
                    if (viewModel.isOnline) {
                        viewModel.showPhotoUploadScreen = true
                    } else {
                        Toast.makeText(
                            context,
                            "AI анализ недоступен без интернета. Используйте ручной ввод.",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.showManualInputDialog = true
                    }
                },
                onManualClick = {
                    viewModel.showManualInputDialog = true
                },
                onSettingsClick = {
                    viewModel.showSettings = true
                }
            )
        }
    }

    // Периодическая проверка интернета

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30000)
            viewModel.checkInternetConnection()
        }
    }
}