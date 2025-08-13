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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.auth.SubscriptionPlan
import com.example.calorietracker.pages.*
import com.example.calorietracker.pages.settings.*
import com.example.calorietracker.pages.subscription.SubscriptionPlansScreen
import com.example.calorietracker.ui.theme.CalorieTrackerTheme
import com.example.calorietracker.workers.CleanupWorker
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.view.WindowCompat
import com.example.calorietracker.presentation.viewmodels.CalorieTrackerViewModel


// Убираем Screen.Auth, теперь это решается состоянием
enum class Screen {
    Main, Calendar, Profile, BodySettings, AppSettings, Subscription, Analytics
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

private fun saveBitmapToCache(context: Context, bitmap: Bitmap): String? {
    return try {
        val photoDir = java.io.File(context.cacheDir, "images").apply { mkdirs() }
        val photoFile = java.io.File.createTempFile("attached_", ".jpg", photoDir)
        photoFile.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        photoFile.absolutePath
    } catch (_: Exception) {
        null
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var authManager: AuthManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkGooglePlayServices()
        CleanupWorker.schedule(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val viewModel: CalorieTrackerViewModel = hiltViewModel()

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
    val appMode by viewModel.appMode.collectAsState()
    var showCalendarScreen by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf<Screen?>(null) }
    var showProfileScreen by remember { mutableStateOf(false) }
    var showBodySettingsScreen by remember { mutableStateOf(false) }
    var showAppSettingsScreen by remember { mutableStateOf(false) }
    var showSubscriptionScreen by remember { mutableStateOf(false) }
    var showAnalyticsScreen by remember { mutableStateOf(false) }

    // Убираем LaunchedEffect для authState - он вызывает проблемы
    // Оставляем только LaunchedEffect для currentUser, который правильно синхронизирует данные
    LaunchedEffect(currentUser) {
        if (authState == AuthManager.AuthState.AUTHENTICATED) {
            currentUser?.let { _ ->
                currentScreen = Screen.Main
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
                viewModel.attachedPhoto = bitmap
                saveBitmapToCache(context, bitmap)?.let { path ->
                    viewModel.attachedPhotoPath = path
                }
            } ?: cameraUri.value?.let { uri ->
                decodeBitmapWithOrientation(context, uri)?.let { bitmap ->
                    viewModel.attachedPhoto = bitmap
                    saveBitmapToCache(context, bitmap)?.let { path ->
                        viewModel.attachedPhotoPath = path
                    }
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            decodeBitmapWithOrientation(context, it)?.let { bmp ->
                viewModel.attachedPhoto = bmp
                saveBitmapToCache(context, bmp)?.let { path ->
                    viewModel.attachedPhotoPath = path
                }
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

    BackHandler(enabled = showCalendarScreen || showProfileScreen || showBodySettingsScreen || showAppSettingsScreen || showAnalyticsScreen || showSubscriptionScreen) {
        when {
            showProfileScreen -> {
                showProfileScreen = false
                currentScreen = Screen.Main
            }
            showSubscriptionScreen -> {
                showSubscriptionScreen = false
                currentScreen = Screen.Main
            }
            showBodySettingsScreen -> {
                showBodySettingsScreen = false
                showProfileScreen = true
            }
            showAppSettingsScreen -> {
                showAppSettingsScreen = false
                currentScreen = Screen.Main
            }
            showAnalyticsScreen -> {
                showAnalyticsScreen = false
                currentScreen = Screen.Main
            }
            showCalendarScreen -> {
                showCalendarScreen = false
                currentScreen = Screen.Main
            }
        }
    }

    // Запускаем загрузку при старте без таймаута
    LaunchedEffect(Unit) {
        viewModel.startLoading()
    }

    when (authState) {
        AuthManager.AuthState.LOADING -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        AuthManager.AuthState.UNAUTHENTICATED -> {
            AuthScreen(authManager = authManager, onAuthSuccess = {
                currentScreen = Screen.Main
            })
        }

        AuthManager.AuthState.AUTHENTICATED -> {
            val targetScreen = when {
                showProfileScreen -> Screen.Profile
                showBodySettingsScreen -> Screen.BodySettings
                showAppSettingsScreen -> Screen.AppSettings
                showSubscriptionScreen -> Screen.Subscription
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

                    Screen.Profile -> {
                        ProfileScreen(
                            authManager = authManager,
                            onBack = {
                                showProfileScreen = false
                                currentScreen = Screen.Main
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
                                        currentScreen = Screen.Main
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
                                currentScreen = Screen.Main
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
                                currentScreen = Screen.Main
                            },
                            onSignOut = { 
                                authManager.signOut() 
                            }
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
                            onSettingsClick = { showAppSettingsScreen = true },
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