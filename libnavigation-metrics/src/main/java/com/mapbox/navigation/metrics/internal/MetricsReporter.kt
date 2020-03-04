package com.mapbox.navigation.metrics.internal

import com.mapbox.navigation.metrics.MetricsObserver
import com.mapbox.navigation.metrics.internal.utils.extentions.MetricEvent

/**
 * Defines API for handling metric events.
 */
interface MetricsReporter {

    /**
     * Adds an event to the metrics reporter when this event occurs.
     *
     * @param metricEvent event that should be handled
     */
    fun addEvent(metricEvent: MetricEvent)

    /**
     * Adds an observer that will be triggered when a metric event is handled
     *
     * @param metricsObserver the [MetricsObserver] that is called when a new metric event is triggered
     */
    fun setMetricsObserver(metricsObserver: MetricsObserver)

    /**
     * Remove metrics observer
     */
    fun removeObserver()
}
