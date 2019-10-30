package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import com.google.gson.Gson
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.services.android.navigation.v5.navigation.metrics.MetricEvent

internal class NavigationAppUserTurnstileEvent(
    val event: AppUserTurnstile
) : MetricEvent {

    override val metric: String
        get() = NavigationMetrics.APP_USER_TURNSTILE

    override fun toJson(gson: Gson): String = gson.toJson(this)
}
