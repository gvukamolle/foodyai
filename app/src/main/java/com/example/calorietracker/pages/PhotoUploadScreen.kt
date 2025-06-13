package com.example.calorietracker.pages

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.calorietracker.CalorieTrackerViewModel
import com.example.calorietracker.network.NetworkModule
import com.example.calorietracker.network.UserProfileData
import com.example.calorietracker.network.safeApiCall
import com.example.calorietracker.utils.calculateAge
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoUploadScreen(
    viewModel: CalorieTrackerViewModel,
    onBack: () -> Unit
) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
        previewBitmap = uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отправка фото") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp)
                )
            } else {
                Text("Выберите изображение")
            }

            Button(onClick = { pickLauncher.launch("image/*") }) {
                Text("Выбрать фото")
            }

            if (imageUri != null) {
                Button(
                    onClick = {
                        scope.launch {
                            isSending = true
                            val age = calculateAge(viewModel.userProfile.birthday)
                            val profile = UserProfileData(
                                age = age,
                                weight = viewModel.userProfile.weight,
                                height = viewModel.userProfile.height,
                                gender = viewModel.userProfile.gender,
                                activityLevel = viewModel.userProfile.condition,
                                goal = viewModel.userProfile.goal
                            )
                            val tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)
                            context.contentResolver.openInputStream(imageUri!!)?.use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val part = MultipartBody.Part.createFormData("photo", tempFile.name, requestBody)

                            val result = safeApiCall {
                                NetworkModule.makeService.analyzeFoodPhoto(
                                    webhookId = "653st2c10rmg92nlltf3y0m8sggxaac6",
                                    photo = part,
                                    userProfile = profile
                                )
                            }
                            resultText = if (result.isSuccess) {
                                result.getOrNull()?.food?.name ?: "Успешно"
                            } else {
                                "Ошибка отправки"
                            }
                            tempFile.delete()
                            isSending = false
                        }
                    },
                    enabled = !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Отправить")
                    }
                }
            }

            resultText?.let { Text(it) }
        }
    }
}
