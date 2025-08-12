package com.example.calorietracker.data.mappers

import com.example.calorietracker.data.DailyIntake
import com.example.calorietracker.data.DailyNutritionSummary
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.common.NutritionTargets
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper for transforming Nutrition entities between domain and data layers
 */
@Singleton
class NutritionMapper @Inject constructor(
    private val foodMapper: FoodMapper
) {
    
    /**
     * Convert domain NutritionIntake to data DailyIntake
     */
    fun mapDomainToData(intake: NutritionIntake): DailyIntake {
        return DailyIntake(
            calories = intake.getTotalCalories(),
            protein = intake.getTotalProtein().toFloat(),
            carbs = intake.getTotalCarbs().toFloat(),
            fat = intake.getTotalFat().toFloat()
        )
    }
    
    /**
     * Convert data DailyIntake to domain NutritionIntake
     */
    fun mapDataToDomain(
        dailyIntake: DailyIntake, 
        date: LocalDate,
        targets: NutritionTargets? = null
    ): NutritionIntake {
        // Note: DailyIntake doesn't contain meal information, so we create empty intake
        // with totals matching the DailyIntake values
        return NutritionIntake(
            date = date,
            meals = emptyList(), // No meal breakdown available from DailyIntake
            targets = targets
        )
    }
    
    /**
     * Convert domain NutritionIntake to data DailyNutritionSummary
     */
    fun mapDomainToSummary(intake: NutritionIntake): DailyNutritionSummary {
        return DailyNutritionSummary(
            date = intake.date,
            totalCalories = intake.getTotalCalories(),
            totalProtein = intake.getTotalProtein().toFloat(),
            totalFat = intake.getTotalFat().toFloat(),
            totalCarbs = intake.getTotalCarbs().toFloat(),
            mealsCount = intake.meals.size
        )
    }
    
    /**
     * Convert data DailyNutritionSummary to domain NutritionIntake
     */
    fun mapSummaryToDomain(summary: DailyNutritionSummary): NutritionIntake {
        return NutritionIntake(
            date = summary.date,
            meals = emptyList(), // Summary doesn't contain meal breakdown
            targets = null // Targets are stored separately in user profile
        )
    }
    
    /**
     * Convert list of domain NutritionIntakes to data DailyNutritionSummaries
     */
    fun mapDomainListToSummaries(intakes: List<NutritionIntake>): List<DailyNutritionSummary> {
        return intakes.map { mapDomainToSummary(it) }
    }
    
    /**
     * Convert list of data DailyNutritionSummaries to domain NutritionIntakes
     */
    fun mapSummariesToDomain(summaries: List<DailyNutritionSummary>): List<NutritionIntake> {
        return summaries.map { mapSummaryToDomain(it) }
    }
    
    /**
     * Create NutritionTargets from legacy data
     */
    fun createTargetsFromLegacy(
        dailyCalories: Int,
        dailyProteins: Int,
        dailyFats: Int,
        dailyCarbs: Int
    ): NutritionTargets {
        return NutritionTargets(
            dailyCalories = dailyCalories,
            dailyProtein = dailyProteins,
            dailyFat = dailyFats,
            dailyCarbs = dailyCarbs
        )
    }
    
    /**
     * Extract nutrition totals from domain NutritionIntake
     */
    fun extractTotals(intake: NutritionIntake): Map<String, Any> {
        return mapOf(
            "calories" to intake.getTotalCalories(),
            "protein" to intake.getTotalProtein(),
            "fat" to intake.getTotalFat(),
            "carbs" to intake.getTotalCarbs(),
            "mealCount" to intake.meals.size
        )
    }
    
    /**
     * Calculate progress percentages
     */
    fun calculateProgress(intake: NutritionIntake): Map<String, Double> {
        val targets = intake.targets
        return if (targets != null) {
            mapOf(
                "calorieProgress" to (intake.getTotalCalories().toDouble() / targets.dailyCalories),
                "proteinProgress" to (intake.getTotalProtein() / targets.dailyProtein),
                "fatProgress" to (intake.getTotalFat() / targets.dailyFat),
                "carbsProgress" to (intake.getTotalCarbs() / targets.dailyCarbs)
            )
        } else {
            mapOf(
                "calorieProgress" to 0.0,
                "proteinProgress" to 0.0,
                "fatProgress" to 0.0,
                "carbsProgress" to 0.0
            )
        }
    }
}