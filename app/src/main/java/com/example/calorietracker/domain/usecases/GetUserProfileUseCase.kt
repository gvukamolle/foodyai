package com.example.calorietracker.domain.usecases

import com.example.calorietracker.domain.common.Result
import com.example.calorietracker.domain.entities.User
import com.example.calorietracker.domain.repositories.UserRepository
import com.example.calorietracker.domain.usecases.base.NoParamsUseCase
import javax.inject.Inject

/**
 * Use case for retrieving current user profile
 */
class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository
) : NoParamsUseCase<User>() {
    
    override suspend fun execute(parameters: Unit): Result<User> {
        return userRepository.getUserProfile()
    }
}