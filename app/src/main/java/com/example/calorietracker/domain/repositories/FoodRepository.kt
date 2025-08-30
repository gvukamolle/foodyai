package com.example.calorietracker.domain.repositories

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.common.DateRange
import com.example.calorietracker.domain.entities.common.MealType

/**
 * Repository interface for food-related operations in the domain layer.
 * 
 * This interface defines the contract for food data access and manipulation,
 * following Clean Architecture principles where the domain layer defines
 * interfaces that are implemented by the data layer.
 * 
 * All methods return [Result] wrapper to provide consistent error handling
 * across the application.
 */
interface FoodRepository {
    
    /**
     * Analyzes food from a photo using AI image recognition.
     * 
     * @param photoPath The file path to the photo to analyze
     * @param caption Optional caption or description to help with analysis
     * @return [Result] containing the analyzed [Food] object or an error
     */
    suspend fun analyzeFoodPhoto(photoPath: String, caption: String): Result<Food>
    
    /**
     * Overload with explicit messageType for routing (e.g., "photo", "recipe_photo").
     */
    suspend fun analyzeFoodPhoto(photoPath: String, caption: String, messageType: String): Result<Food>
    
    /**
     * Analyzes food from a text description using AI natural language processing.
     * 
     * @param description Text description of the food (e.g., "one medium apple")
     * @return [Result] containing the analyzed [Food] object or an error
     */
    suspend fun analyzeFoodDescription(description: String): Result<Food>
    
    /**
     * Saves a food intake entry for a specific meal type.
     * 
     * @param food The food item to save
     * @param mealType The type of meal (breakfast, lunch, dinner, snack)
     * @return [Result] indicating success or failure
     */
    suspend fun saveFoodIntake(food: Food, mealType: MealType): Result<Unit>
    
    /**
     * Retrieves food history for a specific date range.
     * 
     * @param dateRange The date range to query
     * @return [Result] containing a list of [Food] items or an error
     */
    suspend fun getFoodHistory(dateRange: DateRange): Result<List<Food>>
    
    /**
     * Searches for food items by name or partial name match.
     * 
     * @param query The search query string
     * @return [Result] containing a list of matching [Food] items or an error
     */
    suspend fun searchFoodByName(query: String): Result<List<Food>>
    
    /**
     * Retrieves recently used food items for quick access.
     * 
     * @param limit Maximum number of items to return (default: 10)
     * @return [Result] containing a list of recent [Food] items or an error
     */
    suspend fun getRecentFoods(limit: Int = 10): Result<List<Food>>
    
    /**
     * Retrieves the user's favorite food items.
     * 
     * @return [Result] containing a list of favorite [Food] items or an error
     */
    suspend fun getFavoriteFoods(): Result<List<Food>>
    
    /**
     * Marks a food item as favorite for quick access.
     * 
     * @param food The food item to mark as favorite
     * @return [Result] indicating success or failure
     */
    suspend fun markFoodAsFavorite(food: Food): Result<Unit>
    
    /**
     * Removes a food item from the favorites list.
     * 
     * @param food The food item to remove from favorites
     * @return [Result] indicating success or failure
     */
    suspend fun removeFoodFromFavorites(food: Food): Result<Unit>
    
    /**
     * Validates food data according to business rules.
     * 
     * Checks for:
     * - Non-blank name
     * - Non-negative nutritional values
     * - Reasonable nutritional ratios
     * - Valid weight format
     * 
     * @param food The food item to validate
     * @return [Result] containing the validated [Food] object or validation errors
     */
    suspend fun validateFoodData(food: Food): Result<Food>
    
    /**
     * Retrieves detailed nutrition information for a food item by name.
     * 
     * @param foodName The name of the food to look up
     * @return [Result] containing the [Food] object with nutrition info, null if not found, or an error
     */
    suspend fun getNutritionInfo(foodName: String): Result<Food?>
}
