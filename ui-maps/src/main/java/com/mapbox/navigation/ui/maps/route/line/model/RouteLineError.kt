package com.mapbox.navigation.ui.maps.route.line.model

/**
 * Represents an error value for route line related updates.
 *
 * @param errorMessage an error message
 * @param throwable an optional throwable value expressing the error
 */
class RouteLineError internal constructor(
    val errorMessage: String,
    val throwable: Throwable?,
) {

    /**
     * @return a class with mutable values for replacing.
     */
    fun toMutableValue() = MutableRouteLineError(
        errorMessage,
        throwable,
    )

    /**
     * Represents a mutable error value for route line related updates.
     *
     * @param errorMessage an error message
     * @param throwable an optional throwable value expressing the error
     */
    class MutableRouteLineError internal constructor(
        var errorMessage: String,
        var throwable: Throwable?,
    ) {

        /**
         * @return a RouteLineError
         */
        fun toImmutableValue() = RouteLineError(
            errorMessage,
            throwable,
        )
    }
}
