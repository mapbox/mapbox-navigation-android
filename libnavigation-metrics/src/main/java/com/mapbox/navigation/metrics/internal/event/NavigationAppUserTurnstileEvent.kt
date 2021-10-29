package com.mapbox.navigation.metrics.internal.event

import com.google.gson.Gson
import com.mapbox.android.telemetry.AppUserTurnstile
import com.mapbox.bindgen.Value
import com.mapbox.navigation.base.metrics.MetricEvent
import com.mapbox.navigation.base.metrics.NavigationMetrics
import org.json.JSONObject

class NavigationAppUserTurnstileEvent(
    val event: AppUserTurnstile
) : MetricEvent {

    override val metricName: String
        get() = NavigationMetrics.APP_USER_TURNSTILE

    override fun toJson(gson: Gson): String = gson.toJson(this)

    override fun toValue(): Value {
        val jsonObject = JSONObject(toJson(Gson()))
        val conversionResult = Value.fromJson(jsonObject.getJSONObject("event").toString())
        return conversionResult.value ?: Value.nullValue()
    }
}
