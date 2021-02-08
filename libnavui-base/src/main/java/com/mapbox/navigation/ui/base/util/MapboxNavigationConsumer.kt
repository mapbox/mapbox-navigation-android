package com.mapbox.navigation.ui.base.util

/**
 * Mapbox navigation version of {@link java.util.function.Consumer}
 * @param <T> the type of the input to the operation
 */
@FunctionalInterface
interface MapboxNavigationConsumer<T> {
    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    fun accept(t: T) {
    }
}
