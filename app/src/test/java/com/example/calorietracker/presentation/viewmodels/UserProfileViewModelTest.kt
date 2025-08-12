package com.example.calorietracker.presentation.viewmodels

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.common.Gender
import com.example.calorietracker.domain.entities.common.NutritionTargets
import com.example.calorietracker.domain.exceptions.DomainException
import com.example.calorietracker.domain.usecases.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModelTest {
    
    private lateinit var getUserProfileUseCase: GetUserProfileUseCase
    private lateinit var saveUserProfileUseCase: SaveUserProfileUseCase
    private lateinit var calculateNutritionTargetsUseCase: CalculateNutritionTargetsUseCase
    private lateinit var validateUserDataUseCase: ValidateUserDataUseCase
    private lateinit var viewModel: UserProfileViewModel
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        getUserProfileUseCase = mockk()
        saveUserProfileUseCase = mockk()
        calculateNutritionTargetsUseCase = mockk()
        validateUserDataUseCase = mockk()
        
        viewModel = UserProfileViewModel(
            getUserProfileUseCase,
            saveUserProfileUseCase,
            calculateNutritionTargetsUseCase,
            validateUserDataUseCase
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `load user profile successfully updates state`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE
        )
        
        coEvery { getUserProfileUseCase() } returns Result.success(user)
        coEvery { 
            calculateNutritionTargetsUseCase(any()) 
        } returns Result.success(NutritionTargets(2000, 150, 65, 250))
        
        // When
        viewModel.loadUserProfile()
        
        // Then
        val userProfile = viewModel.userProfile.first()
        assertEquals(user, userProfile)
        
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertNull(uiState.error)
        
        coVerify { getUserProfileUseCase() }
        coVerify { calculateNutritionTargetsUseCase(any()) }
    }
    
    @Test
    fun `load user profile failure updates error state`() = runTest {
        // Given
        val error = DomainException.StorageException("Storage error")
        coEvery { getUserProfileUseCase() } returns Result.error(error)
        
        // When
        viewModel.loadUserProfile()
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertEquals("Failed to load profile: Storage error", uiState.error)
        
        val userProfile = viewModel.userProfile.first()
        assertNull(userProfile)
    }
    
    @Test
    fun `save user profile with valid data succeeds`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE
        )
        val targets = NutritionTargets(2000, 150, 65, 250)
        
        coEvery { validateUserDataUseCase(any()) } returns Result.success(user)
        coEvery { saveUserProfileUseCase(any()) } returns Result.success(Unit)
        coEvery { calculateNutritionTargetsUseCase(any()) } returns Result.success(targets)
        
        // When
        viewModel.saveUserProfile(user)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertEquals("Profile saved successfully", uiState.successMessage)
        assertNull(uiState.error)
        
        val userProfile = viewModel.userProfile.first()
        assertEquals(user, userProfile)
        
        val nutritionTargets = viewModel.nutritionTargets.first()
        assertEquals(targets, nutritionTargets)
        
        coVerify { validateUserDataUseCase(any()) }
        coVerify { saveUserProfileUseCase(any()) }
        coVerify { calculateNutritionTargetsUseCase(any()) }
    }
    
    @Test
    fun `save user profile with validation error shows error`() = runTest {
        // Given
        val user = User(name = "") // Invalid user
        val validationError = DomainException.ValidationException("Name cannot be blank")
        
        coEvery { validateUserDataUseCase(any()) } returns Result.error(validationError)
        
        // When
        viewModel.saveUserProfile(user)
        
        // Then
        val uiState = viewModel.uiState.first()
        assertFalse(uiState.isLoading)
        assertEquals("Validation failed: Name cannot be blank", uiState.error)
        assertNull(uiState.successMessage)
        
        coVerify { validateUserDataUseCase(any()) }
        coVerify(exactly = 0) { saveUserProfileUseCase(any()) }
    }
    
    @Test
    fun `calculate nutrition targets updates targets state`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE
        )
        val targets = NutritionTargets(2200, 165, 70, 275)
        
        coEvery { 
            calculateNutritionTargetsUseCase(any()) 
        } returns Result.success(targets)
        
        // When
        viewModel.calculateNutritionTargets(user)
        
        // Then
        val nutritionTargets = viewModel.nutritionTargets.first()
        assertEquals(targets, nutritionTargets)
        
        val userProfile = viewModel.userProfile.first()
        assertEquals(targets, userProfile?.nutritionTargets)
        
        coVerify { calculateNutritionTargetsUseCase(any()) }
    }
    
    @Test
    fun `clear error removes error from state`() = runTest {
        // Given - set an error first
        val error = DomainException.ValidationException("Test error")
        coEvery { getUserProfileUseCase() } returns Result.error(error)
        viewModel.loadUserProfile()
        
        // Verify error is set
        var uiState = viewModel.uiState.first()
        assertNotNull(uiState.error)
        
        // When
        viewModel.clearError()
        
        // Then
        uiState = viewModel.uiState.first()
        assertNull(uiState.error)
    }
    
    @Test
    fun `is setup complete returns correct value`() = runTest {
        // Given
        val completeUser = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE,
            isSetupComplete = true
        )
        
        coEvery { getUserProfileUseCase() } returns Result.success(completeUser)
        viewModel.loadUserProfile()
        
        // When & Then
        assertTrue(viewModel.isSetupComplete())
    }
    
    @Test
    fun `get current BMI returns correct value`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 72, // BMI = 22.22
            gender = Gender.MALE
        )
        
        coEvery { getUserProfileUseCase() } returns Result.success(user)
        viewModel.loadUserProfile()
        
        // When
        val bmi = viewModel.getCurrentBMI()
        
        // Then
        assertNotNull(bmi)
        assertEquals(22.22, bmi!!, 0.01)
    }
    
    @Test
    fun `get current BMI category returns correct category`() = runTest {
        // Given
        val user = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 72, // BMI = 22.22 (normal)
            gender = Gender.MALE
        )
        
        coEvery { getUserProfileUseCase() } returns Result.success(user)
        viewModel.loadUserProfile()
        
        // When
        val category = viewModel.getCurrentBMICategory()
        
        // Then
        assertEquals("Нормальный вес", category)
    }
}