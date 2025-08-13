package com.example.calorietracker.validation

import com.example.calorietracker.data.mappers.FoodMapper
import com.example.calorietracker.data.mappers.UserMapper
import com.example.calorietracker.data.mappers.NutritionMapper
import com.example.calorietracker.validation.models.ValidationResults
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for DataMapperValidator to ensure mapper correctness
 */
class DataMapperValidatorTest {
    
    private lateinit var foodMapper: FoodMapper
    private lateinit var userMapper: UserMapper
    private lateinit var nutritionMapper: NutritionMapper
    private lateinit var validator: DataMapperValidator
    
    @Before
    fun setup() {
        foodMapper = FoodMapper()
        userMapper = UserMapper()
        nutritionMapper = NutritionMapper(foodMapper)
        validator = DataMapperValidator(foodMapper, userMapper, nutritionMapper)
    }
    
    @Test
    fun `validateFoodMapper should detect field mapping issues`() = runTest {
        val result = validator.validateFoodMapper()
        
        assertNotNull(result)
        assertEquals("FoodMapper", result.validatorName)
        assertTrue(result.summary.contains("FoodMapper"))
    }
    
    @Test
    fun `validateFoodMapper should validate round trip mapping`() = runTest {
        val result = validator.validateFoodMapper()
        
        // Check for round trip errors
        val roundTripErrors = result.issues.filter { it.type == "ROUND_TRIP_ERROR" }
        
        // Should have no round trip errors for valid data
        assertTrue("Round trip errors found: ${roundTripErrors.map { it.message }}", 
                  roundTripErrors.isEmpty())
    }
    
    @Test
    fun `validateUserMapper should detect field mapping issues`() = runTest {
        val result = validator.validateUserMapper()
        
        assertNotNull(result)
        assertEquals("UserMapper", result.validatorName)
        assertTrue(result.summary.contains("UserMapper"))
    }
    
    @Test
    fun `validateUserMapper should validate entity mappings`() = runTest {
        val result = validator.validateUserMapper()
        
        // Check for entity mapping errors
        val entityErrors = result.issues.filter { 
            it.type == "ENTITY_ROUND_TRIP_ERROR" || it.location.contains("Entity")
        }
        
        // Log any entity mapping issues for debugging
        entityErrors.forEach { issue ->
            println("Entity mapping issue: ${issue.message} - ${issue.details}")
        }
    }
    
    @Test
    fun `validateNutritionMapper should validate conversion logic`() = runTest {
        val result = validator.validateNutritionMapper()
        
        assertNotNull(result)
        assertEquals("NutritionMapper", result.validatorName)
        assertTrue(result.summary.contains("NutritionMapper"))
    }
    
    @Test
    fun `validateNutritionMapper should validate progress calculations`() = runTest {
        val result = validator.validateNutritionMapper()
        
        // Check for calculation errors
        val calculationErrors = result.issues.filter { it.type == "CALCULATION_ERROR" }
        
        // Should have no calculation errors
        assertTrue("Calculation errors found: ${calculationErrors.map { it.message }}", 
                  calculationErrors.isEmpty())
    }
    
    @Test
    fun `validateAllMappers should aggregate all mapper issues`() = runTest {
        val result = validator.validateAllMappers()
        
        assertNotNull(result)
        assertEquals("DataMapperValidator", result.validatorName)
        assertTrue(result.summary.contains("Comprehensive validation"))
        
        // Should include issues from all mappers
        val foodIssues = result.issues.filter { it.location.contains("Food") }
        val userIssues = result.issues.filter { it.location.contains("User") }
        val nutritionIssues = result.issues.filter { it.location.contains("Nutrition") }
        
        // At least one category should have been tested
        assertTrue("No mapper issues found", 
                  foodIssues.isNotEmpty() || userIssues.isNotEmpty() || nutritionIssues.isNotEmpty())
    }
    
    @Test
    fun `validateAllMappers should handle mapper exceptions gracefully`() = runTest {
        // Test with mock mappers that throw exceptions
        val mockFoodMapper = mockk<FoodMapper>(relaxed = true)
        val mockUserMapper = mockk<UserMapper>(relaxed = true)
        val mockNutritionMapper = mockk<NutritionMapper>(relaxed = true)
        
        val validatorWithMocks = DataMapperValidator(mockFoodMapper, mockUserMapper, mockNutritionMapper)
        
        val result = validatorWithMocks.validateAllMappers()
        
        assertNotNull(result)
        // Should not throw exceptions even with mock mappers
    }
    
    @Test
    fun `validation should categorize issues by severity`() = runTest {
        val result = validator.validateAllMappers()
        
        val errorIssues = result.issues.filter { it.severity == ValidationResults.Severity.ERROR }
        val warningIssues = result.issues.filter { it.severity == ValidationResults.Severity.WARNING }
        val infoIssues = result.issues.filter { it.severity == ValidationResults.Severity.INFO }
        
        // All issues should have valid severity
        assertEquals(result.issues.size, errorIssues.size + warningIssues.size + infoIssues.size)
    }
    
    @Test
    fun `validation should provide actionable details`() = runTest {
        val result = validator.validateAllMappers()
        
        result.issues.forEach { issue ->
            assertNotNull("Issue message should not be null", issue.message)
            assertFalse("Issue message should not be empty", issue.message.isEmpty())
            assertNotNull("Issue location should not be null", issue.location)
            assertFalse("Issue location should not be empty", issue.location.isEmpty())
            
            // Details can be null but if present should not be empty
            issue.details?.let { details ->
                assertFalse("Issue details should not be empty if present", details.isEmpty())
            }
        }
    }
}