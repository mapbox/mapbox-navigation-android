package com.mapbox.navigation.core.telemetry

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import com.mapbox.common.TelemetrySystemUtils.isPluggedIn
import com.mapbox.common.TelemetrySystemUtils.obtainBatteryLevel
import com.mapbox.common.TelemetrySystemUtils.obtainCellularNetworkType
import com.mapbox.navigation.base.options.EventsAppMetadata
import com.mapbox.navigation.core.telemetry.audio.AudioTypeChain
import com.mapbox.navigation.core.telemetry.audio.AudioTypeResolver
import com.mapbox.navigator.AppMetadata
import com.mapbox.navigator.EventsMetadata
import com.mapbox.navigator.EventsMetadataInterface
import kotlin.math.floor

internal class EventsMetadataInterfaceImpl(
    private val context: Context,
    private val lifecycleMonitor: ApplicationLifecycleMonitor,
    eventsAppMetadata: EventsAppMetadata?,
) : EventsMetadataInterface {

    private val nativeAppMetadata = eventsAppMetadata?.toNativeAppMetadata()

    override fun provideEventsMetadata(): EventsMetadata {
        return EventsMetadata(
            obtainVolumeLevel(context).toByte(),
            obtainAudioTypeNew(context).toNativeAudioType(),
            obtainScreenBrightness(context).toByte(),
            lifecycleMonitor.obtainForegroundPercentage().toByte(),
            lifecycleMonitor.obtainPortraitPercentage().toByte(),
            isPluggedIn(context),
            obtainBatteryLevel(context).toByte(),
            obtainCellularNetworkType(context),
            nativeAppMetadata,
        )
    }

    private companion object {

        const val PERCENT_NORMALIZER = 100.0
        const val SCREEN_BRIGHTNESS_MAX = 255.0
        const val BRIGHTNESS_EXCEPTION_VALUE = -1

        /**
         * Provide the volume level in the percentages(range is *0..100*)
         */
        fun obtainVolumeLevel(context: Context): Int {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return floor(
                PERCENT_NORMALIZER * audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) /
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            ).toInt()
        }

        /**
         * Provide screen brightness in range *0..100*
         */
        fun obtainScreenBrightness(context: Context): Int =
            try {
                val systemScreenBrightness = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                )
                calculateScreenBrightnessPercentage(systemScreenBrightness)
            } catch (exception: Settings.SettingNotFoundException) {
                BRIGHTNESS_EXCEPTION_VALUE
            }

        fun obtainAudioTypeNew(context: Context): AudioTypeResolver =
            AudioTypeChain().setup().obtainAudioType(context)

        fun calculateScreenBrightnessPercentage(screenBrightness: Int): Int =
            floor(PERCENT_NORMALIZER * screenBrightness / SCREEN_BRIGHTNESS_MAX).toInt()

        fun EventsAppMetadata.toNativeAppMetadata(): AppMetadata = AppMetadata(
            name,
            version,
            userId,
            sessionId,
        )
    }
}
