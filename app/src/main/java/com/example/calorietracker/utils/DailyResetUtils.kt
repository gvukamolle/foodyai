package com.example.calorietracker.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Объект-утилита для всей логики, связанной с датами и ежедневным сбросом данных.
 */
object DailyResetUtils {
    // Час обнуления счетчиков (4 утра). Выбран, чтобы ночные перекусы
    // относились к предыдущему дню.
    private const val RESET_HOUR = 4

    /**
     * Получить "пищевую дату" — дату, к которой относится текущий прием пищи.
     * Если сейчас раньше 4 утра, то дата будет вчерашней.
     * @return Дата в формате "YYYY-MM-DD".
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
        return foodDate.toString()
    }

    /**
     * Получить текущую календарную дату (меняется ровно в полночь).
     * @return Дата в формате "YYYY-MM-DD".
     */
    fun getCurrentDisplayDate(): String {
        return LocalDate.now().toString()
    }

    /**
     * Проверить, нужно ли обнулить счетчики.
     * Сброс нужен, если последняя сохраненная "пищевая" дата не совпадает с текущей.
     * @param lastDate последняя сохраненная дата (в формате "YYYY-MM-DD")
     * @return `true` если нужно обнулить счетчики.
     */
    fun shouldResetCounters(lastDate: String?): Boolean {
        if (lastDate == null) return true // Если записей еще не было, сбрасывать нечего, но считаем, что день "новый"
        val currentFoodDate = getFoodDate()
        return currentFoodDate != lastDate
    }

    /**
     * Получить точное время следующего сброса данных (следующие 4:00 утра).
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
     * Получить читаемую дату для отображения пользователю (например, "Ср, 25 Июня").
     * Этот метод теперь использует стандартные средства для форматирования.
     * @param date Дата в формате "YYYY-MM-DD".
     */
    fun getDisplayDate(date: String): String {
        val localDate = LocalDate.parse(date)
        // Создаем локаль для русского языка, чтобы названия были на русском
        val russianLocale = Locale("ru")
        // Создаем форматтер: "Сокр. день недели, число, полное название месяца"
        // Пример: "Ср, 25 Июня"
        val formatter = DateTimeFormatter.ofPattern("E, d MMMM", russianLocale)

        // Применяем форматтер. Стандартный формат дает день недели с маленькой буквы ("ср").
        // Делаем первую букву заглавной, чтобы было красиво: "Ср".
        return formatter.format(localDate)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }
    }

    /**
     * Получить форматированную текущую календарную дату для отображения на главном экране.
     */
    fun getFormattedDisplayDate(): String {
        return getDisplayDate(getCurrentDisplayDate())
    }
}
