package com.mapbox.navigation.ui.utils.internal

import kotlin.reflect.KProperty

/**
 * An object capable of providing instances of type T.
 */
fun interface Provider<T> {
    /**
     * Provides an instance of T.
     */
    fun get(): T
}

/**
 * Shorthand for calling [Provider.get].
 */
operator fun <T> Provider<T>.invoke() = get()

/**
 * Property delegate that exposes [Provider.get] as property getter.
 */
operator fun <T> Provider<T>.getValue(thisRef: Any?, property: KProperty<*>): T = get()
