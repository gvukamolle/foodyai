# 🔄 Make.com Migration Guide - Clean Architecture Update

## 🚨 КРИТИЧЕСКИЕ ИЗМЕНЕНИЯ

После исправления AI интеграции в приложении, **НЕОБХОДИМО** обновить Make.com сценарий.

---

## 📋 Что изменилось

### ❌ УДАЛЕНО из UserProfile
```json
{
  "name": "string",
  "birthday": "string", 
  "condition": "string",
  "bodyFeeling": "string",
  "dailyCalories": "number",    // ← УДАЛЕНО
  "dailyProteins": "number",    // ← УДАЛЕНО  
  "dailyFats": "number",        // ← УДАЛЕНО
  "dailyCarbs": "number"        // ← УДАЛЕНО
}
```

### ✅ ДОБАВЛЕНО в UserProfile
```json
{
  "age": "number",              // ← НОВОЕ (вместо birthday)
  "weight": "number",
  "height": "number",
  "gender": "string",           // ← ОБНОВЛЕНО (male/female/other)
  "activityLevel": "string",    // ← НОВОЕ (вместо condition)
  "goal": "string"              // ← ОБНОВЛЕНО (lose_weight/maintain_weight/etc)
}
```

---

## 🔧 Необходимые изменения в Make.com

### 1. Обновить Webhook Trigger
```javascript
// Старый код - УДАЛИТЬ
const dailyCalories = data.userProfile.dailyCalories;
const dailyProteins = data.userProfile.dailyProteins;
const birthday = data.userProfile.birthday;
const condition = data.userProfile.condition;

// Новый код - ДОБАВИТЬ
const age = data.userProfile.age;
const activityLevel = data.userProfile.activityLevel;
const weight = data.userProfile.weight;
const height = data.userProfile.height;
const gender = data.userProfile.gender;
const goal = data.userProfile.goal;
```

### 2. Обновить расчет калорий
```javascript
// Новая логика расчета BMR
function calculateBMR(age, weight, height, gender) {
  if (gender === 'male') {
    return (10 * weight) + (6.25 * height) - (5 * age) + 5;
  } else if (gender === 'female') {
    return (10 * weight) + (6.25 * height) - (5 * age) - 161;
  } else {
    // other - среднее значение
    const maleBMR = (10 * weight) + (6.25 * height) - (5 * age) + 5;
    const femaleBMR = (10 * weight) + (6.25 * height) - (5 * age) - 161;
    return (maleBMR + femaleBMR) / 2;
  }
}

// Коэффициенты активности
const activityMultipliers = {
  'sedentary': 1.2,
  'lightly_active': 1.375,
  'moderately_active': 1.55,
  'very_active': 1.725,
  'extremely_active': 1.9
};

const bmr = calculateBMR(age, weight, height, gender);
const tdee = bmr * activityMultipliers[activityLevel];
```

---

## 📝 Примеры новых запросов

### Анализ фото
```json
{
  "imageBase64": "base64_image_data",
  "userProfile": {
    "age": 28,
    "weight": 70,
    "height": 175,
    "gender": "male",
    "activityLevel": "moderately_active",
    "goal": "maintain_weight"
  }
}
```

### Анализ текста
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

---

## ⚡ Быстрый чеклист

### В Make.com сценарии:
- [ ] Удалить все ссылки на `dailyCalories`, `dailyProteins`, `dailyFats`, `dailyCarbs`
- [ ] Удалить обработку `birthday` (теперь приходит `age`)
- [ ] Удалить обработку `condition` (теперь приходит `activityLevel`)
- [ ] Добавить обработку `age` (число, а не дата)
- [ ] Добавить обработку `activityLevel` (строка с подчеркиваниями)
- [ ] Обновить логику расчета BMR/TDEE
- [ ] Протестировать с новыми данными

### Тестирование:
- [ ] Отправить тестовый запрос анализа фото
- [ ] Отправить тестовый запрос анализа текста
- [ ] Проверить корректность ответов
- [ ] Убедиться что приложение корректно парсит ответы

---

## 🆘 Если что-то не работает

1. **Проверьте логи Make.com** на наличие ошибок парсинга
2. **Убедитесь** что удалили все старые поля
3. **Проверьте** что новые поля обрабатываются корректно
4. **Протестируйте** с дефолтным профилем:
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

---

## 📞 Поддержка

Если возникают проблемы с миграцией, проверьте:
- Webhook ID: `653st2c10rmg92nlltf3y0m8sggxaac6`
- Структуру JSON в документации выше
- Логи Make.com для диагностики ошибок

**Статус:** 🔴 ТРЕБУЕТ ОБНОВЛЕНИЯ Make.com сценария
**После обновления:** 🟢 ГОТОВ К PRODUCTION