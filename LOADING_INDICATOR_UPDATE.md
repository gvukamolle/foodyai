# Обновление индикатора загрузки в чате

## Дата: 05.08.2025
## Выполнено Senior Kotlin Developer

### Внесенные изменения:

#### 1. **Индикатор загрузки без bubble**
- Индикатор загрузки теперь отображается без фона (bubble)
- Используется та же логика, что и для текстовых сообщений AI

#### 2. **Выравнивание по левому краю**
- Индикатор выровнен по левому краю экрана
- Использует всю доступную ширину с учетом полей

#### 3. **Единый стиль текста**
- Размер шрифта: `16.sp * 1.05f` (как у всех сообщений)
- Высота строки: `24.sp * 1.1f`
- Толщина шрифта: `FontWeight.Normal`
- Цвет: `Color.Black`

#### 4. **Сохраненные анимации**
- ✅ Плавная смена фраз с эффектом растворения (fade in/out + scale)
- ✅ Анимированные точки загрузки (пульсация и градиент)
- ✅ Анимация появления сообщений (AnimatedMessageWithBlur)
- ✅ Все анимации работают плавно и красиво

### Технические детали:

**Обновленные файлы:**
1. `/app/src/main/java/com/example/calorietracker/pages/AnimatedMainScreen.kt`
2. `/app/src/main/java/com/example/calorietracker/ui/animations/AIAnalysisLoadingScreen.kt`

**Ключевые изменения в AnimatedPhrases:**
```kotlin
// Теперь принимает modifier для гибкого позиционирования
fun AnimatedPhrases(
    inputMethod: String? = null,
    modifier: Modifier = Modifier
)

// Выравнивание по левому краю
Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Start,
    verticalAlignment = Alignment.CenterVertically
)

// Единый стиль текста с остальными сообщениями
style = MaterialTheme.typography.bodyMedium.copy(
    fontSize = 16.sp * 1.05f,
    lineHeight = 24.sp * 1.1f,
    fontWeight = FontWeight.Normal
)
```

**Логика отображения:**
```kotlin
val isPlainAIMessage = message.type == MessageType.AI && 
    !(message.content.isEmpty() && message.isExpandable && message.foodItem != null) &&
    message.foodItem == null

if (isPlainAIMessage) {
    if (message.isProcessing) {
        // Индикатор загрузки без фона
    } else {
        // Текстовое сообщение без фона
    }
}
```

### Результат:
- Единообразный стиль всех текстовых элементов в чате
- Индикатор загрузки выглядит как часть основного контента
- Сохранены все красивые анимации
- Улучшена читаемость и визуальная иерархия

### Проверено:
- ✅ Индикатор загрузки без bubble
- ✅ Выравнивание по левому краю
- ✅ Единый размер и стиль шрифта
- ✅ Анимации не пострадали
- ✅ Плавное появление сообщений (AnimatedMessageWithBlur)

### Дополнительные улучшения:
- Минималистичные анимированные точки (5dp вместо 6dp)
- Более мягкие цвета для точек (alpha 0.5 и 0.25)
- Оптимальное расстояние между текстом и точками (8dp)
