package com.mapbox.navigation.utils.internal

import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.provider.Settings

/**
 * Check if enabled one of location providers: GPS_PROVIDER or NETWORK_PROVIDER.
 *
 * @return true if enabled at least one of location providers
 */
fun Context.isLocationProviderEnabled(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        lm?.isLocationEnabled ?: false
    } else {
        try {
            val mode = Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            mode != Settings.Secure.LOCATION_MODE_OFF
        } catch (e: Settings.SettingNotFoundException) {
            false
        }
    }
