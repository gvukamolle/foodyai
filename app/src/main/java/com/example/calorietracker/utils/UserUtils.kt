package com.example.calorietracker.utils

import android.content.Context
import java.util.UUID

fun getOrCreateUserId(context: Context): String {
    val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    var id = prefs.getString("user_id", null)
    if (id == null) {
        id = UUID.randomUUID().toString()
        prefs.edit().putString("user_id", id).apply()
    }
    return id
}
