package com.mapbox.navigation.base.metrics

/**
 * Interface for observe metric events handled.
 */
interface MetricsObserver {

    /**
     * Called when metric event handled by [MetricsReporter.addEvent]
     *
     * @param metricName metric event name
     * @param jsonStringData metric data in JSON string representation
     */
    fun onMetricUpdated(@MetricEvent.Metric metricName: String, jsonStringData: String)
}
