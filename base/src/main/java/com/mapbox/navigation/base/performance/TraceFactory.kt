package com.mapbox.navigation.base.performance

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Factory to use to create a [Trace] instance. This gives the application the ability to
 * implement a specific ingestion mechanism for the performance trace data. For example,
 * Firebase can be used to track the performance of the [Trace].
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface TraceFactory {

    /**
     * Create a [Trace] instance with the given name.
     *
     * @param name unique name for the trace.
     */
    fun create(name: TraceName): Trace
}
