# Clean Architecture Refactoring - Design Document

## Overview

This document outlines the design for refactoring the CalorieTracker Android application to implement Clean Architecture principles. The refactoring will transform the current monolithic ViewModel-centric architecture into a layered architecture with clear separation of concerns, improved testability, and better maintainability.

## Architecture

### Current Architecture Issues
- **Mixed Responsibilities**: ViewModels contain business logic, data access, and UI state management
- **Tight Coupling**: Direct dependencies between presentation and data layers
- **Testing Difficulties**: Hard to unit test business logic due to Android dependencies
- **Code Duplication**: Similar logic scattered across multiple ViewModels

### Target Architecture: Clean Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  ViewModels │  │  Composables│  │  Activities │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  Use Cases  │  │  Entities   │  │ Repository  │         │
│  │             │  │             │  │ Interfaces  │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ Repository  │  │  Data       │  │  Network    │         │
│  │ Impl        │  │  Sources    │  │  Services   │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### Domain Layer Structure

#### Entities (`domain/entities/`)
Core business models that represent the fundamental concepts of the application:

```kotlin
// domain/entities/Food.kt
data class Food(
    val name: String,
    val calories: Int,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val weight: String,
    val source: FoodSource,
    val aiOpinion: String? = null
)

// domain/entities/User.kt  
data class User(
    val name: String,
    val birthday: String,
    val height: Int,
    val weight: Int,
    val gender: Gender,
    val activityLevel: ActivityLevel,
    val goal: NutritionGoal,
    val dailyTargets: NutritionTargets
)

// domain/entities/NutritionIntake.kt
data class NutritionIntake(
    val date: LocalDate,
    val meals: List<Meal>,
    val totalCalories: Int,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double
)
```

#### Repository Interfaces (`domain/repositories/`)
Contracts that define data access operations without implementation details:

```kotlin
// domain/repositories/FoodRepository.kt
interface FoodRepository {
    suspend fun analyzeFoodPhoto(photoPath: String, caption: String): Result<Food>
    suspend fun analyzeFoodDescription(description: String): Result<Food>
    suspend fun saveFoodIntake(food: Food, mealType: MealType): Result<Unit>
    suspend fun getFoodHistory(dateRange: DateRange): Result<List<NutritionIntake>>
}

// domain/repositories/UserRepository.kt
interface UserRepository {
    suspend fun getUserProfile(): Result<User>
    suspend fun saveUserProfile(user: User): Result<Unit>
    suspend fun calculateNutritionTargets(user: User): NutritionTargets
}

// domain/repositories/NutritionRepository.kt
interface NutritionRepository {
    suspend fun getDailyIntake(date: LocalDate): Result<NutritionIntake>
    suspend fun getWeeklyIntake(startDate: LocalDate): Result<List<NutritionIntake>>
    suspend fun getMonthlyIntake(month: YearMonth): Result<List<NutritionIntake>>
}
```

#### Use Cases (`domain/usecases/`)
Encapsulate business operations and orchestrate data flow:

```kotlin
// domain/usecases/AnalyzeFoodPhotoUseCase.kt
class AnalyzeFoodPhotoUseCase(
    private val foodRepository: FoodRepository
) {
    suspend operator fun invoke(photoPath: String, caption: String): Result<Food> {
        return foodRepository.analyzeFoodPhoto(photoPath, caption)
    }
}

// domain/usecases/SaveFoodIntakeUseCase.kt
class SaveFoodIntakeUseCase(
    private val foodRepository: FoodRepository,
    private val nutritionRepository: NutritionRepository
) {
    suspend operator fun invoke(food: Food, mealType: MealType): Result<Unit> {
        return try {
            foodRepository.saveFoodIntake(food, mealType)
            // Update daily intake cache
            nutritionRepository.invalidateDailyCache(LocalDate.now())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// domain/usecases/CalculateNutritionUseCase.kt
class CalculateNutritionUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(user: User): Result<NutritionTargets> {
        return try {
            val targets = userRepository.calculateNutritionTargets(user)
            Result.success(targets)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Data Layer Structure

#### Repository Implementations (`data/repositories/`)
Concrete implementations of domain repository interfaces:

```kotlin
// data/repositories/FoodRepositoryImpl.kt
@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val makeService: MakeService,
    private val localDataSource: LocalDataSource,
    private val mapper: FoodMapper
) : FoodRepository {
    
    override suspend fun analyzeFoodPhoto(photoPath: String, caption: String): Result<Food> {
        return try {
            val response = makeService.analyzeFoodPhoto(photoPath, caption)
            val food = mapper.mapNetworkToEntity(response)
            Result.success(food)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun saveFoodIntake(food: Food, mealType: MealType): Result<Unit> {
        return try {
            val entity = mapper.mapEntityToLocal(food)
            localDataSource.saveFoodIntake(entity, mealType)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

#### Data Mappers (`data/mappers/`)
Transform data between different representations:

```kotlin
// data/mappers/FoodMapper.kt
@Singleton
class FoodMapper @Inject constructor() {
    
    fun mapNetworkToEntity(networkFood: NetworkFood): Food {
        return Food(
            name = networkFood.name,
            calories = networkFood.calories,
            protein = networkFood.protein,
            fat = networkFood.fat,
            carbs = networkFood.carbs,
            weight = networkFood.weight,
            source = FoodSource.AI_ANALYSIS,
            aiOpinion = networkFood.opinion
        )
    }
    
    fun mapEntityToLocal(food: Food): FoodEntity {
        return FoodEntity(
            name = food.name,
            calories = food.calories,
            protein = food.protein,
            fat = food.fat,
            carbs = food.carbs,
            weight = food.weight,
            source = food.source.name,
            aiOpinion = food.aiOpinion
        )
    }
}
```

### Presentation Layer Updates

#### Updated ViewModels
ViewModels will be simplified to focus only on UI state and user interactions:

```kotlin
// presentation/viewmodels/CalorieTrackerViewModel.kt
@HiltViewModel
class CalorieTrackerViewModel @Inject constructor(
    private val analyzeFoodPhotoUseCase: AnalyzeFoodPhotoUseCase,
    private val saveFoodIntakeUseCase: SaveFoodIntakeUseCase,
    private val getDailyIntakeUseCase: GetDailyIntakeUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CalorieTrackerUiState())
    val uiState: StateFlow<CalorieTrackerUiState> = _uiState.asStateFlow()
    
    fun analyzePhoto(photoPath: String, caption: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            
            analyzeFoodPhotoUseCase(photoPath, caption)
                .onSuccess { food ->
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        pendingFood = food
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        error = error.message
                    )
                }
        }
    }
    
    fun confirmFood(food: Food, mealType: MealType) {
        viewModelScope.launch {
            saveFoodIntakeUseCase(food, mealType)
                .onSuccess {
                    loadDailyIntake()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
        }
    }
}
```

## Data Models

### Domain Entities Migration
Current data models will be moved and refactored:

| Current Location | New Location | Changes |
|------------------|--------------|---------|
| `data/FoodItem.kt` | `domain/entities/Food.kt` | Add enums for source, remove Android dependencies |
| `data/UserProfile.kt` | `domain/entities/User.kt` | Split into User and NutritionTargets |
| `data/ChatMessage.kt` | `domain/entities/ChatMessage.kt` | Remove UI-specific fields |
| `data/DailyIntake.kt` | `domain/entities/NutritionIntake.kt` | Rename and add validation |

### Value Objects and Enums
```kotlin
// domain/entities/common/FoodSource.kt
enum class FoodSource {
    MANUAL_INPUT,
    AI_PHOTO_ANALYSIS,
    AI_TEXT_ANALYSIS,
    BARCODE_SCAN
}

// domain/entities/common/MealType.kt
enum class MealType(val displayName: String) {
    BREAKFAST("Завтрак"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    SNACK("Перекус")
}

// domain/entities/common/NutritionTargets.kt
data class NutritionTargets(
    val dailyCalories: Int,
    val dailyProtein: Int,
    val dailyFat: Int,
    val dailyCarbs: Int
)
```

## Error Handling

### Result Wrapper
Implement a consistent Result type for error handling across layers:

```kotlin
// domain/common/Result.kt
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    inline fun onFailure(action: (Throwable) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }
}
```

### Domain Exceptions
```kotlin
// domain/exceptions/DomainExceptions.kt
sealed class DomainException(message: String) : Exception(message) {
    class NetworkException(message: String) : DomainException(message)
    class ValidationException(message: String) : DomainException(message)
    class DataNotFoundException(message: String) : DomainException(message)
    class AIAnalysisException(message: String) : DomainException(message)
}
```

## Testing Strategy

### Domain Layer Testing
- **Use Cases**: Pure unit tests without Android dependencies
- **Entities**: Test business logic and validation rules
- **Repository Interfaces**: Define test contracts

### Data Layer Testing
- **Repository Implementations**: Test with mocked network and database
- **Mappers**: Test data transformations
- **Data Sources**: Integration tests

### Presentation Layer Testing
- **ViewModels**: Test with mocked use cases
- **UI State**: Test state transformations
- **User Interactions**: Test event handling

## Migration Plan

### Phase 1: Domain Layer Creation
1. Create domain package structure
2. Move and refactor entities
3. Create repository interfaces
4. Implement core use cases

### Phase 2: Data Layer Refactoring
1. Implement repository interfaces
2. Create data mappers
3. Update dependency injection
4. Migrate data sources

### Phase 3: Presentation Layer Updates
1. Refactor ViewModels to use use cases
2. Remove direct repository dependencies
3. Update UI state management
4. Fix all imports and references

### Phase 4: Testing and Validation
1. Add comprehensive tests for each layer
2. Validate functionality preservation
3. Performance testing
4. Code cleanup and documentation

## Dependency Injection Updates

### New Hilt Modules
```kotlin
// di/DomainModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {
    
    @Binds
    abstract fun bindFoodRepository(
        foodRepositoryImpl: FoodRepositoryImpl
    ): FoodRepository
    
    @Binds
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
}

// di/UseCaseModule.kt
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    
    @Provides
    fun provideAnalyzeFoodPhotoUseCase(
        foodRepository: FoodRepository
    ): AnalyzeFoodPhotoUseCase {
        return AnalyzeFoodPhotoUseCase(foodRepository)
    }
}
```

This design ensures clean separation of concerns, improved testability, and maintainable code structure while preserving all existing functionality.