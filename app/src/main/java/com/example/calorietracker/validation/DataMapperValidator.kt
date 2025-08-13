package com.example.calorietracker.validation

import com.example.calorietracker.data.FoodItem
import com.example.calorietracker.data.UserProfile
import com.example.calorietracker.data.UserProfileEntity
import com.example.calorietracker.data.DailyIntake
import com.example.calorietracker.data.DailyNutritionSummary
import com.example.calorietracker.data.mappers.FoodMapper
import com.example.calorietracker.data.mappers.UserMapper
import com.example.calorietracker.data.mappers.NutritionMapper
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.common.FoodSource
import com.example.calorietracker.domain.entities.common.Gender
import com.example.calorietracker.domain.entities.common.ActivityLevel
import com.example.calorietracker.domain.entities.common.NutritionGoal
import com.example.calorietracker.domain.entities.common.NutritionTargets
import com.example.calorietracker.validation.ValidationResults
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validator for data mapper correctness and field mappings
 */
@Singleton
class DataMapperValidator @Inject constructor(
    private val foodMapper: FoodMapper,
    private val userMapper: UserMapper,
    private val nutritionMapper: NutritionMapper
) {
    
    /**
     * Validate FoodMapper data transformations
     */
    fun validateFoodMapper(): ValidationResults {
        val results = mutableListOf<ValidationResults.ValidationIssue>()
        
        try {
            // Test domain to data mapping
            val domainFood = createTestFood()
            val dataFood = foodMapper.mapDomainToData(domainFood)
            
            // Validate field mappings
            validateFoodFieldMapping(domainFood, dataFood, results)
            
            // Test data to domain mapping (round trip)
            val roundTripFood = foodMapper.mapDataToDomain(dataFood)
            validateFoodRoundTrip(domainFood, roundTripFood, results)
            
            // Test list mappings
            val domainFoods = listOf(domainFood, createTestFood("Test Food 2"))
            val dataFoods = foodMapper.mapDomainListToData(domainFoods)
            val roundTripFoods = foodMapper.mapDataListToDomain(dataFoods)
            
            if (domainFoods.size != roundTripFoods.size) {
                results.add(ValidationResults.ValidationIssue(
                    type = "MAPPING_ERROR",
                    severity = ValidationResults.Severity.ERROR,
                    message = "Food list mapping size mismatch",
                    details = "Original: ${domainFoods.size}, Round trip: ${roundTripFoods.size}",
                    location = "FoodMapper.mapDomainListToData/mapDataListToDomain"
                ))
            }
            
        } catch (e: Exception) {
            results.add(ValidationResults.ValidationIssue(
                type = "MAPPING_EXCEPTION",
                severity = ValidationResults.Severity.ERROR,
                message = "FoodMapper validation failed with exception",
                details = e.message ?: "Unknown error",
                location = "FoodMapper"
            ))
        }
        
        return ValidationResults(
            validatorName = "FoodMapper",
            issues = results,
            summary = "Validated FoodMapper data transformations"
        )
    }
    
    /**
     * Validate UserMapper field mappings
     */
    fun validateUserMapper(): ValidationResults {
        val results = mutableListOf<ValidationResults.ValidationIssue>()
        
        try {
            // Test domain to data mapping
            val domainUser = createTestUser()
            val dataUser = userMapper.mapDomainToData(domainUser)
            
            // Validate field mappings
            validateUserFieldMapping(domainUser, dataUser, results)
            
            // Test data to domain mapping (round trip)
            val roundTripUser = userMapper.mapDataToDomain(dataUser)
            validateUserRoundTrip(domainUser, roundTripUser, results)
            
            // Test entity mappings
            val entityUser = userMapper.mapDomainToEntity(domainUser)
            val entityRoundTrip = userMapper.mapEntityToDomain(entityUser)
            validateUserEntityMapping(domainUser, entityUser, entityRoundTrip, results)
            
        } catch (e: Exception) {
            results.add(ValidationResults.ValidationIssue(
                type = "MAPPING_EXCEPTION",
                severity = ValidationResults.Severity.ERROR,
                message = "UserMapper validation failed with exception",
                details = e.message ?: "Unknown error",
                location = "UserMapper"
            ))
        }
        
        return ValidationResults(
            validatorName = "UserMapper",
            issues = results,
            summary = "Validated UserMapper field mappings"
        )
    }
    
    /**
     * Validate NutritionMapper conversion logic
     */
    fun validateNutritionMapper(): ValidationResults {
        val results = mutableListOf<ValidationResults.ValidationIssue>()
        
        try {
            // Test nutrition intake mapping
            val nutritionIntake = createTestNutritionIntake()
            val dailyIntake = nutritionMapper.mapDomainToData(nutritionIntake)
            
            // Validate nutrition totals
            validateNutritionTotals(nutritionIntake, dailyIntake, results)
            
            // Test summary mapping
            val summary = nutritionMapper.mapDomainToSummary(nutritionIntake)
            validateNutritionSummary(nutritionIntake, summary, results)
            
            // Test progress calculations
            val progress = nutritionMapper.calculateProgress(nutritionIntake)
            validateProgressCalculations(nutritionIntake, progress, results)
            
        } catch (e: Exception) {
            results.add(ValidationResults.ValidationIssue(
                type = "MAPPING_EXCEPTION",
                severity = ValidationResults.Severity.ERROR,
                message = "NutritionMapper validation failed with exception",
                details = e.message ?: "Unknown error",
                location = "NutritionMapper"
            ))
        }
        
        return ValidationResults(
            validatorName = "NutritionMapper",
            issues = results,
            summary = "Validated NutritionMapper conversion logic"
        )
    }
    
    /**
     * Validate all mappers comprehensively
     */
    fun validateAllMappers(): ValidationResults {
        val foodResults = validateFoodMapper()
        val userResults = validateUserMapper()
        val nutritionResults = validateNutritionMapper()
        
        val allIssues = foodResults.issues + userResults.issues + nutritionResults.issues
        
        return ValidationResults(
            validatorName = "DataMapperValidator",
            issues = allIssues,
            summary = "Comprehensive validation of all data mappers: ${allIssues.size} issues found"
        )
    }
    
    // Helper methods for validation
    
    private fun validateFoodFieldMapping(
        domain: Food,
        data: FoodItem,
        results: MutableList<ValidationResults.ValidationIssue>
    ) {
        if (domain.name != data.name) {
            results.add(createFieldMismatchIssue("Food.name", domain.name, data.name))
        }
        if (domain.calories != data.calories) {
            results.add(createFieldMismatchIssue("Food.calories", domain.calories, data.calories))
        }
        if (domain.protein != data.protein) {
            results.add(createFieldMismatchIssue("Food.protein", domain.protein, data.protein))
        }
        if (domain.fat != data.fat) {
            results.add(createFieldMismatchIssue("Food.fat", domain.fat, data.fat))
        }
        if (domain.carbs != data.carbs) {
            results.add(createFieldMismatchIssue("Food.carbs", domain.carbs, data.carbs))
        }
        if (domain.weight != data.weight) {
            results.add(createFieldMismatchIssue("Food.weight", domain.weight, data.weight))
        }
        if (domain.source.name.lowercase() != data.source) {
            results.add(createFieldMismatchIssue("Food.source", domain.source.name.lowercase(), data.source))
        }
        if (domain.aiOpinion != data.aiOpinion) {
            results.add(createFieldMismatchIssue("Food.aiOpinion", domain.aiOpinion, data.aiOpinion))
        }
    }
    
    private fun validateFoodRoundTrip(
        original: Food,
        roundTrip: Food,
        results: MutableList<ValidationResults.ValidationIssue>
    ) {
        if (original != roundTrip) {
            results.add(ValidationResults.ValidationIssue(
                type = "ROUND_TRIP_ERROR",
                severity = ValidationResults.Severity.ERROR,
                message = "Food round trip mapping failed",
                details = "Original and round trip objects are not equal",
                location = "FoodMapper round trip"
            ))
        }
    }
    
    private fun validateUserFieldMapping(
        domain: User,
        data: UserProfile,
        results: MutableList<ValidationResults.ValidationIssue>
    ) {
        if (domain.name != data.name) {
            results.add(createFieldMismatchIssue("User.name", domain.name, data.name))
        }
        if (domain.birthday != data.birthday) {
            results.add(createFieldMismatchIssue("User.birthday", domain.birthday, data.birthday))
        }
        if (domain.height != data.height) {
            results.add(createFieldMismatchIssue("User.height", domain.height, data.height))
        }
        if (domain.weight != data.weight) {
            results.add(createFieldMismatchIssue("User.weight", domain.weight, data.weight))
        }
        if (domain.gender.name.lowercase() != data.gender) {
            results.add(createFieldMismatchIssue("User.gender", domain.gender.name.lowercase(), data.gender))
        }
        if (domain.activityLevel.name.lowercase() != data.condition) {
            results.add(createFieldMismatchIssue("User.activityLevel", domain.activityLevel.name.lowercase(), data.condition))
        }
        if (domain.nutritionTargets.dailyCalories != data.dailyCalories) {
            results.add(createFieldMismatchIssue("User.dailyCalories", domain.nutritionTargets.dailyCalories, data.dailyCalories))
        }
    }
    
    private fun validateUserRoundTrip(
        original: User,
        roundTrip: User,
        results: MutableList<ValidationResults.ValidationIssue>
    ) {
        // Check critical fields that should survive round trip
        if (original.name != roundTrip.name ||
            original.birthday != roundTrip.birthday ||
            original.height != roundTrip.height ||
            original.weight != roundTrip.weight ||
            original.gender != roundTrip.gender ||
            original.activityLevel != roundTrip.activityLevel) {
            
            results.add(ValidationResults.ValidationIssue(
                type = "ROUND_TRIP_ERROR",
                severity = ValidationResults.Severity.ERROR,
                message = "User round trip mapping failed",
                details = "Critical fields changed during round trip mapping",
                location = "UserMapper round trip"
            ))
        }
    }
    
    private fun validateUserEntityMapping(
        domain: User,
        entity: UserProfileEntity,
        entityRoundTrip: User,
        results: MutableList<ValidationResults.ValidationIssue>
    ) {
        // Validate entity mapping preserves data
        if (domain.name != entity.name) {
            results.add(createFieldMismatchIssue("User->Entity.name", domain.name, entity.name))
        }
        
        // Validate entity round trip
        if (domain.name != entityRoundTrip.name ||
            domain.birthday != entityRoundTrip.birthday ||
            domain.height != entityRoundTrip.height ||
            domain.weight != entityRoundTrip.weight) {
            
            results.add(ValidationResults.ValidationIssue(
                type = "ENTITY_ROUND_TRIP_ERROR",
                severity = ValidationResults.Severity.ERROR,
                message = "User entity round trip mapping failed",
                details = "Data lost during entity mapping round trip",
                location = "UserMapper entity mapping"
            ))
        }
    }
    
    private fun validateNutritionTotals(
        intake: NutritionIntake,
        dailyIntake: DailyIntake,
        results: MutableList<ValidationResults.ValidationIssue>
    ) {
        if (intake.getTotalCalories() != dailyIntake.calories) {
            results.add(createFieldMismatchIssue("NutritionIntake.calories", intake.getTotalCalories(), dailyIntake.calories))
        }
        if (intake.getTotalProtein().toFloat() != dailyIntake.protein) {
            results.add(createFieldMismatchIssue("NutritionIntake.protein", intake.getTotalProtein().toFloat(), dailyIntake.protein))
        }
        if (intake.getTotalFat().toFloat() != dailyIntake.fat) {
            results.add(createFieldMismatchIssue("NutritionIntake.fat", intake.getTotalFat().toFloat(), dailyIntake.fat))
        }
        if (intake.getTotalCarbs().toFloat() != dailyIntake.carbs) {
            results.add(createFieldMismatchIssue("NutritionIntake.carbs", intake.getTotalCarbs().toFloat(), dailyIntake.carbs))
        }
    }
    
    private fun validateNutritionSummary(
        intake: NutritionIntake,
        summary: DailyNutritionSummary,
        results: MutableList<ValidationResults.ValidationIssue>
    ) {
        if (intake.date != summary.date) {
            results.add(createFieldMismatchIssue("NutritionSummary.date", intake.date, summary.date))
        }
        if (intake.getTotalCalories() != summary.totalCalories) {
            results.add(createFieldMismatchIssue("NutritionSummary.totalCalories", intake.getTotalCalories(), summary.totalCalories))
        }
        if (intake.meals.size != summary.mealsCount) {
            results.add(createFieldMismatchIssue("NutritionSummary.mealsCount", intake.meals.size, summary.mealsCount))
        }
    }
    
    private fun validateProgressCalculations(
        intake: NutritionIntake,
        progress: Map<String, Double>,
        results: MutableList<ValidationResults.ValidationIssue>
    ) {
        val targets = intake.targets
        if (targets != null) {
            val expectedCalorieProgress = intake.getTotalCalories().toDouble() / targets.dailyCalories
            val actualCalorieProgress = progress["calorieProgress"] ?: 0.0
            
            if (kotlin.math.abs(expectedCalorieProgress - actualCalorieProgress) > 0.001) {
                results.add(ValidationResults.ValidationIssue(
                    type = "CALCULATION_ERROR",
                    severity = ValidationResults.Severity.ERROR,
                    message = "Calorie progress calculation mismatch",
                    details = "Expected: $expectedCalorieProgress, Actual: $actualCalorieProgress",
                    location = "NutritionMapper.calculateProgress"
                ))
            }
        }
    }
    
    private fun createFieldMismatchIssue(field: String, expected: Any?, actual: Any?): ValidationResults.ValidationIssue {
        return ValidationResults.ValidationIssue(
            type = "FIELD_MAPPING_ERROR",
            severity = ValidationResults.Severity.ERROR,
            message = "Field mapping mismatch for $field",
            details = "Expected: $expected, Actual: $actual",
            location = field
        )
    }
    
    // Test data creation methods
    
    private fun createTestFood(name: String = "Test Food"): Food {
        return Food(
            name = name,
            calories = 100,
            protein = 10.0,
            fat = 5.0,
            carbs = 15.0,
            weight = "100г",
            source = FoodSource.AI_TEXT_ANALYSIS,
            aiOpinion = "Test AI opinion"
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
            type = com.example.calorietracker.domain.entities.common.MealType.BREAKFAST,
            foods = listOf(testFood)
        )
        
        return NutritionIntake(
            date = LocalDate.now(),
            meals = listOf(testMeal),
            targets = NutritionTargets(2000, 150, 65, 250)
        )
    }
}