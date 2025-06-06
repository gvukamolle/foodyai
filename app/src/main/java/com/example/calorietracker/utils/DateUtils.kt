package com.example.calorietracker.utils

import java.time.LocalDate
import java.time.Period

fun calculateAge(birthday: String): Int {
    if (birthday.isBlank()) return 0
    val birthDate = LocalDate.parse(birthday)  // "YYYY-MM-DD"
    val today = LocalDate.now()
    return Period.between(birthDate, today).years
}