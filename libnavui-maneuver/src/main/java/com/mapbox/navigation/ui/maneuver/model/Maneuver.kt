package com.mapbox.navigation.ui.maneuver.model

import com.mapbox.api.directions.v5.models.BannerInstructions

/**
 * A simplified data structure representing a single [BannerInstructions]
 * @property primary PrimaryManeuver represents [BannerInstructions.primary]
 * @property totalManeuverDistance TotalManeuverDistance represents [BannerInstructions.distanceAlongGeometry]
 * @property secondary SecondaryManeuver? represents [BannerInstructions.secondary]
 * @property sub SubManeuver? represents [BannerInstructions.sub] with type text
 * @property laneGuidance Lane? represents [BannerInstructions.sub] with type lane
 */
class Maneuver internal constructor(
    val primary: PrimaryManeuver,
    val totalManeuverDistance: TotalManeuverDistance,
    val secondary: SecondaryManeuver?,
    val sub: SubManeuver?,
    val laneGuidance: Lane?
)
