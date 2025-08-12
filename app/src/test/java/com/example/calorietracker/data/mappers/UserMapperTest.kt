package com.example.calorietracker.data.mappers

import com.example.calorietracker.data.UserProfile
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.entities.common.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for UserMapper
 */
class UserMapperTest {
    
    private lateinit var mapper: UserMapper
    
    @Before
    fun setup() {
        mapper = UserMapper()
    }
    
    @Test
    fun `map domain user to data user profile correctly`() {
        // Given
        val domainUser = User(
            name = "John Doe",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            goal = NutritionGoal.MAINTAIN_WEIGHT,
            nutritionTargets = NutritionTargets(2000, 150, 67, 250),
            isSetupComplete = true
        )
        
        // When
        val dataProfile = mapper.mapDomainToData(domainUser)
        
        // Then
        assertEquals("John Doe", dataProfile.name)
        assertEquals("1990-01-01", dataProfile.birthday)
        assertEquals(180, dataProfile.height)
        assertEquals(75, dataProfile.weight)
        assertEquals("мужской", dataProfile.gender)
        assertEquals("умеренная активность", dataProfile.condition)
        assertEquals("поддержание веса", dataProfile.goal)
        assertEquals(2000, dataProfile.dailyCalories)
        assertEquals(150, dataProfile.dailyProteins)
        assertEquals(67, dataProfile.dailyFats)
        assertEquals(250, dataProfile.dailyCarbs)
        assertTrue(dataProfile.isSetupComplete)
    }
    
    @Test
    fun `map data user profile to domain user correctly`() {
        // Given
        val dataProfile = UserProfile(
            name = "Jane Smith",
            birthday = "1985-05-15",
            height = 165,
            weight = 60,
            gender = "женский",
            condition = "высокая активность",
            bodyFeeling = "Отлично",
            goal = "набор веса",
            dailyCalories = 2200,
            dailyProteins = 165,
            dailyFats = 73,
            dailyCarbs = 275,
            isSetupComplete = true
        )
        
        // When
        val domainUser = mapper.mapDataToDomain(dataProfile)
        
        // Then
        assertEquals("Jane Smith", domainUser.name)
        assertEquals("1985-05-15", domainUser.birthday)
        assertEquals(165, domainUser.height)
        assertEquals(60, domainUser.weight)
        assertEquals(Gender.FEMALE, domainUser.gender)
        assertEquals(ActivityLevel.VERY_ACTIVE, domainUser.activityLevel)
        assertEquals(NutritionGoal.GAIN_WEIGHT, domainUser.goal)
        assertEquals(2200, domainUser.nutritionTargets.dailyCalories)
        assertEquals(165, domainUser.nutritionTargets.dailyProteins)
        assertEquals(73, domainUser.nutritionTargets.dailyFats)
        assertEquals(275, domainUser.nutritionTargets.dailyCarbs)
        assertTrue(domainUser.isSetupComplete)
    }
    
    @Test
    fun `map gender correctly from domain to data`() {
        // Test all gender mappings
        val genderMappings = mapOf(
            Gender.MALE to "мужской",
            Gender.FEMALE to "женский"
        )
        
        genderMappings.forEach { (domainGender, expectedDataGender) ->
            val domainUser = createTestDomainUser().copy(gender = domainGender)
            val dataProfile = mapper.mapDomainToData(domainUser)
            assertEquals(expectedDataGender, dataProfile.gender)
        }
    }
    
    @Test
    fun `map gender correctly from data to domain`() {
        // Test all gender mappings
        val genderMappings = mapOf(
            "мужской" to Gender.MALE,
            "женский" to Gender.FEMALE,
            "male" to Gender.MALE,
            "female" to Gender.FEMALE,
            "м" to Gender.MALE,
            "ж" to Gender.FEMALE
        )
        
        genderMappings.forEach { (dataGender, expectedDomainGender) ->
            val dataProfile = createTestDataProfile().copy(gender = dataGender)
            val domainUser = mapper.mapDataToDomain(dataProfile)
            assertEquals(expectedDomainGender, domainUser.gender)
        }
    }
    
    @Test
    fun `map activity level correctly from domain to data`() {
        // Test all activity level mappings
        val activityMappings = mapOf(
            ActivityLevel.SEDENTARY to "малоподвижный образ жизни",
            ActivityLevel.LIGHTLY_ACTIVE to "легкая активность",
            ActivityLevel.MODERATELY_ACTIVE to "умеренная активность",
            ActivityLevel.VERY_ACTIVE to "высокая активность",
            ActivityLevel.EXTREMELY_ACTIVE to "очень высокая активность"
        )
        
        activityMappings.forEach { (domainActivity, expectedDataActivity) ->
            val domainUser = createTestDomainUser().copy(activityLevel = domainActivity)
            val dataProfile = mapper.mapDomainToData(domainUser)
            assertEquals(expectedDataActivity, dataProfile.condition)
        }
    }
    
    @Test
    fun `map activity level correctly from data to domain`() {
        // Test all activity level mappings
        val activityMappings = mapOf(
            "малоподвижный образ жизни" to ActivityLevel.SEDENTARY,
            "легкая активность" to ActivityLevel.LIGHTLY_ACTIVE,
            "умеренная активность" to ActivityLevel.MODERATELY_ACTIVE,
            "высокая активность" to ActivityLevel.VERY_ACTIVE,
            "очень высокая активность" to ActivityLevel.EXTREMELY_ACTIVE,
            "sedentary" to ActivityLevel.SEDENTARY,
            "light" to ActivityLevel.LIGHTLY_ACTIVE,
            "moderate" to ActivityLevel.MODERATELY_ACTIVE,
            "active" to ActivityLevel.VERY_ACTIVE,
            "very_active" to ActivityLevel.EXTREMELY_ACTIVE
        )
        
        activityMappings.forEach { (dataActivity, expectedDomainActivity) ->
            val dataProfile = createTestDataProfile().copy(condition = dataActivity)
            val domainUser = mapper.mapDataToDomain(dataProfile)
            assertEquals(expectedDomainActivity, domainUser.activityLevel)
        }
    }
    
    @Test
    fun `map nutrition goal correctly from domain to data`() {
        // Test all goal mappings
        val goalMappings = mapOf(
            NutritionGoal.LOSE_WEIGHT to "похудение",
            NutritionGoal.MAINTAIN_WEIGHT to "поддержание веса",
            NutritionGoal.GAIN_WEIGHT to "набор веса"
        )
        
        goalMappings.forEach { (domainGoal, expectedDataGoal) ->
            val domainUser = createTestDomainUser().copy(goal = domainGoal)
            val dataProfile = mapper.mapDomainToData(domainUser)
            assertEquals(expectedDataGoal, dataProfile.goal)
        }
    }
    
    @Test
    fun `map nutrition goal correctly from data to domain`() {
        // Test all goal mappings
        val goalMappings = mapOf(
            "похудение" to NutritionGoal.LOSE_WEIGHT,
            "поддержание веса" to NutritionGoal.MAINTAIN_WEIGHT,
            "набор веса" to NutritionGoal.GAIN_WEIGHT,
            "lose_weight" to NutritionGoal.LOSE_WEIGHT,
            "maintain_weight" to NutritionGoal.MAINTAIN_WEIGHT,
            "gain_weight" to NutritionGoal.GAIN_WEIGHT,
            "weight_loss" to NutritionGoal.LOSE_WEIGHT,
            "maintenance" to NutritionGoal.MAINTAIN_WEIGHT,
            "weight_gain" to NutritionGoal.GAIN_WEIGHT
        )
        
        goalMappings.forEach { (dataGoal, expectedDomainGoal) ->
            val dataProfile = createTestDataProfile().copy(goal = dataGoal)
            val domainUser = mapper.mapDataToDomain(dataProfile)
            assertEquals(expectedDomainGoal, domainUser.goal)
        }
    }
    
    @Test
    fun `map user with null optional fields correctly`() {
        // Given
        val domainUser = User(
            name = "Test User",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = null,
            activityLevel = null,
            goal = null,
            nutritionTargets = NutritionTargets(0, 0, 0, 0),
            isSetupComplete = false
        )
        
        // When
        val dataProfile = mapper.mapDomainToData(domainUser)
        
        // Then
        assertEquals("Test User", dataProfile.name)
        assertEquals("", dataProfile.gender) // Should map null to empty string
        assertEquals("", dataProfile.condition)
        assertEquals("", dataProfile.goal)
        assertFalse(dataProfile.isSetupComplete)
    }
    
    @Test
    fun `map user profile with empty strings correctly`() {
        // Given
        val dataProfile = UserProfile(
            name = "Test User",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = "",
            condition = "",
            bodyFeeling = "",
            goal = "",
            dailyCalories = 0,
            dailyProteins = 0,
            dailyFats = 0,
            dailyCarbs = 0,
            isSetupComplete = false
        )
        
        // When
        val domainUser = mapper.mapDataToDomain(dataProfile)
        
        // Then
        assertEquals("Test User", domainUser.name)
        assertNull(domainUser.gender) // Should map empty string to null
        assertNull(domainUser.activityLevel)
        assertNull(domainUser.goal)
        assertFalse(domainUser.isSetupComplete)
    }
    
    @Test
    fun `map user with edge case values correctly`() {
        // Given
        val domainUser = User(
            name = "Edge Case User",
            birthday = "1900-01-01",
            height = 50, // Minimum height
            weight = 20, // Minimum weight
            gender = Gender.FEMALE,
            activityLevel = ActivityLevel.SEDENTARY,
            goal = NutritionGoal.LOSE_WEIGHT,
            nutritionTargets = NutritionTargets(1200, 100, 40, 150),
            isSetupComplete = true
        )
        
        // When
        val dataProfile = mapper.mapDomainToData(domainUser)
        val mappedBack = mapper.mapDataToDomain(dataProfile)
        
        // Then - should be able to round-trip correctly
        assertEquals(domainUser.name, mappedBack.name)
        assertEquals(domainUser.birthday, mappedBack.birthday)
        assertEquals(domainUser.height, mappedBack.height)
        assertEquals(domainUser.weight, mappedBack.weight)
        assertEquals(domainUser.gender, mappedBack.gender)
        assertEquals(domainUser.activityLevel, mappedBack.activityLevel)
        assertEquals(domainUser.goal, mappedBack.goal)
        assertEquals(domainUser.isSetupComplete, mappedBack.isSetupComplete)
    }
    
    private fun createTestDomainUser(): User {
        return User(
            name = "Test User",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATELY_ACTIVE,
            goal = NutritionGoal.MAINTAIN_WEIGHT,
            nutritionTargets = NutritionTargets(2000, 150, 67, 250),
            isSetupComplete = true
        )
    }
    
    private fun createTestDataProfile(): UserProfile {
        return UserProfile(
            name = "Test User",
            birthday = "1990-01-01",
            height = 180,
            weight = 75,
            gender = "мужской",
            condition = "умеренная активность",
            bodyFeeling = "Хорошо",
            goal = "поддержание веса",
            dailyCalories = 2000,
            dailyProteins = 150,
            dailyFats = 67,
            dailyCarbs = 250,
            isSetupComplete = true
        )
    }
}