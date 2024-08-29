package com.mapbox.navigation.core.telemetry.events

import android.annotation.SuppressLint
import com.mapbox.bindgen.Value
import com.mapbox.navigation.base.metrics.NavigationMetrics

@SuppressLint("ParcelCreator")
internal class NavigationArriveEvent(
    phoneState: PhoneState,
) : NavigationEvent(phoneState) {

    override fun getEventName(): String = NavigationMetrics.ARRIVE

    override fun customFields(): Map<String, Value>? = null
}
