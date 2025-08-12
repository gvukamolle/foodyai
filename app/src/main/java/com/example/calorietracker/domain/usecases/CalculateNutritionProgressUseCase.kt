package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.common.NutritionTargets
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.usecases.base.UseCase
import javax.inject.Inject

/**
 * Use case for calculating nutrition progress against targets
 */
class CalculateNutritionProgressUseCase @Inject constructor() : UseCase<CalculateNutritionProgressUseCase.Params, NutritionProgress>() {
    
    override suspend fun execute(parameters: Params): Result<NutritionProgress> {
        val intake = parameters.intake
        val targets = parameters.targets
        
        // Validate targets
        if (targets.dailyCalories <= 0) {
            return Result.error(DomainException.ValidationException("Invalid calorie target"))
        }
        
        // Calculate progress percentages
        val calorieProgress = intake.getTotalCalories().toDouble() / targets.dailyCalories
        val proteinProgress = if (targets.dailyProtein > 0) {
            intake.getTotalProtein() / targets.dailyProtein
        } else 0.0
        val fatProgress = if (targets.dailyFat > 0) {
            intake.getTotalFat() / targets.dailyFat
        } else 0.0
        val carbsProgress = if (targets.dailyCarbs > 0) {
            intake.getTotalCarbs() / targets.dailyCarbs
        } else 0.0
        
        // Calculate remaining amounts
        val remainingCalories = targets.dailyCalories - intake.getTotalCalories()
        val remainingProtein = targets.dailyProtein - intake.getTotalProtein()
        val remainingFat = targets.dailyFat - intake.getTotalFat()
        val remainingCarbs = targets.dailyCarbs - intake.getTotalCarbs()
        
        // Determine overall status
        val status = when {
            calorieProgress < 0.5 -> ProgressStatus.UNDER_TARGET
            calorieProgress > 1.2 -> ProgressStatus.OVER_TARGET
            calorieProgress >= 0.9 && calorieProgress <= 1.1 -> ProgressStatus.ON_TARGET
            else -> ProgressStatus.APPROACHING_TARGET
        }
        
        val progress = NutritionProgress(
            calorieProgress = calorieProgress,
            proteinProgress = proteinProgress,
            fatProgress = fatProgress,
            carbsProgress = carbsProgress,
            remainingCalories = remainingCalories,
            remainingProtein = remainingProtein,
            remainingFat = remainingFat,
            remainingCarbs = remainingCarbs,
            status = status,
            isGoalMet = calorieProgress >= 0.9 && calorieProgress <= 1.1
        )
        
        return Result.success(progress)
    }
    
    data class Params(
        val intake: NutritionIntake,
        val targets: NutritionTargets
    )
}

/**
 * Represents nutrition progress against targets
 */
data class NutritionProgress(
    val calorieProgress: Double,
    val proteinProgress: Double,
    val fatProgress: Double,
    val carbsProgress: Double,
    val remainingCalories: Int,
    val remainingProtein: Double,
    val remainingFat: Double,
    val remainingCarbs: Double,
    val status: ProgressStatus,
    val isGoalMet: Boolean
) {
    /**
     * Get progress as percentage (0-100)
     */
    fun getCalorieProgressPercentage(): Int = (calorieProgress * 100).toInt()
    fun getProteinProgressPercentage(): Int = (proteinProgress * 100).toInt()
    fun getFatProgressPercentage(): Int = (fatProgress * 100).toInt()
    fun getCarbsProgressPercentage(): Int = (carbsProgress * 100).toInt()
    
    /**
     * Check if any macro is significantly over target (>150%)
     */
    fun hasExcessiveMacros(): Boolean {
        return proteinProgress > 1.5 || fatProgress > 1.5 || carbsProgress > 1.5
    }
}

/**
 * Status of nutrition progress
 */
enum class ProgressStatus {
    UNDER_TARGET,
    APPROACHING_TARGET,
    ON_TARGET,
    OVER_TARGET
}