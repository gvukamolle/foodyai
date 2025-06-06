package com.example.calorietracker.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(MakeService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val makeService: MakeService by lazy {
        retrofit.create(MakeService::class.java)
    }
}