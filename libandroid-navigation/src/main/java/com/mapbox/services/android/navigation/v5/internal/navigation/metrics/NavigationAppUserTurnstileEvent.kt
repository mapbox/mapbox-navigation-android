package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import com.google.gson.Gson
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricEvent
import com.mapbox.services.android.navigation.v5.navigation.metrics.NavigationMetrics

internal class NavigationAppUserTurnstileEvent(
    val event: AppUserTurnstile
) : MetricEvent {

    override val metricName: String
        get() = NavigationMetrics.APP_USER_TURNSTILE

    override fun toJson(gson: Gson): String = gson.toJson(this)
}
