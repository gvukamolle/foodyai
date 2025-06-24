package com.example.calorietracker.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object DailyResetUtils {
    // Час обнуления счетчиков (4 утра)
    private const val RESET_HOUR = 4

    /**
     * Получить "пищевую дату" - дату, к которой относится текущий прием пищи
     * До 4 утра считаем предыдущий день
     */
    fun getFoodDate(): String {
        val now = LocalDateTime.now()
        val resetTime = LocalTime.of(RESET_HOUR, 0)

        val foodDate = if (now.toLocalTime().isBefore(resetTime)) {
            // До 4 утра - считаем предыдущий день
            now.toLocalDate().minusDays(1)
        } else {
            // После 4 утра - текущий день
            now.toLocalDate()
        }

        return foodDate.toString() // формат YYYY-MM-DD
    }

    /**
     * Проверить, нужно ли обнулить счетчики
     * @param lastResetDate последняя дата обнуления (YYYY-MM-DD)
     * @return true если нужно обнулить
     */
    fun shouldResetCounters(lastResetDate: String?): Boolean {
        if (lastResetDate == null) return true

        val currentFoodDate = getFoodDate()
        return currentFoodDate != lastResetDate
    }

    /**
     * Получить время следующего обнуления
     */
    fun getNextResetTime(): LocalDateTime {
        val now = LocalDateTime.now()
        val todayReset = now.toLocalDate().atTime(RESET_HOUR, 0)

        return if (now.isBefore(todayReset)) {
            todayReset
        } else {
            todayReset.plusDays(1)
        }
    }

    /**
     * Получить читаемую дату для отображения пользователю
     */
    fun getDisplayDate(date: String): String {
        val localDate = LocalDate.parse(date)
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        return when (localDate) {
            today -> "Сегодня"
            yesterday -> "Вчера"
            else -> {
                val months = arrayOf(
                    "января", "февраля", "марта", "апреля", "мая", "июня",
                    "июля", "августа", "сентября", "октября", "ноября", "декабря"
                )
                "${localDate.dayOfMonth} ${months[localDate.monthValue - 1]}"
            }
        }
    }
}
