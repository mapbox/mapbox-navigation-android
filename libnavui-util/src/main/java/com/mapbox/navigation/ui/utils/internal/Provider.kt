package com.mapbox.navigation.ui.utils.internal

/**
 * An object capable of providing instances of type T.
 */
fun interface Provider<T> {
    /**
     * Provides an instance of T.
     */
    fun get(): T
}

operator fun <T> Provider<T>.invoke() = get()
