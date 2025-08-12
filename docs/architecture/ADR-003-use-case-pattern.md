# ADR-003: Use Case Pattern for Business Logic

## Status
Accepted

## Context
In the original architecture, business logic was scattered across ViewModels and repositories:
- ViewModels contained complex business operations
- Repositories mixed data access with business rules
- Business logic was duplicated across different ViewModels
- Difficult to test business operations in isolation
- No clear place for complex business workflows

Examples of scattered business logic:
- Nutrition target calculations in ViewModels
- Food validation logic in repositories
- AI usage limit checking in multiple places
- Complex meal saving workflows spread across layers

## Decision
Implement the Use Case pattern to encapsulate business logic in dedicated classes:

### Use Case Structure
```kotlin
abstract class UseCase<in P, out R> {
    suspend operator fun invoke(params: P): Result<R>
}

// For use cases without parameters
abstract class NoParamsUseCase<out R> {
    suspend operator fun invoke(): Result<R>
}
```

### Example Implementation
```kotlin
class SaveFoodIntakeUseCase(
    private val foodRepository: FoodRepository,
    private val nutritionRepository: NutritionRepository
) : UseCase<SaveFoodIntakeUseCase.Params, Unit>() {
    
    data class Params(
        val food: Food,
        val mealType: MealType,
        val date: LocalDate = LocalDate.now()
    )
    
    override suspend fun invoke(params: Params): Result<Unit> {
        // 1. Validate input
        if (params.food.name.isBlank()) {
            return Result.error(DomainException.ValidationException("Food name cannot be blank"))
        }
        
        // 2. Validate food data through repository
        val validationResult = foodRepository.validateFoodData(params.food)
        if (validationResult is Result.Error) {
            return validationResult
        }
        
        // 3. Save food intake
        val saveResult = foodRepository.saveFoodIntake(params.food, params.mealType)
        if (saveResult is Result.Error) {
            return saveResult
        }
        
        // 4. Update daily nutrition data
        val meal = Meal(params.mealType, listOf(params.food))
        val nutritionResult = nutritionRepository.addMealToDay(params.date, meal)
        if (nutritionResult is Result.Error) {
            return nutritionResult
        }
        
        // 5. Invalidate cache
        nutritionRepository.invalidateDailyCache(params.date)
        
        return Result.success(Unit)
    }
}
```

## Use Case Categories

### 1. Food Operations
- `AnalyzeFoodPhotoUseCase` - AI photo analysis
- `AnalyzeFoodDescriptionUseCase` - AI text analysis
- `SaveFoodIntakeUseCase` - Save food consumption
- `GetFoodHistoryUseCase` - Retrieve food history

### 2. User Management
- `GetUserProfileUseCase` - Get user profile
- `SaveUserProfileUseCase` - Save user profile
- `CalculateNutritionTargetsUseCase` - Calculate nutrition goals
- `ValidateUserDataUseCase` - Validate user input

### 3. Nutrition Tracking
- `GetDailyIntakeUseCase` - Get daily nutrition data
- `GetWeeklyIntakeUseCase` - Get weekly nutrition analysis
- `GetMonthlyIntakeUseCase` - Get monthly nutrition trends
- `CalculateNutritionProgressUseCase` - Calculate progress to goals

### 4. Chat and AI
- `SendChatMessageUseCase` - Send chat message
- `GetChatHistoryUseCase` - Get chat history
- `ProcessAIResponseUseCase` - Process AI responses
- `ValidateAIUsageLimitsUseCase` - Check usage limits

## Consequences

### Positive
- **Single Responsibility**: Each use case has one specific business operation
- **Testability**: Business logic can be tested in isolation
- **Reusability**: Use cases can be reused across different ViewModels
- **Clear Business Intent**: Use case names clearly express business operations
- **Dependency Management**: Clear dependencies for each business operation
- **Composition**: Complex workflows can be composed from simpler use cases
- **Maintainability**: Easy to modify business logic without affecting other layers

### Negative
- **More Classes**: Increased number of files and classes
- **Boilerplate**: Some repetitive structure across use cases
- **Learning Curve**: Team needs to understand use case pattern

### Implementation Guidelines

#### 1. Naming Convention
- Use descriptive names that express business intent
- End with "UseCase" suffix
- Use verb-noun format (e.g., `SaveFoodIntakeUseCase`, `CalculateNutritionTargetsUseCase`)

#### 2. Parameters
- Use data classes for parameters when multiple inputs are needed
- Use meaningful parameter names
- Provide default values where appropriate

#### 3. Error Handling
- Always return `Result<T>` for consistent error handling
- Validate inputs and return appropriate domain exceptions
- Wrap unexpected exceptions in `DomainException.UnknownException`

#### 4. Dependencies
- Inject only repository interfaces (from domain layer)
- Avoid dependencies on presentation or data layer implementations
- Keep dependencies minimal and focused

#### 5. Business Logic
- Encapsulate all business rules within use cases
- Coordinate between multiple repositories when needed
- Perform validation and transformation as required

### ViewModels Integration
```kotlin
class CalorieTrackerViewModel(
    private val analyzeFoodPhotoUseCase: AnalyzeFoodPhotoUseCase,
    private val saveFoodIntakeUseCase: SaveFoodIntakeUseCase,
    private val getDailyIntakeUseCase: GetDailyIntakeUseCase
) : ViewModel() {
    
    fun analyzeFoodPhoto(photoPath: String, caption: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
            
            val result = analyzeFoodPhotoUseCase(
                AnalyzeFoodPhotoUseCase.Params(photoPath, caption)
            )
            
            when (result) {
                is Result.Success -> {
                    _analyzedFood.value = result.data
                    _uiState.value = _uiState.value.copy(isAnalyzing = false)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isAnalyzing = false,
                        error = "Failed to analyze photo: ${result.exception.message}"
                    )
                }
            }
        }
    }
}
```

## Testing Strategy

### Unit Testing Use Cases
```kotlin
class SaveFoodIntakeUseCaseTest {
    
    @Mock
    private lateinit var foodRepository: FoodRepository
    
    @Mock
    private lateinit var nutritionRepository: NutritionRepository
    
    private lateinit var useCase: SaveFoodIntakeUseCase
    
    @Test
    fun `save food intake successfully`() = runTest {
        // Given
        val food = createTestFood()
        val params = SaveFoodIntakeUseCase.Params(food, MealType.BREAKFAST)
        
        whenever(foodRepository.validateFoodData(food)).thenReturn(Result.success(food))
        whenever(foodRepository.saveFoodIntake(food, MealType.BREAKFAST)).thenReturn(Result.success(Unit))
        whenever(nutritionRepository.addMealToDay(any(), any())).thenReturn(Result.success(Unit))
        whenever(nutritionRepository.invalidateDailyCache(any())).thenReturn(Result.success(Unit))
        
        // When
        val result = useCase(params)
        
        // Then
        assertTrue(result is Result.Success)
        verify(foodRepository).validateFoodData(food)
        verify(foodRepository).saveFoodIntake(food, MealType.BREAKFAST)
        verify(nutritionRepository).addMealToDay(any(), any())
        verify(nutritionRepository).invalidateDailyCache(any())
    }
}
```

## Alternatives Considered

### 1. Keep Business Logic in ViewModels
- **Pros**: Fewer classes, familiar pattern
- **Cons**: Tight coupling, hard to test, code duplication

### 2. Service Layer Pattern
- **Pros**: Similar benefits to use cases
- **Cons**: Less focused, can become god classes

### 3. Command Pattern
- **Pros**: Encapsulates operations, supports undo
- **Cons**: More complex, overkill for most operations

### 4. Repository Pattern Only
- **Pros**: Simpler architecture
- **Cons**: Business logic mixed with data access

## Migration Strategy
1. **Phase 1**: Create use case base classes and structure
2. **Phase 2**: Implement use cases for core operations
3. **Phase 3**: Update ViewModels to use use cases
4. **Phase 4**: Remove business logic from repositories
5. **Phase 5**: Add comprehensive use case tests
6. **Phase 6**: Optimize and refactor based on usage patterns

## Validation Criteria
- All business operations are encapsulated in use cases
- ViewModels only coordinate UI state and call use cases
- Repositories only handle data access, no business logic
- Each use case has comprehensive unit tests
- Use cases can be composed for complex workflows
- Clear separation between business logic and data access

## References
- [Clean Architecture Use Cases](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Use Case Driven Object Modeling](https://www.amazon.com/Use-Case-Driven-Object-Modeling/dp/1590597745)
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)

---
**Date**: 2024-01-15  
**Author**: Development Team  
**Reviewers**: Tech Lead, Senior Developers