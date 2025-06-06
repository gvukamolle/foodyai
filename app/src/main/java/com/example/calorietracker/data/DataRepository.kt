package com.example.calorietracker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.calorietracker.data.UserProfile
import com.google.gson.Gson

class DataRepository(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("calorie_tracker_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveUserProfile(profile: UserProfile) {
        val json = gson.toJson(profile)
        sharedPreferences.edit { putString("user_profile", json) }
    }

    fun getUserProfile(): UserProfile? {
        val json = sharedPreferences.getString("user_profile", null)
        return if (json != null) gson.fromJson(json, UserProfile::class.java) else null
    }

    fun saveDailyIntake(intake: DailyIntake, date: String = getCurrentDate()) {
        val json = gson.toJson(intake)
        sharedPreferences.edit { putString("daily_intake_$date", json) }
    }

    fun getDailyIntake(date: String = getCurrentDate()): DailyIntake {
        val json = sharedPreferences.getString("daily_intake_$date", null)
        return if (json != null) gson.fromJson(json, DailyIntake::class.java) else DailyIntake()
    }

    private fun getCurrentDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }
}
