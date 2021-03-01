package com.mapbox.navigation.examples.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AppCompatActivity

object Utils {
    /**
     * Returns the Mapbox access token set in the app resources.
     *
     * @param context The [Context] of the [android.app.Activity] or [android.app.Fragment].
     * @return The Mapbox access token or null if not found.
     */
    fun getMapboxAccessToken(context: Context): String {
        val tokenResId = context.resources
            .getIdentifier("mapbox_access_token", "string", context.packageName)
        return if (tokenResId != 0) context.getString(tokenResId) else ""
    }

    fun vibrate(context: Context) {
        val vibrator = context.getSystemService(AppCompatActivity.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(100L)
        }
    }
}
