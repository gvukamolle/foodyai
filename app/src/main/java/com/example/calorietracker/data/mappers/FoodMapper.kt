package com.example.calorietracker.data.mappers

import com.example.calorietracker.data.FoodItem
import com.example.calorietracker.data.Meal
import com.example.calorietracker.data.MealType as DataMealType
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.Meal as DomainMeal
import com.example.calorietracker.domain.entities.common.FoodSource
import com.example.calorietracker.domain.entities.common.MealType
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for transforming Food entities between domain and data layers
 */
@Singleton
class FoodMapper @Inject constructor() {
    
    /**
     * Convert domain Food to data FoodItem
     */
    fun mapDomainToData(food: Food): FoodItem {
        return FoodItem(
            name = food.name,
            calories = food.calories,
            protein = food.protein,
            fat = food.fat,
            carbs = food.carbs,
            weight = food.weight,
            source = food.source.name.lowercase(),
            aiOpinion = food.aiOpinion
        )
    }
    
    /**
     * Convert data FoodItem to domain Food
     */
    fun mapDataToDomain(foodItem: FoodItem): Food {
        return Food(
            name = foodItem.name,
            calories = foodItem.calories,
            protein = foodItem.protein,
            fat = foodItem.fat,
            carbs = foodItem.carbs,
            weight = foodItem.weight,
            source = FoodSource.fromString(foodItem.source),
            aiOpinion = foodItem.aiOpinion
        )
    }
    
    /**
     * Convert domain Meal to data Meal
     */
    fun mapDomainMealToData(meal: DomainMeal): Meal {
        return Meal(
            type = mapDomainMealTypeToData(meal.type),
            foods = meal.foods.map { mapDomainToData(it) },
            time = meal.getTimestampMillis()
        )
    }
    
    /**
     * Convert data Meal to domain Meal
     */
    fun mapDataMealToDomain(meal: Meal): DomainMeal {
        return DomainMeal.fromLegacyData(
            type = mapDataMealTypeToDomain(meal.type),
            foods = meal.foods.map { mapDataToDomain(it) },
            timestampMillis = meal.time
        )
    }
    
    /**
     * Convert domain MealType to data MealType
     */
    private fun mapDomainMealTypeToData(mealType: MealType): DataMealType {
        return when (mealType) {
            MealType.BREAKFAST -> DataMealType.BREAKFAST
            MealType.LUNCH -> DataMealType.LUNCH
            MealType.DINNER -> DataMealType.DINNER
            MealType.SNACK -> DataMealType.SUPPER // Legacy mapping
        }
    }
    
    /**
     * Convert data MealType to domain MealType
     */
    private fun mapDataMealTypeToDomain(mealType: DataMealType): MealType {
        return when (mealType) {
            DataMealType.BREAKFAST -> MealType.BREAKFAST
            DataMealType.LUNCH -> MealType.LUNCH
            DataMealType.DINNER -> MealType.DINNER
            DataMealType.SUPPER -> MealType.SNACK // Legacy mapping
        }
    }
    
    /**
     * Convert list of domain Foods to data FoodItems
     */
    fun mapDomainListToData(foods: List<Food>): List<FoodItem> {
        return foods.map { mapDomainToData(it) }
    }
    
    /**
     * Convert list of data FoodItems to domain Foods
     */
    fun mapDataListToDomain(foodItems: List<FoodItem>): List<Food> {
        return foodItems.map { mapDataToDomain(it) }
    }
    
    /**
     * Convert list of domain Meals to data Meals
     */
    fun mapDomainMealsToData(meals: List<DomainMeal>): List<Meal> {
        return meals.map { mapDomainMealToData(it) }
    }
    
    /**
     * Convert list of data Meals to domain Meals
     */
    fun mapDataMealsToDomain(meals: List<Meal>): List<DomainMeal> {
        return meals.map { mapDataMealToDomain(it) }
    }
}