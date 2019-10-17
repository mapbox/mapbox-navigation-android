package com.mapbox.services.android.navigation.v5.internal.location.replay

import android.location.Location

interface ReplayLocationListener {

    fun onLocationReplay(location: Location)
}
