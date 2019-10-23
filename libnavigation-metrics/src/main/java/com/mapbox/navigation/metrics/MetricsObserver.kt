package com.mapbox.navigation.metrics

import android.os.Bundle

interface MetricsObserver {

    // fun onMetricUpdated(data: MetricEntity)

    fun onStringMetricUpdated(eventName: String, stringData: String)

    fun onBundleMetricUpdated(eventName: String, bundleData: Bundle)

    fun onJsonStringMetricUpdated(eventName: String, jsonStringData: String)

    // fun onMetricUpdated(metric: String, data: Bundle)

    // fun toJson(data: Bundle): String
}