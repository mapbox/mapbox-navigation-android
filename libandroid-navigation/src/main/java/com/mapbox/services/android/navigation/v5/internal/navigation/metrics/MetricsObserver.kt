package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

interface MetricsObserver {

    fun onMetricUpdated(@MetricEvent.Metric metric: String, jsonStringData: String)
}
