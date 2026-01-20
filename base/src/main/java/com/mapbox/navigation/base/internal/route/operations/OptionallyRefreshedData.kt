package com.mapbox.navigation.base.internal.route.operations

internal sealed class OptionallyRefreshedData<T> {
    abstract fun update(currentValue: T): T
    class NoUpdates<T> : OptionallyRefreshedData<T>() {
        override fun update(currentValue: T) = currentValue
    }
    data class Updated<T>(val newValue: T) : OptionallyRefreshedData<T>() {
        override fun update(currentValue: T) = newValue
    }
}
