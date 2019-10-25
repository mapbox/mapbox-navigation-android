package com.mapbox.navigation.exception

/**
 * Generic Exception for all things Mapbox Navigation.
 *
 * A form of `Throwable` that indicates conditions that a reasonable application might
 * want to catch.
 *
 * @param message the detail message (which is saved for later retrieval by the
 * [Throwable.message] method).
 *
 * @since 0.2.0
 */
class NavigationException(message: String) : RuntimeException(message)
