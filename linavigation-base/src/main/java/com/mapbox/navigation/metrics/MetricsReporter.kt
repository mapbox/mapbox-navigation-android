package com.mapbox.navigation.metrics

interface MetricsReporter {

    // fun addEvent(@Metric metric: String, eventJsonString: String)

    fun addEvent(@Metric metric: String, event: Event)

    fun setMetricsObserver(metricsObserver: MetricsObserver)
}