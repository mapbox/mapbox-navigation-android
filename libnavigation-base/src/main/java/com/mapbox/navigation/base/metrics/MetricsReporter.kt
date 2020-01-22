package com.mapbox.navigation.base.metrics

/**
 * Defines API for handling metric events.
 *
 * @since 1.0.0
 */
interface MetricsReporter {

    /**
     * Add event to metrics reporter when this event occurs.
     *
     * @param metricEvent event that should be handled
     * @since 1.0.0
     */
    fun addEvent(metricEvent: MetricEvent)

    /**
     * Add observer that triggered when metric event handled
     *
     * @param metricsObserver is [MetricsObserver] that triggered when metric event handled
     * @since 1.0.0
     */
    fun setMetricsObserver(metricsObserver: MetricsObserver)

    /**
     * Remove metrics observer
     *
     * @since 1.0.0
     */
    fun removeObserver()
}
