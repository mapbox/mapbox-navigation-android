package com.mapbox.navigation.base.metrics

import com.google.gson.Gson

interface MetricEvent {

    fun toJson(gson: Gson): String
}
