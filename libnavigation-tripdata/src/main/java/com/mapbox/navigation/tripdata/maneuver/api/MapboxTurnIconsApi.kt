package com.mapbox.navigation.tripdata.maneuver.api

import com.mapbox.api.directions.v5.models.ManeuverModifier
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.internal.maneuver.ManeuverTurnIcon
import com.mapbox.navigation.base.internal.maneuver.TurnIconHelper
import com.mapbox.navigation.tripdata.maneuver.model.TurnIconError
import com.mapbox.navigation.tripdata.maneuver.model.TurnIconResources
import com.mapbox.navigation.ui.utils.internal.ifNonNull

/**
 * An API for generating turn icon drawable resource references.
 *
 * @param turnIconResources a [TurnIconResources] reference
 */
class MapboxTurnIconsApi(var turnIconResources: TurnIconResources) {

    private var turnIconHelper = TurnIconHelper(turnIconResources)

    /**
     * Returns a reference to a drawable for turn icons.
     *
     * @param type a maneuver type
     * @param degrees a value describing the degree of turn
     * @param modifier a [ManeuverModifier] value
     * @param drivingSide the driving side describing the turn
     */
    fun generateTurnIcon(
        type: String?,
        degrees: Float?,
        modifier: String?,
        drivingSide: String?,
    ): Expected<TurnIconError, ManeuverTurnIcon> {
        return ifNonNull(
            turnIconHelper.retrieveTurnIcon(
                type,
                degrees,
                modifier,
                drivingSide,
            ),
        ) { icon ->
            ExpectedFactory.createValue(icon)
        } ?: ExpectedFactory.createError(generateError(type, degrees, modifier, drivingSide))
    }

    /**
     * Invoke the method if there is a need to use other turn icon drawables than the default icons
     * supplied.
     * @param resources TurnIconResources
     */
    fun updateResources(resources: TurnIconResources) {
        turnIconHelper = TurnIconHelper(resources)
    }

    private fun generateError(
        type: String?,
        degrees: Float?,
        modifier: String?,
        drivingSide: String?,
    ): TurnIconError {
        val errorMessage = "Unrecognized turn $type, " +
            "degrees $degrees, " +
            "modifier $modifier, " +
            "drivingSide: $drivingSide"
        return TurnIconError(errorMessage, type, degrees, modifier, drivingSide)
    }
}
