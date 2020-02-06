package com.mapbox.navigation.base.network.replay

import android.location.Location

interface ReplayLocationListener {

    fun onLocationReplay(location: Location)
}
