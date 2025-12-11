package com.mapbox.navigation.testing.utils.assertions

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.reroute.RerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.core.reroute.RerouteStateV2
import com.mapbox.navigation.testing.ui.assertions.ValueTransitionAssertion
import org.junit.Assert
import org.junit.Assert.assertEquals

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

@OptIn(ExperimentalMapboxNavigationAPI::class)
fun MapboxNavigation.recordRerouteStatesV2(): List<RerouteStateV2> {
    val rerouteStates = mutableListOf<RerouteStateV2>()
    getRerouteController()?.registerRerouteStateV2Observer {
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

@OptIn(ExperimentalMapboxNavigationAPI::class)
fun assertRerouteFailedTransitionV2(rerouteStates: List<RerouteStateV2>) {
    Assert.assertEquals(
        "reroute states are: $rerouteStates",
        4,
        rerouteStates.size
    )
    assertIs<RerouteStateV2.Idle>(rerouteStates[0])
    assertIs<RerouteStateV2.FetchingRoute>(rerouteStates[1])
    assertIs<RerouteStateV2.Failed>(rerouteStates[2])
    assertIs<RerouteStateV2.Idle>(rerouteStates[3])
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

@OptIn(ExperimentalMapboxNavigationAPI::class)
fun assertSuccessfulRouteAppliedRerouteStateTransition(rerouteStates: List<RerouteStateV2>) {
    assertEquals(
        "reroute states are: $rerouteStates",
        5,
        rerouteStates.size
    )
    assertIs<RerouteStateV2.Idle>(rerouteStates[0])
    assertIs<RerouteStateV2.FetchingRoute>(rerouteStates[1])
    assertIs<RerouteStateV2.RouteFetched>(rerouteStates[2])
    assertEquals(RouterOrigin.ONLINE, (rerouteStates[2] as RerouteStateV2.RouteFetched).routerOrigin)
    assertIs<RerouteStateV2.Deviation.ApplyingRoute>(rerouteStates[3])
    assertIs<RerouteStateV2.Idle>(rerouteStates[4])
}

@OptIn(ExperimentalMapboxNavigationAPI::class)
fun assertSuccessfulRouteIgnoredRerouteStateTransition(rerouteStates: List<RerouteStateV2>) {
    assertEquals(
        "reroute states are: $rerouteStates",
        5,
        rerouteStates.size
    )
    assertIs<RerouteStateV2.Idle>(rerouteStates[0])
    assertIs<RerouteStateV2.FetchingRoute>(rerouteStates[1])
    assertIs<RerouteStateV2.RouteFetched>(rerouteStates[2])
    assertEquals(RouterOrigin.ONLINE, (rerouteStates[2] as RerouteStateV2.RouteFetched).routerOrigin)
    assertIs<RerouteStateV2.Deviation.RouteIgnored>(rerouteStates[3])
    assertIs<RerouteStateV2.Idle>(rerouteStates[4])
}

@OptIn(ExperimentalMapboxNavigationAPI::class)
fun assertSuccessfulRouteReplanRerouteStateTransition(rerouteStates: List<RerouteStateV2>) {
    assertEquals(
        "reroute states are: $rerouteStates",
        4,
        rerouteStates.size
    )
    assertIs<RerouteStateV2.Idle>(rerouteStates[0])
    assertIs<RerouteStateV2.FetchingRoute>(rerouteStates[1])
    assertIs<RerouteStateV2.RouteFetched>(rerouteStates[2])
    assertEquals(RouterOrigin.ONLINE, (rerouteStates[2] as RerouteStateV2.RouteFetched).routerOrigin)
    assertIs<RerouteStateV2.Idle>(rerouteStates[3])
}

