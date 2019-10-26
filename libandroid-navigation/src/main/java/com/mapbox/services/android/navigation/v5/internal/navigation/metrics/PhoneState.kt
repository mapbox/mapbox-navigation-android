package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.content.Context
import com.mapbox.android.telemetry.TelemetryUtils

/**
 * Class that will hold the current states of the device phone.
 */
data class PhoneState(val context: Context) {
    val volumeLevel: Int = NavigationUtils.obtainVolumeLevel(context)
    val batteryLevel: Int = TelemetryUtils.obtainBatteryLevel(context)
    val screenBrightness: Int = NavigationUtils.obtainScreenBrightness(context)
    val isBatteryPluggedIn: Boolean = TelemetryUtils.isPluggedIn(context)
    val connectivity: String? = TelemetryUtils.obtainCellularNetworkType(context)
    val audioType: String = NavigationUtils.obtainAudioType(context)
    val applicationState: String = TelemetryUtils.obtainApplicationState(context)
    val created: String = TelemetryUtils.obtainCurrentDate()
    val feedbackId: String = TelemetryUtils.obtainUniversalUniqueIdentifier()
    val userId: String = TelemetryUtils.retrieveVendorId()
}
