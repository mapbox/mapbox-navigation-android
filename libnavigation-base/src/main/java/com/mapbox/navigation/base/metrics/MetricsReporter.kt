package com.mapbox.navigation.base.metrics

interface MetricsReporter {

    fun addEvent(@Metric metric: String, metricEvent: MetricEvent)

    fun setMetricsObserver(metricsObserver: MetricsObserver)
}
