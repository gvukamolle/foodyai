package com.example.calorietracker.domain.usecases.base

import com.example.calorietracker.domain.common.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base class for all use cases that provides common functionality
 */
abstract class UseCase<in P, R>(
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    /**
     * Execute the use case with parameters
     */
    suspend operator fun invoke(parameters: P): Result<R> {
        return try {
            withContext(coroutineDispatcher) {
                execute(parameters)
            }
        } catch (exception: Exception) {
            Result.error(exception)
        }
    }
    
    /**
     * Execute the business logic - to be implemented by subclasses
     */
    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): Result<R>
}

/**
 * Base class for use cases that don't require parameters
 */
abstract class NoParamsUseCase<R>(
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UseCase<Unit, R>(coroutineDispatcher) {
    
    suspend operator fun invoke(): Result<R> {
        return invoke(Unit)
    }
}