package com.mapbox.navigation.testing.utils.assertions

import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
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

fun MapboxNavigation.recordRerouteStates(): List<RerouteState> {
    val rerouteStates = mutableListOf<RerouteState>()
    getRerouteController()?.registerRerouteStateObserver {
        rerouteStates.add(it)
    }
    return rerouteStates
}

fun assertRerouteFailedTransition(rerouteStates: List<RerouteState>) {
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

fun assertSuccessfulRerouteStateTransition(rerouteStates: List<RerouteState>) {
    Assert.assertEquals(
        listOf(
            RerouteState.Idle,
            RerouteState.FetchingRoute,
            RerouteState.RouteFetched(RouterOrigin.ONLINE),
            RerouteState.Idle,
        ),
        rerouteStates,
    )
}
