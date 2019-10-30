package com.mapbox.services.android.navigation.v5.navigation.metrics

/**
 * Interface for handling metric events.
 *
 * @since 0.43.0
 */
interface MetricsReporter {

    /**
     * Add event to metrics reporter when this event occurs.
     *
     * @param metricEvent event that should be handled
     * @since 0.43.0
     */
    fun addEvent(metricEvent: MetricEvent)

    /**
     * Add observer that triggered when metric event handled
     *
     * @param metricsObserver metric event handle observer
     * @since 0.43.0
     */
    fun setMetricsObserver(metricsObserver: MetricsObserver)
}
