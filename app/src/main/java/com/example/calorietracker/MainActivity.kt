@file:Suppress("DEPRECATION")

package com.example.calorietracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.auth.SubscriptionPlan
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.pages.*
import com.example.calorietracker.pages.settings.*
import com.example.calorietracker.pages.subscription.SubscriptionPlansScreen
import com.example.calorietracker.ui.theme.CalorieTrackerTheme
import com.example.calorietracker.workers.CleanupWorker
import kotlinx.coroutines.launch
import com.example.calorietracker.pages.subscription.SubscriptionPlansScreen
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import androidx.core.view.WindowCompat


// Убираем Screen.Auth, теперь это решается состоянием
enum class Screen {
    Setup, Main, SettingsV2, Calendar, Profile, BodySettings, AppSettings, Subscription, Feedback, Analytics
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
        checkGooglePlayServices()
        CleanupWorker.schedule(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val repository = remember { DataRepository(this@MainActivity) }
            val authManager = remember { AuthManager(this@MainActivity) }
            val viewModel: CalorieTrackerViewModel = remember {
                CalorieTrackerViewModel(repository, this@MainActivity, authManager)
            }

            CalorieTrackerTheme {
                CalorieTrackerApp(authManager, viewModel, this@MainActivity)
            }
        }
    }

    private fun checkGooglePlayServices(): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val result = availability.isGooglePlayServicesAvailable(this)
        return if (result == ConnectionResult.SUCCESS) {
            true
        } else {
            if (availability.isUserResolvableError(result)) {
                availability.makeGooglePlayServicesAvailable(this)
            } else {
                Toast.makeText(this, "Google Play Services required", Toast.LENGTH_LONG).show()
            }
            false
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
    var showProfileScreen by remember { mutableStateOf(false) }
    var showBodySettingsScreen by remember { mutableStateOf(false) }
    var showAppSettingsScreen by remember { mutableStateOf(false) }
    var showSubscriptionScreen by remember { mutableStateOf(false) }
    var showFeedbackScreen by remember { mutableStateOf(false) }
    var showAnalyticsScreen by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            viewModel.syncWithUserData(user)

            if (viewModel.userProfile.isSetupComplete) {
                if (!user.isSetupComplete) {
                    authManager.updateUserSetupComplete(true)
                }
                currentScreen = Screen.Main
            } else {
                currentScreen = Screen.Setup
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

    BackHandler(enabled = showSettingsScreen || showCalendarScreen || showProfileScreen || showBodySettingsScreen || showAppSettingsScreen || showFeedbackScreen || showAnalyticsScreen || showSubscriptionScreen) {
        when {
            showProfileScreen -> {
                showProfileScreen = false
                showSettingsScreen = true
            }
            showSubscriptionScreen -> {
                showSubscriptionScreen = false
                showSettingsScreen = true
            }
            showFeedbackScreen -> {
                showFeedbackScreen = false
                showSettingsScreen = true
            }
            showBodySettingsScreen -> {
                showBodySettingsScreen = false
                showSettingsScreen = true
            }
            showAppSettingsScreen -> {
                showAppSettingsScreen = false
                showSettingsScreen = true
            }
            showAnalyticsScreen -> {
                showAnalyticsScreen = false
                currentScreen = Screen.Main
            }
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
            val targetScreen = when {
                showProfileScreen -> Screen.Profile
                showBodySettingsScreen -> Screen.BodySettings
                showAppSettingsScreen -> Screen.AppSettings
                showSettingsScreen -> Screen.SettingsV2
                showSubscriptionScreen -> Screen.Subscription
                showFeedbackScreen -> Screen.Feedback
                showAnalyticsScreen -> Screen.Analytics
                else -> currentScreen
            }

            Crossfade(
                targetState = targetScreen,
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
                    
                    Screen.Analytics -> {
                        AnalyticsScreen(
                            viewModel = viewModel,
                            onBack = {
                                showAnalyticsScreen = false
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
                            onNavigateToProfile = {
                                showProfileScreen = true
                                showSettingsScreen = false
                            },
                            onNavigateToBodySettings = {
                                showBodySettingsScreen = true
                                showSettingsScreen = false
                            },
                            onNavigateToAppSettings = {
                                showAppSettingsScreen = true
                                showSettingsScreen = false
                            },
                            onNavigateToSubscription = {
                                showSubscriptionScreen = true
                                showSettingsScreen = false
                            },
                            onNavigateToFeedback = {
                                showFeedbackScreen = true
                                showSettingsScreen = false
                            },
                            onSignOut = { authManager.signOut() },
                        )
                    }

                    Screen.Profile -> {
                        ProfileScreen(
                            authManager = authManager,
                            onBack = {
                                showProfileScreen = false
                                showSettingsScreen = true
                            },
                            onNavigateToBodySettings = {
                                showProfileScreen = false
                                showBodySettingsScreen = true
                            }
                        )
                    }
                    Screen.Subscription -> {
                        SubscriptionPlansScreen(
                            currentPlan = currentUser?.subscriptionPlan ?: SubscriptionPlan.FREE,
                            onSelectPlan = { newPlan ->
                                coroutineScope.launch {
                                    val result = authManager.updateSubscriptionPlan(newPlan)
                                    if (result.isSuccess) {
                                        showSubscriptionScreen = false
                                        showSettingsScreen = true
                                        Toast.makeText(
                                            context,
                                            "План подписки обновлен на ${newPlan.displayName}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onBack = {
                                showSubscriptionScreen = false
                                showSettingsScreen = true
                            }
                        )
                    }

                    Screen.Feedback -> {
                        FeedbackScreen(
                            onBack = {
                                showFeedbackScreen = false
                                showSettingsScreen = true
                            }
                        )
                    }

                    Screen.BodySettings -> {
                        BodySettingsScreen(
                            viewModel = viewModel,
                            onBack = {
                                showBodySettingsScreen = false
                                showProfileScreen = true
                            }
                        )
                    }

                    Screen.AppSettings -> {
                        AppSettingsScreen(
                            viewModel = viewModel,
                            authManager = authManager,
                            onBack = {
                                showAppSettingsScreen = false
                                showSettingsScreen = true
                            },
                            onSignOut = { authManager.signOut() }
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
                            onCalendarClick = {
                                currentScreen = Screen.Calendar
                                showCalendarScreen = true
                            },
                            onNavigateToSubscription = {
                                showSubscriptionScreen = true
                            },
                            onProfileClick = {
                                showProfileScreen = true
                            },
                            onAnalyticsClick = {
                                currentScreen = Screen.Analytics
                                showAnalyticsScreen = true
                            },
                            onFeedbackClick = {
                                showFeedbackScreen = true
                            },
                            modifier = Modifier.fillMaxSize()
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