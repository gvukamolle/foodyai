# ADR-002: Result Wrapper Pattern for Error Handling

## Status
Accepted

## Context
In the original architecture, error handling was inconsistent across the application:
- Some methods threw exceptions
- Others returned nullable types
- Network errors were handled differently from business logic errors
- ViewModels had to handle various error types differently
- No consistent way to distinguish between different error categories

This led to:
- Inconsistent error handling patterns
- Difficult error propagation across layers
- Poor user experience with generic error messages
- Hard to test error scenarios

## Decision
Implement a `Result<T>` wrapper pattern for consistent error handling across all layers:

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: DomainException) : Result<Nothing>()
    
    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun <T> error(exception: DomainException): Result<T> = Error(exception)
    }
}
```

### Domain Exceptions Hierarchy
```kotlin
sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ValidationException(message: String, cause: Throwable? = null) : DomainException(message, cause)
    class NetworkException(message: String, cause: Throwable? = null) : DomainException(message, cause)
    class StorageException(message: String, cause: Throwable? = null) : DomainException(message, cause)
    class AIAnalysisException(message: String, cause: Throwable? = null) : DomainException(message, cause)
    class UsageLimitException(message: String, cause: Throwable? = null) : DomainException(message, cause)
    class UnknownException(message: String, cause: Throwable? = null) : DomainException(message, cause)
}
```

## Consequences

### Positive
- **Consistent Error Handling**: All operations return `Result<T>` for uniform handling
- **Type Safety**: Compile-time guarantee that errors are handled
- **Clear Error Categories**: Domain-specific exceptions provide context
- **Better Testing**: Easy to test both success and error scenarios
- **Improved UX**: Specific error messages for different error types
- **Error Propagation**: Errors can be easily passed through layers
- **No Silent Failures**: All operations must explicitly handle success/error cases

### Negative
- **Verbosity**: More code required for error handling
- **Learning Curve**: Team needs to understand Result pattern
- **Wrapping Overhead**: Additional object creation for each operation

### Implementation Guidelines

#### Use Cases
```kotlin
class SaveFoodIntakeUseCase(
    private val foodRepository: FoodRepository,
    private val nutritionRepository: NutritionRepository
) {
    suspend operator fun invoke(params: Params): Result<Unit> {
        return try {
            // Validate input
            if (params.food.name.isBlank()) {
                return Result.error(DomainException.ValidationException("Food name cannot be blank"))
            }
            
            // Perform business operation
            val validationResult = foodRepository.validateFoodData(params.food)
            if (validationResult is Result.Error) {
                return validationResult
            }
            
            // Save food
            foodRepository.saveFoodIntake(params.food, params.mealType)
        } catch (e: Exception) {
            Result.error(DomainException.UnknownException("Unexpected error", e))
        }
    }
}
```

#### Repository Implementations
```kotlin
class FoodRepositoryImpl(
    private val dataRepository: DataRepository,
    private val foodMapper: FoodMapper
) : FoodRepository {
    
    override suspend fun saveFoodIntake(food: Food, mealType: MealType): Result<Unit> {
        return try {
            val dataFood = foodMapper.mapDomainToData(food)
            dataRepository.saveFoodIntake(dataFood, mealType)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.error(DomainException.StorageException("Failed to save food intake", e))
        }
    }
}
```

#### ViewModels
```kotlin
class CalorieTrackerViewModel(
    private val saveFoodIntakeUseCase: SaveFoodIntakeUseCase
) : ViewModel() {
    
    fun saveFoodIntake(food: Food, mealType: MealType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            
            when (val result = saveFoodIntakeUseCase(SaveFoodIntakeUseCase.Params(food, mealType))) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        successMessage = "Food saved successfully"
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = getErrorMessage(result.exception)
                    )
                }
            }
        }
    }
    
    private fun getErrorMessage(exception: DomainException): String {
        return when (exception) {
            is DomainException.ValidationException -> "Validation failed: ${exception.message}"
            is DomainException.NetworkException -> "Network error: ${exception.message}"
            is DomainException.StorageException -> "Storage error: ${exception.message}"
            else -> "An unexpected error occurred"
        }
    }
}
```

## Alternatives Considered

### 1. Traditional Exception Handling
- **Pros**: Familiar pattern, less boilerplate
- **Cons**: Inconsistent handling, hard to test, can crash app

### 2. Nullable Return Types
- **Pros**: Simple, built into Kotlin
- **Cons**: No error information, doesn't distinguish error types

### 3. Either/Try Monads
- **Pros**: Functional programming approach, composable
- **Cons**: Learning curve, not idiomatic in Android development

### 4. Callback-based Error Handling
- **Pros**: Explicit error handling
- **Cons**: Callback hell, not suitable for coroutines

## Migration Strategy
1. **Phase 1**: Implement Result wrapper and domain exceptions
2. **Phase 2**: Update use cases to return Result<T>
3. **Phase 3**: Update repository interfaces and implementations
4. **Phase 4**: Update ViewModels to handle Result<T>
5. **Phase 5**: Add comprehensive error handling tests
6. **Phase 6**: Remove old exception-based error handling

## Validation Criteria
- All use cases return Result<T>
- All repository methods return Result<T>
- ViewModels handle both success and error cases
- Specific error messages for different exception types
- Comprehensive test coverage for error scenarios
- No unhandled exceptions in production

## References
- [Railway Oriented Programming](https://fsharpforfunandprofit.com/rop/)
- [Kotlin Result Class](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/)
- [Error Handling in Clean Architecture](https://blog.cleancoder.com/uncle-bob/2014/11/24/FPvsOO.html)

---
**Date**: 2024-01-15  
**Author**: Development Team  
**Reviewers**: Tech Lead, Senior Developers