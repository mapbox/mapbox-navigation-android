package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

interface MetricsReporter {

    fun addEvent(metricEvent: MetricEvent)

    fun setMetricsObserver(metricsObserver: MetricsObserver)
}
