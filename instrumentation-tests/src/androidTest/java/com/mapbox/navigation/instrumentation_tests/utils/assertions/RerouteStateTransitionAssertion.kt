package com.mapbox.navigation.instrumentation_tests.utils.assertions

import com.mapbox.navigation.core.reroute.NavigationRerouteController
import com.mapbox.navigation.core.reroute.RerouteState
import com.mapbox.navigation.testing.ui.assertions.ValueTransitionAssertion

class RerouteStateTransitionAssertion(
    rerouteController: NavigationRerouteController,
    expectedBlock: ValueTransitionAssertion<RerouteState>.() -> Unit
) : ValueTransitionAssertion<RerouteState>(expectedBlock) {
    init {
        rerouteController.registerRerouteStateObserver { state ->
            onNewValue(state)
        }
    }
}
