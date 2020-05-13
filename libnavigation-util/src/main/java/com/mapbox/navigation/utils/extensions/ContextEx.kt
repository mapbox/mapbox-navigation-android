package com.mapbox.navigation.utils.extensions

import android.content.Context
import android.location.LocationManager

fun Context.isLocationProviderEnabled(): Boolean {
    val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}


