package com.example.calorietracker.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*


// Data classes for requests
data class FoodAnalysisRequest(
    val name: String,
    val weight: Int,
    val userProfile: UserProfileData
)

data class ImageAnalysisRequest(
    val imageBase64: String,
    val userProfile: UserProfileData
)

// Запрос при отправке ссылки на изображение
data class ImageUrlAnalysisRequest(
    val imageUrl: String,
    val userProfile: UserProfileData
)

data class MealPlanRequest(
    val userProfile: UserProfileData,
    val preferences: List<String>,
    val allergies: List<String>?,
    val days: Int = 7
)

data class NutritionRequest(
    val dailyIntakes: List<DailyIntakeData>,
    val userProfile: UserProfileData,
    val period: String = "week"
)

data class RecommendationRequest(
    val userProfile: UserProfileData,
    val recentFoods: List<FoodItemData>,
    val goal: String
)

data class UserProfileData(
    val age: Int,
    val weight: Int,
    val height: Int,
    val gender: String,
    val activityLevel: String,
    val goal: String
)

data class DailyIntakeData(
    val date: String,
    val calories: Int,
    val proteins: Int,
    val fats: Int,
    val carbs: Int
)

data class FoodItemData(
    val name: String,
    val calories: Int,
    val proteins: Int,
    val fats: Int,
    val carbs: Int,
    val weight: Int
)

// Запрос для записи съеденной еды
data class LogFoodRequest(
    val userId: String,
    val foodData: FoodItemData,
    val mealType: String,
    val timestamp: Long,
    val date: String, // YYYY-MM-DD
    val time: String, // HH:mm
    val source: String, // "ai_photo" или "manual"
    val userProfile: UserProfileData
)

data class LogFoodResponse(
    val status: String,
    val message: String,
    val threadId: String? = null
)

// NEW: Data classes for AI chat
data class AiChatRequest(
    val message: String,
    val userProfile: UserProfileData,
    val userId: String
)

data class AiChatResponse(
    val status: String,
    val answer: String
)

// Response classes
// Исправленный response для парсинга вложенного JSON как строки
data class FoodAnalysisResponse(
    val status: String,
    val answer: String? = null,
)

// Отдельный класс для парсинга содержимого answer
data class FoodDataFromAnswer(
    val food: String,      // "да" или "нет"
    val name: String,      // название продукта
    val calories: Int,     // Gson автоматически распарсит число
    val proteins: Int,     // Gson автоматически распарсит число
    val fats: Int,         // Gson автоматически распарсит число
    val carbs: Int,        // Gson автоматически распарсит число
    val weight: String     // оставляем String (может быть "100" или "100г")
)

data class EnhancedFoodData(
    val name: String,
    val calories: Double,
    val proteins: Double,
    val fats: Double,
    val carbs: Double,
    val weight: Int,
    val micronutrients: Map<String, Double>?,
    val healthScore: Int?,
    val tags: List<String>?,
    val alternatives: List<FoodAlternative>?
)

data class FoodAlternative(
    val name: String,
    val reason: String,
    val nutritionComparison: String
)

data class MealPlanResponse(
    val status: String,
    val mealPlan: List<DayMealPlan>?,
    val shoppingList: List<ShoppingItem>?
)

data class DayMealPlan(
    val day: String,
    val meals: List<PlannedMeal>
)

data class PlannedMeal(
    val type: String,
    val name: String,
    val calories: Int,
    val recipe: String?,
    val ingredients: List<String>?
)

data class ShoppingItem(
    val item: String,
    val quantity: String,
    val category: String
)

data class NutritionResponse(
    val status: String,
    val analysis: NutritionAnalysis?,
    val charts: NutritionCharts?,
    val insights: List<NutritionInsight>?
)

data class NutritionAnalysis(
    val averageDailyCalories: Double,
    val macroBalance: MacroBalance,
    val trend: String,
    val score: Int
)

data class MacroBalance(
    val proteinsPercent: Double,
    val fatsPercent: Double,
    val carbsPercent: Double
)

data class NutritionCharts(
    val caloriesTrend: List<ChartPoint>,
    val macrosDistribution: List<ChartPoint>,
    val weeklyComparison: List<ChartPoint>
)

data class ChartPoint(
    val label: String,
    val value: Double
)

data class NutritionInsight(
    val type: String,
    val message: String,
    val priority: String
)

data class RecommendationResponse(
    val status: String,
    val recommendations: List<PersonalizedRecommendation>?
)

data class PersonalizedRecommendation(
    val id: String,
    val type: String,
    val title: String,
    val content: String,
    val priority: Int,
    val actionItems: List<String>?
)

data class HealthResponse(
    val status: String,
    val timestamp: Long
)

// Make.com Service Interface
interface MakeService {
    companion object {
        const val BASE_URL = "https://hook.us2.make.com/"
        const val WEBHOOK_ID = "653st2c10rmg92nlltf3y0m8sggxaac6"
    }

    @Headers("Content-Type: application/json")
    @POST("{webhookId}")
    suspend fun analyzeFood(
        @Path("webhookId") webhookId: String,
        @Body request: FoodAnalysisRequest
    ): FoodAnalysisResponse

    @Headers("Content-Type: application/json")
    @POST("{webhookId}")
    suspend fun analyzeFoodImage(
        @Path("webhookId") webhookId: String,
        @Body request: ImageAnalysisRequest
    ): FoodAnalysisResponse

    @Headers("Content-Type: application/json")
    @POST("{webhookId}")
    suspend fun planMealWeek(
        @Path("webhookId") webhookId: String,
        @Body request: MealPlanRequest
    ): MealPlanResponse

    @Headers("Content-Type: application/json")
    @POST("{webhookId}")
    suspend fun analyzeNutrition(
        @Path("webhookId") webhookId: String,
        @Body request: NutritionRequest
    ): NutritionResponse

    @Headers("Content-Type: application/json")
    @POST("{webhookId}")
    suspend fun getRecommendations(
        @Path("webhookId") webhookId: String,
        @Body request: RecommendationRequest
    ): RecommendationResponse

    // Добавьте этот метод в interface MakeService:
    @Headers("Content-Type: application/json")
    @POST("{webhookId}")
    suspend fun logFoodToThread(
        @Path("webhookId") webhookId: String,
        @Body request: LogFoodRequest
    ): LogFoodResponse

    // Загрузка реального файла изображения
    @Multipart
    @POST("{webhookId}")
    suspend fun analyzeFoodPhoto(
        @Path("webhookId") webhookId: String,
        @Part photo: MultipartBody.Part,
        @Part("userProfile") userProfile: RequestBody,
        @Part("userId") userId: RequestBody
    ): FoodAnalysisResponse


    // Анализ по URL на изображение
    @Headers("Content-Type: application/json")
    @POST("{webhookId}")
    suspend fun analyzeFoodImageByUrl(
        @Path("webhookId") webhookId: String,
        @Body request: ImageUrlAnalysisRequest
    ): FoodAnalysisResponse

    // --- Новый метод для AI чата ---
    @Headers("Content-Type: application/json")
    @POST("{webhookId}")
    suspend fun askAiDietitian(
        @Path("webhookId") webhookId: String,
        @Body request: AiChatRequest
    ): AiChatResponse

    @GET("{webhookId}")
    suspend fun checkHealth(
        @Path("webhookId") webhookId: String
    ): HealthResponse
}