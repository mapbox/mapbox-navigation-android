package com.mapbox.navigation.testing.utils.assertions

import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.testing.ui.assertions.ValueTransitionAssertion
import org.junit.Assert

class RerouteStateTransitionAssertion(
    rerouteController: RerouteController,
    expectedBlock: ValueTransitionAssertion<RerouteState>.() -> Unit
) : ValueTransitionAssertion<RerouteState>(expectedBlock) {
    init {
        rerouteController.registerRerouteStateObserver { state ->
            onNewValue(state)
        }
    }
}

fun assertRerouteFailedTransition(rerouteStates: MutableList<RerouteState>) {
    Assert.assertEquals(
        "reroute states are: $rerouteStates",
        4,
        rerouteStates.size
    )
    Assert.assertEquals(
        RerouteState.Idle,
        rerouteStates[0]
    )
    Assert.assertEquals(
        RerouteState.FetchingRoute,
        rerouteStates[1]
    )
    assertIs<RerouteState.Failed>(rerouteStates[2])
    Assert.assertEquals(
        RerouteState.Idle,
        rerouteStates[3]
    )
}
