package com.mapbox.navigation.core.telemetry

import android.content.Context
import com.mapbox.common.TelemetrySystemUtils.isPluggedIn
import com.mapbox.common.TelemetrySystemUtils.obtainApplicationState
import com.mapbox.common.TelemetrySystemUtils.obtainBatteryLevel
import com.mapbox.common.TelemetrySystemUtils.obtainCellularNetworkType
import com.mapbox.navigation.base.options.EventsAppMetadata
import com.mapbox.navigator.AppMetadata
import com.mapbox.navigator.ApplicationState
import com.mapbox.navigator.AudioType
import com.mapbox.navigator.EventsMetadata
import com.mapbox.navigator.EventsMetadataInterface

internal class EventsMetadataInterfaceImpl(
    private val appContext: Context,
    private val lifecycleMonitor: ApplicationLifecycleMonitor,
    eventsAppMetadata: EventsAppMetadata?
) : EventsMetadataInterface {

    private val nativeAppMetadata: AppMetadata? = eventsAppMetadata?.mapToNative()

    override fun provideEventsMetadata(): EventsMetadata =
        EventsMetadata(
            obtainVolumeLevel(appContext).toByte(),
            obtainAudioType(appContext).mapToNativeAudioType(),
            obtainScreenBrightness(appContext).toByte(),
            lifecycleMonitor.obtainForegroundPercentage().toByte(),
            lifecycleMonitor.obtainPortraitPercentage().toByte(),
            isPluggedIn(appContext),
            obtainBatteryLevel(appContext).toByte(),
            obtainCellularNetworkType(appContext),
            nativeAppMetadata,
        )
}
