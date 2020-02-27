package com.mapbox.navigation.metrics.internal

import com.google.gson.Gson
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.navigation.metrics.internal.utils.extentions.MetricEvent
import com.mapbox.navigation.metrics.internal.utils.extentions.NavigationMetrics

class NavigationAppUserTurnstileEvent(
    val event: AppUserTurnstile
) : MetricEvent {

    override val metricName: String
        get() = NavigationMetrics.APP_USER_TURNSTILE

    override fun toJson(gson: Gson): String = gson.toJson(this)
}
