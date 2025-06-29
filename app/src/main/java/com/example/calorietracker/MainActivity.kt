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
// --- VVV --- ВОТ ЭТА СТРОКА РЕШАЕТ ПРОБЛЕМУ --- VVV ---
import com.example.calorietracker.data.UserProfile
// --- ^^^ --- ВОТ ЭТА СТРОКА РЕШАЕТ ПРОБЛЕМУ --- ^^^ ---
import com.example.calorietracker.pages.*
import com.example.calorietracker.workers.CleanupWorker
import kotlinx.coroutines.launch

// Убираем Screen.Auth, теперь это решается состоянием
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
                val viewModel: CalorieTrackerViewModel = remember { CalorieTrackerViewModel(repository, this@MainActivity) }

                CalorieTrackerApp(authManager, viewModel, this@MainActivity)
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
fun CalorieTrackerApp(
    authManager: AuthManager,
    viewModel: CalorieTrackerViewModel,
    context: android.content.Context
) {
    val coroutineScope = rememberCoroutineScope()
    val authState by authManager.authState.collectAsState()
    val currentUser by authManager.currentUser.collectAsState()
    var showSettingsScreen by remember { mutableStateOf(false) }

    var currentScreen by remember { mutableStateOf<Screen?>(null) }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
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
        bitmap?.let { coroutineScope.launch { viewModel.analyzePhotoWithAI(it) } }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                coroutineScope.launch { viewModel.analyzePhotoWithAI(bitmap) }
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

    if (viewModel.showPhotoDialog) { /* ... */ }
    if (viewModel.showManualInputDialog) { /* ... */ }

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
                            onNavigateToSubscription = {},
                            onSignOut = { authManager.signOut() }
                        )
                    }
                    Screen.Main -> {
                        UpdatedMainScreen(
                            viewModel = viewModel,
                            onCameraClick = { /* ... */ },
                            onGalleryClick = { /* ... */ },
                            onManualClick = { /* ... */ },
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