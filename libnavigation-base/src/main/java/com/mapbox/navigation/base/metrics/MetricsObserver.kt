package com.mapbox.navigation.base.metrics

/**
 * Interface for observe metric events handled.
 *
 * @since 1.0.0
 */
interface MetricsObserver {

    /**
     * Called when metric event handled by [MetricsReporter.addEvent]
     *
     * @param metric metric event name
     * @param jsonStringData metric data in JSON string representation
     * @since 1.0.0
     */
    fun onMetricUpdated(@MetricEvent.Metric metric: String, jsonStringData: String)
}
