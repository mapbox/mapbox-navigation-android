package com.mapbox.navigation.core.telemetry.events

import android.content.Context
import com.mapbox.common.TelemetrySystemUtils.isPluggedIn
import com.mapbox.common.TelemetrySystemUtils.obtainBatteryLevel
import com.mapbox.common.TelemetrySystemUtils.obtainCellularNetworkType
import com.mapbox.common.TelemetrySystemUtils.obtainCurrentDate
import com.mapbox.common.TelemetrySystemUtils.obtainUniversalUniqueIdentifier
import com.mapbox.navigation.core.telemetry.obtainAudioType
import com.mapbox.navigation.core.telemetry.obtainScreenBrightness
import com.mapbox.navigation.core.telemetry.obtainVolumeLevel

/**
 * Class that will hold the current states of the device phone.
 */
internal data class PhoneState(
    val volumeLevel: Int,
    val batteryLevel: Int,
    val screenBrightness: Int,
    val isBatteryPluggedIn: Boolean,
    val connectivity: String,
    val audioType: String,
    val applicationState: String,
    val created: String,
    val feedbackId: String,
    val userId: String,
) {
    internal companion object {
        internal fun newInstance(context: Context): PhoneState =
            PhoneState(
                volumeLevel = obtainVolumeLevel(context),
                batteryLevel = obtainBatteryLevel(context),
                screenBrightness = obtainScreenBrightness(context),
                isBatteryPluggedIn = isPluggedIn(context),
                connectivity = obtainCellularNetworkType(context),
                audioType = obtainAudioType(context),
                applicationState = LifecycleStateProvider.instance.currentState.name,
                created = obtainCurrentDate(),
                feedbackId = obtainUniversalUniqueIdentifier(),
                // Hardcoded to '-' for privacy concerns
                userId = "-",
            )
    }
}
