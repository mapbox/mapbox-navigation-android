package com.mapbox.navigation.tripdata.maneuver

import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.ManeuverModifier.LEFT
import com.mapbox.api.directions.v5.models.ManeuverModifier.RIGHT
import com.mapbox.api.directions.v5.models.ManeuverModifier.UTURN
import com.mapbox.navigation.tripdata.maneuver.model.LaneIcon
import com.mapbox.navigation.tripdata.maneuver.model.LaneIconResources
import com.mapbox.navigation.tripdata.maneuver.model.LaneIndicator

internal class LaneIconProcessor(val laneIconResources: LaneIconResources) {

    private companion object {
        private const val OPPOSITE_SHARP_TURN = "opposite sharp turn"
        private const val OPPOSITE_TURN = "opposite turn"
        private const val OPPOSITE_SLIGHT_TURN = "opposite slight turn"
        private const val STRAIGHT = "straight"
        private const val SLIGHT_TURN = "slight turn"
        private const val TURN = "turn"
        private const val SHARP_TURN = "sharp turn"
        private const val U_TURN = "uturn"
        private const val LANE = "lane"
        private const val USING = "using"
        private const val OPPOSITE = "opposite"
    }

    private val availableLaneNames = mapOf(
        "lane opposite slight turn or slight turn" to
            laneIconResources.laneOppositeSlightTurnOrSlightTurn,
        "lane opposite slight turn or slight turn using slight turn" to
            laneIconResources.laneOppositeSlightTurnOrSlightTurnUsingSlightTurn,
        "lane opposite slight turn or straight or slight turn" to
            laneIconResources.laneOppositeSlightTurnOrStraightOrSlightTurn,
        "lane opposite slight turn or straight or slight turn using slight turn" to
            laneIconResources.laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn,
        "lane opposite slight turn or straight or slight turn using straight" to
            laneIconResources.laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight,
        "lane opposite slight turn or straight or turn" to
            laneIconResources.laneOppositeSlightTurnOrStraightOrTurn,
        "lane opposite slight turn or straight or turn using straight" to
            laneIconResources.laneOppositeSlightTurnOrStraightOrTurnUsingStraight,
        "lane opposite slight turn or straight or turn using turn" to
            laneIconResources.laneOppositeSlightTurnOrStraightOrTurnUsingTurn,
        "lane opposite slight turn or turn" to
            laneIconResources.laneOppositeSlightTurnOrTurn,
        "lane opposite slight turn or turn using turn" to
            laneIconResources.laneOppositeSlightTurnOrTurnUsingTurn,
        "lane opposite turn or slight turn" to
            laneIconResources.laneOppositeTurnOrSlightTurn,
        "lane opposite turn or slight turn using slight turn" to
            laneIconResources.laneOppositeTurnOrSlightTurnUsingSlightTurn,
        "lane opposite turn or straight or slight turn" to
            laneIconResources.laneOppositeTurnOrStraightOrSlightTurn,
        "lane opposite turn or straight or slight turn using slight turn" to
            laneIconResources.laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn,
        "lane opposite turn or straight or slight turn using straight" to
            laneIconResources.laneOppositeTurnOrStraightOrSlightTurnUsingStraight,
        "lane opposite turn or straight or turn" to
            laneIconResources.laneOppositeTurnOrStraightOrTurn,
        "lane opposite turn or straight or turn using straight" to
            laneIconResources.laneOppositeTurnOrStraightOrTurnUsingStraight,
        "lane opposite turn or straight or turn using turn" to
            laneIconResources.laneOppositeTurnOrStraightOrTurnUsingTurn,
        "lane opposite turn or turn" to
            laneIconResources.laneOppositeTurnOrTurn,
        "lane opposite turn or turn using turn" to
            laneIconResources.laneOppositeTurnOrTurnUsingTurn,
        "lane sharp turn" to
            laneIconResources.laneSharpTurn,
        "lane sharp turn using sharp turn" to
            laneIconResources.laneSharpTurnUsingSharpTurn,
        "lane slight turn" to
            laneIconResources.laneSlightTurn,
        "lane slight turn or sharp turn" to
            laneIconResources.laneSlightTurnOrSharpTurn,
        "lane slight turn or sharp turn using sharp turn" to
            laneIconResources.laneSlightTurnOrSharpTurnUsingSharpTurn,
        "lane slight turn or sharp turn using slight turn" to
            laneIconResources.laneSlightTurnOrSharpTurnUsingSlightTurn,
        "lane slight turn or turn" to
            laneIconResources.laneSlightTurnOrTurn,
        "lane slight turn or turn using slight turn" to
            laneIconResources.laneSlightTurnOrTurnUsingSlightTurn,
        "lane slight turn or turn using turn" to
            laneIconResources.laneSlightTurnOrTurnUsingTurn,
        "lane slight turn or uturn" to
            laneIconResources.laneSlightTurnOrUturn,
        "lane slight turn or uturn using slight turn" to
            laneIconResources.laneSlightTurnOrUturnUsingSlightTurn,
        "lane slight turn or uturn using uturn" to
            laneIconResources.laneSlightTurnOrUturnUsingUturn,
        "lane slight turn using slight turn" to
            laneIconResources.laneSlightTurnUsingSlightTurn,
        "lane straight" to
            laneIconResources.laneStraight,
        "lane straight or sharp turn" to
            laneIconResources.laneStraightOrSharpTurn,
        "lane straight or sharp turn using sharp turn" to
            laneIconResources.laneStraightOrSharpTurnUsingSharpTurn,
        "lane straight or sharp turn using straight" to
            laneIconResources.laneStraightOrSharpTurnUsingStraight,
        "lane straight or slight turn" to
            laneIconResources.laneStraightOrSlightTurn,
        "lane straight or slight turn or turn" to
            laneIconResources.laneStraightOrSlightTurnOrTurn,
        "lane straight or slight turn or turn using slight turn" to
            laneIconResources.laneStraightOrSlightTurnOrTurnUsingSlightTurn,
        "lane straight or slight turn or turn using straight" to
            laneIconResources.laneStraightOrSlightTurnOrTurnUsingStraight,
        "lane straight or slight turn or turn using turn" to
            laneIconResources.laneStraightOrSlightTurnOrTurnUsingTurn,
        "lane straight or slight turn using slight turn" to
            laneIconResources.laneStraightOrSlightTurnUsingSlightTurn,
        "lane straight or slight turn using straight" to
            laneIconResources.laneStraightOrSlightTurnUsingStraight,
        "lane straight or turn" to
            laneIconResources.laneStraightOrTurn,
        "lane straight or turn or uturn" to
            laneIconResources.laneStraightOrTurnOrUturn,
        "lane straight or turn or uturn using straight" to
            laneIconResources.laneStraightOrTurnOrUturnUsingStraight,
        "lane straight or turn or uturn using turn" to
            laneIconResources.laneStraightOrTurnOrUturnUsingTurn,
        "lane straight or turn or uturn using uturn" to
            laneIconResources.laneStraightOrTurnOrUturnUsingUturn,
        "lane straight or turn using straight" to
            laneIconResources.laneStraightOrTurnUsingStraight,
        "lane straight or turn using turn" to
            laneIconResources.laneStraightOrTurnUsingTurn,
        "lane straight or uturn" to
            laneIconResources.laneStraightOrUturn,
        "lane straight or uturn using straight" to
            laneIconResources.laneStraightOrUturnUsingStraight,
        "lane straight or uturn using uturn" to
            laneIconResources.laneStraightOrUturnUsingUturn,
        "lane straight using straight" to
            laneIconResources.laneStraightUsingStraight,
        "lane turn" to
            laneIconResources.laneTurn,
        "lane turn or sharp turn" to
            laneIconResources.laneTurnOrSharpTurn,
        "lane turn or sharp turn using sharp turn" to
            laneIconResources.laneTurnOrSharpTurnUsingSharpTurn,
        "lane turn or sharp turn using turn" to
            laneIconResources.laneTurnOrSharpTurnUsingTurn,
        "lane turn or uturn" to
            laneIconResources.laneTurnOrUturn,
        "lane turn or uturn using turn" to
            laneIconResources.laneTurnOrUturnUsingTurn,
        "lane turn or uturn using uturn" to
            laneIconResources.laneTurnOrUturnUsingUturn,
        "lane turn using turn" to
            laneIconResources.laneTurnUsingTurn,
        "lane uturn" to
            laneIconResources.laneUturn,
        "lane uturn using uturn" to
            laneIconResources.laneUturnUsingUturn,
    )

    fun getLaneIcon(laneIndication: LaneIndicator): LaneIcon {
        val pair = convertDirectionsToLaneName(laneIndication)
        val laneName = pair.first
        val shouldFlip = pair.second
        return when {
            availableLaneNames.contains(laneName) -> {
                LaneIcon(availableLaneNames[laneName]!!, shouldFlip)
            }
            laneIndication.activeDirection != null -> {
                // Draw "lane laneIndication.activeDirection using laneIndication.activeDirection"
                val active = laneIndication.activeDirection.replace(RIGHT, TURN).replace(LEFT, TURN)
                val name = "$LANE $active $USING $active"
                if (availableLaneNames.contains(laneName)) {
                    LaneIcon(availableLaneNames[name]!!, shouldFlip)
                } else {
                    LaneIcon(availableLaneNames["$LANE $STRAIGHT"]!!, shouldFlip)
                }
            }
            else -> {
                // Draw "lane straight"
                LaneIcon(availableLaneNames["$LANE $STRAIGHT"]!!, shouldFlip)
            }
        }
    }

    /**
     * A lane is leftward when it is "sharp left", "left" or "slight left".
     * "u-turn" is leftward when the driving side is to the right.
     */
    private fun isLaneLeftward(direction: String, drivingSide: String): Boolean {
        return if (direction == UTURN) {
            drivingSide == RIGHT
        } else {
            direction.contains(LEFT)
        }
    }

    /**
     * A lane is rightward when it is "sharp right", "right" or "slight right".
     * "u-turn" is rightward when the driving side is to the left.
     */
    private fun isLaneRightward(direction: String, drivingSide: String): Boolean {
        return if (direction == UTURN) {
            drivingSide == LEFT
        } else {
            direction.contains(RIGHT)
        }
    }

    private fun getSortedIndications(directions: List<String>): MutableList<String> {
        val sortedDirections = mutableListOf<String>()
        if (directions.contains(OPPOSITE_SHARP_TURN)) {
            sortedDirections.add(OPPOSITE_SHARP_TURN)
        }
        if (directions.contains(OPPOSITE_TURN)) {
            sortedDirections.add(OPPOSITE_TURN)
        }
        if (directions.contains(OPPOSITE_SLIGHT_TURN)) {
            sortedDirections.add(OPPOSITE_SLIGHT_TURN)
        }
        if (directions.contains(STRAIGHT)) {
            sortedDirections.add(STRAIGHT)
        }
        if (directions.contains(SLIGHT_TURN)) {
            sortedDirections.add(SLIGHT_TURN)
        }
        if (directions.contains(TURN)) {
            sortedDirections.add(TURN)
        }
        if (directions.contains(SHARP_TURN)) {
            sortedDirections.add(SHARP_TURN)
        }
        if (directions.contains(U_TURN)) {
            sortedDirections.add(U_TURN)
        }
        return sortedDirections
    }

    internal fun <T> MutableList<T>.rearrange(item: T, newIndex: Int) {
        val currentIndex = indexOf(item)
        if (currentIndex < 0) return
        removeAt(currentIndex)
        add(newIndex, item)
    }

    private fun convertDirectionsToLaneName(laneIndication: LaneIndicator): Pair<String, Boolean> {
        var hasLeftwardLane = false
        var hasRightwardLane = false
        var shouldFlip = false

        /**
         * The drivingSide used here is a coming from [LegStep.drivingSide] which as per the
         * documentation from directions API is not optional and will always be either right or left
         */
        val drivingSide = laneIndication.drivingSide
        var activeDirection: String? = laneIndication.activeDirection
        var turnLaneList = mutableListOf<String>()
        turnLaneList.addAll(laneIndication.directions)
        // Verify whether every direction in list of directions is leftward or rightward
        for (direction in turnLaneList) {
            if (isLaneLeftward(direction, drivingSide)) {
                hasLeftwardLane = true
            } else if (isLaneRightward(direction, drivingSide)) {
                hasRightwardLane = true
            }
        }
        // If the list of direction contains both leftward and rightward directions
        if (hasLeftwardLane && hasRightwardLane) {
            when {
                // Check if activeDirection is null
                activeDirection == null -> {
                    prependOpposite(true, drivingSide, turnLaneList)
                }
                // Check if activeDirection is leftward
                isLaneLeftward(activeDirection, drivingSide) -> {
                    shouldFlip = true
                    prependOpposite(false, drivingSide, turnLaneList)
                }
                // activeDirection is rightward or straight
                else -> {
                    prependOpposite(true, drivingSide, turnLaneList)
                }
            }
        }
        // else if the list of direction contains only leftward directions set shouldFlip to true.
        else if (hasLeftwardLane) {
            shouldFlip = true
        }
        // For each indication, replace any occurrence of "left" or "right" with "turn".
        turnLaneList.forEachIndexed { index, direction ->
            turnLaneList[index] = direction.replace(LEFT, TURN).replace(RIGHT, TURN)
        }
        // If activeDirection is set, replace any occurrence of "left" or "right" with "turn".
        activeDirection = activeDirection?.replace(LEFT, TURN)?.replace(RIGHT, TURN)
        // If there are more than three indications
        // If activeDirection is set,
        //   - move the activeDirection in the list to the first position.
        // Keep only first 3 items of the list and discard the rest.
        if (turnLaneList.size > 3) {
            activeDirection?.let { direction ->
                turnLaneList.rearrange(direction, 0)
            }
            turnLaneList = turnLaneList.take(3).toMutableList()
        }
        // Sort the list of directions in the following order - "opposite sharp turn", "opposite turn",
        // "opposite slight turn", "straight", "slight turn", "turn", "sharp turn", "uturn"
        turnLaneList = getSortedIndications(turnLaneList)
        // Set the drawable file name to "lane" followed by the array of directions joined by "or"
        var laneName = "$LANE ".plus(turnLaneList.joinToString(" or "))
        // If activeDirection is set, append "using" and the value of activeDirection to the
        // drawable file name.
        activeDirection?.let {
            laneName = laneName.plus(" $USING $it")
        }
        return Pair(laneName, shouldFlip)
    }

    /**
     * If [toLeftward] is true, prepend "opposite" to any leftward direction else prepend
     * opposite to any rightward direction.
     */
    private fun prependOpposite(
        toLeftward: Boolean,
        drivingSide: String,
        turnLaneList: MutableList<String>,
    ) {
        when (toLeftward) {
            true -> {
                turnLaneList.forEachIndexed { index, direction ->
                    if (isLaneLeftward(direction, drivingSide)) {
                        turnLaneList[index] = "$OPPOSITE $direction"
                    }
                }
            }
            false -> {
                turnLaneList.forEachIndexed { index, direction ->
                    if (isLaneRightward(direction, drivingSide)) {
                        turnLaneList[index] = "$OPPOSITE $direction"
                    }
                }
            }
        }
    }
}
