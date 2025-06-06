package com.example.calorietracker.data

import androidx.room.*

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Update
    suspend fun updateUserProfile(profile: UserProfileEntity)

    @Delete
    suspend fun deleteUserProfile(profile: UserProfileEntity)
}
