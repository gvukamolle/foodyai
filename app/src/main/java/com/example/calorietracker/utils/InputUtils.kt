package com.example.calorietracker.utils

import java.util.Locale

/** Utility to capitalize the first character of a string. */
fun String.capitalizeFirst(): String =
    if (isNotEmpty()) replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    } else this

/**
 * Allow only digits and at most one decimal point. Commas are
 * automatically converted to dots.
 */
fun filterDecimal(input: String): String {
    val normalized = input.replace(',', '.')
    val result = StringBuilder()
    var dotFound = false
    for (ch in normalized) {
        when {
            ch.isDigit() -> result.append(ch)
            ch == '.' && !dotFound -> {
                result.append('.')
                dotFound = true
            }
        }
    }
    return result.toString()
}
