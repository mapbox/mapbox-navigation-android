package com.mapbox.navigation.base.metrics

import com.mapbox.android.telemetry.Event

interface MetricsReporter {

    fun addEvent(@Metric metric: String, event: Event)

    fun setMetricsObserver(metricsObserver: MetricsObserver)
}
