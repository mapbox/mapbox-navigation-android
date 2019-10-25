package com.mapbox.navigation.base.metrics

interface MetricsObserver {

    fun onMetricUpdated(@Metric metric: String, jsonStringData: String)
}
