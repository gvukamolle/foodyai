package com.example.calorietracker.auth

// Просто создай этот файл с таким кодом, и все ошибки, связанные с подпиской, исчезнут.
enum class SubscriptionPlan {
    FREE(
        displayName = "Бесплатный",
        features = listOf(
            "Подсчет калорий и БЖУ",
            "База из 100,000+ продуктов",
            "История питания"
        )
    ),
    PLUS(
        displayName = "PLUS",
        features = listOf(
            "Все функции Бесплатного плана",
            "Расширенные отчеты и аналитика",
            "Экспорт данных в PDF",
            "Приоритетная поддержка"
        )
    ),
    PRO(
        displayName = "PRO",
        features = listOf(
            "Все функции Pro плана",
            "Персональные рекомендации от AI",
            "Интеграция с фитнес-трекерами",
            "Экспорт данных в JSON"
        )
    );

    val displayName: String
    val features: List<String>

    constructor(displayName: String, features: List<String>) {
        this.displayName = displayName
        this.features = features
    }
}
