package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import com.mapbox.services.android.navigation.v5.internal.navigation.metrics.audio.AudioTypeChain
import kotlin.math.floor

internal object NavigationUtils {

    private const val PERCENT_NORMALIZER = 100.0
    private const val SCREEN_BRIGHTNESS_MAX = 255.0
    private const val BRIGHTNESS_EXCEPTION_VALUE = -1

    fun obtainVolumeLevel(context: Context): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return floor(
            PERCENT_NORMALIZER * audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) /
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        ).toInt()
    }

    fun obtainScreenBrightness(context: Context): Int =
        try {
            val systemScreenBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            calculateScreenBrightnessPercentage(systemScreenBrightness)
        } catch (exception: Settings.SettingNotFoundException) {
            BRIGHTNESS_EXCEPTION_VALUE
        }

    fun obtainAudioType(context: Context): String =
        AudioTypeChain().setup().obtainAudioType(context)

    private fun calculateScreenBrightnessPercentage(screenBrightness: Int): Int =
        floor(PERCENT_NORMALIZER * screenBrightness / SCREEN_BRIGHTNESS_MAX).toInt()
}
