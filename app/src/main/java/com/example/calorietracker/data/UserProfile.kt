// UserProfile.kt
package com.example.calorietracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.calorietracker.Meal

data class DailyIntake(
    val calories: Int = 0,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val meals: List<Meal> = emptyList()  // Добавляем список приемов пищи
)

// ====== 1. Основная модель для работы в приложении ======
data class UserProfile(
    val name: String = "",
    val birthday: String = "",  // хранить как "YYYY-MM-DD", это удобно
    val height: Int = 0,
    val weight: Int = 0,
    val gender: String = "",
    val condition: String = "",
    val bodyFeeling: String = "",
    val goal: String = "",
    val dailyCalories: Int = 0,
    val dailyProteins: Int = 0,
    val dailyFats: Int = 0,
    val dailyCarbs: Int = 0,
    var isSetupComplete: Boolean = false // <-- ДОБАВЬ ЭТУ СТРОКУ
)

// ====== 2. Модель для хранения в Room (БД) ======
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String = "",
    val birthday: String = "",
    val height: Int = 0,
    val weight: Int = 0,
    val gender: String = "",
    val condition: String = "",
    val bodyFeeling: String = "",
    val goal: String = "",
    val dailyCalories: Int = 0,
    val dailyProteins: Int = 0,
    val dailyFats: Int = 0,
    val dailyCarbs: Int = 0
)

// ====== 3. Конвертеры между моделями ======
fun UserProfile.toEntity(): UserProfileEntity {
    return UserProfileEntity(
        name = this.name,
        birthday = this.birthday,
        height = this.height,
        weight = this.weight,
        gender = this.gender,
        condition = this.condition,
        bodyFeeling = this.bodyFeeling,
        goal = this.goal,
        dailyCalories = this.dailyCalories,
        dailyProteins = this.dailyProteins,
        dailyFats = this.dailyFats,
        dailyCarbs = this.dailyCarbs
    )
}

fun UserProfileEntity.toModel(): UserProfile {
    return UserProfile(
        name = this.name,
        birthday = this.birthday,
        height = this.height,
        weight = this.weight,
        gender = this.gender,
        condition = this.condition,
        bodyFeeling = this.bodyFeeling,
        goal = this.goal,
        dailyCalories = this.dailyCalories,
        dailyProteins = this.dailyProteins,
        dailyFats = this.dailyFats,
        dailyCarbs = this.dailyCarbs
    )
}