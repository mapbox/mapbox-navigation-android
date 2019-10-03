package com.mapbox.services.android.navigation.v5.internal.navigation

import android.annotation.SuppressLint
import android.os.Parcelable

@SuppressLint("ParcelCreator")
internal data class BatteryEvent(
    private val sessionId: String,
    private val batteryPercentage: Float,
    private val isPluggedIn: Boolean,
    override var metadata: NavigationPerformanceMetadata?
) : NavigationPerformanceEvent(sessionId, BATTERY_EVENT_NAME, metadata), Parcelable {

    companion object {
        private const val BATTERY_PERCENTAGE_KEY = "battery_percentage"
        private const val IS_PLUGGED_IN_KEY = "is_plugged_in"
        private const val BATTERY_EVENT_NAME = "battery_event"
    }

    init {
        addCounter(
            FloatCounter(
                BATTERY_PERCENTAGE_KEY,
                batteryPercentage
            )
        )
        addAttribute(
            Attribute(
                IS_PLUGGED_IN_KEY,
                isPluggedIn.toString()
            )
        )
    }
}
