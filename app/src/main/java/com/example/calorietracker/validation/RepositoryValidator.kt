package com.example.calorietracker.validation

import com.example.calorietracker.domain.repositories.FoodRepository
import com.example.calorietracker.domain.repositories.UserRepository
import com.example.calorietracker.domain.repositories.NutritionRepository
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.common.FoodSource
import com.example.calorietracker.domain.entities.common.Gender
import com.example.calorietracker.domain.entities.common.ActivityLevel
import com.example.calorietracker.domain.entities.common.NutritionGoal
import com.example.calorietracker.domain.entities.common.NutritionTargets
import com.example.calorietracker.domain.entities.common.MealType
import com.example.calorietracker.domain.entities.common.DateRange
import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.validation.ValidationResults
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validator for repository implementations against their interfaces
 */
@Singleton
class RepositoryValidator @Inject constructor(
    private val foodRepository: FoodRepository,
    private val userRepository: UserRepository,
    private val nutritionRepository: NutritionRepository
) {
    
    /**
     * Validate FoodRepository implementation against interface
     */
    fun validateFoodRepository(): ValidationResults {
        val results = mutableListOf<ValidationResults.ValidationIssue>()
        
        try {
            runBlocking {
                // Test basic food operations
                validateFoodAnalysis(results)
                validateFoodStorage(results)
                validateFoodRetrieval(results)
                validateFoodValidation(results)
            }
        } catch (e: Exception) {
            results.add(ValidationResults.ValidationIssue(
                type = "REPOSITORY_EXCEPTION",
                severity = ValidationResults.Severity.ERROR,
                message = "FoodRepository validation failed with exception",
                details = e.message ?: "Unknown error",
                location = "FoodRepository"
            ))
        }
        
        return ValidationResults(
            validatorName = "FoodRepository",
            issues = results,
            summary = "Validated FoodRepository implementation against interface"
        )
    }
    
    /**
     * Validate UserRepository implementation against interface
     */
    fun validateUserRepository(): ValidationResults {
        val results = mutableListOf<ValidationResults.ValidationIssue>()
        
        try {
            runBlocking {
                // Test user profile operations
                validateUserProfileOperations(results)
                validateUserTargetCalculations(results)
                validateUserValidation(results)
                validateUserSetupOperations(results)
            }
        } catch (e: Exception) {
            results.add(ValidationResults.ValidationIssue(
                type = "REPOSITORY_EXCEPTION",
                severity = ValidationResults.Severity.ERROR,
                message = "UserRepository validation failed with exception",
                details = e.message ?: "Unknown error",
                location = "UserRepository"
            ))
        }
        
        return ValidationResults(
            validatorName = "UserRepository",
            issues = results,
            summary = "Validated UserRepository implementation against interface"
        )
    }
    
    /**
     * Validate NutritionRepository implementation against interface
     */
    fun validateNutritionRepository(): ValidationResults {
        val results = mutableListOf<ValidationResults.ValidationIssue>()
        
        try {
            runBlocking {
                // Test nutrition tracking operations
                validateNutritionIntakeOperations(results)
                validateNutritionStatistics(results)
                validateNutritionDataManagement(results)
            }
        } catch (e: Exception) {
            results.add(ValidationResults.ValidationIssue(
                type = "REPOSITORY_EXCEPTION",
                severity = ValidationResults.Severity.ERROR,
                message = "NutritionRepository validation failed with exception",
                details = e.message ?: "Unknown error",
                location = "NutritionRepository"
            ))
        }
        
        return ValidationResults(
            validatorName = "NutritionRepository",
            issues = results,
            summary = "Validated NutritionRepository implementation against interface"
        )
    }
    
    /**
     * Validate all repositories comprehensively
     */
    fun validateAllRepositories(): ValidationResults {
        val foodResults = validateFoodRepository()
        val userResults = validateUserRepository()
        val nutritionResults = validateNutritionRepository()
        
        val allIssues = foodResults.issues + userResults.issues + nutritionResults.issues
        
        return ValidationResults(
            validatorName = "RepositoryValidator",
            issues = allIssues,
            summary = "Comprehensive validation of all repository implementations: ${allIssues.size} issues found"
        )
    }
    
    // FoodRepository validation methods
    
    private suspend fun validateFoodAnalysis(results: MutableList<ValidationResults.ValidationIssue>) {
        try {
            // Test food description analysis
            val descriptionResult = foodRepository.analyzeFoodDescription("one apple")
            validateResultType(descriptionResult, "analyzeFoodDescription", results)
            
            // Test food validation
            val testFood = createTestFood()
            val validationResult = foodRepository.validateFoodData(testFood)
            validateResultType(validationResult, "validateFoodData", results)
            
        } catch (e: Exception) {
            results.add(createRepositoryMethodError("FoodRepository.analyzeFoodDescription", e))
        }
    }
    
    private suspend fun validateFoodStorage(results: MutableList<ValidationResults.ValidationIssue>) {
        try {
            val testFood = createTestFood()
            val saveResult = foodRepository.saveFoodIntake(testFood, MealType.BREAKFAST)
            validateResultType(saveResult, "saveFoodIntake", results)
            
        } catch (e: Exception) {
            results.add(createRepositoryMethodError("FoodRepository.saveFoodIntake", e))
        }
    }
    
    private suspend fun validateFoodRetrieval(results: MutableList<ValidationResults.ValidationIssue>) {
        try {
            // Test food history retrieval
            val dateRange = DateRange(LocalDate.now().minusDays(7), LocalDate.now())
            val historyResult = foodRepository.getFoodHistory(dateRange)
            validateResultType(historyResult, "getFoodHistory", results)
            
            // Test food search
            val searchResult = foodRepository.searchFoodByName("apple")
            validateResultType(searchResult, "searchFoodByName", results)
            
            // Test recent foods
            val recentResult = foodRepository.getRecentFoods(5)
            validateResultType(recentResult, "getRecentFoods", results)
            
        } catch (e: Exception) {
            results.add(createRepositoryMethodError("FoodRepository retrieval methods", e))
        }
    }
    
    private suspend fun validateFoodValidation(results: MutableList<ValidationResults.ValidationIssue>) {
        try {
            // Test with valid food
            val validFood = createTestFood()
            val validResult = foodRepository.validateFoodData(validFood)
            if (validResult is Result.Error) {
                results.add(ValidationResults.ValidationIssue(
                    type = "VALIDATION_LOGIC_ERROR",
                    severity = ValidationResults.Severity.WARNING,
                    message = "Valid food failed validation",
                    details = "Food validation rejected valid food data",
                    location = "FoodRepository.validateFoodData"
                ))
            }
            
            // Test with invalid food
            val invalidFood = Food(
                name = "", // Invalid: blank name
                calories = -100, // Invalid: negative calories
                protein = 0.0,
                fat = 0.0,
                carbs = 0.0,
                weight = "100g"
            )
            val invalidResult = foodRepository.validateFoodData(invalidFood)
            if (invalidResult is Result.Success) {
                results.add(ValidationResults.ValidationIssue(
                    type = "VALIDATION_LOGIC_ERROR",
                    severity = ValidationResults.Severity.ERROR,
                    message = "Invalid food passed validation",
                    details = "Food validation accepted invalid food data",
                    location = "FoodRepository.validateFoodData"
                ))
            }
            
        } catch (e: Exception) {
            results.add(createRepositoryMethodError("FoodRepository.validateFoodData", e))
        }
    }
    
    // UserRepository validation methods
    
    private suspend fun validateUserProfileOperations(results: MutableList<ValidationResults.ValidationIssue>) {
        try {
            // Test user profile retrieval
            val profileResult = userRepository.getUserProfile()
            validateResultType(profileResult, "getUserProfile", results)
            
            // Test user profile saving
            val testUser = createTestUser()
            val saveResult = userRepository.saveUserProfile(testUser)
            validateResultType(saveResult, "saveUserProfile", results)
            
        } catch (e: Exception) {
            results.add(createRepositoryMethodError("UserRepository profile operations", e))
        }
    }
    
    private suspend fun validateUserTargetCalculations(results: MutableList<ValidationResults.ValidationIssue>) {
        try {
            val testUser = createTestUser()
            val targetsResult = userRepository.calculateNutritionTargets(testUser)
            validateResultType(targetsResult, "calculateNutritionTargets", results)
            
            // Validate calculation logic
            if (targetsResult is Result.Success) {
                val targets = targetsResult.data
                if (targets.dailyCalories <= 0) {
                    results.add(ValidationResults.ValidationIssue(
                        type = "CALCULATION_ERROR",
                        severity = ValidationResults.Severity.ERROR,
                        message = "Invalid calorie calculation",
                        details = "Calculated daily calories: ${targets.dailyCalories}",
                        location = "UserRepository.calculateNutritionTargets"
                    ))
                }
            }
            
        } catch (e: Exception) {
            results.add(createRepositoryMethodError("UserRepository.calculateNutritionTargets", e))
        }
    }
    
    private suspend fun validateUserValidation(results: MutableList<ValidationResults.ValidationIssue>) {
        try {
            // Test with valid user
            val validUser = createTestUser()
            val validResult = userRepository.validateUserProfile(validUser)
            if (validResult is Result.Error) {
                results.add(ValidationResults.ValidationIssue(
                    type = "VALIDATION_LOGIC_ERROR",
                    severity = ValidationResults.Severity.WARNING,
                    message = "Valid user failed validation",
                    details = "User validation rejected valid user data",
                    location = "UserRepository.validateUserProfile"
                ))
            }
            
            // Test with invalid user
            val invalidUser = User(
                name = "", // Invalid: blank name
                height = -10, // Invalid: negative height
                weight = 0 // Invalid: zero weight
            )
            val invalidResult = userRepository.validateUserProfile(invalidUser)
            if (invalidResult is Result.Success) {
                results.add(ValidationResults.ValidationIssue(
                    type = "VALIDATION_LOGIC_ERROR",
                    severity = ValidationResults.Severity.ERROR,
                    message = "Invalid user passed validation",
                    details = "User validation accepted invalid user data",
                    location = "UserRepository.validateUserProfile"
                ))
            }
            
        } catch (e: Exception) {
            results.add(createRepositoryMethodError("UserRepository.validateUserProfile", e))
        }
    }
    
    private suspend fun validateUserSetupOperations(results: MutableList<ValidationResults.ValidationIssue>) {
        try {
            val setupResult = userRepository.isSetupComplete()
            validateResultType(setupResult, "isSetupComplete", results)
            
            val markCompleteResult = userRepository.markSetupComplete()
            validateResultType(markCompleteResult, "markSetupComplete", results)
            
        } catch (e: Exception) {
            results.add(createRepositoryMethodError("UserRepository setup operations", e))
        }
    }
    
    // NutritionRepository validation methods
    
    private suspend fun validateNutritionIntakeOperations(results: MutableList<ValidationResults.ValidationIssue>) {
        try {
            val today = LocalDate.now()
            
            // Test daily intake retrieval
            val dailyResult = nutritionRepository.getDailyIntake(today)
            validateResultType(dailyResult, "getDailyIntake", results)
            
            // Test weekly intake retrieval
            val weeklyResult = nutritionRepository.getWeeklyIntake(today.minusDays(7))
            validateResultType(weeklyResult, "getWeeklyIntake", results)
            
            // Test monthly intake retrieval
            val monthlyResult = nutritionRepository.getMonthlyIntake(YearMonth.now())
            validateResultType(monthlyResult, "getMonthlyIntake", results)
            
        } catch (e: Exception) {
            results.add(createRepositoryMethodError("NutritionRepository intake operations", e))
        }
    }
    
    private suspend fun validateNutritionStatistics(results: MutableList<ValidationResults.ValidationIssue>) {
        try {
            val dateRange = DateRange(LocalDate.now().minusDays(30), LocalDate.now())
            
            val statsResult = nutritionRepository.getNutritionStatistics(dateRange)
            validateResultType(statsResult, "getNutritionStatistics", results)
            
            val trendsResult = nutritionRepository.getNutritionTrends(dateRange)
            validateResultType(trendsResult, "getNutritionTrends", results)
            
        } catch (e: Exception) {
            results.add(createRepositoryMethodError("NutritionRepository statistics operations", e))
        }
    }
    
    private suspend fun validateNutritionDataManagement(results: MutableList<ValidationResults.ValidationIssue>) {
        try {
            val today = LocalDate.now()
            val testIntake = createTestNutritionIntake()
            
            // Test saving intake
            val saveResult = nutritionRepository.saveDailyIntake(testIntake)
            validateResultType(saveResult, "saveDailyIntake", results)
            
            // Test clearing data
            val clearResult = nutritionRepository.clearDayData(today)
            validateResultType(clearResult, "clearDayData", results)
            
            // Test export
            val dateRange = DateRange(today.minusDays(7), today)
            val exportResult = nutritionRepository.exportNutritionData(dateRange)
            validateResultType(exportResult, "exportNutritionData", results)
            
        } catch (e: Exception) {
            results.add(createRepositoryMethodError("NutritionRepository data management", e))
        }
    }
    
    // Helper methods
    
    private fun validateResultType(result: Result<*>, methodName: String, results: MutableList<ValidationResults.ValidationIssue>) {
        when (result) {
            is Result.Success -> {
                // Success is expected for most operations
            }
            is Result.Error -> {
                // Error might be expected for some operations, but we should log it
                results.add(ValidationResults.ValidationIssue(
                    type = "REPOSITORY_ERROR_RESULT",
                    severity = ValidationResults.Severity.WARNING,
                    message = "Repository method returned error",
                    details = "Method $methodName returned error: ${result.exception.message}",
                    location = methodName
                ))
            }
        }
    }
    
    private fun createRepositoryMethodError(methodName: String, exception: Exception): ValidationResults.ValidationIssue {
        return ValidationResults.ValidationIssue(
            type = "REPOSITORY_METHOD_EXCEPTION",
            severity = ValidationResults.Severity.ERROR,
            message = "Repository method threw exception",
            details = "Method $methodName threw: ${exception.message}",
            location = methodName
        )
    }
    
    // Test data creation methods
    
    private fun createTestFood(): Food {
        return Food(
            name = "Test Apple",
            calories = 95,
            protein = 0.5,
            fat = 0.3,
            carbs = 25.0,
            weight = "150g",
            source = FoodSource.MANUAL_INPUT
        )
    }
    
    private fun createTestUser(): User {
        return User(
            name = "Test User",
            birthday = "1990-01-01",
            height = 175,
            weight = 70,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            bodyFeeling = "Good",
            goal = NutritionGoal.MAINTAIN_WEIGHT,
            nutritionTargets = NutritionTargets(2000, 150, 65, 250),
            isSetupComplete = true
        )
    }
    
    private fun createTestNutritionIntake(): NutritionIntake {
        val testFood = createTestFood()
        val testMeal = com.example.calorietracker.domain.entities.Meal(
            type = MealType.BREAKFAST,
            foods = listOf(testFood)
        )
        
        return NutritionIntake(
            date = LocalDate.now(),
            meals = listOf(testMeal),
            targets = NutritionTargets(2000, 150, 65, 250)
        )
    }
}