package com.example.calorietracker.di

import com.example.calorietracker.data.repositories.ChatRepositoryImpl
import com.example.calorietracker.data.repositories.FoodRepositoryImpl
import com.example.calorietracker.data.repositories.NutritionRepositoryImpl
import com.example.calorietracker.data.repositories.UserRepositoryImpl
import com.example.calorietracker.domain.repositories.ChatRepository
import com.example.calorietracker.domain.repositories.FoodRepository
import com.example.calorietracker.domain.repositories.NutritionRepository
import com.example.calorietracker.domain.repositories.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding domain repository interfaces to their data layer implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {
    
    /**
     * Bind FoodRepository interface to its implementation
     */
    @Binds
    @Singleton
    abstract fun bindFoodRepository(
        foodRepositoryImpl: FoodRepositoryImpl
    ): FoodRepository
    
    /**
     * Bind UserRepository interface to its implementation
     */
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
    
    /**
     * Bind NutritionRepository interface to its implementation
     */
    @Binds
    @Singleton
    abstract fun bindNutritionRepository(
        nutritionRepositoryImpl: NutritionRepositoryImpl
    ): NutritionRepository
    
    /**
     * Bind ChatRepository interface to its implementation
     */
    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository
}