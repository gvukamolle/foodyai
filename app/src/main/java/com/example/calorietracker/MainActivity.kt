@file:Suppress("DEPRECATION")

package com.example.calorietracker

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.pages.*
import com.example.calorietracker.workers.CleanupWorker
import kotlinx.coroutines.launch

enum class Screen {
    Setup, Main, SettingsV2
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CleanupWorker.schedule(this)

        setContent {
            CalorieTrackerTheme {
                val repository = remember { DataRepository(this@MainActivity) }
                val authManager = remember { AuthManager(this@MainActivity) }
                CalorieTrackerApp(repository, authManager, this@MainActivity)
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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalorieTrackerApp(repository: DataRepository, authManager: AuthManager, context: android.content.Context) {
    val viewModel: CalorieTrackerViewModel = remember { CalorieTrackerViewModel(repository, context) }
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember {
        mutableStateOf(if (viewModel.userProfile.isSetupComplete) Screen.Main else Screen.Setup)
    }

    var showSettingsScreen by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            coroutineScope.launch { // ИСПРАВЛЕНО
                viewModel.analyzePhotoWithAI(it)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                coroutineScope.launch { // ИСПРАВЛЕНО
                    viewModel.analyzePhotoWithAI(bitmap)
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Необходимо разрешение на использование камеры", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = showSettingsScreen || currentScreen == Screen.Setup) {
        if (showSettingsScreen) {
            showSettingsScreen = false
        } else if (currentScreen == Screen.Setup) {
            (context as? Activity)?.finish()
        }
    }

    if (viewModel.showPhotoDialog) {
        PhotoUploadDialog(
            onDismiss = { viewModel.showPhotoDialog = false },
            onCameraClick = {
                viewModel.showPhotoDialog = false
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraLauncher.launch(null)
                } else {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onGalleryClick = {
                viewModel.showPhotoDialog = false
                galleryLauncher.launch("image/*")
            }
        )
    }

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

    Crossfade(
        targetState = if (showSettingsScreen) Screen.SettingsV2 else currentScreen,
        animationSpec = tween(durationMillis = 300),
        label = "screen_crossfade"
    ) { targetScreen ->
        when (targetScreen) {
            Screen.Setup -> {
                SetupScreen(
                    viewModel = viewModel,
                    onFinish = {
                        viewModel.userProfile.isSetupComplete = true
                        repository.saveUserProfile(viewModel.userProfile)
                        currentScreen = Screen.Main
                    }
                )
            }
            Screen.SettingsV2 -> {
                SettingsScreenV2(
                    authManager = authManager,
                    onBack = { showSettingsScreen = false },
                    onNavigateToProfile = {},
                    onNavigateToBodySettings = {},
                    onNavigateToSubscription = {},
                    onSignOut = {
                        authManager.signOut()
                    }
                )
            }
            Screen.Main -> {
                UpdatedMainScreen(
                    viewModel = viewModel,
                    onCameraClick = {
                        if (viewModel.isOnline) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                cameraLauncher.launch(null)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        } else {
                            Toast.makeText(context, "AI анализ недоступен без интернета.", Toast.LENGTH_LONG).show()
                        }
                    },
                    onGalleryClick = {
                        if (viewModel.isOnline) {
                            galleryLauncher.launch("image/*")
                        } else {
                            Toast.makeText(context, "AI анализ недоступен без интернета.", Toast.LENGTH_LONG).show()
                        }
                    },
                    onManualClick = { viewModel.showManualInputDialog = true },
                    onSettingsClick = { showSettingsScreen = true }
                )
            }
        }
    }
}