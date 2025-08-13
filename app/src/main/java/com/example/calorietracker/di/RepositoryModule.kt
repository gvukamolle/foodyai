package com.example.calorietracker.di

import com.example.calorietracker.data.DataRepository
import com.example.calorietracker.data.mappers.ChatMapper
import com.example.calorietracker.data.mappers.FoodMapper
import com.example.calorietracker.data.mappers.NutritionMapper
import com.example.calorietracker.data.mappers.UserMapper
import com.example.calorietracker.data.repositories.ChatRepositoryImpl
import com.example.calorietracker.data.repositories.FoodRepositoryImpl
import com.example.calorietracker.data.repositories.NutritionRepositoryImpl
import com.example.calorietracker.data.repositories.UserRepositoryImpl
import com.example.calorietracker.network.MakeService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations with proper dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideFoodRepositoryImpl(
        makeService: MakeService,
        dataRepository: DataRepository,
        foodMapper: FoodMapper,
        userRepositoryImpl: UserRepositoryImpl
    ): FoodRepositoryImpl {
        return FoodRepositoryImpl(makeService, dataRepository, foodMapper, userRepositoryImpl)
    }
    
    @Provides
    @Singleton
    fun provideUserRepositoryImpl(
        dataRepository: DataRepository,
        userMapper: UserMapper
    ): UserRepositoryImpl {
        return UserRepositoryImpl(dataRepository, userMapper)
    }
    
    @Provides
    @Singleton
    fun provideNutritionRepositoryImpl(
        dataRepository: DataRepository,
        nutritionMapper: NutritionMapper,
        foodMapper: FoodMapper
    ): NutritionRepositoryImpl {
        return NutritionRepositoryImpl(dataRepository, nutritionMapper, foodMapper)
    }
    
    @Provides
    @Singleton
    fun provideChatRepositoryImpl(
        dataRepository: DataRepository,
        chatMapper: ChatMapper,
        makeService: MakeService
    ): ChatRepositoryImpl {
        return ChatRepositoryImpl(dataRepository, chatMapper, makeService)
    }
}