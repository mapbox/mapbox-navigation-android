package com.mapbox.navigation.core.telemetry.events

import com.google.gson.Gson
import com.mapbox.bindgen.Value
import com.mapbox.common.TurnstileEvent
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics
import com.mapbox.navigation.metrics.internal.event.AppUserTurnstileInterface
import org.json.JSONObject

internal class NavigationAppUserTurnstileEvent(
    override val event: TurnstileEvent
) : MetricEvent, AppUserTurnstileInterface {

    override val metricName: String
        get() = NavigationMetrics.APP_USER_TURNSTILE

    override fun toJson(gson: Gson): String = gson.toJson(this)

    override fun toValue(): Value {
        val jsonObject = JSONObject(toJson(Gson()))
        val conversionResult = Value.fromJson(jsonObject.getJSONObject("event").toString())
        return conversionResult.value ?: Value.nullValue()
    }
}
