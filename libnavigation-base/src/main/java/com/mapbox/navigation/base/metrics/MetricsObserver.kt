package com.mapbox.navigation.base.metrics

interface MetricsObserver {

    fun onMetricUpdated(@MetricEvent.Metric metric: String, jsonStringData: String)
}
