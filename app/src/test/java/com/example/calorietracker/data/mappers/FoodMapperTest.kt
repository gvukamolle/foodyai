package com.example.calorietracker.data.mappers

import com.example.calorietracker.data.FoodItem
import com.example.calorietracker.data.Meal
import com.example.calorietracker.data.MealType as DataMealType
import com.example.calorietracker.domain.entities.Food
import com.example.calorietracker.domain.entities.common.FoodSource
import com.example.calorietracker.domain.entities.common.MealType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FoodMapperTest {
    
    private lateinit var mapper: FoodMapper
    
    @Before
    fun setup() {
        mapper = FoodMapper()
    }
    
    @Test
    fun `map domain food to data food item correctly`() {
        // Given
        val domainFood = Food(
            name = "Apple",
            calories = 80,
            protein = 0.3,
            fat = 0.2,
            carbs = 21.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT,
            aiOpinion = "Healthy choice"
        )
        
        // When
        val dataFood = mapper.mapDomainToData(domainFood)
        
        // Then
        assertEquals("Apple", dataFood.name)
        assertEquals(80, dataFood.calories)
        assertEquals(0.3, dataFood.protein, 0.01)
        assertEquals(0.2, dataFood.fat, 0.01)
        assertEquals(21.0, dataFood.carbs, 0.01)
        assertEquals("100г", dataFood.weight)
        assertEquals("manual_input", dataFood.source)
        assertEquals("Healthy choice", dataFood.aiOpinion)
    }
    
    @Test
    fun `map data food item to domain food correctly`() {
        // Given
        val dataFood = FoodItem(
            name = "Banana",
            calories = 90,
            protein = 1.1,
            fat = 0.3,
            carbs = 23.0,
            weight = "100г",
            source = "photo",
            aiOpinion = "Good source of potassium"
        )
        
        // When
        val domainFood = mapper.mapDataToDomain(dataFood)
        
        // Then
        assertEquals("Banana", domainFood.name)
        assertEquals(90, domainFood.calories)
        assertEquals(1.1, domainFood.protein, 0.01)
        assertEquals(0.3, domainFood.fat, 0.01)
        assertEquals(23.0, domainFood.carbs, 0.01)
        assertEquals("100г", domainFood.weight)
        assertEquals(FoodSource.AI_PHOTO_ANALYSIS, domainFood.source)
        assertEquals("Good source of potassium", domainFood.aiOpinion)
    }
    
    @Test
    fun `map domain meal to data meal correctly`() {
        // Given
        val domainFood = Food(
            name = "Oatmeal",
            calories = 150,
            protein = 5.0,
            fat = 3.0,
            carbs = 27.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
        val domainMeal = com.example.calorietracker.domain.entities.Meal(
            type = MealType.BREAKFAST,
            foods = listOf(domainFood)
        )
        
        // When
        val dataMeal = mapper.mapDomainMealToData(domainMeal)
        
        // Then
        assertEquals(DataMealType.BREAKFAST, dataMeal.type)
        assertEquals(1, dataMeal.foods.size)
        assertEquals("Oatmeal", dataMeal.foods[0].name)
        assertEquals(150, dataMeal.foods[0].calories)
        assertTrue(dataMeal.time > 0) // Should have a timestamp
    }
    
    @Test
    fun `map data meal to domain meal correctly`() {
        // Given
        val dataFood = FoodItem(
            name = "Salad",
            calories = 50,
            protein = 2.0,
            fat = 0.5,
            carbs = 10.0,
            weight = "100г",
            source = "manual"
        )
        val dataMeal = Meal(
            type = DataMealType.LUNCH,
            foods = listOf(dataFood),
            time = System.currentTimeMillis()
        )
        
        // When
        val domainMeal = mapper.mapDataMealToDomain(dataMeal)
        
        // Then
        assertEquals(MealType.LUNCH, domainMeal.type)
        assertEquals(1, domainMeal.foods.size)
        assertEquals("Salad", domainMeal.foods[0].name)
        assertEquals(50, domainMeal.foods[0].calories)
        assertNotNull(domainMeal.timestamp)
    }
    
    @Test
    fun `map meal type domain to data correctly`() {
        // Test all meal type mappings
        val domainMeal1 = com.example.calorietracker.domain.entities.Meal(
            type = MealType.BREAKFAST,
            foods = listOf(createTestDomainFood())
        )
        val domainMeal2 = com.example.calorietracker.domain.entities.Meal(
            type = MealType.LUNCH,
            foods = listOf(createTestDomainFood())
        )
        val domainMeal3 = com.example.calorietracker.domain.entities.Meal(
            type = MealType.DINNER,
            foods = listOf(createTestDomainFood())
        )
        val domainMeal4 = com.example.calorietracker.domain.entities.Meal(
            type = MealType.SNACK,
            foods = listOf(createTestDomainFood())
        )
        
        assertEquals(DataMealType.BREAKFAST, mapper.mapDomainMealToData(domainMeal1).type)
        assertEquals(DataMealType.LUNCH, mapper.mapDomainMealToData(domainMeal2).type)
        assertEquals(DataMealType.DINNER, mapper.mapDomainMealToData(domainMeal3).type)
        assertEquals(DataMealType.SUPPER, mapper.mapDomainMealToData(domainMeal4).type) // SNACK -> SUPPER
    }
    
    @Test
    fun `map food source correctly`() {
        // Test all food source mappings
        val sources = mapOf(
            FoodSource.MANUAL_INPUT to "manual_input",
            FoodSource.AI_PHOTO_ANALYSIS to "ai_photo_analysis",
            FoodSource.AI_TEXT_ANALYSIS to "ai_text_analysis",
            FoodSource.BARCODE_SCAN to "barcode_scan",
            FoodSource.RECIPE_ANALYSIS to "recipe_analysis"
        )
        
        sources.forEach { (domainSource, expectedDataSource) ->
            val domainFood = Food(
                name = "Test",
                calories = 100,
                protein = 5.0,
                fat = 3.0,
                carbs = 15.0,
                weight = "100г",
                source = domainSource
            )
            
            val dataFood = mapper.mapDomainToData(domainFood)
            assertEquals(expectedDataSource, dataFood.source)
        }
    }
    
    @Test
    fun `map list of domain foods to data foods correctly`() {
        // Given
        val domainFoods = listOf(
            Food("Apple", 80, 0.3, 0.2, 21.0, "100г", FoodSource.MANUAL_INPUT),
            Food("Banana", 90, 1.1, 0.3, 23.0, "100г", FoodSource.AI_PHOTO_ANALYSIS)
        )
        
        // When
        val dataFoods = mapper.mapDomainListToData(domainFoods)
        
        // Then
        assertEquals(2, dataFoods.size)
        assertEquals("Apple", dataFoods[0].name)
        assertEquals("Banana", dataFoods[1].name)
        assertEquals("manual_input", dataFoods[0].source)
        assertEquals("ai_photo_analysis", dataFoods[1].source)
    }
    
    private fun createTestDomainFood(): Food {
        return Food(
            name = "Test Food",
            calories = 100,
            protein = 5.0,
            fat = 3.0,
            carbs = 15.0,
            weight = "100г",
            source = FoodSource.MANUAL_INPUT
        )
    }
}