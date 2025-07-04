@file:Suppress("DEPRECATION")

package com.example.calorietracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.core.content.ContextCompat
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.pages.*
import com.example.calorietracker.ui.theme.CalorieTrackerTheme
import com.example.calorietracker.workers.CleanupWorker
import kotlinx.coroutines.launch

// Убираем Screen.Auth, теперь это решается состоянием
enum class Screen {
    Setup, Main, SettingsV2, Calendar
}

private fun decodeBitmapWithOrientation(file: java.io.File): Bitmap {
    val original = BitmapFactory.decodeFile(file.absolutePath)
    val exif = ExifInterface(file)
    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )
    val rotation = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    return if (rotation != 0f) {
        val matrix = Matrix().apply { postRotate(rotation) }
        Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true).also {
            if (it != original) original.recycle()
        }
    } else {
        original
    }
}

private fun decodeBitmapWithOrientation(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val tempFile = kotlin.io.path.createTempFile(prefix = "import_", suffix = ".jpg").toFile()
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
            val bitmap = decodeBitmapWithOrientation(tempFile)
            tempFile.delete()
            bitmap
        }
    } catch (_: Exception) {
        null
    }
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
    var showCalendarScreen by remember { mutableStateOf(false) }
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

    // Камера и галерея
    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    val cameraFile = remember { mutableStateOf<java.io.File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraFile.value?.let { file ->
                val bitmap = decodeBitmapWithOrientation(file)
                viewModel.onPhotoSelected(bitmap)
            } ?: cameraUri.value?.let { uri ->
                decodeBitmapWithOrientation(context, uri)?.let { bitmap ->
                    viewModel.onPhotoSelected(bitmap)
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            decodeBitmapWithOrientation(context, it)?.let { bmp ->
                viewModel.onPhotoSelected(bmp)
            }
        }
    }

    val launchCamera: () -> Unit = {
        val photoDir = java.io.File(context.cacheDir, "images").apply { mkdirs() }
        val photoFile = java.io.File.createTempFile("camera_photo", ".jpg", photoDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        cameraFile.value = photoFile
        cameraUri.value = uri
        cameraLauncher.launch(uri)
    }

    val permissionLauncher = rememberLauncherForActivityResult<String, Boolean>(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(
                context,
                "Необходимо разрешение на использование камеры",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    BackHandler(enabled = showSettingsScreen || showCalendarScreen) {
        when {
            showCalendarScreen -> {
                showCalendarScreen = false
                currentScreen = Screen.Main
            }
            showSettingsScreen -> {
                showSettingsScreen = false
            }
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
                    Screen.Calendar -> {
                        CalendarScreen(
                            viewModel = viewModel,
                            onBack = {
                                showCalendarScreen = false
                                currentScreen = Screen.Main
                            }
                        )
                    }

                    Screen.Setup -> {
                        SetupScreen(
                            viewModel = viewModel,
                            onFinish = { finishedProfile ->
                                coroutineScope.launch {
                                    viewModel.updateUserProfile(finishedProfile)
                                    val result = authManager.updateUserSetupComplete(true)
                                    if (result.isSuccess) {
                                        currentScreen = Screen.Main
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Не удалось сохранить настройки. Попробуйте снова.",
                                            Toast.LENGTH_LONG
                                        ).show()
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
                        AnimatedMainScreen(
                            viewModel = viewModel,
                            onCameraClick = {
                                val permission = Manifest.permission.CAMERA
                                if (ContextCompat.checkSelfPermission(
                                        context,
                                        permission
                                    ) == PackageManager.PERMISSION_GRANTED
                                ) {
                                    launchCamera()
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
                            onSettingsClick = { showSettingsScreen = true },
                            onCalendarClick = {  // Новый обработчик
                                currentScreen = Screen.Calendar
                                showCalendarScreen = true
                            }
                        )
                    }

                    null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}