# Calorie Tracker - Clean Architecture

Современное Android приложение для отслеживания калорий и питания, построенное с использованием принципов Clean Architecture.

## 🏗️ Архитектура

Приложение следует принципам **Clean Architecture** с четким разделением на слои:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   ViewModels    │  │   UI Components │  │  Composables │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │   Use Cases     │  │    Entities     │  │ Repositories │ │
│  │                 │  │                 │  │ (Interfaces) │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │  Repositories   │  │     Mappers     │  │ Data Sources │ │
│  │ (Implementations)│  │                 │  │              │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Слои архитектуры

#### 🎨 Presentation Layer
- **ViewModels**: Управляют состоянием UI и взаимодействуют с Use Cases
- **UI Components**: Jetpack Compose компоненты для отображения данных
- **Навигация**: Управление переходами между экранами

#### 🏢 Domain Layer (Бизнес-логика)
- **Entities**: Основные бизнес-объекты (User, Food, NutritionIntake)
- **Use Cases**: Конкретные бизнес-операции (SaveFoodIntakeUseCase, CalculateNutritionTargetsUseCase)
- **Repository Interfaces**: Абстракции для доступа к данным
- **Common**: Общие типы (Result, исключения)

#### 🗄️ Data Layer
- **Repository Implementations**: Реализации интерфейсов из Domain слоя
- **Mappers**: Преобразование между data и domain моделями
- **Data Sources**: Room database, SharedPreferences, Network API

## 🚀 Основные функции

### 📊 Отслеживание питания
- Анализ еды по фотографии с помощью AI
- Анализ еды по текстовому описанию
- Ручной ввод данных о питании
- Отслеживание калорий, белков, жиров и углеводов

### 👤 Управление профилем
- Настройка личных данных (рост, вес, возраст)
- Расчет BMI и целевых значений питания
- Выбор уровня активности и целей

### 📈 Аналитика
- Дневная статистика питания
- Недельные и месячные отчеты
- Прогресс к целям
- Визуализация данных

### 💬 AI Ассистент
- Чат с AI для анализа питания
- Рекомендации по питанию
- Ответы на вопросы о здоровье

## 🛠️ Технологический стек

### Core
- **Kotlin** - основной язык разработки
- **Jetpack Compose** - современный UI toolkit
- **Coroutines** - асинхронное программирование
- **Flow** - реактивные потоки данных

### Architecture & DI
- **Clean Architecture** - архитектурный паттерн
- **Hilt** - dependency injection
- **ViewModel** - управление состоянием UI

### Data & Storage
- **Room** - локальная база данных
- **SharedPreferences** - простое хранение настроек
- **Retrofit** - HTTP клиент для API
- **Gson** - JSON сериализация

### Testing
- **JUnit** - unit тестирование
- **MockK** - мокирование для Kotlin
- **Coroutines Test** - тестирование корутин

## 📁 Структура проекта

```
app/src/main/java/com/example/calorietracker/
├── data/                           # Data Layer
│   ├── mappers/                   # Преобразователи данных
│   ├── repositories/              # Реализации репозиториев
│   └── [legacy data classes]      # Старые модели данных
├── domain/                        # Domain Layer
│   ├── entities/                  # Бизнес-сущности
│   ├── usecases/                  # Варианты использования
│   ├── repositories/              # Интерфейсы репозиториев
│   ├── common/                    # Общие типы
│   └── exceptions/                # Доменные исключения
├── presentation/                  # Presentation Layer
│   └── viewmodels/               # ViewModels
├── di/                           # Dependency Injection
├── pages/                        # UI экраны (Compose)
├── components/                   # Переиспользуемые UI компоненты
├── utils/                        # Утилиты
└── [other packages]              # Другие компоненты
```

## 🧪 Тестирование

Проект имеет comprehensive test coverage на всех уровнях:

### Unit Tests
```bash
# Запуск всех unit тестов
./gradlew test

# Тесты domain слоя
./gradlew testDebugUnitTest --tests "*domain*"

# Тесты data слоя  
./gradlew testDebugUnitTest --tests "*data*"

# Тесты presentation слоя
./gradlew testDebugUnitTest --tests "*presentation*"
```

### Integration Tests
```bash
# Интеграционные тесты
./gradlew testDebugUnitTest --tests "*integration*"
```

### Test Coverage
```bash
# Генерация отчета о покрытии кода
./gradlew jacocoTestReport
```

## 🔧 Настройка проекта

### Требования
- Android Studio Arctic Fox или новее
- Kotlin 1.8+
- Android SDK 24+
- JDK 11+

### Установка
1. Клонируйте репозиторий
2. Откройте проект в Android Studio
3. Синхронизируйте Gradle
4. Запустите приложение

### Конфигурация
- API ключи настраиваются в `local.properties`
- Настройки базы данных в `DatabaseModule`
- Настройки сети в `NetworkModule`

## 📋 Принципы разработки

### Clean Architecture Principles
- **Dependency Rule**: Зависимости направлены внутрь к domain слою
- **Separation of Concerns**: Каждый слой имеет свою ответственность
- **Testability**: Все компоненты легко тестируются изолированно
- **Independence**: Слои независимы от внешних фреймворков

### Code Quality
- **SOLID принципы** - основа архитектуры
- **DRY (Don't Repeat Yourself)** - избегаем дублирования кода
- **KISS (Keep It Simple, Stupid)** - простота решений
- **YAGNI (You Aren't Gonna Need It)** - не добавляем лишнюю функциональность

### Error Handling
- Использование `Result<T>` wrapper для обработки ошибок
- Доменные исключения для бизнес-логики
- Graceful degradation при сетевых ошибках

## 🔄 Миграция на Clean Architecture

Проект был успешно мигрирован с традиционной MVVM архитектуры на Clean Architecture:

### Что изменилось
- ✅ Выделен отдельный Domain слой с бизнес-логикой
- ✅ Созданы Use Cases для всех операций
- ✅ Добавлены Repository интерфейсы в Domain слое
- ✅ Реализованы Mappers для преобразования данных
- ✅ Обновлены ViewModels для работы с Use Cases
- ✅ Добавлена comprehensive система тестирования

### Преимущества новой архитектуры
- 🧪 **Лучшая тестируемость** - каждый слой тестируется изолированно
- 🔧 **Легкость поддержки** - четкое разделение ответственности
- 🚀 **Масштабируемость** - простое добавление новых функций
- 🔄 **Гибкость** - легкая замена реализаций
- 📚 **Читаемость** - понятная структура кода

## 🤝 Вклад в проект

1. Fork проекта
2. Создайте feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit изменения (`git commit -m 'Add some AmazingFeature'`)
4. Push в branch (`git push origin feature/AmazingFeature`)
5. Откройте Pull Request

## 📄 Лицензия

Этот проект лицензирован под MIT License - см. файл [LICENSE](LICENSE) для деталей.

## 📞 Контакты

- **Разработчик**: [Ваше имя]
- **Email**: [ваш email]
- **Telegram**: [ваш telegram]

---

**Calorie Tracker** - современное решение для здорового образа жизни! 🥗💪