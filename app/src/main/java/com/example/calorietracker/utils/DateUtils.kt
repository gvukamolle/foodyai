package com.example.calorietracker.utils

import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

// Эта функция теперь может парсить и '2000-1-1', и '2000-01-01'
fun calculateAge(birthdayString: String): Int {
    if (birthdayString.isBlank()) return 0
    return try {
        // Создаем форматтер, который не требует ведущих нулей
        val formatter = DateTimeFormatter.ofPattern("yyyy-M-d", Locale.ENGLISH)
        val birthDate = LocalDate.parse(birthdayString, formatter)
        Period.between(birthDate, LocalDate.now()).years
    } catch (e: DateTimeParseException) {
        0 // Если дата все равно неверная, возвращаем 0
    }
}