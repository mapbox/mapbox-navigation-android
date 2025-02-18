package com.mapbox.navigation.mapgpt.core.analytics

sealed class MapGptAnalyticsParam<T>(val value: T) {
    class LongAnalyticsParam(value: Long) : MapGptAnalyticsParam<Long>(value)
    class DoubleAnalyticsParam(value: Double) : MapGptAnalyticsParam<Double>(value)
    class StringAnalyticsParam(value: String) : MapGptAnalyticsParam<String>(value)

    override fun toString(): String {
        return "${this::class.simpleName}(value=$value)"
    }
}
