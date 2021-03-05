package com.mapbox.navigation.core.trip.session

/**
 * This annotation marks the experimental preview of the EHorizon API.
 *
 * > Note that this API is in a preview state and has a very high chance of being changed in the
 * future.
 *
 * Any usage of a declaration annotated with `@ExperimentalEHorizonAPI` must be accepted either by
 * annotating that usage with the [OptIn] annotation, e.g. `@OptIn(ExperimentalEHorizonAPI::class)`,
 * or by using the compiler argument `-Xopt-in=kotlin.time.ExperimentalEHorizonAPI`.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class ExperimentalEHorizonAPI


