package com.example.calorietracker.auth

// Просто создай этот файл с таким кодом, и все ошибки, связанные с подпиской, исчезнут.
enum class SubscriptionPlan(
    val displayName: String,
    val features: List<String>
) {
    FREE(
        "Бесплатный",
        listOf(
            "Подсчет калорий и БЖУ",
            "База из 100,000+ продуктов",
            "История питания"
        )
    ),
    PRO(
        "PRO",
        listOf(
            "Все функции Pro плана",
            "Персональные рекомендации от AI",
            "Интеграция с фитнес-трекерами",
            "Экспорт данных в JSON"
        )
    )
}
