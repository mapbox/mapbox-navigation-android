package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.navigation.base.formatter.DistanceFormatter

/**
 * Data structure representing distance associated with the step. Can be either distance
 * remaining to finish the step or total step distance.
 * @property distanceFormatter DistanceFormatter to format the distance with proper units.
 * @property distance Double
 */
class StepDistance internal constructor(
    val distanceFormatter: DistanceFormatter,
    val distance: Double
)
