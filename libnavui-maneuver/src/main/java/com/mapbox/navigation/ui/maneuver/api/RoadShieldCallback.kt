package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.RoadShieldError
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.ui.maneuver.model.SubManeuver

/**
 * An interface that is triggered when road shields are available.
 */
@Deprecated(
    message = "The interface is only capable of delivering one shield for a maneuver even if it " +
        "contains multiple",
    replaceWith = ReplaceWith("RoadShieldsCallback")
)
fun interface RoadShieldCallback {

    /**
     * The callback is invoked when road shields are ready.
     *
     * The return maps of [String] to [RoadShield] or [RoadShieldError] can be used when displaying
     * [PrimaryManeuver], [SecondaryManeuver], and [SubManeuver].
     *
     * @param maneuvers list of [Maneuver]s for which the shields were requested.
     * @param shields map of a key to [RoadShield] where key is an ID of a maneuver's banner, one of
     * [PrimaryManeuver.id], [SecondaryManeuver.id] or [SubManeuver.id].
     * You can use those IDs to associate the maneuver's banner with the icon.
     * @param errors map of a key to [RoadShieldError] where key is an ID of a maneuver's banner, one of
     * [PrimaryManeuver.id], [SecondaryManeuver.id] or [SubManeuver.id].
     * You can use those IDs to associate the maneuver's banner with the error.
     */
    @Deprecated(
        message = "The method is only capable of delivering one shield for a maneuver even if it " +
            "contains multiple",
        replaceWith = ReplaceWith("onRoadShields(maneuvers, shields, errors)")
    )
    fun onRoadShields(
        maneuvers: List<Maneuver>,
        shields: Map<String, RoadShield?>,
        errors: Map<String, RoadShieldError>
    )
}
