package com.mapbox.navigation.ui.base.model

/**
 * A wrapper class for success or failure results.
 */
sealed class Expected<out V, out E> {
    /**
     * Represents a successful value.
     *
     * @param value a success value
     */
    data class Success<out V>(val value: V) : Expected<V, Nothing>()

    /**
     * Represents a failure value.
     *
     * @param error an error value
     */
    data class Failure<out E>(val error: E) : Expected<Nothing, E>()
}
