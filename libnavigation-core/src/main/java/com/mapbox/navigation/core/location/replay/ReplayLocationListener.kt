package com.mapbox.navigation.core.location.replay

import android.location.Location

interface ReplayLocationListener {

    fun onLocationReplay(location: Location)
}
