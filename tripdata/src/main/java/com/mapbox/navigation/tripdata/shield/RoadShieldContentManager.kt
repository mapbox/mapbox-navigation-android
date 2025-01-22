package com.mapbox.navigation.tripdata.shield

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.tripdata.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.tripdata.shield.model.RouteShieldError
import com.mapbox.navigation.tripdata.shield.model.RouteShieldResult

internal interface RoadShieldContentManager {
    suspend fun getShields(
        shieldsToDownload: List<RouteShieldToDownload>,
    ): List<Expected<RouteShieldError, RouteShieldResult>>

    fun cancelAll()
}
