package com.mapbox.navigation.metrics.internal

import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.navigation.base.internal.metrics.NavigationMetrics
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.util.JsonMapper

internal class NavigationAppUserTurnstileEvent(
    val event: AppUserTurnstile
) : MetricEvent {

    override val metric: String
        get() = NavigationMetrics.APP_USER_TURNSTILE

    override fun toJson(jsonMapper: JsonMapper): String = jsonMapper.toJson(this)
}
