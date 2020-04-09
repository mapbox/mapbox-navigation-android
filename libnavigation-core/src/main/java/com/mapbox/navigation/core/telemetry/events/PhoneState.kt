package com.mapbox.navigation.core.telemetry.events

import android.content.Context
import com.mapbox.android.telemetry.TelemetryUtils.isPluggedIn
import com.mapbox.android.telemetry.TelemetryUtils.obtainApplicationState
import com.mapbox.android.telemetry.TelemetryUtils.obtainBatteryLevel
import com.mapbox.android.telemetry.TelemetryUtils.obtainCellularNetworkType
import com.mapbox.android.telemetry.TelemetryUtils.obtainCurrentDate
import com.mapbox.android.telemetry.TelemetryUtils.obtainUniversalUniqueIdentifier
import com.mapbox.android.telemetry.TelemetryUtils.retrieveVendorId
import com.mapbox.navigation.core.telemetry.obtainAudioType
import com.mapbox.navigation.core.telemetry.obtainScreenBrightness
import com.mapbox.navigation.core.telemetry.obtainVolumeLevel

/**
 * Class that will hold the current states of the device phone.
 */
data class PhoneState(val context: Context) {
    val volumeLevel: Int = obtainVolumeLevel(context)
    val batteryLevel: Int = obtainBatteryLevel(context)
    val screenBrightness: Int = obtainScreenBrightness(context)
    val isBatteryPluggedIn: Boolean = isPluggedIn(context)
    val connectivity: String? = obtainCellularNetworkType(context)
    val audioType: String = obtainAudioType(context)
    val applicationState: String = obtainApplicationState(context)
    val created: String = obtainCurrentDate()
    val feedbackId: String = obtainUniversalUniqueIdentifier()
    val userId: String = retrieveVendorId()
}
