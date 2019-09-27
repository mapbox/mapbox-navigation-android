package com.mapbox.services.android.navigation.v5.location.replay

import android.location.Location


internal interface ReplayLocationListener {

    fun onLocationReplay(location: Location)
}
