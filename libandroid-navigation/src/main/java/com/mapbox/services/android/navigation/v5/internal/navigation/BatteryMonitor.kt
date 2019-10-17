package com.mapbox.services.android.navigation.v5.internal.navigation

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build

internal class BatteryMonitor(private val currentVersionChecker: SdkVersionChecker) {

    companion object {
        private const val UNAVAILABLE_BATTERY_LEVEL = -1f
        private const val DEFAULT_BATTERY_LEVEL = -1
        private const val DEFAULT_PLUGGED_SOURCE = -1
        private const val DEFAULT_SCALE = 100
        private const val PERCENT_SCALE = 100.0f
    }

    fun obtainPercentage(context: Context): Float {
        val batteryStatus = registerBatteryUpdates(context) ?: return UNAVAILABLE_BATTERY_LEVEL
        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL,
            DEFAULT_BATTERY_LEVEL
        )
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE,
            DEFAULT_SCALE
        )
        return level / scale.toFloat() * PERCENT_SCALE
    }

    fun isPluggedIn(context: Context): Boolean {
        val batteryStatus = registerBatteryUpdates(context) ?: return false

        val chargePlug =
            batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED,
                DEFAULT_PLUGGED_SOURCE
            )
        val pluggedUsb = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val pluggedAc = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
        var pluggedWireless = false
        if (currentVersionChecker.isGreaterThan(Build.VERSION_CODES.JELLY_BEAN)) {
            pluggedWireless = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS
        }
        return pluggedUsb || pluggedAc || pluggedWireless
    }

    private fun registerBatteryUpdates(context: Context): Intent? {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        return context.registerReceiver(null, filter)
    }
}
