package com.example.calorietracker.analyzer

import android.graphics.Bitmap
import kotlinx.coroutines.delay
import com.example.calorietracker.FoodItem

class FoodAnalyzer {
    suspend fun analyzeFood(bitmap: Bitmap): FoodItem {
        delay(2000)
        val foods = listOf(
            FoodItem("Куриная грудка с рисом", 450, 35, 8, 55, 300),
            FoodItem("Овсяная каша с ягодами", 320, 12, 6, 58, 250),
            FoodItem("Греческий салат", 280, 8, 22, 15, 200),
            FoodItem("Стейк из лосося", 380, 42, 18, 2, 180),
            FoodItem("Паста болоньезе", 520, 24, 16, 68, 350)
        )
        return foods.random()
    }
}
