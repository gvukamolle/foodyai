@file:Suppress("DEPRECATION")

package com.example.calorietracker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.UserProfile
import com.example.calorietracker.pages.*
import com.example.calorietracker.ui.theme.CalorieTrackerTheme
import com.example.calorietracker.workers.CleanupWorker
import kotlinx.coroutines.launch
import com.example.calorietracker.pages.BeautifulManualFoodInputDialog
import com.example.calorietracker.pages.BeautifulDescribeFoodDialog
import com.example.calorietracker.pages.BeautifulPhotoUploadDialog
import com.example.calorietracker.pages.BeautifulPhotoConfirmDialog

// Убираем Screen.Auth, теперь это решается состоянием
enum class Screen {
    Setup, Main, SettingsV2
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CleanupWorker.schedule(this)

        setContent {
            val repository = remember { DataRepository(this@MainActivity) }
            val authManager = remember { AuthManager(this@MainActivity) }
            val viewModel: CalorieTrackerViewModel = remember { CalorieTrackerViewModel(repository, this@MainActivity) }

            CalorieTrackerTheme {
                CalorieTrackerApp(authManager, viewModel, this@MainActivity)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalorieTrackerApp(
    authManager: AuthManager,
    viewModel: CalorieTrackerViewModel,
    context: Context
) {
    val coroutineScope = rememberCoroutineScope()
    val authState by authManager.authState.collectAsState()
    val currentUser by authManager.currentUser.collectAsState()
    var showSettingsScreen by remember { mutableStateOf(false) }

    var currentScreen by remember { mutableStateOf<Screen?>(null) }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            val localSetupComplete = viewModel.userProfile.isSetupComplete
            viewModel.syncWithUserData(user)
            if (!user.isSetupComplete && viewModel.userProfile.isSetupComplete) {
                authManager.updateUserSetupComplete(true)
                currentScreen = Screen.Main
            } else {
                currentScreen = if (user.isSetupComplete) Screen.Main else Screen.Setup
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { viewModel.onPhotoSelected(it) }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                viewModel.onPhotoSelected(bitmap)
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) cameraLauncher.launch(null)
        else Toast.makeText(context, "Необходимо разрешение на использование камеры", Toast.LENGTH_SHORT).show()
    }

    BackHandler(enabled = showSettingsScreen) {
        showSettingsScreen = false
    }

    if (viewModel.showPhotoDialog) {
        BeautifulPhotoUploadDialog(
            onDismiss = { viewModel.showPhotoDialog = false },
            onCameraClick = {
                viewModel.showPhotoDialog = false
                val permission = Manifest.permission.CAMERA
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    cameraLauncher.launch(null)
                } else {
                    permissionLauncher.launch(permission)
                }
            },
            onGalleryClick = {
                viewModel.showPhotoDialog = false
                galleryLauncher.launch("image/*")
            }
        )
    }
    if (viewModel.showManualInputDialog) {
        val prefill = viewModel.prefillFood
        BeautifulManualFoodInputDialog(
            initialFoodName = prefill?.name ?: "",
            initialCalories = prefill?.calories?.toString() ?: "",
            initialProteins = prefill?.proteins?.toString() ?: "",
            initialFats = prefill?.fats?.toString() ?: "",
            initialCarbs = prefill?.carbs?.toString() ?: "",
            initialWeight = prefill?.weight ?: "100",
            onDismiss = { viewModel.showManualInputDialog = false },
            onConfirm = { name, calories, proteins, fats, carbs, weight ->
                viewModel.handleManualInput(name, calories, proteins, fats, carbs, weight)
            }
        )
    }
    if (viewModel.showDescriptionDialog) {
        var text by remember { mutableStateOf("") }
        BeautifulDescribeFoodDialog(
            text = text,
            onTextChange = { text = it },
            onDismiss = { viewModel.showDescriptionDialog = false },
            onSend = { viewModel.analyzeDescription(text) },
            isLoading = viewModel.isAnalyzing
        )
    }
    if (viewModel.showPhotoConfirmDialog) {
        viewModel.pendingPhoto?.let { bmp ->
            BeautifulPhotoConfirmDialog(
                bitmap = bmp,
                caption = viewModel.photoCaption,
                onCaptionChange = { viewModel.photoCaption = it },
                onConfirm = { viewModel.confirmPhoto() },
                onDismiss = { viewModel.showPhotoConfirmDialog = false }
            )
        }
    }

    when (authState) {
        AuthManager.AuthState.LOADING -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        AuthManager.AuthState.UNAUTHENTICATED -> {
            AuthScreen(authManager = authManager, onAuthSuccess = {})
        }
        AuthManager.AuthState.AUTHENTICATED -> {
            Crossfade(
                targetState = if (showSettingsScreen) Screen.SettingsV2 else currentScreen,
                animationSpec = tween(durationMillis = 300),
                label = "main_nav"
            ) { screen ->
                when (screen) {
                    Screen.Setup -> {
                        SetupScreen(
                            viewModel = viewModel,
                            onFinish = { finishedProfile -> // <-- Теперь тут нет ошибки
                                coroutineScope.launch {
                                    viewModel.updateUserProfile(finishedProfile)
                                    val result = authManager.updateUserSetupComplete(true)
                                    if (result.isSuccess) {
                                        currentScreen = Screen.Main
                                    } else {
                                        Toast.makeText(context, "Не удалось сохранить настройки. Попробуйте снова.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    }
                    Screen.SettingsV2 -> {
                        SettingsScreenV2(
                            authManager = authManager,
                            viewModel = viewModel,
                            onBack = { showSettingsScreen = false },
                            onNavigateToProfile = {},
                            onNavigateToBodySettings = {},
                            onSignOut = { authManager.signOut() },
                        )
                    }
                    Screen.Main -> {
                        UpdatedMainScreen(
                            viewModel = viewModel,
                            onCameraClick = {
                                val permission = Manifest.permission.CAMERA
                                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                    cameraLauncher.launch(null)
                                } else {
                                    permissionLauncher.launch(permission)
                                }
                            },
                            onGalleryClick = {
                                galleryLauncher.launch("image/*")
                            },
                            onManualClick = {
                                viewModel.prefillFood = null
                                viewModel.showManualInputDialog = true
                            },
                            onDescribeClick = {
                                viewModel.showDescriptionDialog = true
                            },
                            onSettingsClick = { showSettingsScreen = true }
                        )
                    }
                    null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}