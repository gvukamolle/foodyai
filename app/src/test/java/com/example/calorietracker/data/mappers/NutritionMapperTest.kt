package com.example.calorietracker.data.mappers

import com.example.calorietracker.data.DailyIntake
import com.example.calorietracker.data.FoodItem
import com.example.calorietracker.data.Meal
import com.example.calorietracker.data.MealType as DataMealType
import com.example.calorietracker.domain.entities.NutritionIntake
import com.example.calorietracker.domain.entities.common.MealType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for NutritionMapper
 */
class NutritionMapperTest {
    
    private lateinit var mapper: NutritionMapper
    
    @Before
    fun setup() {
        mapper = NutritionMapper()
    }
    
    @Test
    fun `map data daily intake to domain nutrition intake correctly`() {
        // Given
        val breakfastFood = FoodItem(
            name = "Oatmeal",
            calories = 150,
            protein = 5.0,
            fat = 3.0,
            carbs = 27.0,
            weight = "100г",
            source = "manual"
        )
        val lunchFood = FoodItem(
            name = "Chicken Salad",
            calories = 300,
            protein = 25.0,
            fat = 15.0,
            carbs = 10.0,
            weight = "200г",
            source = "photo"
        )
        
        val dataDailyIntake = DailyIntake(
            date = "2024-01-15",
            meals = listOf(
                Meal(DataMealType.BREAKFAST, listOf(breakfastFood), System.currentTimeMillis()),
                Meal(DataMealType.LUNCH, listOf(lunchFood), System.currentTimeMillis())
            ),
            totalCalories = 450,
            totalProteins = 30.0,
            totalFats = 18.0,
            totalCarbs = 37.0
        )
        
        // When
        val domainIntake = mapper.mapDataToDomain(dataDailyIntake)
        
        // Then
        assertEquals(LocalDate.of(2024, 1, 15), domainIntake.date)
        assertEquals(2, domainIntake.meals.size)
        assertEquals(450, domainIntake.totalCalories)
        assertEquals(30.0, domainIntake.totalProteins, 0.01)
        assertEquals(18.0, domainIntake.totalFats, 0.01)
        assertEquals(37.0, domainIntake.totalCarbs, 0.01)
        
        // Check meals
        assertEquals(MealType.BREAKFAST, domainIntake.meals[0].type)
        assertEquals(MealType.LUNCH, domainIntake.meals[1].type)
        assertEquals("Oatmeal", domainIntake.meals[0].foods[0].name)
        assertEquals("Chicken Salad", domainIntake.meals[1].foods[0].name)
    }
    
    @Test
    fun `map domain nutrition intake to data daily intake correctly`() {
        // Given
        val domainFood1 = com.example.calorietracker.domain.entities.Food(
            name = "Apple",
            calories = 80,
            protein = 0.3,
            fat = 0.2,
            carbs = 21.0,
            weight = "100г",
            source = com.example.calorietracker.domain.entities.common.FoodSource.MANUAL_INPUT
        )
        val domainFood2 = com.example.calorietracker.domain.entities.Food(
            name = "Banana",
            calories = 90,
            protein = 1.1,
            fat = 0.3,
            carbs = 23.0,
            weight = "100г",
            source = com.example.calorietracker.domain.entities.common.FoodSource.AI_PHOTO_ANALYSIS
        )
        
        val domainIntake = NutritionIntake(
            date = LocalDate.of(2024, 1, 15),
            meals = listOf(
                com.example.calorietracker.domain.entities.Meal(MealType.BREAKFAST, listOf(domainFood1)),
                com.example.calorietracker.domain.entities.Meal(MealType.SNACK, listOf(domainFood2))
            ),
            totalCalories = 170,
            totalProteins = 1.4,
            totalFats = 0.5,
            totalCarbs = 44.0
        )
        
        // When
        val dataDailyIntake = mapper.mapDomainToData(domainIntake)
        
        // Then
        assertEquals("2024-01-15", dataDailyIntake.date)
        assertEquals(2, dataDailyIntake.meals.size)
        assertEquals(170, dataDailyIntake.totalCalories)
        assertEquals(1.4, dataDailyIntake.totalProteins, 0.01)
        assertEquals(0.5, dataDailyIntake.totalFats, 0.01)
        assertEquals(44.0, dataDailyIntake.totalCarbs, 0.01)
        
        // Check meals
        assertEquals(DataMealType.BREAKFAST, dataDailyIntake.meals[0].type)
        assertEquals(DataMealType.SUPPER, dataDailyIntake.meals[1].type) // SNACK -> SUPPER
        assertEquals("Apple", dataDailyIntake.meals[0].foods[0].name)
        assertEquals("Banana", dataDailyIntake.meals[1].foods[0].name)
    }
    
    @Test
    fun `map empty daily intake correctly`() {
        // Given
        val emptyDataIntake = DailyIntake(
            date = "2024-01-15",
            meals = emptyList(),
            totalCalories = 0,
            totalProteins = 0.0,
            totalFats = 0.0,
            totalCarbs = 0.0
        )
        
        // When
        val domainIntake = mapper.mapDataToDomain(emptyDataIntake)
        
        // Then
        assertEquals(LocalDate.of(2024, 1, 15), domainIntake.date)
        assertTrue(domainIntake.meals.isEmpty())
        assertEquals(0, domainIntake.totalCalories)
        assertEquals(0.0, domainIntake.totalProteins, 0.01)
        assertEquals(0.0, domainIntake.totalFats, 0.01)
        assertEquals(0.0, domainIntake.totalCarbs, 0.01)
    }
    
    @Test
    fun `map daily intake with multiple foods per meal correctly`() {
        // Given
        val food1 = FoodItem("Food1", 100, 5.0, 3.0, 15.0, "100г", "manual")
        val food2 = FoodItem("Food2", 200, 10.0, 6.0, 30.0, "100г", "photo")
        val food3 = FoodItem("Food3", 150, 7.5, 4.5, 22.5, "100г", "manual")
        
        val dataDailyIntake = DailyIntake(
            date = "2024-01-15",
            meals = listOf(
                Meal(DataMealType.BREAKFAST, listOf(food1, food2), System.currentTimeMillis()),
                Meal(DataMealType.LUNCH, listOf(food3), System.currentTimeMillis())
            ),
            totalCalories = 450,
            totalProteins = 22.5,
            totalFats = 13.5,
            totalCarbs = 67.5
        )
        
        // When
        val domainIntake = mapper.mapDataToDomain(dataDailyIntake)
        
        // Then
        assertEquals(2, domainIntake.meals.size)
        assertEquals(2, domainIntake.meals[0].foods.size) // Breakfast has 2 foods
        assertEquals(1, domainIntake.meals[1].foods.size) // Lunch has 1 food
        
        assertEquals("Food1", domainIntake.meals[0].foods[0].name)
        assertEquals("Food2", domainIntake.meals[0].foods[1].name)
        assertEquals("Food3", domainIntake.meals[1].foods[0].name)
    }
    
    @Test
    fun `map daily intake with all meal types correctly`() {
        // Given
        val food = FoodItem("Test Food", 100, 5.0, 3.0, 15.0, "100г", "manual")
        
        val dataDailyIntake = DailyIntake(
            date = "2024-01-15",
            meals = listOf(
                Meal(DataMealType.BREAKFAST, listOf(food), System.currentTimeMillis()),
                Meal(DataMealType.LUNCH, listOf(food), System.currentTimeMillis()),
                Meal(DataMealType.DINNER, listOf(food), System.currentTimeMillis()),
                Meal(DataMealType.SUPPER, listOf(food), System.currentTimeMillis())
            ),
            totalCalories = 400,
            totalProteins = 20.0,
            totalFats = 12.0,
            totalCarbs = 60.0
        )
        
        // When
        val domainIntake = mapper.mapDataToDomain(dataDailyIntake)
        
        // Then
        assertEquals(4, domainIntake.meals.size)
        assertEquals(MealType.BREAKFAST, domainIntake.meals[0].type)
        assertEquals(MealType.LUNCH, domainIntake.meals[1].type)
        assertEquals(MealType.DINNER, domainIntake.meals[2].type)
        assertEquals(MealType.SNACK, domainIntake.meals[3].type) // SUPPER -> SNACK
    }
    
    @Test
    fun `map date string to LocalDate correctly`() {
        // Test various date formats
        val dateFormats = mapOf(
            "2024-01-15" to LocalDate.of(2024, 1, 15),
            "2024-12-31" to LocalDate.of(2024, 12, 31),
            "2023-02-28" to LocalDate.of(2023, 2, 28)
        )
        
        dateFormats.forEach { (dateString, expectedDate) ->
            val dataDailyIntake = DailyIntake(
                date = dateString,
                meals = emptyList(),
                totalCalories = 0,
                totalProteins = 0.0,
                totalFats = 0.0,
                totalCarbs = 0.0
            )
            
            val domainIntake = mapper.mapDataToDomain(dataDailyIntake)
            assertEquals(expectedDate, domainIntake.date)
        }
    }
    
    @Test
    fun `map LocalDate to date string correctly`() {
        // Given
        val domainIntake = NutritionIntake(
            date = LocalDate.of(2024, 1, 15),
            meals = emptyList(),
            totalCalories = 0,
            totalProteins = 0.0,
            totalFats = 0.0,
            totalCarbs = 0.0
        )
        
        // When
        val dataDailyIntake = mapper.mapDomainToData(domainIntake)
        
        // Then
        assertEquals("2024-01-15", dataDailyIntake.date)
    }
    
    @Test
    fun `map nutrition intake with high precision values correctly`() {
        // Given
        val domainIntake = NutritionIntake(
            date = LocalDate.of(2024, 1, 15),
            meals = emptyList(),
            totalCalories = 1234,
            totalProteins = 123.456,
            totalFats = 67.890,
            totalCarbs = 234.567
        )
        
        // When
        val dataDailyIntake = mapper.mapDomainToData(domainIntake)
        val mappedBack = mapper.mapDataToDomain(dataDailyIntake)
        
        // Then - should preserve precision
        assertEquals(1234, mappedBack.totalCalories)
        assertEquals(123.456, mappedBack.totalProteins, 0.001)
        assertEquals(67.890, mappedBack.totalFats, 0.001)
        assertEquals(234.567, mappedBack.totalCarbs, 0.001)
    }
    
    @Test
    fun `round trip mapping should preserve data integrity`() {
        // Given
        val originalData = DailyIntake(
            date = "2024-01-15",
            meals = listOf(
                Meal(
                    DataMealType.BREAKFAST,
                    listOf(FoodItem("Apple", 80, 0.3, 0.2, 21.0, "100г", "manual")),
                    System.currentTimeMillis()
                )
            ),
            totalCalories = 80,
            totalProteins = 0.3,
            totalFats = 0.2,
            totalCarbs = 21.0
        )
        
        // When - round trip mapping
        val domainIntake = mapper.mapDataToDomain(originalData)
        val mappedBack = mapper.mapDomainToData(domainIntake)
        
        // Then - should preserve all data
        assertEquals(originalData.date, mappedBack.date)
        assertEquals(originalData.totalCalories, mappedBack.totalCalories)
        assertEquals(originalData.totalProteins, mappedBack.totalProteins, 0.01)
        assertEquals(originalData.totalFats, mappedBack.totalFats, 0.01)
        assertEquals(originalData.totalCarbs, mappedBack.totalCarbs, 0.01)
        assertEquals(originalData.meals.size, mappedBack.meals.size)
        assertEquals(originalData.meals[0].type, mappedBack.meals[0].type)
        assertEquals(originalData.meals[0].foods[0].name, mappedBack.meals[0].foods[0].name)
    }
}