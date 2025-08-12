package com.example.calorietracker.domain.entities.common

import java.time.LocalDate

/**
 * Represents a date range for querying historical data
 */
data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
) {
    init {
        require(!startDate.isAfter(endDate)) { "Start date cannot be after end date" }
    }
    
    /**
     * Check if a date falls within this range (inclusive)
     */
    fun contains(date: LocalDate): Boolean {
        return !date.isBefore(startDate) && !date.isAfter(endDate)
    }
    
    /**
     * Get the number of days in this range
     */
    fun dayCount(): Long {
        return startDate.until(endDate).days.toLong() + 1
    }
    
    companion object {
        /**
         * Create a date range for the current week
         */
        fun currentWeek(): DateRange {
            val today = LocalDate.now()
            val startOfWeek = today.minusDays(today.dayOfWeek.value - 1L)
            val endOfWeek = startOfWeek.plusDays(6)
            return DateRange(startOfWeek, endOfWeek)
        }
        
        /**
         * Create a date range for the current month
         */
        fun currentMonth(): DateRange {
            val today = LocalDate.now()
            val startOfMonth = today.withDayOfMonth(1)
            val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
            return DateRange(startOfMonth, endOfMonth)
        }
        
        /**
         * Create a date range for a single day
         */
        fun singleDay(date: LocalDate): DateRange {
            return DateRange(date, date)
        }
    }
}