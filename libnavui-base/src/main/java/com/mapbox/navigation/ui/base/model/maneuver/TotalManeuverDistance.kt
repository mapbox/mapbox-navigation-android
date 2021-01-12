package com.mapbox.navigation.ui.base.model.maneuver

import com.mapbox.api.directions.v5.models.BannerInstructions

/**
 * "bannerInstructions": [
 *      {
 *          "primary": "...",
 *          "secondary": "...",
 *          "sub": "...",
 *          "distanceAlongGeometry": 580
 *      }
 * ]
 *
 * A simplified data structure representing [BannerInstructions.distanceAlongGeometry]
 * @property totalDistance Double represents the total step distance.
 */
data class TotalManeuverDistance(
    val totalDistance: Double
)
