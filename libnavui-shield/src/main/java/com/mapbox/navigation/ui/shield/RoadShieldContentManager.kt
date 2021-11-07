package com.mapbox.navigation.ui.shield

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.ui.shield.model.RouteShieldError
import com.mapbox.navigation.ui.shield.model.RouteShieldResult

internal interface RoadShieldContentManager {
    suspend fun getShields(
        shieldsToDownload: List<RouteShieldToDownload>
    ): List<Expected<RouteShieldError, RouteShieldResult>>

    fun cancelAll()
}
