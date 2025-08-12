package com.example.calorietracker.di

import com.example.calorietracker.data.mappers.ChatMapper
import com.example.calorietracker.data.mappers.FoodMapper
import com.example.calorietracker.data.mappers.NutritionMapper
import com.example.calorietracker.data.mappers.UserMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing data mappers
 */
@Module
@InstallIn(SingletonComponent::class)
object MapperModule {
    
    @Provides
    @Singleton
    fun provideFoodMapper(): FoodMapper {
        return FoodMapper()
    }
    
    @Provides
    @Singleton
    fun provideUserMapper(): UserMapper {
        return UserMapper()
    }
    
    @Provides
    @Singleton
    fun provideNutritionMapper(
        foodMapper: FoodMapper
    ): NutritionMapper {
        return NutritionMapper(foodMapper)
    }
    
    @Provides
    @Singleton
    fun provideChatMapper(
        foodMapper: FoodMapper
    ): ChatMapper {
        return ChatMapper(foodMapper)
    }
}