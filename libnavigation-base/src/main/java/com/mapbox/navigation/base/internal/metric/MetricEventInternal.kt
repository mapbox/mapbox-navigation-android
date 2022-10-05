package com.mapbox.navigation.base.internal.metric

import com.mapbox.bindgen.Value
import com.mapbox.navigation.base.metrics.MetricEvent

interface MetricEventInternal : MetricEvent {

    /**
     * Present [MetricEvent] as [Value]
     *
     * @return Value
     */
    fun toValue(): Value
}
