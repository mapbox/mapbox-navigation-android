package com.mapbox.navigation.tripdata.maneuver.model

import com.mapbox.navigation.testing.BuilderTest
import org.junit.Test
import kotlin.reflect.KClass

class LaneIconResourcesTest : BuilderTest<LaneIconResources,
    LaneIconResources.Builder,>() {

    override fun getImplementationClass(): KClass<LaneIconResources> =
        LaneIconResources::class

    override fun getFilledUpBuilder(): LaneIconResources.Builder {
        return LaneIconResources.Builder()
            .laneOppositeSlightTurnOrSlightTurn(1)
            .laneOppositeSlightTurnOrSlightTurnUsingSlightTurn(1)
            .laneOppositeSlightTurnOrStraightOrSlightTurn(1)
            .laneOppositeSlightTurnOrStraightOrSlightTurnUsingSlightTurn(1)
            .laneOppositeSlightTurnOrStraightOrSlightTurnUsingStraight(1)
            .laneOppositeSlightTurnOrStraightOrTurn(1)
            .laneOppositeSlightTurnOrStraightOrTurnUsingStraight(1)
            .laneOppositeSlightTurnOrStraightOrTurnUsingTurn(1)
            .laneOppositeSlightTurnOrTurn(1)
            .laneOppositeSlightTurnOrTurnUsingTurn(1)
            .laneOppositeTurnOrSlightTurn(1)
            .laneOppositeTurnOrSlightTurnUsingSlightTurn(1)
            .laneOppositeTurnOrStraightOrSlightTurn(1)
            .laneOppositeTurnOrStraightOrSlightTurnUsingSlightTurn(1)
            .laneOppositeTurnOrStraightOrSlightTurnUsingStraight(1)
            .laneOppositeTurnOrStraightOrTurn(1)
            .laneOppositeTurnOrStraightOrTurnUsingStraight(1)
            .laneOppositeTurnOrStraightOrTurnUsingTurn(1)
            .laneOppositeTurnOrTurn(1)
            .laneOppositeTurnOrTurnUsingTurn(1)
            .laneSharpTurn(1)
            .laneSharpTurnUsingSharpTurn(1)
            .laneSlightTurn(1)
            .laneSlightTurnOrSharpTurn(1)
            .laneSlightTurnOrSharpTurnUsingSharpTurn(1)
            .laneSlightTurnOrSharpTurnUsingSlightTurn(1)
            .laneSlightTurnOrTurn(1)
            .laneSlightTurnOrTurnUsingSlightTurn(1)
            .laneSlightTurnOrTurnUsingTurn(1)
            .laneSlightTurnOrUturn(1)
            .laneSlightTurnOrUturnUsingSlightTurn(1)
            .laneSlightTurnOrUturnUsingUturn(1)
            .laneSlightTurnUsingSlightTurn(1)
            .laneStraight(1)
            .laneStraightOrSharpTurn(1)
            .laneStraightOrSharpTurnUsingSharpTurn(1)
            .laneStraightOrSharpTurnUsingStraight(1)
            .laneStraightOrSlightTurn(1)
            .laneStraightOrSlightTurnOrTurn(1)
            .laneStraightOrSlightTurnOrTurnUsingSlightTurn(1)
            .laneStraightOrSlightTurnOrTurnUsingStraight(1)
            .laneStraightOrSlightTurnOrTurnUsingTurn(1)
            .laneStraightOrSlightTurnUsingSlightTurn(1)
            .laneStraightOrSlightTurnUsingStraight(1)
            .laneStraightOrTurn(1)
            .laneStraightOrTurnOrUturn(1)
            .laneStraightOrTurnOrUturnUsingStraight(1)
            .laneStraightOrTurnOrUturnUsingTurn(1)
            .laneStraightOrTurnOrUturnUsingUturn(1)
            .laneStraightOrTurnUsingStraight(1)
            .laneStraightOrTurnUsingTurn(1)
            .laneStraightOrUturn(1)
            .laneStraightOrUturnUsingStraight(1)
            .laneStraightOrUturnUsingUturn(1)
            .laneStraightUsingStraight(1)
            .laneTurn(1)
            .laneTurnOrSharpTurn(1)
            .laneTurnOrSharpTurnUsingSharpTurn(1)
            .laneTurnOrSharpTurnUsingTurn(1)
            .laneTurnOrUturn(1)
            .laneTurnOrUturnUsingTurn(1)
            .laneTurnOrUturnUsingUturn(1)
            .laneTurnUsingTurn(1)
            .laneUturn(1)
            .laneUturnUsingUturn(1)
    }

    @Test
    override fun trigger() {
        // see comments
    }
}
