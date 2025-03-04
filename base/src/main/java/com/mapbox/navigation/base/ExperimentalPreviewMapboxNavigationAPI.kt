package com.mapbox.navigation.base

/**
 * This annotation marks the experimental preview of the MapboxNavigation API.
 * This API is in a preview state and has a very high chance of being changed in the future.
 *
 * Any usage of a declaration annotated with `@ExperimentalPreviewMapboxNavigationAPI` must be
 * accepted either by annotating that usage with the [OptIn] annotation,
 * e.g. `@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)`, or by using the compiler argument
 * `-Xopt-in=kotlin.time.ExperimentalPreviewMapboxNavigationAPI`.
 */
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
)
annotation class ExperimentalPreviewMapboxNavigationAPI
