package com.example.calorietracker.di

import com.example.calorietracker.domain.repositories.ChatRepository
import com.example.calorietracker.domain.repositories.FoodRepository
import com.example.calorietracker.domain.repositories.NutritionRepository
import com.example.calorietracker.domain.repositories.UserRepository
import com.example.calorietracker.domain.usecases.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Hilt module for providing Use Cases to ViewModels
 */
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    
    // ========== Food Use Cases ==========
    
    @Provides
    @ViewModelScoped
    fun provideAnalyzeFoodPhotoUseCase(
        foodRepository: FoodRepository
    ): AnalyzeFoodPhotoUseCase {
        return AnalyzeFoodPhotoUseCase(foodRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideAnalyzeFoodDescriptionUseCase(
        foodRepository: FoodRepository
    ): AnalyzeFoodDescriptionUseCase {
        return AnalyzeFoodDescriptionUseCase(foodRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideSaveFoodIntakeUseCase(
        foodRepository: FoodRepository,
        nutritionRepository: NutritionRepository
    ): SaveFoodIntakeUseCase {
        return SaveFoodIntakeUseCase(foodRepository, nutritionRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideGetFoodHistoryUseCase(
        foodRepository: FoodRepository
    ): GetFoodHistoryUseCase {
        return GetFoodHistoryUseCase(foodRepository)
    }
    
    // ========== User Use Cases ==========
    
    @Provides
    @ViewModelScoped
    fun provideGetUserProfileUseCase(
        userRepository: UserRepository
    ): GetUserProfileUseCase {
        return GetUserProfileUseCase(userRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideSaveUserProfileUseCase(
        userRepository: UserRepository
    ): SaveUserProfileUseCase {
        return SaveUserProfileUseCase(userRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideCalculateNutritionTargetsUseCase(
        userRepository: UserRepository
    ): CalculateNutritionTargetsUseCase {
        return CalculateNutritionTargetsUseCase(userRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideValidateUserDataUseCase(): ValidateUserDataUseCase {
        return ValidateUserDataUseCase()
    }
    
    // ========== Nutrition Use Cases ==========
    
    @Provides
    @ViewModelScoped
    fun provideGetDailyIntakeUseCase(
        nutritionRepository: NutritionRepository,
        userRepository: UserRepository
    ): GetDailyIntakeUseCase {
        return GetDailyIntakeUseCase(nutritionRepository, userRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideGetWeeklyIntakeUseCase(
        nutritionRepository: NutritionRepository
    ): GetWeeklyIntakeUseCase {
        return GetWeeklyIntakeUseCase(nutritionRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideGetMonthlyIntakeUseCase(
        nutritionRepository: NutritionRepository
    ): GetMonthlyIntakeUseCase {
        return GetMonthlyIntakeUseCase(nutritionRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideCalculateNutritionProgressUseCase(): CalculateNutritionProgressUseCase {
        return CalculateNutritionProgressUseCase()
    }
    
    // ========== Chat & AI Use Cases ==========
    
    @Provides
    @ViewModelScoped
    fun provideSendChatMessageUseCase(
        chatRepository: ChatRepository
    ): SendChatMessageUseCase {
        return SendChatMessageUseCase(chatRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideGetChatHistoryUseCase(
        chatRepository: ChatRepository
    ): GetChatHistoryUseCase {
        return GetChatHistoryUseCase(chatRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideProcessAIResponseUseCase(
        chatRepository: ChatRepository
    ): ProcessAIResponseUseCase {
        return ProcessAIResponseUseCase(chatRepository)
    }
    
    @Provides
    @ViewModelScoped
    fun provideValidateAIUsageLimitsUseCase(
        chatRepository: ChatRepository
    ): ValidateAIUsageLimitsUseCase {
        return ValidateAIUsageLimitsUseCase(chatRepository)
    }
}