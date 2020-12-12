package com.mapbox.navigation.ui.base.api.snapshotter

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * An Api that allows you to generate snapshot based on [RouteProgress]
 */
interface SnapshotterApi {

    /**
     * The method takes in [RouteProgress] and generates a snapshot based on the presence of
     * [BannerComponents] of type [BannerComponents.GUIDANCE_VIEW] and subType [BannerComponents.SIGNBOARD]
     * @param progress object representing [RouteProgress]
     */
    fun generateSnapshot(progress: RouteProgress)
}
