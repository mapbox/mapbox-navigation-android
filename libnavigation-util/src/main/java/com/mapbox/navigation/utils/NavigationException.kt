package com.mapbox.navigation.utils

/**
 * Generic Exception for all things Mapbox Navigation.
 *
 * A form of `Throwable` that indicates conditions that a reasonable application might
 * want to catch.
 *
 * @param message the detail message (which is saved for later retrieval by the
 * [Throwable.message] method).
 */
class NavigationException(message: String) : RuntimeException(message)
