@file:Suppress("DEPRECATION")

package com.example.calorietracker

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
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
import com.example.calorietracker.pages.PhotoUploadDialog
import kotlinx.coroutines.launch

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

// Замените CalorieTrackerApp в MainActivity.kt на это:

@Composable
fun CalorieTrackerApp(repository: DataRepository, context: android.content.Context) {
    val viewModel: CalorieTrackerViewModel = remember {
        CalorieTrackerViewModel(repository, context)
    }
    val coroutineScope = rememberCoroutineScope()

    // Лаунчер для камеры
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            viewModel.showPhotoDialog = false
            coroutineScope.launch {
                viewModel.analyzePhotoWithAI(it)
            }
        }
    }

    // Лаунчер для галереи
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.showPhotoDialog = false
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                coroutineScope.launch {
                    viewModel.analyzePhotoWithAI(bitmap)
                }
            }
        }
    }

    // Лаунчер для разрешений камеры
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Необходимо разрешение на использование камеры", Toast.LENGTH_SHORT).show()
        }
    }

    // Диалог загрузки фото
    if (viewModel.showPhotoDialog) {
        PhotoUploadDialog(
            onDismiss = { viewModel.showPhotoDialog = false },
            onCameraClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    cameraLauncher.launch(null)
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onGalleryClick = {
                galleryLauncher.launch("image/*")
            }
        )
    }

    // Диалог ручного ввода
    if (viewModel.showManualInputDialog) {
        ManualFoodInputDialog(
            // УБЕДИТЕСЬ, что передаете данные из prefillFood!
            initialFoodName = viewModel.prefillFood?.name ?: "",
            initialCalories = viewModel.prefillFood?.calories?.toString() ?: "",
            initialProteins = viewModel.prefillFood?.proteins?.toString() ?: "",
            initialFats = viewModel.prefillFood?.fats?.toString() ?: "",
            initialCarbs = viewModel.prefillFood?.carbs?.toString() ?: "",
            initialWeight = viewModel.prefillFood?.weight?.toString() ?: "100",
            onDismiss = {
                viewModel.showManualInputDialog = false
                viewModel.prefillFood = null  // очищаем после закрытия
            },
            onConfirm = { name, calories, proteins, fats, carbs, weight ->
                viewModel.handleManualInput(name, calories, proteins, fats, carbs, weight)
                viewModel.prefillFood = null  // очищаем после подтверждения
            }
        )
    }


    // Навигация между экранами
    when {
        viewModel.currentStep == "setup" -> {
            SetupScreen(
                viewModel = viewModel,
                onFinish = {
                    viewModel.checkInternetConnection()
                }
            )
        }

        viewModel.showSettings -> {
            SettingsScreen(
                viewModel = viewModel,
                onSave = {
                    viewModel.showSettings = false
                    viewModel.checkInternetConnection()
                },
                onBack = { viewModel.showSettings = false }
            )
        }

        else -> {
            UpdatedMainScreen(
                viewModel = viewModel,
                onPhotoClick = {
                    if (viewModel.isOnline) {
                        viewModel.showPhotoDialog = true
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