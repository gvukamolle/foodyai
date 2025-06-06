package com.example.calorietracker.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserProfileEntity::class], // тут будут все Entity, через запятую
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
}
