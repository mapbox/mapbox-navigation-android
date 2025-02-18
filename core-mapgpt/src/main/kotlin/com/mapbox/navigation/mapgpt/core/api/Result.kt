package com.mapbox.navigation.mapgpt.core.api

/**
 * A discriminated union that encapsulates a successful outcome with a value of type [T]
 * or a failure with an arbitrary [Throwable] exception.
 */
sealed class Result<out T : Any> {

    /**
     * Returns `true` if this instance represents a successful outcome.
     * In this case [isFailure] returns `false`.
     */
    abstract val isSuccess: Boolean

    /**
     * Returns `true` if this instance represents a failed outcome.
     * In this case [isSuccess] returns `false`.
     */
    abstract val isFailure: Boolean

    /**
     * Returns the encapsulated value if this instance represents [success][Result.isSuccess]
     * or `null` if it is [failure][Result.isFailure].
     */
    abstract fun getOrNull(): T?

    /**
     * Returns the encapsulated [Throwable] exception if this instance represents [failure][isFailure] or `null`
     * if it is [success][isSuccess].
     */
    abstract fun exceptionOrNull(): Throwable?

    /**
     * Returns the encapsulated value if this instance represents [success][Result.isSuccess]
     * or throws the encapsulated [Throwable] exception if it is [failure][Result.isFailure].
     */
    abstract fun getOrThrow(): T

    /**
     * Returns a string `Success(v)` if this instance represents [success][Result.isSuccess]
     * where `v` is a string representation of the value or a string `Failure(x)` if
     * it is [failure][isFailure] where `x` is a string representation of the exception.
     */
    abstract override fun toString(): String

    /**
     * Encapsulates the given [value] as successful value.
     *
     * @param value the encapsulated value
     */
    class Success<out T : Any> internal constructor(val value: T) : Result<T>() {

        override val isSuccess get() = true
        override val isFailure get() = false
        override fun getOrNull() = value
        override fun exceptionOrNull(): Nothing? = null
        override fun getOrThrow() = value
        override fun toString() = "Success($value)"
    }

    /**
     * Encapsulates the given [Throwable] [exception] as failure.
     *
     * @param exception the encapsulated [Throwable]
     */
    class Failure internal constructor(val exception: Throwable) : Result<Nothing>() {

        override val isSuccess get() = false
        override val isFailure get() = true
        override fun getOrNull() = null
        override fun exceptionOrNull() = exception
        override fun getOrThrow() = throw exception
        override fun toString() = "Failure($exception)"
    }

    companion object {

        internal inline fun <R : Any> fromCatching(block: () -> R): Result<R> {
            return try {
                Success(block())
            } catch (e: Throwable) {
                Failure(e)
            }
        }
    }
}
