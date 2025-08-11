package com.example.calorietracker

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.auth.AuthManager
import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.managers.NetworkManager
import com.example.calorietracker.network.FoodAnalysisRequest
import com.example.calorietracker.network.MakeService
import com.example.calorietracker.network.NetworkModule
import com.example.calorietracker.network.safeApiCall
import com.example.calorietracker.utils.AIUsageManager
import com.example.calorietracker.data.UserProfile
import com.example.calorietracker.extensions.toNetworkProfile
import com.example.calorietracker.utils.getOrCreateUserId
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

/**
 * ViewModel responsible for photo and text analysis using AI.
 * Contains the original complex logic from the monolithic CalorieTrackerViewModel.
 * Many helper methods still reside in other ViewModels (e.g. ChatViewModel) and are
 * referenced through optional setters.
 */
@HiltViewModel
class FoodAnalysisViewModel @Inject constructor(
    private val repository: DataRepository,
    @ApplicationContext private val context: Context,
    private val authManager: AuthManager,
    private val networkManager: NetworkManager
) : ViewModel() {

    // --- External view models for communication ---
    private var chatViewModel: ChatViewModel? = null
    private var nutritionViewModel: NutritionViewModel? = null

    fun attachChatViewModel(vm: ChatViewModel) { chatViewModel = vm }
    fun attachNutritionViewModel(vm: NutritionViewModel) { nutritionViewModel = vm }

    // --- State copied from the old CalorieTrackerViewModel ---
    var isAnalyzing by mutableStateOf(false)
        private set

    var attachedPhoto by mutableStateOf<Bitmap?>(null)
        private set

    var pendingPhoto by mutableStateOf<Bitmap?>(null)
        private set

    var currentFoodSource by mutableStateOf<String?>(null)
        private set

    var inputMethod by mutableStateOf<String?>(null)
        private set

    var pendingDescription by mutableStateOf("")
        private set

    var showAILoadingScreen by mutableStateOf(false)
        private set

    private var lastApiCall: (() -> Unit)? = null

    var showManualInputDialog by mutableStateOf(false)
        private set

    var showAILimitDialog by mutableStateOf(false)
        private set

    var pendingAIAction by mutableStateOf<(() -> Unit)?>(null)
        private set

    var prefillFood by mutableStateOf<FoodItem?>(null)
        private set

    var showPhotoDialog by mutableStateOf(false)
        private set

    private var lastPhotoPath: String? = null
    private var lastPhotoCaption: String = ""
    private var lastDescriptionMessage: String = ""
    var isRecordMode by mutableStateOf(false)
    var isDailyAnalysisEnabled by mutableStateOf(false)

    // User information
    private val userId = getOrCreateUserId(context)
    var userProfile: UserProfile = UserProfile()

    init {
        viewModelScope.launch {
            userProfile = repository.getUserProfile() ?: UserProfile()
        }
    }

    // --- Public setters for mutable states ---
    fun setAttachedPhoto(value: Bitmap?) { attachedPhoto = value }
    fun setPendingPhoto(value: Bitmap?) { pendingPhoto = value }
    fun setCurrentFoodSource(value: String?) { currentFoodSource = value }
    fun setInputMethod(value: String?) { inputMethod = value }
    fun setPendingDescription(value: String) { pendingDescription = value }

    fun onPhotoSelected(bitmap: Bitmap) {
        attachedPhoto = bitmap
        pendingPhoto = bitmap
    }

    fun removeAttachedPhoto() {
        attachedPhoto = null
        pendingPhoto = null
    }

    fun canAnalyzeDescription(): Boolean {
        return pendingDescription.isNotBlank() && !isAnalyzing && networkManager.isOnline.value
    }

    private suspend fun checkInternetConnection(): Boolean {
        return networkManager.isOnline.value
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val aspect = bitmap.height.toFloat() / bitmap.width.toFloat()
        val newHeight = (maxWidth * aspect).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }

    private fun handleError(errorMessage: String, retryAction: (() -> Unit)? = null) {
        val errorMsgId = UUID.randomUUID().toString()
        val errorMsg = ChatMessage(
            id = errorMsgId,
            type = MessageType.AI,
            content = "$errorMessage. Введите данные вручную.",
            isError = true,
            retryAction = if (retryAction != null || lastApiCall != null) {
                {
                    chatViewModel?.removeMessageWithAnimation(errorMsgId)
                    (retryAction ?: lastApiCall)?.invoke()
                }
            } else null
        )
        chatViewModel?.addMessage(errorMsg)
        showManualInputDialog = true

        if (retryAction == null && lastApiCall == null) {
            viewModelScope.launch {
                delay(5000)
                chatViewModel?.removeMessageWithAnimation(errorMsgId)
            }
        }
    }

    data class FoodDataFromAnswer(
        val food: String,
        val name: String,
        val calories: Int,
        val protein: Double,
        val fat: Double,
        val carbs: Double,
        val weight: String,
        val opinion: String? = null
    )

    /**
     * Full implementation of photo analysis using AI.
     * Copied from the original CalorieTrackerViewModel with minor adjustments
     * to work with external ChatViewModel and NutritionViewModel.
     */
    suspend fun analyzePhotoWithAI(bitmap: Bitmap, caption: String = "") {
        isAnalyzing = true
        currentFoodSource = "ai_photo"
        inputMethod = "photo"

        val chatFile = File.createTempFile("photo_chat", ".jpg", context.cacheDir)
        FileOutputStream(chatFile).use { outputStream ->
            val scaledBitmap = scaleBitmap(bitmap, 800)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        }
        lastPhotoPath = chatFile.absolutePath
        lastPhotoCaption = caption
        val tempFile = chatFile

        if (!checkInternetConnection()) {
            val errorMsgId = UUID.randomUUID().toString()
            val errorMsg = ChatMessage(
                id = errorMsgId,
                type = MessageType.AI,
                content = "Нет подключения к интернету. Пожалуйста, введите данные о продукте вручную.",
                isError = true,
                retryAction = {
                    chatViewModel?.removeMessageWithAnimation(errorMsgId)
                    viewModelScope.launch { analyzePhotoWithAI(bitmap, caption) }
                }
            )
            chatViewModel?.addMessage(errorMsg)
            isAnalyzing = false
            showManualInputDialog = true
            return
        }

        val currentUser = authManager.currentUser.value
        if (currentUser != null && !AIUsageManager.canUseAI(currentUser)) {
            showAILimitDialog = true
            pendingAIAction = { viewModelScope.launch { analyzePhotoWithAI(bitmap, caption) } }
            isAnalyzing = false
            return
        }

        chatViewModel?.addMessage(
            ChatMessage(
                type = MessageType.USER,
                content = caption.ifEmpty { "Фото для анализа" },
                imagePath = lastPhotoPath,
                animate = true
            )
        )

        chatViewModel?.startLoadingPhrases("photo")

        lastApiCall = { viewModelScope.launch { analyzePhotoWithAI(bitmap, caption) } }

        try {
            val gson = Gson()
            val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val photoPart = MultipartBody.Part.createFormData("photo", tempFile.name, requestBody)

            val profileJson = gson.toJson(userProfile.toNetworkProfile())
            val profileBody = profileJson.toRequestBody("application/json".toMediaTypeOrNull())
            val userIdBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())
            val captionBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())
            val messageTypeBody = "photo".toRequestBody("text/plain".toMediaTypeOrNull())
            val firstBody = repository.isFirstMessageOfDay().toString().toRequestBody("text/plain".toMediaTypeOrNull())
            repository.recordLastUserMessageTime()

            val response = safeApiCall {
                NetworkModule.makeService.analyzeFoodPhoto(
                    webhookId = MakeService.WEBHOOK_ID,
                    photo = photoPart,
                    userProfile = profileBody,
                    userId = userIdBody,
                    caption = captionBody,
                    messageType = messageTypeBody,
                    isFirstMessageOfDay = firstBody
                )
            }

            if (response.isSuccess && currentUser != null) {
                viewModelScope.launch {
                    val updated = AIUsageManager.incrementUsage(currentUser)
                    authManager.updateUserData(updated)
                }
            }

            if (!response.isSuccess) {
                chatViewModel?.stopLoadingPhrases()
                handleError("Ошибка соединения", lastApiCall)
                return
            }

            val result = response.getOrNull()
            if (result?.answer == null) {
                chatViewModel?.stopLoadingPhrases()
                handleError("Сервер не вернул данные", lastApiCall)
                return
            }

            try {
                Log.d("FoodAnalysis", "Ответ от сервера: ${result.answer}")
                val foodData = gson.fromJson(result.answer, FoodDataFromAnswer::class.java)
                when (foodData.food.trim().lowercase()) {
                    "нет", "no" -> {
                        chatViewModel?.stopLoadingPhrases()
                        chatViewModel?.addMessage(
                            ChatMessage(
                                type = MessageType.AI,
                                content = "❌ На фото не обнаружено еды. Попробуйте сделать другое фото или введите данные вручную."
                            )
                        )
                        Toast.makeText(context, "На фото не обнаружено еды", Toast.LENGTH_LONG).show()
                        showPhotoDialog = false
                    }
                    "да", "yes" -> {
                        val weightValue = foodData.weight.toFloatOrNull() ?: 100f
                        val factor = weightValue / 100f
                        val foodItem = FoodItem(
                            name = foodData.name,
                            calories = (foodData.calories * factor).roundToInt(),
                            protein = foodData.protein * factor,
                            fat = foodData.fat * factor,
                            carbs = foodData.carbs * factor,
                            weight = foodData.weight,
                            source = currentFoodSource ?: "ai_photo",
                            aiOpinion = foodData.opinion
                        )

                        chatViewModel?.stopLoadingPhrases()
                        chatViewModel?.addMessage(
                            ChatMessage(
                                type = MessageType.FOOD_CONFIRMATION,
                                content = "",
                                foodItem = foodItem,
                                animate = true
                            )
                        )
                        prefillFood = foodItem
                    }
                    else -> {
                        chatViewModel?.stopLoadingPhrases()
                        handleError("Не удалось определить тип продукта", lastApiCall)
                    }
                }
            } catch (e: Exception) {
                Log.e("FoodAnalysis", "Ошибка парсинга ответа", e)
                chatViewModel?.stopLoadingPhrases()
                handleError("Неверный формат ответа от сервера", lastApiCall)
            }
        } catch (e: Exception) {
            chatViewModel?.stopLoadingPhrases()
            handleError("Ошибка анализа изображения: ${e.message}", lastApiCall)
        } finally {
            isAnalyzing = false
            if (chatViewModel?.messages?.any { it.isError } != true) {
                lastApiCall = null
            }
        }
    }

    /**
     * Full implementation of text description analysis. Adapted from original code.
     */
    fun analyzeDescriptionWithAI() {
        if (!canAnalyzeDescription()) return

        val textToAnalyze = pendingDescription
        pendingDescription = ""
        lastDescriptionMessage = textToAnalyze

        if (isRecordMode) {
            chatViewModel?.addMessage(
                ChatMessage(
                    type = MessageType.USER,
                    content = textToAnalyze,
                    animate = true
                )
            )
        }

        viewModelScope.launch {
            isAnalyzing = true
            currentFoodSource = "ai_description"
            inputMethod = "text"

            lastApiCall = {
                pendingDescription = textToAnalyze
                analyzeDescriptionWithAI()
            }

            chatViewModel?.startLoadingPhrases("text")

            if (!checkInternetConnection()) {
                chatViewModel?.stopLoadingPhrases()
                handleError("Нет подключения к интернету") {
                    pendingDescription = textToAnalyze
                    analyzeDescriptionWithAI()
                }
                isAnalyzing = false
                return@launch
            }

            val currentUser = authManager.currentUser.value
            if (currentUser != null && !AIUsageManager.canUseAI(currentUser)) {
                showAILimitDialog = true
                pendingAIAction = { analyzeDescriptionWithAI() }
                isAnalyzing = false
                return@launch
            }

            try {
                val isFirstOfDay = repository.isFirstMessageOfDay()
                repository.recordLastUserMessageTime()
                val request = FoodAnalysisRequest(
                    weight = 100,
                    userProfile = userProfile.toNetworkProfile(),
                    message = textToAnalyze,
                    userId = userId,
                    messageType = if (isDailyAnalysisEnabled) "dayfood_analysis" else "analysis",
                    isFirstMessageOfDay = isFirstOfDay
                )

                val response = safeApiCall {
                    NetworkModule.makeService.analyzeFood(
                        webhookId = MakeService.WEBHOOK_ID,
                        request = request
                    )
                }

                if (!response.isSuccess) {
                    chatViewModel?.stopLoadingPhrases()
                    handleError("Ошибка соединения", lastApiCall)
                    return@launch
                }

                val answer = response.getOrNull()?.answer
                if (answer == null) {
                    chatViewModel?.stopLoadingPhrases()
                    handleError("Сервер не вернул данные", lastApiCall)
                    return@launch
                }

                val foodData = Gson().fromJson(answer, FoodDataFromAnswer::class.java)
                val weightValue = foodData.weight.toFloatOrNull() ?: 100f
                val factor = weightValue / 100f
                val foodItem = FoodItem(
                    name = foodData.name,
                    calories = (foodData.calories * factor).roundToInt(),
                    protein = foodData.protein * factor,
                    fat = foodData.fat * factor,
                    carbs = foodData.carbs * factor,
                    weight = foodData.weight,
                    source = currentFoodSource ?: "ai_description",
                    aiOpinion = foodData.opinion
                )

                chatViewModel?.stopLoadingPhrases()
                chatViewModel?.addMessage(
                    ChatMessage(
                        type = MessageType.FOOD_CONFIRMATION,
                        content = "",
                        foodItem = foodItem,
                        animate = true
                    )
                )
                prefillFood = foodItem

                if (currentUser != null) {
                    val updatedUserData = AIUsageManager.incrementUsage(currentUser)
                    authManager.updateUserData(updatedUserData)
                }
            } catch (e: Exception) {
                Log.e("FoodAnalysis", "Ошибка анализа описания", e)
                chatViewModel?.stopLoadingPhrases()
                handleError("Не удалось проанализировать", lastApiCall)
            } finally {
                isAnalyzing = false
                if (chatViewModel?.messages?.any { it.isError } != true) {
                    lastApiCall = null
                }
            }
        }
    }
}