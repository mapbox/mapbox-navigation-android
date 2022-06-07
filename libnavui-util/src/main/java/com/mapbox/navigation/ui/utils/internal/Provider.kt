package com.mapbox.navigation.ui.utils.internal

fun interface Provider<T> {
    fun get(): T
}

operator fun <T> Provider<T>.invoke() = get()
