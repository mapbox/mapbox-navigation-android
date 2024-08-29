package com.mapbox.navigation.base

/**
 * This annotation marks the experimental state of the MapboxNavigation API.
 * This API is stable in nature, but it's likely that properties might be added or removed in the
 * future.
 * Any usage of a declaration annotated with `@ExperimentalMapboxNavigationAPI` must be accepted
 * either by annotating that usage with the [OptIn] annotation,
 * e.g. `@OptIn(ExperimentalMapboxNavigationAPI::class)`, or by using the compiler argument
 * `-Xopt-in=kotlin.time.ExperimentalMapboxNavigationAPI`.
 */
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
)
annotation class ExperimentalMapboxNavigationAPI
