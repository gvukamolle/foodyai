# Make.com Webhook JSON Schemas - Обновленные после Clean Architecture

## 📋 Обзор
После исправления AI интеграции в FoodRepositoryImpl, приложение теперь отправляет корректные JSON объекты на Make.com webhook.

**Webhook URL:** `https://hook.us2.make.com/653st2c10rmg92nlltf3y0m8sggxaac6`

---

## 1. 📸 Анализ фото еды - `analyzeFoodImage`

### Endpoint
```
POST https://hook.us2.make.com/653st2c10rmg92nlltf3y0m8sggxaac6
```

### JSON Schema
```json
{
  "imageBase64": "string (base64 encoded image)",
  "userProfile": {
    "age": "number",
    "weight": "number (kg)",
    "height": "number (cm)",
    "gender": "string (male/female/other)",
    "activityLevel": "string (sedentary/lightly_active/moderately_active/very_active/extremely_active)",
    "goal": "string (lose_weight/maintain_weight/gain_weight/gain_muscle)"
  },
  "caption": "string (optional user description of the photo)"
}
```

### Пример запроса
```json
{
  "imageBase64": "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k=",
  "userProfile": {
    "age": 28,
    "weight": 70,
    "height": 175,
    "gender": "male",
    "activityLevel": "moderately_active",
    "goal": "maintain_weight"
  },
  "caption": "Это мое яблоко на завтрак"
}
```

### Ожидаемый ответ
```json
{
  "status": "success",
  "answer": "{\"name\":\"Яблоко\",\"calories\":52,\"protein\":0.3,\"fat\":0.2,\"carbs\":14,\"weight\":\"100г\",\"opinion\":\"Отличный выбор! Яблоко содержит много витаминов и клетчатки.\"}"
}
```

---

## 2. 📝 Анализ текстового описания еды - `analyzeFood`

### Endpoint
```
POST https://hook.us2.make.com/653st2c10rmg92nlltf3y0m8sggxaac6
```

### JSON Schema
```json
{
  "weight": "number (default: 100)",
  "userProfile": {
    "age": "number",
    "weight": "number (kg)",
    "height": "number (cm)",
    "gender": "string",
    "activityLevel": "string",
    "goal": "string"
  },
  "message": "string (food description)",
  "userId": "string (generated: user_timestamp)",
  "messageType": "string (always: 'analysis')",
  "includeOpinion": "boolean (always: true)"
}
```

### Пример запроса
```json
{
  "weight": 100,
  "userProfile": {
    "age": 28,
    "weight": 70,
    "height": 175,
    "gender": "male",
    "activityLevel": "moderately_active",
    "goal": "maintain_weight"
  },
  "message": "яблоко 150г",
  "userId": "user_1703123456789",
  "messageType": "analysis",
  "includeOpinion": true
}
```

### Ожидаемый ответ
```json
{
  "status": "success",
  "answer": "{\"name\":\"Яблоко\",\"calories\":78,\"protein\":0.4,\"fat\":0.3,\"carbs\":21,\"weight\":\"150г\",\"opinion\":\"Хороший перекус! Увеличенная порция даст больше энергии.\"}"
}
```

---

## 3. 🔄 Изменения в UserProfile структуре

### Старая структура (до Clean Architecture)
```json
{
  "name": "string",
  "birthday": "string",
  "height": "number",
  "weight": "number",
  "gender": "string",
  "condition": "string",
  "bodyFeeling": "string",
  "goal": "string",
  "dailyCalories": "number",
  "dailyProteins": "number",
  "dailyFats": "number",
  "dailyCarbs": "number"
}
```

### Новая структура (после Clean Architecture)
```json
{
  "age": "number (calculated from birthday)",
  "weight": "number",
  "height": "number", 
  "gender": "string (male/female/other)",
  "activityLevel": "string (mapped from condition)",
  "goal": "string (lose_weight/maintain_weight/gain_weight/gain_muscle)"
}
```

---

## 4. � Приpмеры анализа фото с подписью

### Пример 1: Фото с описательной подписью
```json
{
  "imageBase64": "base64_image_data",
  "userProfile": { /* user profile */ },
  "caption": "Мой завтрак - овсянка с ягодами и орехами"
}
```

### Пример 2: Фото с вопросом
```json
{
  "imageBase64": "base64_image_data", 
  "userProfile": { /* user profile */ },
  "caption": "Сколько калорий в этом салате?"
}
```

### Пример 3: Фото без подписи
```json
{
  "imageBase64": "base64_image_data",
  "userProfile": { /* user profile */ },
  "caption": ""
}
```

### Пример 4: Фото с контекстом
```json
{
  "imageBase64": "base64_image_data",
  "userProfile": { /* user profile */ },
  "caption": "Ужин после тренировки, нужно больше белка"
}
```

---

## 5. 📊 Mapping значений

### Gender Mapping
```javascript
// Domain -> Network
Gender.MALE -> "male"
Gender.FEMALE -> "female"  
Gender.OTHER -> "other"
```

### Activity Level Mapping
```javascript
// Domain -> Network
ActivityLevel.SEDENTARY -> "sedentary"
ActivityLevel.LIGHTLY_ACTIVE -> "lightly_active"
ActivityLevel.MODERATELY_ACTIVE -> "moderately_active"
ActivityLevel.VERY_ACTIVE -> "very_active"
ActivityLevel.EXTREMELY_ACTIVE -> "extremely_active"
```

### Goal Mapping
```javascript
// Domain -> Network
NutritionGoal.LOSE_WEIGHT -> "lose_weight"
NutritionGoal.MAINTAIN_WEIGHT -> "maintain_weight"
NutritionGoal.GAIN_WEIGHT -> "gain_weight"
NutritionGoal.GAIN_MUSCLE -> "gain_muscle"
```

---

## 6. 🛡️ Error Handling

### Default UserProfile (если профиль не найден)
```json
{
  "age": 25,
  "weight": 70,
  "height": 170,
  "gender": "other",
  "activityLevel": "moderately_active",
  "goal": "maintain_weight"
}
```

### Обработка ошибок ответа
```kotlin
// Если JSON парсинг не удался, используется fallback парсинг
// Извлекает значения из текста с помощью regex
```

---

## 7. 🔧 Настройки Make.com сценария

### Что нужно обновить в Make.com:

1. **Webhook trigger** должен принимать новую структуру `userProfile`
2. **Удалить обработку** старых полей: `dailyCalories`, `dailyProteins`, `dailyFats`, `dailyCarbs`
3. **Добавить обработку** новых полей: `age`, `activityLevel`
4. **Обновить логику** расчета рекомендаций на основе `age` вместо `birthday`

### Пример обновления в Make.com:
```javascript
// Старый код
const dailyCalories = data.userProfile.dailyCalories;

// Новый код  
const age = data.userProfile.age;
const bmr = calculateBMR(age, weight, height, gender);
const tdee = bmr * getActivityMultiplier(activityLevel);
```

---

## 7. 🧪 Тестирование

### Тест анализа фото
```bash
curl -X POST https://hook.us2.make.com/653st2c10rmg92nlltf3y0m8sggxaac6 \
  -H "Content-Type: application/json" \
  -d '{
    "imageBase64": "base64_string_here",
    "userProfile": {
      "age": 28,
      "weight": 70,
      "height": 175,
      "gender": "male",
      "activityLevel": "moderately_active",
      "goal": "maintain_weight"
    },
    "caption": "Мой обед сегодня"
  }'
```

### Тест анализа текста
```bash
curl -X POST https://hook.us2.make.com/653st2c10rmg92nlltf3y0m8sggxaac6 \
  -H "Content-Type: application/json" \
  -d '{
    "weight": 100,
    "userProfile": {
      "age": 28,
      "weight": 70,
      "height": 175,
      "gender": "male",
      "activityLevel": "moderately_active",
      "goal": "maintain_weight"
    },
    "message": "яблоко 150г",
    "userId": "user_1703123456789",
    "messageType": "analysis",
    "includeOpinion": true
  }'
```

---

## ✅ Checklist для Make.com

- [ ] Обновить webhook trigger для новой структуры `userProfile`
- [ ] Удалить обработку `dailyCalories`, `dailyProteins`, `dailyFats`, `dailyCarbs`
- [ ] Добавить обработку `age` и `activityLevel`
- [ ] Обновить логику расчета BMR/TDEE
- [ ] Протестировать анализ фото с новой структурой
- [ ] Протестировать анализ текста с новой структурой
- [ ] Проверить корректность парсинга ответов в приложении

---

*Документация обновлена после исправления AI интеграции в Clean Architecture*