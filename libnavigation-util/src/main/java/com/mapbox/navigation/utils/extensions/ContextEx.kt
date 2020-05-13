package com.mapbox.navigation.utils.extensions

import android.content.Context
import android.location.LocationManager

/**
 * Check if enabled one of location providers: GPS_PROVIDER or NETWORK_PROVIDER.
 *
 * @return true if enabled at least one of location providers
 */
fun Context.isLocationProviderEnabled(): Boolean {
    val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}
