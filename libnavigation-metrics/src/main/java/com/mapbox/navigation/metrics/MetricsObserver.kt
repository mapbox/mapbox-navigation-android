package com.mapbox.navigation.metrics

import com.mapbox.navigation.metrics.internal.utils.extentions.MetricEvent

/**
 * Interface for observe metric events handled.
 *
 * @since 1.0.0
 */
interface MetricsObserver {

    /**
     * Called when metric event handled by [MetricsReporter.addEvent]
     *
     * @param metricName metric event name
     * @param jsonStringData metric data in JSON string representation
     * @since 1.0.0
     */
    fun onMetricUpdated(@MetricEvent.Metric metricName: String, jsonStringData: String)
}
