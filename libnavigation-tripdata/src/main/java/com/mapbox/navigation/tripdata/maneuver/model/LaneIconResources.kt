package com.mapbox.navigation.tripdata.maneuver.model

import androidx.annotation.DrawableRes
import com.mapbox.navigation.tripdata.R

/**
 * A class that allows you to define your own turn lane icons to be rendered.
 * @property laneOppositeSlightTurnOrSlightTurn Int
 * @property laneOppositeSlightTurnOrSlightTurnUsingSlightTurn Int
 * @property laneOppositeSlightTurnOrStraightOrSlightTurn Int
 * @property laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn Int
 * @property laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight Int
 * @property laneOppositeSlightTurnOrStraightOrTurn Int
 * @property laneOppositeSlightTurnOrStraightOrTurnUsingStraight Int
 * @property laneOppositeSlightTurnOrStraightOrTurnUsingTurn Int
 * @property laneOppositeSlightTurnOrTurn Int
 * @property laneOppositeSlightTurnOrTurnUsingTurn Int
 * @property laneOppositeTurnOrSlightTurn Int
 * @property laneOppositeTurnOrSlightTurnUsingSlightTurn Int
 * @property laneOppositeTurnOrStraightOrSlightTurn Int
 * @property laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn Int
 * @property laneOppositeTurnOrStraightOrSlightTurnUsingStraight Int
 * @property laneOppositeTurnOrStraightOrTurn Int
 * @property laneOppositeTurnOrStraightOrTurnUsingStraight Int
 * @property laneOppositeTurnOrStraightOrTurnUsingTurn Int
 * @property laneOppositeTurnOrTurn Int
 * @property laneOppositeTurnOrTurnUsingTurn Int
 * @property laneSharpTurn Int
 * @property laneSharpTurnUsingSharpTurn Int
 * @property laneSlightTurn Int
 * @property laneSlightTurnOrSharpTurn Int
 * @property laneSlightTurnOrSharpTurnUsingSharpTurn Int
 * @property laneSlightTurnOrSharpTurnUsingSlightTurn Int
 * @property laneSlightTurnOrTurn Int
 * @property laneSlightTurnOrTurnUsingSlightTurn Int
 * @property laneSlightTurnOrTurnUsingTurn Int
 * @property laneSlightTurnOrUturn Int
 * @property laneSlightTurnOrUturnUsingSlightTurn Int
 * @property laneSlightTurnOrUturnUsingUturn Int
 * @property laneSlightTurnUsingSlightTurn Int
 * @property laneStraight Int
 * @property laneStraightOrSharpTurn Int
 * @property laneStraightOrSharpTurnUsingSharpTurn Int
 * @property laneStraightOrSharpTurnUsingStraight Int
 * @property laneStraightOrSlightTurn Int
 * @property laneStraightOrSlightTurnOrTurn Int
 * @property laneStraightOrSlightTurnOrTurnUsingSlightTurn Int
 * @property laneStraightOrSlightTurnOrTurnUsingStraight Int
 * @property laneStraightOrSlightTurnOrTurnUsingTurn Int
 * @property laneStraightOrSlightTurnUsingSlightTurn Int
 * @property laneStraightOrSlightTurnUsingStraight Int
 * @property laneStraightOrTurn Int
 * @property laneStraightOrTurnOrUturn Int
 * @property laneStraightOrTurnOrUturnUsingStraight Int
 * @property laneStraightOrTurnOrUturnUsingTurn Int
 * @property laneStraightOrTurnOrUturnUsingUturn Int
 * @property laneStraightOrTurnUsingStraight Int
 * @property laneStraightOrTurnUsingTurn Int
 * @property laneStraightOrUturn Int
 * @property laneStraightOrUturnUsingStraight Int
 * @property laneStraightOrUturnUsingUturn Int
 * @property laneStraightUsingStraight Int
 * @property laneTurn Int
 * @property laneTurnOrSharpTurn Int
 * @property laneTurnOrSharpTurnUsingSharpTurn Int
 * @property laneTurnOrSharpTurnUsingTurn Int
 * @property laneTurnOrUturn Int
 * @property laneTurnOrUturnUsingTurn Int
 * @property laneTurnOrUturnUsingUturn Int
 * @property laneTurnUsingTurn Int
 * @property laneUturn Int
 * @property laneUturnUsingUturn Int
 */
class LaneIconResources private constructor(
    @DrawableRes val laneOppositeSlightTurnOrSlightTurn: Int,
    @DrawableRes val laneOppositeSlightTurnOrSlightTurnUsingSlightTurn: Int,
    @DrawableRes val laneOppositeSlightTurnOrStraightOrSlightTurn: Int,
    @DrawableRes val laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn: Int,
    @DrawableRes val laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight: Int,
    @DrawableRes val laneOppositeSlightTurnOrStraightOrTurn: Int,
    @DrawableRes val laneOppositeSlightTurnOrStraightOrTurnUsingStraight: Int,
    @DrawableRes val laneOppositeSlightTurnOrStraightOrTurnUsingTurn: Int,
    @DrawableRes val laneOppositeSlightTurnOrTurn: Int,
    @DrawableRes val laneOppositeSlightTurnOrTurnUsingTurn: Int,
    @DrawableRes val laneOppositeTurnOrSlightTurn: Int,
    @DrawableRes val laneOppositeTurnOrSlightTurnUsingSlightTurn: Int,
    @DrawableRes val laneOppositeTurnOrStraightOrSlightTurn: Int,
    @DrawableRes val laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn: Int,
    @DrawableRes val laneOppositeTurnOrStraightOrSlightTurnUsingStraight: Int,
    @DrawableRes val laneOppositeTurnOrStraightOrTurn: Int,
    @DrawableRes val laneOppositeTurnOrStraightOrTurnUsingStraight: Int,
    @DrawableRes val laneOppositeTurnOrStraightOrTurnUsingTurn: Int,
    @DrawableRes val laneOppositeTurnOrTurn: Int,
    @DrawableRes val laneOppositeTurnOrTurnUsingTurn: Int,
    @DrawableRes val laneSharpTurn: Int,
    @DrawableRes val laneSharpTurnUsingSharpTurn: Int,
    @DrawableRes val laneSlightTurn: Int,
    @DrawableRes val laneSlightTurnOrSharpTurn: Int,
    @DrawableRes val laneSlightTurnOrSharpTurnUsingSharpTurn: Int,
    @DrawableRes val laneSlightTurnOrSharpTurnUsingSlightTurn: Int,
    @DrawableRes val laneSlightTurnOrTurn: Int,
    @DrawableRes val laneSlightTurnOrTurnUsingSlightTurn: Int,
    @DrawableRes val laneSlightTurnOrTurnUsingTurn: Int,
    @DrawableRes val laneSlightTurnOrUturn: Int,
    @DrawableRes val laneSlightTurnOrUturnUsingSlightTurn: Int,
    @DrawableRes val laneSlightTurnOrUturnUsingUturn: Int,
    @DrawableRes val laneSlightTurnUsingSlightTurn: Int,
    @DrawableRes val laneStraight: Int,
    @DrawableRes val laneStraightOrSharpTurn: Int,
    @DrawableRes val laneStraightOrSharpTurnUsingSharpTurn: Int,
    @DrawableRes val laneStraightOrSharpTurnUsingStraight: Int,
    @DrawableRes val laneStraightOrSlightTurn: Int,
    @DrawableRes val laneStraightOrSlightTurnOrTurn: Int,
    @DrawableRes val laneStraightOrSlightTurnOrTurnUsingSlightTurn: Int,
    @DrawableRes val laneStraightOrSlightTurnOrTurnUsingStraight: Int,
    @DrawableRes val laneStraightOrSlightTurnOrTurnUsingTurn: Int,
    @DrawableRes val laneStraightOrSlightTurnUsingSlightTurn: Int,
    @DrawableRes val laneStraightOrSlightTurnUsingStraight: Int,
    @DrawableRes val laneStraightOrTurn: Int,
    @DrawableRes val laneStraightOrTurnOrUturn: Int,
    @DrawableRes val laneStraightOrTurnOrUturnUsingStraight: Int,
    @DrawableRes val laneStraightOrTurnOrUturnUsingTurn: Int,
    @DrawableRes val laneStraightOrTurnOrUturnUsingUturn: Int,
    @DrawableRes val laneStraightOrTurnUsingStraight: Int,
    @DrawableRes val laneStraightOrTurnUsingTurn: Int,
    @DrawableRes val laneStraightOrUturn: Int,
    @DrawableRes val laneStraightOrUturnUsingStraight: Int,
    @DrawableRes val laneStraightOrUturnUsingUturn: Int,
    @DrawableRes val laneStraightUsingStraight: Int,
    @DrawableRes val laneTurn: Int,
    @DrawableRes val laneTurnOrSharpTurn: Int,
    @DrawableRes val laneTurnOrSharpTurnUsingSharpTurn: Int,
    @DrawableRes val laneTurnOrSharpTurnUsingTurn: Int,
    @DrawableRes val laneTurnOrUturn: Int,
    @DrawableRes val laneTurnOrUturnUsingTurn: Int,
    @DrawableRes val laneTurnOrUturnUsingUturn: Int,
    @DrawableRes val laneTurnUsingTurn: Int,
    @DrawableRes val laneUturn: Int,
    @DrawableRes val laneUturnUsingUturn: Int,
) {

    /**
     * @return builder matching the one used to create this instance
     */
    fun toBuilder(): Builder {
        return Builder()
            .laneOppositeSlightTurnOrSlightTurn(laneOppositeSlightTurnOrSlightTurn)
            .laneOppositeSlightTurnOrSlightTurnUsingSlightTurn(
                laneOppositeSlightTurnOrSlightTurnUsingSlightTurn,
            )
            .laneOppositeSlightTurnOrStraightOrSlightTurn(
                laneOppositeSlightTurnOrStraightOrSlightTurn,
            )
            .laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn(
                laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn,
            )
            .laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight(
                laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight,
            )
            .laneOppositeSlightTurnOrStraightOrTurn(laneOppositeSlightTurnOrStraightOrTurn)
            .laneOppositeSlightTurnOrStraightOrTurnUsingStraight(
                laneOppositeSlightTurnOrStraightOrTurnUsingStraight,
            )
            .laneOppositeSlightTurnOrStraightOrTurnUsingTurn(
                laneOppositeSlightTurnOrStraightOrTurnUsingTurn,
            )
            .laneOppositeSlightTurnOrTurn(laneOppositeSlightTurnOrTurn)
            .laneOppositeSlightTurnOrTurnUsingTurn(laneOppositeSlightTurnOrTurnUsingTurn)
            .laneOppositeTurnOrSlightTurn(laneOppositeTurnOrSlightTurn)
            .laneOppositeTurnOrSlightTurnUsingSlightTurn(
                laneOppositeTurnOrSlightTurnUsingSlightTurn,
            )
            .laneOppositeTurnOrStraightOrSlightTurn(laneOppositeTurnOrStraightOrSlightTurn)
            .laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn(
                laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn,
            )
            .laneOppositeTurnOrStraightOrSlightTurnUsingStraight(
                laneOppositeTurnOrStraightOrSlightTurnUsingStraight,
            )
            .laneOppositeTurnOrStraightOrTurn(laneOppositeTurnOrStraightOrTurn)
            .laneOppositeTurnOrStraightOrTurnUsingStraight(
                laneOppositeTurnOrStraightOrTurnUsingStraight,
            )
            .laneOppositeTurnOrStraightOrTurnUsingTurn(laneOppositeTurnOrStraightOrTurnUsingTurn)
            .laneOppositeTurnOrTurn(laneOppositeTurnOrTurn)
            .laneOppositeTurnOrTurnUsingTurn(laneOppositeTurnOrTurnUsingTurn)
            .laneSharpTurn(laneSharpTurn)
            .laneSharpTurnUsingSharpTurn(laneSharpTurnUsingSharpTurn)
            .laneSlightTurn(laneSlightTurn)
            .laneSlightTurnOrSharpTurn(laneSlightTurnOrSharpTurn)
            .laneSlightTurnOrSharpTurnUsingSharpTurn(laneSlightTurnOrSharpTurnUsingSharpTurn)
            .laneSlightTurnOrSharpTurnUsingSlightTurn(laneSlightTurnOrSharpTurnUsingSlightTurn)
            .laneSlightTurnOrTurn(laneSlightTurnOrTurn)
            .laneSlightTurnOrTurnUsingSlightTurn(laneSlightTurnOrTurnUsingSlightTurn)
            .laneSlightTurnOrTurnUsingTurn(laneSlightTurnOrTurnUsingTurn)
            .laneSlightTurnOrUturn(laneSlightTurnOrUturn)
            .laneSlightTurnOrUturnUsingSlightTurn(laneSlightTurnOrUturnUsingSlightTurn)
            .laneSlightTurnOrUturnUsingUturn(laneSlightTurnOrUturnUsingUturn)
            .laneSlightTurnUsingSlightTurn(laneSlightTurnUsingSlightTurn)
            .laneStraight(laneStraight)
            .laneStraightOrSharpTurn(laneStraightOrSharpTurn)
            .laneStraightOrSharpTurnUsingSharpTurn(laneStraightOrSharpTurnUsingSharpTurn)
            .laneStraightOrSharpTurnUsingStraight(laneStraightOrSharpTurnUsingStraight)
            .laneStraightOrSlightTurn(laneStraightOrSlightTurn)
            .laneStraightOrSlightTurnOrTurn(laneStraightOrSlightTurnOrTurn)
            .laneStraightOrSlightTurnOrTurnUsingSlightTurn(
                laneStraightOrSlightTurnOrTurnUsingSlightTurn,
            )
            .laneStraightOrSlightTurnOrTurnUsingStraight(
                laneStraightOrSlightTurnOrTurnUsingStraight,
            )
            .laneStraightOrSlightTurnOrTurnUsingTurn(laneStraightOrSlightTurnOrTurnUsingTurn)
            .laneStraightOrSlightTurnUsingSlightTurn(laneStraightOrSlightTurnUsingSlightTurn)
            .laneStraightOrSlightTurnUsingStraight(laneStraightOrSlightTurnUsingStraight)
            .laneStraightOrTurn(laneStraightOrTurn)
            .laneStraightOrTurnOrUturn(laneStraightOrTurnOrUturn)
            .laneStraightOrTurnOrUturnUsingStraight(laneStraightOrTurnOrUturnUsingStraight)
            .laneStraightOrTurnOrUturnUsingTurn(laneStraightOrTurnOrUturnUsingTurn)
            .laneStraightOrTurnOrUturnUsingUturn(laneStraightOrTurnOrUturnUsingUturn)
            .laneStraightOrTurnUsingStraight(laneStraightOrTurnUsingStraight)
            .laneStraightOrTurnUsingTurn(laneStraightOrTurnUsingTurn)
            .laneStraightOrUturn(laneStraightOrUturn)
            .laneStraightOrUturnUsingStraight(laneStraightOrUturnUsingStraight)
            .laneStraightOrUturnUsingUturn(laneStraightOrUturnUsingUturn)
            .laneStraightUsingStraight(laneStraightUsingStraight)
            .laneTurn(laneTurn)
            .laneTurnOrSharpTurn(laneTurnOrSharpTurn)
            .laneTurnOrSharpTurnUsingSharpTurn(laneTurnOrSharpTurnUsingSharpTurn)
            .laneTurnOrSharpTurnUsingTurn(laneTurnOrSharpTurnUsingTurn)
            .laneTurnOrUturn(laneTurnOrUturn)
            .laneTurnOrUturnUsingTurn(laneTurnOrUturnUsingTurn)
            .laneTurnOrUturnUsingUturn(laneTurnOrUturnUsingUturn)
            .laneTurnUsingTurn(laneTurnUsingTurn)
            .laneUturn(laneUturn)
            .laneUturnUsingUturn(laneUturnUsingUturn)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LaneIconResources

        if (laneOppositeSlightTurnOrSlightTurn !=
            other.laneOppositeSlightTurnOrSlightTurn
        ) {
            return false
        }
        if (laneOppositeSlightTurnOrSlightTurnUsingSlightTurn !=
            other.laneOppositeSlightTurnOrSlightTurnUsingSlightTurn
        ) {
            return false
        }
        if (laneOppositeSlightTurnOrStraightOrSlightTurn !=
            other.laneOppositeSlightTurnOrStraightOrSlightTurn
        ) {
            return false
        }
        if (laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn !=
            other.laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn
        ) {
            return false
        }
        if (laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight !=
            other.laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight
        ) {
            return false
        }
        if (laneOppositeSlightTurnOrStraightOrTurn !=
            other.laneOppositeSlightTurnOrStraightOrTurn
        ) {
            return false
        }
        if (laneOppositeSlightTurnOrStraightOrTurnUsingStraight !=
            other.laneOppositeSlightTurnOrStraightOrTurnUsingStraight
        ) {
            return false
        }
        if (laneOppositeSlightTurnOrStraightOrTurnUsingTurn !=
            other.laneOppositeSlightTurnOrStraightOrTurnUsingTurn
        ) {
            return false
        }
        if (laneOppositeSlightTurnOrTurn !=
            other.laneOppositeSlightTurnOrTurn
        ) {
            return false
        }
        if (laneOppositeSlightTurnOrTurnUsingTurn !=
            other.laneOppositeSlightTurnOrTurnUsingTurn
        ) {
            return false
        }
        if (laneOppositeTurnOrSlightTurn !=
            other.laneOppositeTurnOrSlightTurn
        ) {
            return false
        }
        if (laneOppositeTurnOrSlightTurnUsingSlightTurn !=
            other.laneOppositeTurnOrSlightTurnUsingSlightTurn
        ) {
            return false
        }
        if (laneOppositeTurnOrStraightOrSlightTurn !=
            other.laneOppositeTurnOrStraightOrSlightTurn
        ) {
            return false
        }
        if (laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn !=
            other.laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn
        ) {
            return false
        }
        if (laneOppositeTurnOrStraightOrSlightTurnUsingStraight !=
            other.laneOppositeTurnOrStraightOrSlightTurnUsingStraight
        ) {
            return false
        }
        if (laneOppositeTurnOrStraightOrTurn !=
            other.laneOppositeTurnOrStraightOrTurn
        ) {
            return false
        }
        if (laneOppositeTurnOrStraightOrTurnUsingStraight !=
            other.laneOppositeTurnOrStraightOrTurnUsingStraight
        ) {
            return false
        }
        if (laneOppositeTurnOrStraightOrTurnUsingTurn !=
            other.laneOppositeTurnOrStraightOrTurnUsingTurn
        ) {
            return false
        }
        if (laneOppositeTurnOrTurn !=
            other.laneOppositeTurnOrTurn
        ) {
            return false
        }
        if (laneOppositeTurnOrTurnUsingTurn !=
            other.laneOppositeTurnOrTurnUsingTurn
        ) {
            return false
        }
        if (laneSharpTurn !=
            other.laneSharpTurn
        ) {
            return false
        }
        if (laneSharpTurnUsingSharpTurn !=
            other.laneSharpTurnUsingSharpTurn
        ) {
            return false
        }
        if (laneSlightTurn !=
            other.laneSlightTurn
        ) {
            return false
        }
        if (laneSlightTurnOrSharpTurn !=
            other.laneSlightTurnOrSharpTurn
        ) {
            return false
        }
        if (laneSlightTurnOrSharpTurnUsingSharpTurn !=
            other.laneSlightTurnOrSharpTurnUsingSharpTurn
        ) {
            return false
        }
        if (laneSlightTurnOrSharpTurnUsingSlightTurn !=
            other.laneSlightTurnOrSharpTurnUsingSlightTurn
        ) {
            return false
        }
        if (laneSlightTurnOrTurn !=
            other.laneSlightTurnOrTurn
        ) {
            return false
        }
        if (laneSlightTurnOrTurnUsingSlightTurn !=
            other.laneSlightTurnOrTurnUsingSlightTurn
        ) {
            return false
        }
        if (laneSlightTurnOrTurnUsingTurn !=
            other.laneSlightTurnOrTurnUsingTurn
        ) {
            return false
        }
        if (laneSlightTurnOrUturn !=
            other.laneSlightTurnOrUturn
        ) {
            return false
        }
        if (laneSlightTurnOrUturnUsingSlightTurn !=
            other.laneSlightTurnOrUturnUsingSlightTurn
        ) {
            return false
        }
        if (laneSlightTurnOrUturnUsingUturn !=
            other.laneSlightTurnOrUturnUsingUturn
        ) {
            return false
        }
        if (laneSlightTurnUsingSlightTurn !=
            other.laneSlightTurnUsingSlightTurn
        ) {
            return false
        }
        if (laneStraight !=
            other.laneStraight
        ) {
            return false
        }
        if (laneStraightOrSharpTurn !=
            other.laneStraightOrSharpTurn
        ) {
            return false
        }
        if (laneStraightOrSharpTurnUsingSharpTurn !=
            other.laneStraightOrSharpTurnUsingSharpTurn
        ) {
            return false
        }
        if (laneStraightOrSharpTurnUsingStraight !=
            other.laneStraightOrSharpTurnUsingStraight
        ) {
            return false
        }
        if (laneStraightOrSlightTurn !=
            other.laneStraightOrSlightTurn
        ) {
            return false
        }
        if (laneStraightOrSlightTurnOrTurn !=
            other.laneStraightOrSlightTurnOrTurn
        ) {
            return false
        }
        if (laneStraightOrSlightTurnOrTurnUsingSlightTurn !=
            other.laneStraightOrSlightTurnOrTurnUsingSlightTurn
        ) {
            return false
        }
        if (laneStraightOrSlightTurnOrTurnUsingStraight !=
            other.laneStraightOrSlightTurnOrTurnUsingStraight
        ) {
            return false
        }
        if (laneStraightOrSlightTurnOrTurnUsingTurn !=
            other.laneStraightOrSlightTurnOrTurnUsingTurn
        ) {
            return false
        }
        if (laneStraightOrSlightTurnUsingSlightTurn !=
            other.laneStraightOrSlightTurnUsingSlightTurn
        ) {
            return false
        }
        if (laneStraightOrSlightTurnUsingStraight !=
            other.laneStraightOrSlightTurnUsingStraight
        ) {
            return false
        }
        if (laneStraightOrTurn != other.laneStraightOrTurn) return false
        if (laneStraightOrTurnOrUturn != other.laneStraightOrTurnOrUturn) return false
        if (laneStraightOrTurnOrUturnUsingStraight !=
            other.laneStraightOrTurnOrUturnUsingStraight
        ) {
            return false
        }
        if (laneStraightOrTurnOrUturnUsingTurn !=
            other.laneStraightOrTurnOrUturnUsingTurn
        ) {
            return false
        }
        if (laneStraightOrTurnOrUturnUsingUturn !=
            other.laneStraightOrTurnOrUturnUsingUturn
        ) {
            return false
        }
        if (laneStraightOrTurnUsingStraight != other.laneStraightOrTurnUsingStraight) return false
        if (laneStraightOrTurnUsingTurn != other.laneStraightOrTurnUsingTurn) return false
        if (laneStraightOrUturn != other.laneStraightOrUturn) return false
        if (laneStraightOrUturnUsingStraight != other.laneStraightOrUturnUsingStraight) return false
        if (laneStraightOrUturnUsingUturn != other.laneStraightOrUturnUsingUturn) return false
        if (laneStraightUsingStraight != other.laneStraightUsingStraight) return false
        if (laneTurn != other.laneTurn) return false
        if (laneTurnOrSharpTurn != other.laneTurnOrSharpTurn) return false
        if (laneTurnOrSharpTurnUsingSharpTurn !=
            other.laneTurnOrSharpTurnUsingSharpTurn
        ) {
            return false
        }
        if (laneTurnOrSharpTurnUsingTurn != other.laneTurnOrSharpTurnUsingTurn) return false
        if (laneTurnOrUturn != other.laneTurnOrUturn) return false
        if (laneTurnOrUturnUsingTurn != other.laneTurnOrUturnUsingTurn) return false
        if (laneTurnOrUturnUsingUturn != other.laneTurnOrUturnUsingUturn) return false
        if (laneTurnUsingTurn != other.laneTurnUsingTurn) return false
        if (laneUturn != other.laneUturn) return false
        if (laneUturnUsingUturn != other.laneUturnUsingUturn) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = laneOppositeSlightTurnOrSlightTurn
        result = 31 * result + laneOppositeSlightTurnOrSlightTurnUsingSlightTurn
        result = 31 * result + laneOppositeSlightTurnOrStraightOrSlightTurn
        result = 31 * result + laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn
        result = 31 * result + laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight
        result = 31 * result + laneOppositeSlightTurnOrStraightOrTurn
        result = 31 * result + laneOppositeSlightTurnOrStraightOrTurnUsingStraight
        result = 31 * result + laneOppositeSlightTurnOrStraightOrTurnUsingTurn
        result = 31 * result + laneOppositeSlightTurnOrTurn
        result = 31 * result + laneOppositeSlightTurnOrTurnUsingTurn
        result = 31 * result + laneOppositeTurnOrSlightTurn
        result = 31 * result + laneOppositeTurnOrSlightTurnUsingSlightTurn
        result = 31 * result + laneOppositeTurnOrStraightOrSlightTurn
        result = 31 * result + laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn
        result = 31 * result + laneOppositeTurnOrStraightOrSlightTurnUsingStraight
        result = 31 * result + laneOppositeTurnOrStraightOrTurn
        result = 31 * result + laneOppositeTurnOrStraightOrTurnUsingStraight
        result = 31 * result + laneOppositeTurnOrStraightOrTurnUsingTurn
        result = 31 * result + laneOppositeTurnOrTurn
        result = 31 * result + laneOppositeTurnOrTurnUsingTurn
        result = 31 * result + laneSharpTurn
        result = 31 * result + laneSharpTurnUsingSharpTurn
        result = 31 * result + laneSlightTurn
        result = 31 * result + laneSlightTurnOrSharpTurn
        result = 31 * result + laneSlightTurnOrSharpTurnUsingSharpTurn
        result = 31 * result + laneSlightTurnOrSharpTurnUsingSlightTurn
        result = 31 * result + laneSlightTurnOrTurn
        result = 31 * result + laneSlightTurnOrTurnUsingSlightTurn
        result = 31 * result + laneSlightTurnOrTurnUsingTurn
        result = 31 * result + laneSlightTurnOrUturn
        result = 31 * result + laneSlightTurnOrUturnUsingSlightTurn
        result = 31 * result + laneSlightTurnOrUturnUsingUturn
        result = 31 * result + laneSlightTurnUsingSlightTurn
        result = 31 * result + laneStraight
        result = 31 * result + laneStraightOrSharpTurn
        result = 31 * result + laneStraightOrSharpTurnUsingSharpTurn
        result = 31 * result + laneStraightOrSharpTurnUsingStraight
        result = 31 * result + laneStraightOrSlightTurn
        result = 31 * result + laneStraightOrSlightTurnOrTurn
        result = 31 * result + laneStraightOrSlightTurnOrTurnUsingSlightTurn
        result = 31 * result + laneStraightOrSlightTurnOrTurnUsingStraight
        result = 31 * result + laneStraightOrSlightTurnOrTurnUsingTurn
        result = 31 * result + laneStraightOrSlightTurnUsingSlightTurn
        result = 31 * result + laneStraightOrSlightTurnUsingStraight
        result = 31 * result + laneStraightOrTurn
        result = 31 * result + laneStraightOrTurnOrUturn
        result = 31 * result + laneStraightOrTurnOrUturnUsingStraight
        result = 31 * result + laneStraightOrTurnOrUturnUsingTurn
        result = 31 * result + laneStraightOrTurnOrUturnUsingUturn
        result = 31 * result + laneStraightOrTurnUsingStraight
        result = 31 * result + laneStraightOrTurnUsingTurn
        result = 31 * result + laneStraightOrUturn
        result = 31 * result + laneStraightOrUturnUsingStraight
        result = 31 * result + laneStraightOrUturnUsingUturn
        result = 31 * result + laneStraightUsingStraight
        result = 31 * result + laneTurn
        result = 31 * result + laneTurnOrSharpTurn
        result = 31 * result + laneTurnOrSharpTurnUsingSharpTurn
        result = 31 * result + laneTurnOrSharpTurnUsingTurn
        result = 31 * result + laneTurnOrUturn
        result = 31 * result + laneTurnOrUturnUsingTurn
        result = 31 * result + laneTurnOrUturnUsingUturn
        result = 31 * result + laneTurnUsingTurn
        result = 31 * result + laneUturn
        result = 31 * result + laneUturnUsingUturn
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "LaneIconResources(" +
            "laneOppositeSlightTurnOrSlightTurn=$laneOppositeSlightTurnOrSlightTurn, " +
            "laneOppositeSlightTurnOrSlightTurnUsingSlightTurn=" +
            "$laneOppositeSlightTurnOrSlightTurnUsingSlightTurn, " +
            "laneOppositeSlightTurnOrStraightOrSlightTurn=" +
            "$laneOppositeSlightTurnOrStraightOrSlightTurn, " +
            "laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn=" +
            "$laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn, " +
            "laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight=" +
            "$laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight, " +
            "laneOppositeSlightTurnOrStraightOrTurn=$laneOppositeSlightTurnOrStraightOrTurn, " +
            "laneOppositeSlightTurnOrStraightOrTurnUsingStraight=" +
            "$laneOppositeSlightTurnOrStraightOrTurnUsingStraight, " +
            "laneOppositeSlightTurnOrStraightOrTurnUsingTurn=" +
            "$laneOppositeSlightTurnOrStraightOrTurnUsingTurn, " +
            "laneOppositeSlightTurnOrTurn=$laneOppositeSlightTurnOrTurn, " +
            "laneOppositeSlightTurnOrTurnUsingTurn=$laneOppositeSlightTurnOrTurnUsingTurn, " +
            "laneOppositeTurnOrSlightTurn=$laneOppositeTurnOrSlightTurn, " +
            "laneOppositeTurnOrSlightTurnUsingSlightTurn=" +
            "$laneOppositeTurnOrSlightTurnUsingSlightTurn, " +
            "laneOppositeTurnOrStraightOrSlightTurn=$laneOppositeTurnOrStraightOrSlightTurn, " +
            "laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn=" +
            "$laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn, " +
            "laneOppositeTurnOrStraightOrSlightTurnUsingStraight=" +
            "$laneOppositeTurnOrStraightOrSlightTurnUsingStraight, " +
            "laneOppositeTurnOrStraightOrTurn=$laneOppositeTurnOrStraightOrTurn, " +
            "laneOppositeTurnOrStraightOrTurnUsingStraight=" +
            "$laneOppositeTurnOrStraightOrTurnUsingStraight, " +
            "laneOppositeTurnOrStraightOrTurnUsingTurn=" +
            "$laneOppositeTurnOrStraightOrTurnUsingTurn, " +
            "laneOppositeTurnOrTurn=$laneOppositeTurnOrTurn, " +
            "laneOppositeTurnOrTurnUsingTurn=$laneOppositeTurnOrTurnUsingTurn, " +
            "laneSharpTurn=$laneSharpTurn, " +
            "laneSharpTurnUsingSharpTurn=$laneSharpTurnUsingSharpTurn, " +
            "laneSlightTurn=$laneSlightTurn, " +
            "laneSlightTurnOrSharpTurn=$laneSlightTurnOrSharpTurn, " +
            "laneSlightTurnOrSharpTurnUsingSharpTurn=$laneSlightTurnOrSharpTurnUsingSharpTurn, " +
            "laneSlightTurnOrSharpTurnUsingSlightTurn=$laneSlightTurnOrSharpTurnUsingSlightTurn, " +
            "laneSlightTurnOrTurn=$laneSlightTurnOrTurn, " +
            "laneSlightTurnOrTurnUsingSlightTurn=$laneSlightTurnOrTurnUsingSlightTurn, " +
            "laneSlightTurnOrTurnUsingTurn=$laneSlightTurnOrTurnUsingTurn, " +
            "laneSlightTurnOrUturn=$laneSlightTurnOrUturn, " +
            "laneSlightTurnOrUturnUsingSlightTurn=$laneSlightTurnOrUturnUsingSlightTurn, " +
            "laneSlightTurnOrUturnUsingUturn=$laneSlightTurnOrUturnUsingUturn, " +
            "laneSlightTurnUsingSlightTurn=$laneSlightTurnUsingSlightTurn, " +
            "laneStraight=$laneStraight, " +
            "laneStraightOrSharpTurn=$laneStraightOrSharpTurn, " +
            "laneStraightOrSharpTurnUsingSharpTurn=$laneStraightOrSharpTurnUsingSharpTurn, " +
            "laneStraightOrSharpTurnUsingStraight=$laneStraightOrSharpTurnUsingStraight, " +
            "laneStraightOrSlightTurn=$laneStraightOrSlightTurn, " +
            "laneStraightOrSlightTurnOrTurn=$laneStraightOrSlightTurnOrTurn, " +
            "laneStraightOrSlightTurnOrTurnUsingSlightTurn=" +
            "$laneStraightOrSlightTurnOrTurnUsingSlightTurn, " +
            "laneStraightOrSlightTurnOrTurnUsingStraight=" +
            "$laneStraightOrSlightTurnOrTurnUsingStraight, " +
            "laneStraightOrSlightTurnOrTurnUsingTurn=$laneStraightOrSlightTurnOrTurnUsingTurn, " +
            "laneStraightOrSlightTurnUsingSlightTurn=$laneStraightOrSlightTurnUsingSlightTurn, " +
            "laneStraightOrSlightTurnUsingStraight=$laneStraightOrSlightTurnUsingStraight, " +
            "laneStraightOrTurn=$laneStraightOrTurn, " +
            "laneStraightOrTurnOrUturn=$laneStraightOrTurnOrUturn, " +
            "laneStraightOrTurnOrUturnUsingStraight=$laneStraightOrTurnOrUturnUsingStraight, " +
            "laneStraightOrTurnOrUturnUsingTurn=$laneStraightOrTurnOrUturnUsingTurn, " +
            "laneStraightOrTurnOrUturnUsingUturn=$laneStraightOrTurnOrUturnUsingUturn, " +
            "laneStraightOrTurnUsingStraight=$laneStraightOrTurnUsingStraight, " +
            "laneStraightOrTurnUsingTurn=$laneStraightOrTurnUsingTurn, " +
            "laneStraightOrUturn=$laneStraightOrUturn, " +
            "laneStraightOrUturnUsingStraight=$laneStraightOrUturnUsingStraight, " +
            "laneStraightOrUturnUsingUturn=$laneStraightOrUturnUsingUturn, " +
            "laneStraightUsingStraight=$laneStraightUsingStraight, " +
            "laneTurn=$laneTurn, " +
            "laneTurnOrSharpTurn=$laneTurnOrSharpTurn, " +
            "laneTurnOrSharpTurnUsingSharpTurn=$laneTurnOrSharpTurnUsingSharpTurn, " +
            "laneTurnOrSharpTurnUsingTurn=$laneTurnOrSharpTurnUsingTurn, " +
            "laneTurnOrUturn=$laneTurnOrUturn, " +
            "laneTurnOrUturnUsingTurn=$laneTurnOrUturnUsingTurn, " +
            "laneTurnOrUturnUsingUturn=$laneTurnOrUturnUsingUturn, " +
            "laneTurnUsingTurn=$laneTurnUsingTurn, " +
            "laneUturn=$laneUturn, " +
            "laneUturnUsingUturn=$laneUturnUsingUturn" +
            ")"
    }

    /**
     * Build a new [LaneIconResources]
     * @property laneOppositeSlightTurnOrSlightTurn Int
     * @property laneOppositeSlightTurnOrSlightTurnUsingSlightTurn Int
     * @property laneOppositeSlightTurnOrStraightOrSlightTurn Int
     * @property laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn Int
     * @property laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight Int
     * @property laneOppositeSlightTurnOrStraightOrTurn Int
     * @property laneOppositeSlightTurnOrStraightOrTurnUsingStraight Int
     * @property laneOppositeSlightTurnOrStraightOrTurnUsingTurn Int
     * @property laneOppositeSlightTurnOrTurn Int
     * @property laneOppositeSlightTurnOrTurnUsingTurn Int
     * @property laneOppositeTurnOrSlightTurn Int
     * @property laneOppositeTurnOrSlightTurnUsingSlightTurn Int
     * @property laneOppositeTurnOrStraightOrSlightTurn Int
     * @property laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn Int
     * @property laneOppositeTurnOrStraightOrSlightTurnUsingStraight Int
     * @property laneOppositeTurnOrStraightOrTurn Int
     * @property laneOppositeTurnOrStraightOrTurnUsingStraight Int
     * @property laneOppositeTurnOrStraightOrTurnUsingTurn Int
     * @property laneOppositeTurnOrTurn Int
     * @property laneOppositeTurnOrTurnUsingTurn Int
     * @property laneSharpTurn Int
     * @property laneSharpTurnUsingSharpTurn Int
     * @property laneSlightTurn Int
     * @property laneSlightTurnOrSharpTurn Int
     * @property laneSlightTurnOrSharpTurnUsingSharpTurn Int
     * @property laneSlightTurnOrSharpTurnUsingSlightTurn Int
     * @property laneSlightTurnOrTurn Int
     * @property laneSlightTurnOrTurnUsingSlightTurn Int
     * @property laneSlightTurnOrTurnUsingTurn Int
     * @property laneSlightTurnOrUturn Int
     * @property laneSlightTurnOrUturnUsingSlightTurn Int
     * @property laneSlightTurnOrUturnUsingUturn Int
     * @property laneSlightTurnUsingSlightTurn Int
     * @property laneStraight Int
     * @property laneStraightOrSharpTurn Int
     * @property laneStraightOrSharpTurnUsingSharpTurn Int
     * @property laneStraightOrSharpTurnUsingStraight Int
     * @property laneStraightOrSlightTurn Int
     * @property laneStraightOrSlightTurnOrTurn Int
     * @property laneStraightOrSlightTurnOrTurnUsingSlightTurn Int
     * @property laneStraightOrSlightTurnOrTurnUsingStraight Int
     * @property laneStraightOrSlightTurnOrTurnUsingTurn Int
     * @property laneStraightOrSlightTurnUsingSlightTurn Int
     * @property laneStraightOrSlightTurnUsingStraight Int
     * @property laneStraightOrTurn Int
     * @property laneStraightOrTurnOrUturn Int
     * @property laneStraightOrTurnOrUturnUsingStraight Int
     * @property laneStraightOrTurnOrUturnUsingTurn Int
     * @property laneStraightOrTurnOrUturnUsingUturn Int
     * @property laneStraightOrTurnUsingStraight Int
     * @property laneStraightOrTurnUsingTurn Int
     * @property laneStraightOrUturn Int
     * @property laneStraightOrUturnUsingStraight Int
     * @property laneStraightOrUturnUsingUturn Int
     * @property laneStraightUsingStraight Int
     * @property laneTurn Int
     * @property laneTurnOrSharpTurn Int
     * @property laneTurnOrSharpTurnUsingSharpTurn Int
     * @property laneTurnOrSharpTurnUsingTurn Int
     * @property laneTurnOrUturn Int
     * @property laneTurnOrUturnUsingTurn Int
     * @property laneTurnOrUturnUsingUturn Int
     * @property laneTurnUsingTurn Int
     * @property laneUturn Int
     * @property laneUturnUsingUturn Int
     */
    class Builder {
        private var laneOppositeSlightTurnOrSlightTurn: Int =
            R.drawable.mapbox_lane_opposite_slight_turn_or_slight_turn
        private var laneOppositeSlightTurnOrSlightTurnUsingSlightTurn: Int =
            R.drawable.mapbox_lane_opposite_slight_turn_or_slight_turn_using_slight_turn
        private var laneOppositeSlightTurnOrStraightOrSlightTurn: Int =
            R.drawable.mapbox_lane_opposite_slight_turn_or_straight_or_slight_turn
        private var laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn: Int =
            R.drawable.mapbox_lane_opposite_slight_turn_or_straight_or_slight_turn_using_slight_turn
        private var laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight: Int =
            R.drawable.mapbox_lane_opposite_slight_turn_or_straight_or_slight_turn_using_straight
        private var laneOppositeSlightTurnOrStraightOrTurn: Int =
            R.drawable.mapbox_lane_opposite_slight_turn_or_straight_or_turn
        private var laneOppositeSlightTurnOrStraightOrTurnUsingStraight: Int =
            R.drawable.mapbox_lane_opposite_slight_turn_or_straight_or_turn_using_straight
        private var laneOppositeSlightTurnOrStraightOrTurnUsingTurn: Int =
            R.drawable.mapbox_lane_opposite_slight_turn_or_straight_or_turn_using_turn
        private var laneOppositeSlightTurnOrTurn: Int =
            R.drawable.mapbox_lane_opposite_slight_turn_or_turn
        private var laneOppositeSlightTurnOrTurnUsingTurn: Int =
            R.drawable.mapbox_lane_opposite_slight_turn_or_turn_using_turn
        private var laneOppositeTurnOrSlightTurn: Int =
            R.drawable.mapbox_lane_opposite_turn_or_slight_turn
        private var laneOppositeTurnOrSlightTurnUsingSlightTurn: Int =
            R.drawable.mapbox_lane_opposite_turn_or_slight_turn_using_slight_turn
        private var laneOppositeTurnOrStraightOrSlightTurn: Int =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_slight_turn
        private var laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn: Int =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_slight_turn_using_slight_turn
        private var laneOppositeTurnOrStraightOrSlightTurnUsingStraight: Int =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_slight_turn_using_straight
        private var laneOppositeTurnOrStraightOrTurn: Int =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_turn
        private var laneOppositeTurnOrStraightOrTurnUsingStraight: Int =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_turn_using_straight
        private var laneOppositeTurnOrStraightOrTurnUsingTurn: Int =
            R.drawable.mapbox_lane_opposite_turn_or_straight_or_turn_using_turn
        private var laneOppositeTurnOrTurn: Int =
            R.drawable.mapbox_lane_opposite_turn_or_turn
        private var laneOppositeTurnOrTurnUsingTurn: Int =
            R.drawable.mapbox_lane_opposite_turn_or_turn_using_turn
        private var laneSharpTurn: Int =
            R.drawable.mapbox_lane_sharp_turn
        private var laneSharpTurnUsingSharpTurn: Int =
            R.drawable.mapbox_lane_sharp_turn_using_sharp_turn
        private var laneSlightTurn: Int =
            R.drawable.mapbox_lane_slight_turn
        private var laneSlightTurnOrSharpTurn: Int =
            R.drawable.mapbox_lane_slight_turn_or_sharp_turn
        private var laneSlightTurnOrSharpTurnUsingSharpTurn: Int =
            R.drawable.mapbox_lane_slight_turn_or_sharp_turn_using_sharp_turn
        private var laneSlightTurnOrSharpTurnUsingSlightTurn: Int =
            R.drawable.mapbox_lane_slight_turn_or_sharp_turn_using_slight_turn
        private var laneSlightTurnOrTurn: Int =
            R.drawable.mapbox_lane_slight_turn_or_turn
        private var laneSlightTurnOrTurnUsingSlightTurn: Int =
            R.drawable.mapbox_lane_slight_turn_or_turn_using_slight_turn
        private var laneSlightTurnOrTurnUsingTurn: Int =
            R.drawable.mapbox_lane_slight_turn_or_turn_using_turn
        private var laneSlightTurnOrUturn: Int =
            R.drawable.mapbox_lane_slight_turn_or_uturn
        private var laneSlightTurnOrUturnUsingSlightTurn: Int =
            R.drawable.mapbox_lane_slight_turn_or_uturn_using_slight_turn
        private var laneSlightTurnOrUturnUsingUturn: Int =
            R.drawable.mapbox_lane_slight_turn_or_uturn_using_uturn
        private var laneSlightTurnUsingSlightTurn: Int =
            R.drawable.mapbox_lane_slight_turn_using_slight_turn
        private var laneStraight: Int =
            R.drawable.mapbox_lane_straight
        private var laneStraightOrSharpTurn: Int =
            R.drawable.mapbox_lane_straight_or_sharp_turn
        private var laneStraightOrSharpTurnUsingSharpTurn: Int =
            R.drawable.mapbox_lane_straight_or_sharp_turn_using_sharp_turn
        private var laneStraightOrSharpTurnUsingStraight: Int =
            R.drawable.mapbox_lane_straight_or_sharp_turn_using_straight
        private var laneStraightOrSlightTurn: Int =
            R.drawable.mapbox_lane_straight_or_slight_turn
        private var laneStraightOrSlightTurnOrTurn: Int =
            R.drawable.mapbox_lane_straight_or_slight_turn_or_turn
        private var laneStraightOrSlightTurnOrTurnUsingSlightTurn: Int =
            R.drawable.mapbox_lane_straight_or_slight_turn_or_turn_using_slight_turn
        private var laneStraightOrSlightTurnOrTurnUsingStraight: Int =
            R.drawable.mapbox_lane_straight_or_slight_turn_or_turn_using_straight
        private var laneStraightOrSlightTurnOrTurnUsingTurn: Int =
            R.drawable.mapbox_lane_straight_or_slight_turn_or_turn_using_turn
        private var laneStraightOrSlightTurnUsingSlightTurn: Int =
            R.drawable.mapbox_lane_straight_or_slight_turn_using_slight_turn
        private var laneStraightOrSlightTurnUsingStraight: Int =
            R.drawable.mapbox_lane_straight_or_slight_turn_using_straight
        private var laneStraightOrTurn: Int =
            R.drawable.mapbox_lane_straight_or_turn
        private var laneStraightOrTurnOrUturn: Int =
            R.drawable.mapbox_lane_straight_or_turn_or_uturn
        private var laneStraightOrTurnOrUturnUsingStraight: Int =
            R.drawable.mapbox_lane_straight_or_turn_or_uturn_using_straight
        private var laneStraightOrTurnOrUturnUsingTurn: Int =
            R.drawable.mapbox_lane_straight_or_turn_or_uturn_using_turn
        private var laneStraightOrTurnOrUturnUsingUturn: Int =
            R.drawable.mapbox_lane_straight_or_turn_or_uturn_using_uturn
        private var laneStraightOrTurnUsingStraight: Int =
            R.drawable.mapbox_lane_straight_or_turn_using_straight
        private var laneStraightOrTurnUsingTurn: Int =
            R.drawable.mapbox_lane_straight_or_turn_using_turn
        private var laneStraightOrUturn: Int =
            R.drawable.mapbox_lane_straight_or_uturn
        private var laneStraightOrUturnUsingStraight: Int =
            R.drawable.mapbox_lane_straight_or_uturn_using_straight
        private var laneStraightOrUturnUsingUturn: Int =
            R.drawable.mapbox_lane_straight_or_uturn_using_uturn
        private var laneStraightUsingStraight: Int =
            R.drawable.mapbox_lane_straight_using_straight
        private var laneTurn: Int = R.drawable.mapbox_lane_turn
        private var laneTurnOrSharpTurn: Int =
            R.drawable.mapbox_lane_turn_or_sharp_turn
        private var laneTurnOrSharpTurnUsingSharpTurn: Int =
            R.drawable.mapbox_lane_turn_or_sharp_turn_using_sharp_turn
        private var laneTurnOrSharpTurnUsingTurn: Int =
            R.drawable.mapbox_lane_turn_or_sharp_turn_using_turn
        private var laneTurnOrUturn: Int =
            R.drawable.mapbox_lane_turn_or_uturn
        private var laneTurnOrUturnUsingTurn: Int =
            R.drawable.mapbox_lane_turn_or_uturn_using_turn
        private var laneTurnOrUturnUsingUturn: Int =
            R.drawable.mapbox_lane_turn_or_uturn_using_uturn
        private var laneTurnUsingTurn: Int =
            R.drawable.mapbox_lane_turn_using_turn
        private var laneUturn: Int =
            R.drawable.mapbox_lane_uturn
        private var laneUturnUsingUturn: Int =
            R.drawable.mapbox_lane_uturn_using_uturn

        /**
         * apply icon to the builder.
         * @param laneOppositeSlightTurnOrSlightTurn Int
         * @return Builder
         */
        fun laneOppositeSlightTurnOrSlightTurn(
            @DrawableRes laneOppositeSlightTurnOrSlightTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeSlightTurnOrSlightTurn =
                    laneOppositeSlightTurnOrSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeSlightTurnOrSlightTurnUsingSlightTurn Int
         * @return Builder
         */
        fun laneOppositeSlightTurnOrSlightTurnUsingSlightTurn(
            @DrawableRes laneOppositeSlightTurnOrSlightTurnUsingSlightTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeSlightTurnOrSlightTurnUsingSlightTurn =
                    laneOppositeSlightTurnOrSlightTurnUsingSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeSlightTurnOrStraightOrSlightTurn Int
         * @return Builder
         */
        fun laneOppositeSlightTurnOrStraightOrSlightTurn(
            @DrawableRes laneOppositeSlightTurnOrStraightOrSlightTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeSlightTurnOrStraightOrSlightTurn =
                    laneOppositeSlightTurnOrStraightOrSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn Int
         * @return Builder
         */
        fun laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn(
            @DrawableRes laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn =
                    laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight Int
         * @return Builder
         */
        fun laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight(
            @DrawableRes laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight: Int,
        ): Builder =
            apply {
                this.laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight =
                    laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeSlightTurnOrStraightOrTurn Int
         * @return Builder
         */
        fun laneOppositeSlightTurnOrStraightOrTurn(
            @DrawableRes laneOppositeSlightTurnOrStraightOrTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeSlightTurnOrStraightOrTurn =
                    laneOppositeSlightTurnOrStraightOrTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeSlightTurnOrStraightOrTurnUsingStraight Int
         * @return Builder
         */
        fun laneOppositeSlightTurnOrStraightOrTurnUsingStraight(
            @DrawableRes laneOppositeSlightTurnOrStraightOrTurnUsingStraight: Int,
        ): Builder =
            apply {
                this.laneOppositeSlightTurnOrStraightOrTurnUsingStraight =
                    laneOppositeSlightTurnOrStraightOrTurnUsingStraight
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeSlightTurnOrStraightOrTurnUsingTurn Int
         * @return Builder
         */
        fun laneOppositeSlightTurnOrStraightOrTurnUsingTurn(
            @DrawableRes laneOppositeSlightTurnOrStraightOrTurnUsingTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeSlightTurnOrStraightOrTurnUsingTurn =
                    laneOppositeSlightTurnOrStraightOrTurnUsingTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeSlightTurnOrTurn Int
         * @return Builder
         */
        fun laneOppositeSlightTurnOrTurn(
            @DrawableRes laneOppositeSlightTurnOrTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeSlightTurnOrTurn =
                    laneOppositeSlightTurnOrTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeSlightTurnOrTurnUsingTurn Int
         * @return Builder
         */
        fun laneOppositeSlightTurnOrTurnUsingTurn(
            @DrawableRes laneOppositeSlightTurnOrTurnUsingTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeSlightTurnOrTurnUsingTurn =
                    laneOppositeSlightTurnOrTurnUsingTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeTurnOrSlightTurn Int
         * @return Builder
         */
        fun laneOppositeTurnOrSlightTurn(
            @DrawableRes laneOppositeTurnOrSlightTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeTurnOrSlightTurn =
                    laneOppositeTurnOrSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeTurnOrSlightTurnUsingSlightTurn Int
         * @return Builder
         */
        fun laneOppositeTurnOrSlightTurnUsingSlightTurn(
            @DrawableRes laneOppositeTurnOrSlightTurnUsingSlightTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeTurnOrSlightTurnUsingSlightTurn =
                    laneOppositeTurnOrSlightTurnUsingSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeTurnOrStraightOrSlightTurn Int
         * @return Builder
         */
        fun laneOppositeTurnOrStraightOrSlightTurn(
            @DrawableRes laneOppositeTurnOrStraightOrSlightTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeTurnOrStraightOrSlightTurn =
                    laneOppositeTurnOrStraightOrSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn Int
         * @return Builder
         */
        fun laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn(
            @DrawableRes laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn =
                    laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeTurnOrStraightOrSlightTurnUsingStraight Int
         * @return Builder
         */
        fun laneOppositeTurnOrStraightOrSlightTurnUsingStraight(
            @DrawableRes laneOppositeTurnOrStraightOrSlightTurnUsingStraight: Int,
        ): Builder =
            apply {
                this.laneOppositeTurnOrStraightOrSlightTurnUsingStraight =
                    laneOppositeTurnOrStraightOrSlightTurnUsingStraight
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeTurnOrStraightOrTurn Int
         * @return Builder
         */
        fun laneOppositeTurnOrStraightOrTurn(
            @DrawableRes laneOppositeTurnOrStraightOrTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeTurnOrStraightOrTurn =
                    laneOppositeTurnOrStraightOrTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeTurnOrStraightOrTurnUsingStraight Int
         * @return Builder
         */
        fun laneOppositeTurnOrStraightOrTurnUsingStraight(
            @DrawableRes laneOppositeTurnOrStraightOrTurnUsingStraight: Int,
        ): Builder =
            apply {
                this.laneOppositeTurnOrStraightOrTurnUsingStraight =
                    laneOppositeTurnOrStraightOrTurnUsingStraight
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeTurnOrStraightOrTurnUsingTurn Int
         * @return Builder
         */
        fun laneOppositeTurnOrStraightOrTurnUsingTurn(
            @DrawableRes laneOppositeTurnOrStraightOrTurnUsingTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeTurnOrStraightOrTurnUsingTurn =
                    laneOppositeTurnOrStraightOrTurnUsingTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeTurnOrTurn Int
         * @return Builder
         */
        fun laneOppositeTurnOrTurn(
            @DrawableRes laneOppositeTurnOrTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeTurnOrTurn =
                    laneOppositeTurnOrTurn
            }

        /**
         * apply icon to the builder.
         * @param laneOppositeTurnOrTurnUsingTurn Int
         * @return Builder
         */
        fun laneOppositeTurnOrTurnUsingTurn(
            @DrawableRes laneOppositeTurnOrTurnUsingTurn: Int,
        ): Builder =
            apply {
                this.laneOppositeTurnOrTurnUsingTurn =
                    laneOppositeTurnOrTurnUsingTurn
            }

        /**
         * apply icon to the builder.
         * @param laneSharpTurn Int
         * @return Builder
         */
        fun laneSharpTurn(
            @DrawableRes laneSharpTurn: Int,
        ): Builder =
            apply {
                this.laneSharpTurn =
                    laneSharpTurn
            }

        /**
         * apply icon to the builder.
         * @param laneSharpTurnUsingSharpTurn Int
         * @return Builder
         */
        fun laneSharpTurnUsingSharpTurn(
            @DrawableRes laneSharpTurnUsingSharpTurn: Int,
        ): Builder =
            apply {
                this.laneSharpTurnUsingSharpTurn =
                    laneSharpTurnUsingSharpTurn
            }

        /**
         * apply icon to the builder.
         * @param laneSlightTurn Int
         * @return Builder
         */
        fun laneSlightTurn(
            @DrawableRes laneSlightTurn: Int,
        ): Builder =
            apply {
                this.laneSlightTurn =
                    laneSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneSlightTurnOrSharpTurn Int
         * @return Builder
         */
        fun laneSlightTurnOrSharpTurn(
            @DrawableRes laneSlightTurnOrSharpTurn: Int,
        ): Builder =
            apply {
                this.laneSlightTurnOrSharpTurn =
                    laneSlightTurnOrSharpTurn
            }

        /**
         * apply icon to the builder.
         * @param laneSlightTurnOrSharpTurnUsingSharpTurn Int
         * @return Builder
         */
        fun laneSlightTurnOrSharpTurnUsingSharpTurn(
            @DrawableRes laneSlightTurnOrSharpTurnUsingSharpTurn: Int,
        ): Builder =
            apply {
                this.laneSlightTurnOrSharpTurnUsingSharpTurn =
                    laneSlightTurnOrSharpTurnUsingSharpTurn
            }

        /**
         * apply icon to the builder.
         * @param laneSlightTurnOrSharpTurnUsingSlightTurn Int
         * @return Builder
         */
        fun laneSlightTurnOrSharpTurnUsingSlightTurn(
            @DrawableRes laneSlightTurnOrSharpTurnUsingSlightTurn: Int,
        ): Builder =
            apply {
                this.laneSlightTurnOrSharpTurnUsingSlightTurn =
                    laneSlightTurnOrSharpTurnUsingSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneSlightTurnOrTurn Int
         * @return Builder
         */
        fun laneSlightTurnOrTurn(
            @DrawableRes laneSlightTurnOrTurn: Int,
        ): Builder =
            apply {
                this.laneSlightTurnOrTurn =
                    laneSlightTurnOrTurn
            }

        /**
         * apply icon to the builder.
         * @param laneSlightTurnOrTurnUsingSlightTurn Int
         * @return Builder
         */
        fun laneSlightTurnOrTurnUsingSlightTurn(
            @DrawableRes laneSlightTurnOrTurnUsingSlightTurn: Int,
        ): Builder =
            apply {
                this.laneSlightTurnOrTurnUsingSlightTurn =
                    laneSlightTurnOrTurnUsingSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneSlightTurnOrTurnUsingTurn Int
         * @return Builder
         */
        fun laneSlightTurnOrTurnUsingTurn(
            @DrawableRes laneSlightTurnOrTurnUsingTurn: Int,
        ): Builder =
            apply {
                this.laneSlightTurnOrTurnUsingTurn =
                    laneSlightTurnOrTurnUsingTurn
            }

        /**
         * apply icon to the builder.
         * @param laneSlightTurnOrUturn Int
         * @return Builder
         */
        fun laneSlightTurnOrUturn(
            @DrawableRes laneSlightTurnOrUturn: Int,
        ): Builder =
            apply {
                this.laneSlightTurnOrUturn =
                    laneSlightTurnOrUturn
            }

        /**
         * apply icon to the builder.
         * @param laneSlightTurnOrUturnUsingSlightTurn Int
         * @return Builder
         */
        fun laneSlightTurnOrUturnUsingSlightTurn(
            @DrawableRes laneSlightTurnOrUturnUsingSlightTurn: Int,
        ): Builder =
            apply {
                this.laneSlightTurnOrUturnUsingSlightTurn =
                    laneSlightTurnOrUturnUsingSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneSlightTurnOrUturnUsingUturn Int
         * @return Builder
         */
        fun laneSlightTurnOrUturnUsingUturn(
            @DrawableRes laneSlightTurnOrUturnUsingUturn: Int,
        ): Builder =
            apply {
                this.laneSlightTurnOrUturnUsingUturn =
                    laneSlightTurnOrUturnUsingUturn
            }

        /**
         * apply icon to the builder.
         * @param laneSlightTurnUsingSlightTurn Int
         * @return Builder
         */
        fun laneSlightTurnUsingSlightTurn(
            @DrawableRes laneSlightTurnUsingSlightTurn: Int,
        ): Builder =
            apply {
                this.laneSlightTurnUsingSlightTurn =
                    laneSlightTurnUsingSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneStraight Int
         * @return Builder
         */
        fun laneStraight(
            @DrawableRes laneStraight: Int,
        ): Builder =
            apply {
                this.laneStraight =
                    laneStraight
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrSharpTurn Int
         * @return Builder
         */
        fun laneStraightOrSharpTurn(
            @DrawableRes laneStraightOrSharpTurn: Int,
        ): Builder =
            apply {
                this.laneStraightOrSharpTurn =
                    laneStraightOrSharpTurn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrSharpTurnUsingSharpTurn Int
         * @return Builder
         */
        fun laneStraightOrSharpTurnUsingSharpTurn(
            @DrawableRes laneStraightOrSharpTurnUsingSharpTurn: Int,
        ): Builder =
            apply {
                this.laneStraightOrSharpTurnUsingSharpTurn =
                    laneStraightOrSharpTurnUsingSharpTurn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrSharpTurnUsingStraight Int
         * @return Builder
         */
        fun laneStraightOrSharpTurnUsingStraight(
            @DrawableRes laneStraightOrSharpTurnUsingStraight: Int,
        ): Builder =
            apply {
                this.laneStraightOrSharpTurnUsingStraight =
                    laneStraightOrSharpTurnUsingStraight
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrSlightTurn Int
         * @return Builder
         */
        fun laneStraightOrSlightTurn(
            @DrawableRes laneStraightOrSlightTurn: Int,
        ): Builder =
            apply {
                this.laneStraightOrSlightTurn =
                    laneStraightOrSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrSlightTurnOrTurn Int
         * @return Builder
         */
        fun laneStraightOrSlightTurnOrTurn(
            @DrawableRes laneStraightOrSlightTurnOrTurn: Int,
        ): Builder =
            apply {
                this.laneStraightOrSlightTurnOrTurn =
                    laneStraightOrSlightTurnOrTurn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrSlightTurnOrTurnUsingSlightTurn Int
         * @return Builder
         */
        fun laneStraightOrSlightTurnOrTurnUsingSlightTurn(
            @DrawableRes laneStraightOrSlightTurnOrTurnUsingSlightTurn: Int,
        ): Builder =
            apply {
                this.laneStraightOrSlightTurnOrTurnUsingSlightTurn =
                    laneStraightOrSlightTurnOrTurnUsingSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrSlightTurnOrTurnUsingStraight Int
         * @return Builder
         */
        fun laneStraightOrSlightTurnOrTurnUsingStraight(
            @DrawableRes laneStraightOrSlightTurnOrTurnUsingStraight: Int,
        ): Builder =
            apply {
                this.laneStraightOrSlightTurnOrTurnUsingStraight =
                    laneStraightOrSlightTurnOrTurnUsingStraight
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrSlightTurnOrTurnUsingTurn Int
         * @return Builder
         */
        fun laneStraightOrSlightTurnOrTurnUsingTurn(
            @DrawableRes laneStraightOrSlightTurnOrTurnUsingTurn: Int,
        ): Builder =
            apply {
                this.laneStraightOrSlightTurnOrTurnUsingTurn =
                    laneStraightOrSlightTurnOrTurnUsingTurn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrSlightTurnUsingSlightTurn Int
         * @return Builder
         */
        fun laneStraightOrSlightTurnUsingSlightTurn(
            @DrawableRes laneStraightOrSlightTurnUsingSlightTurn: Int,
        ): Builder =
            apply {
                this.laneStraightOrSlightTurnUsingSlightTurn =
                    laneStraightOrSlightTurnUsingSlightTurn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrSlightTurnUsingStraight Int
         * @return Builder
         */
        fun laneStraightOrSlightTurnUsingStraight(
            @DrawableRes laneStraightOrSlightTurnUsingStraight: Int,
        ): Builder =
            apply {
                this.laneStraightOrSlightTurnUsingStraight =
                    laneStraightOrSlightTurnUsingStraight
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrTurn Int
         * @return Builder
         */
        fun laneStraightOrTurn(
            @DrawableRes laneStraightOrTurn: Int,
        ): Builder =
            apply {
                this.laneStraightOrTurn =
                    laneStraightOrTurn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrTurnOrUturn Int
         * @return Builder
         */
        fun laneStraightOrTurnOrUturn(
            @DrawableRes laneStraightOrTurnOrUturn: Int,
        ): Builder =
            apply {
                this.laneStraightOrTurnOrUturn =
                    laneStraightOrTurnOrUturn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrTurnOrUturnUsingStraight Int
         * @return Builder
         */
        fun laneStraightOrTurnOrUturnUsingStraight(
            @DrawableRes laneStraightOrTurnOrUturnUsingStraight: Int,
        ): Builder =
            apply {
                this.laneStraightOrTurnOrUturnUsingStraight =
                    laneStraightOrTurnOrUturnUsingStraight
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrTurnOrUturnUsingTurn Int
         * @return Builder
         */
        fun laneStraightOrTurnOrUturnUsingTurn(
            @DrawableRes laneStraightOrTurnOrUturnUsingTurn: Int,
        ): Builder =
            apply {
                this.laneStraightOrTurnOrUturnUsingTurn =
                    laneStraightOrTurnOrUturnUsingTurn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrTurnOrUturnUsingUturn Int
         * @return Builder
         */
        fun laneStraightOrTurnOrUturnUsingUturn(
            @DrawableRes laneStraightOrTurnOrUturnUsingUturn: Int,
        ): Builder =
            apply {
                this.laneStraightOrTurnOrUturnUsingUturn =
                    laneStraightOrTurnOrUturnUsingUturn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrTurnUsingStraight Int
         * @return Builder
         */
        fun laneStraightOrTurnUsingStraight(
            @DrawableRes laneStraightOrTurnUsingStraight: Int,
        ): Builder =
            apply {
                this.laneStraightOrTurnUsingStraight =
                    laneStraightOrTurnUsingStraight
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrTurnUsingTurn Int
         * @return Builder
         */
        fun laneStraightOrTurnUsingTurn(
            @DrawableRes laneStraightOrTurnUsingTurn: Int,
        ): Builder =
            apply {
                this.laneStraightOrTurnUsingTurn =
                    laneStraightOrTurnUsingTurn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrUturn Int
         * @return Builder
         */
        fun laneStraightOrUturn(
            @DrawableRes laneStraightOrUturn: Int,
        ): Builder =
            apply {
                this.laneStraightOrUturn =
                    laneStraightOrUturn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrUturnUsingStraight Int
         * @return Builder
         */
        fun laneStraightOrUturnUsingStraight(
            @DrawableRes laneStraightOrUturnUsingStraight: Int,
        ): Builder =
            apply {
                this.laneStraightOrUturnUsingStraight =
                    laneStraightOrUturnUsingStraight
            }

        /**
         * apply icon to the builder.
         * @param laneStraightOrUturnUsingUturn Int
         * @return Builder
         */
        fun laneStraightOrUturnUsingUturn(
            @DrawableRes laneStraightOrUturnUsingUturn: Int,
        ): Builder =
            apply {
                this.laneStraightOrUturnUsingUturn =
                    laneStraightOrUturnUsingUturn
            }

        /**
         * apply icon to the builder.
         * @param laneStraightUsingStraight Int
         * @return Builder
         */
        fun laneStraightUsingStraight(
            @DrawableRes laneStraightUsingStraight: Int,
        ): Builder =
            apply {
                this.laneStraightUsingStraight =
                    laneStraightUsingStraight
            }

        /**
         * apply icon to the builder.
         * @param laneTurn Int
         * @return Builder
         */
        fun laneTurn(
            @DrawableRes laneTurn: Int,
        ): Builder =
            apply {
                this.laneTurn =
                    laneTurn
            }

        /**
         * apply icon to the builder.
         * @param laneTurnOrSharpTurn Int
         * @return Builder
         */
        fun laneTurnOrSharpTurn(
            @DrawableRes laneTurnOrSharpTurn: Int,
        ): Builder =
            apply {
                this.laneTurnOrSharpTurn =
                    laneTurnOrSharpTurn
            }

        /**
         * apply icon to the builder.
         * @param laneTurnOrSharpTurnUsingSharpTurn Int
         * @return Builder
         */
        fun laneTurnOrSharpTurnUsingSharpTurn(
            @DrawableRes laneTurnOrSharpTurnUsingSharpTurn: Int,
        ): Builder =
            apply {
                this.laneTurnOrSharpTurnUsingSharpTurn =
                    laneTurnOrSharpTurnUsingSharpTurn
            }

        /**
         * apply icon to the builder.
         * @param laneTurnOrSharpTurnUsingTurn Int
         * @return Builder
         */
        fun laneTurnOrSharpTurnUsingTurn(
            @DrawableRes laneTurnOrSharpTurnUsingTurn: Int,
        ): Builder =
            apply {
                this.laneTurnOrSharpTurnUsingTurn =
                    laneTurnOrSharpTurnUsingTurn
            }

        /**
         * apply icon to the builder.
         * @param laneTurnOrUturn Int
         * @return Builder
         */
        fun laneTurnOrUturn(
            @DrawableRes laneTurnOrUturn: Int,
        ): Builder =
            apply {
                this.laneTurnOrUturn =
                    laneTurnOrUturn
            }

        /**
         * apply icon to the builder.
         * @param laneTurnOrUturnUsingTurn Int
         * @return Builder
         */
        fun laneTurnOrUturnUsingTurn(
            @DrawableRes laneTurnOrUturnUsingTurn: Int,
        ): Builder =
            apply {
                this.laneTurnOrUturnUsingTurn =
                    laneTurnOrUturnUsingTurn
            }

        /**
         * apply icon to the builder.
         * @param laneTurnOrUturnUsingUturn Int
         * @return Builder
         */
        fun laneTurnOrUturnUsingUturn(
            @DrawableRes laneTurnOrUturnUsingUturn: Int,
        ): Builder =
            apply {
                this.laneTurnOrUturnUsingUturn =
                    laneTurnOrUturnUsingUturn
            }

        /**
         * apply icon to the builder.
         * @param laneTurnUsingTurn Int
         * @return Builder
         */
        fun laneTurnUsingTurn(
            @DrawableRes laneTurnUsingTurn: Int,
        ): Builder =
            apply {
                this.laneTurnUsingTurn =
                    laneTurnUsingTurn
            }

        /**
         * apply icon to the builder.
         * @param laneUturn Int
         * @return Builder
         */
        fun laneUturn(
            @DrawableRes laneUturn: Int,
        ): Builder =
            apply {
                this.laneUturn =
                    laneUturn
            }

        /**
         * apply icon to the builder.
         * @param laneUturnUsingUturn Int
         * @return Builder
         */
        fun laneUturnUsingUturn(
            @DrawableRes laneUturnUsingUturn: Int,
        ): Builder =
            apply {
                this.laneUturnUsingUturn =
                    laneUturnUsingUturn
            }

        /**
         * Build the [LaneIconResources].
         * @return LaneIconResources
         */
        fun build(): LaneIconResources {
            return LaneIconResources(
                laneOppositeSlightTurnOrSlightTurn,
                laneOppositeSlightTurnOrSlightTurnUsingSlightTurn,
                laneOppositeSlightTurnOrStraightOrSlightTurn,
                laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn,
                laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight,
                laneOppositeSlightTurnOrStraightOrTurn,
                laneOppositeSlightTurnOrStraightOrTurnUsingStraight,
                laneOppositeSlightTurnOrStraightOrTurnUsingTurn,
                laneOppositeSlightTurnOrTurn,
                laneOppositeSlightTurnOrTurnUsingTurn,
                laneOppositeTurnOrSlightTurn,
                laneOppositeTurnOrSlightTurnUsingSlightTurn,
                laneOppositeTurnOrStraightOrSlightTurn,
                laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn,
                laneOppositeTurnOrStraightOrSlightTurnUsingStraight,
                laneOppositeTurnOrStraightOrTurn,
                laneOppositeTurnOrStraightOrTurnUsingStraight,
                laneOppositeTurnOrStraightOrTurnUsingTurn,
                laneOppositeTurnOrTurn,
                laneOppositeTurnOrTurnUsingTurn,
                laneSharpTurn,
                laneSharpTurnUsingSharpTurn,
                laneSlightTurn,
                laneSlightTurnOrSharpTurn,
                laneSlightTurnOrSharpTurnUsingSharpTurn,
                laneSlightTurnOrSharpTurnUsingSlightTurn,
                laneSlightTurnOrTurn,
                laneSlightTurnOrTurnUsingSlightTurn,
                laneSlightTurnOrTurnUsingTurn,
                laneSlightTurnOrUturn,
                laneSlightTurnOrUturnUsingSlightTurn,
                laneSlightTurnOrUturnUsingUturn,
                laneSlightTurnUsingSlightTurn,
                laneStraight,
                laneStraightOrSharpTurn,
                laneStraightOrSharpTurnUsingSharpTurn,
                laneStraightOrSharpTurnUsingStraight,
                laneStraightOrSlightTurn,
                laneStraightOrSlightTurnOrTurn,
                laneStraightOrSlightTurnOrTurnUsingSlightTurn,
                laneStraightOrSlightTurnOrTurnUsingStraight,
                laneStraightOrSlightTurnOrTurnUsingTurn,
                laneStraightOrSlightTurnUsingSlightTurn,
                laneStraightOrSlightTurnUsingStraight,
                laneStraightOrTurn,
                laneStraightOrTurnOrUturn,
                laneStraightOrTurnOrUturnUsingStraight,
                laneStraightOrTurnOrUturnUsingTurn,
                laneStraightOrTurnOrUturnUsingUturn,
                laneStraightOrTurnUsingStraight,
                laneStraightOrTurnUsingTurn,
                laneStraightOrUturn,
                laneStraightOrUturnUsingStraight,
                laneStraightOrUturnUsingUturn,
                laneStraightUsingStraight,
                laneTurn,
                laneTurnOrSharpTurn,
                laneTurnOrSharpTurnUsingSharpTurn,
                laneTurnOrSharpTurnUsingTurn,
                laneTurnOrUturn,
                laneTurnOrUturnUsingTurn,
                laneTurnOrUturnUsingUturn,
                laneTurnUsingTurn,
                laneUturn,
                laneUturnUsingUturn,
            )
        }
    }
}
