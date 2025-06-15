package com.example.calorietracker.pages

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.example.calorietracker.network.MakeService
import com.example.calorietracker.network.safeApiCall
import com.example.calorietracker.utils.calculateAge
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoUploadScreen(
    viewModel: CalorieTrackerViewModel,
    onBack: () -> Unit
) {
    var imageFile by remember { mutableStateOf<File?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        previewBitmap = uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
        imageFile = uri?.let {
            val temp = File.createTempFile("upload", ".jpg", context.cacheDir)
            context.contentResolver.openInputStream(it)?.use { input ->
                temp.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            temp
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        previewBitmap = bitmap
        imageFile = bitmap?.let { bmp ->
            val temp = File.createTempFile("camera", ".jpg", context.cacheDir)
            FileOutputStream(temp).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            temp
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pickLauncher.launch("image/*") }) {
                    Text("Выбрать фото")
                }
                OutlinedButton(onClick = { cameraLauncher.launch(null) }) {
                    Text("Сделать фото")
                }
            }

            if (imageFile != null) {
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
                            val file = imageFile!!
                            val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val part = MultipartBody.Part.createFormData("photo", file.name, requestBody)

                            // Конвертируем UserProfileData в JSON RequestBody
                            val gson = Gson()
                            val profileJson = gson.toJson(profile)
                            val profileRequestBody = profileJson.toRequestBody("application/json".toMediaTypeOrNull())

                            // Конвертируем userId в RequestBody
                            val userIdRequestBody = viewModel.userId.toRequestBody("text/plain".toMediaTypeOrNull())

                            val response = safeApiCall {
                                NetworkModule.makeService.analyzeFoodPhoto(
                                    webhookId = MakeService.WEBHOOK_ID,
                                    photo = part,
                                    userProfile = profileRequestBody,
                                    userId = userIdRequestBody
                                )
                            }
                            resultText = response.getOrNull()?.food?.name ?: "Ошибка отправки"
                            file.delete()
                            imageFile = null
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