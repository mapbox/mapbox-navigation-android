package com.mapbox.navigation.mapgpt.core.performance

/**
 * Factory to use to create a [DashTrace] instance. This gives the application the ability to
 * implement a specific ingestion mechanism for the performance trace data. For example,
 * Firebase can be used to track the performance of the [DashTrace].
 */
fun interface DashTraceFactory {

    /**
     * Create a [DashTrace] instance with the given name.
     *
     * @param name unique name for the trace.
     */
    fun create(name: TraceName): DashTrace
}
