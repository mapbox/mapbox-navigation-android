package com.mapbox.navigation.dropin.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Facade for OS Vibrator.
 */
internal class HapticFeedback(context: Context) {

    internal val vibrator = if (Build.VERSION_CODES.S <= Build.VERSION.SDK_INT) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    }

    fun tick() {
        vibrate(TICK_DURATION_MS)
    }

    fun vibrate(milliseconds: Long) {
        if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(
                    milliseconds,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator?.vibrate(milliseconds)
        }
    }

    companion object {
        const val TICK_DURATION_MS: Long = 10

        fun create(context: Context) = HapticFeedback(context)
    }
}
