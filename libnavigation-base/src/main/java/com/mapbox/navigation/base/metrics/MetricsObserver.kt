package com.mapbox.navigation.metrics

interface MetricsObserver {

    fun onMetricUpdated(@Metric metric: String, jsonStringData: String)
}
