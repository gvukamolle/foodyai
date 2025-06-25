package com.example.calorietracker.extensions

import com.example.calorietracker.data.UserProfile
import com.example.calorietracker.network.UserProfileData
import com.example.calorietracker.utils.calculateAge

/**
 * Расширение для преобразования UserProfile в UserProfileData для сети
 */
fun UserProfile.toNetworkProfile(): UserProfileData {
    val age = calculateAge(birthday)
    return UserProfileData(
        age = age,
        weight = weight,
        height = height,
        gender = gender,
        activityLevel = condition,
        goal = goal
    )
}
