package com.mapbox.navigation.base.metrics

interface MetricsReporter {

    fun addEvent(metricEvent: MetricEvent)

    fun setMetricsObserver(metricsObserver: MetricsObserver)
}
