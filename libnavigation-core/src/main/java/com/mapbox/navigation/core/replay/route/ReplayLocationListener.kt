package com.mapbox.navigation.core.replay.route

import android.location.Location

interface ReplayLocationListener {

    fun onLocationReplay(location: Location)
}
