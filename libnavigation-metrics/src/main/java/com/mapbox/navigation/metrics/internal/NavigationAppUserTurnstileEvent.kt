package com.mapbox.navigation.metrics.internal

import com.google.gson.Gson
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.navigation.base.internal.metrics.NavigationMetrics
import com.mapbox.navigation.base.metrics.MetricEvent

internal class NavigationAppUserTurnstileEvent(
    val event: AppUserTurnstile
) : MetricEvent {

    override val metric: String
        get() = NavigationMetrics.APP_USER_TURNSTILE

    override fun toJson(gson: Gson): String = gson.toJson(this)
}
