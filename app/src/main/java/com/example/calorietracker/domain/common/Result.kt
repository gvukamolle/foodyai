package com.example.calorietracker.domain.common

/**
 * A generic wrapper for handling success and error states consistently across the domain layer.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    
    /**
     * Execute action if result is successful
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * Execute action if result is an error
     */
    inline fun onFailure(action: (Throwable) -> Unit): Result<T> {
        if (this is Error) action(exception)
        return this
    }
    
    /**
     * Transform success data to another type
     */
    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }
    
    /**
     * Get data or null if error
     */
    fun getOrNull(): T? {
        return when (this) {
            is Success -> data
            is Error -> null
        }
    }
    
    /**
     * Get data or throw exception if error
     */
    fun getOrThrow(): T {
        return when (this) {
            is Success -> data
            is Error -> throw exception
        }
    }
    
    companion object {
        /**
         * Create a successful result
         */
        fun <T> success(data: T): Result<T> = Success(data)

        /**
         * Create an error result
         */
        fun <T> error(exception: Throwable): Result<T> = Error(exception)
    }
}