package com.mapbox.services.android.navigation.v5.internal.navigation

interface TelemetrySerializationInterface<T> {
    fun addEvent(routeEvent: T)
    fun updateLastEvent(predicate: (T) -> T)
    fun applyToEach(predicate: (T) -> Boolean)
    fun removeEventIf(predicate: (T) -> Boolean)
    suspend fun findIf(predicate: (T) -> Boolean): T?
}
