package com.mapbox.navigation.base.metrics

import com.mapbox.android.telemetry.Event
import com.mapbox.navigation.metrics.Metric
import com.mapbox.navigation.metrics.MetricsObserver

interface MetricsReporter {

    fun addEvent(@Metric metric: String, event: Event)

    fun setMetricsObserver(metricsObserver: MetricsObserver)
}
