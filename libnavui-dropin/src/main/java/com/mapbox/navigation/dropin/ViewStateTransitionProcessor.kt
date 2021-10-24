package com.mapbox.navigation.dropin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal sealed class ViewStateTransitionProcessor {
    abstract fun Flow<ViewStateTransitionProcessor>.process(): Flow<NavigationStateTransitionResult>

    object TransitionToEmpty: ViewStateTransitionProcessor() {
        override fun Flow<ViewStateTransitionProcessor>.process(): Flow<NavigationStateTransitionResult> =
            flowOf(NavigationStateTransitionResult.ToEmpty(NavigationState.Empty))
    }
    object TransitionToFreeDrive: ViewStateTransitionProcessor() {
        override fun Flow<ViewStateTransitionProcessor>.process(): Flow<NavigationStateTransitionResult> =
            flowOf(NavigationStateTransitionResult.ToFreeDrive(NavigationState.FreeDrive))
    }
    object TransitionToRoutePreview: ViewStateTransitionProcessor() {
        override fun Flow<ViewStateTransitionProcessor>.process(): Flow<NavigationStateTransitionResult> =
            flowOf(NavigationStateTransitionResult.ToRoutePreview(NavigationState.RoutePreview))
    }
    object TransitionToActiveNavigation: ViewStateTransitionProcessor() {
        override fun Flow<ViewStateTransitionProcessor>.process(): Flow<NavigationStateTransitionResult> =
            flowOf(NavigationStateTransitionResult.ToActiveNavigation(NavigationState.ActiveNavigation))
    }
    object TransitionToArrival: ViewStateTransitionProcessor() {
        override fun Flow<ViewStateTransitionProcessor>.process(): Flow<NavigationStateTransitionResult> =
            flowOf(NavigationStateTransitionResult.ToArrival(NavigationState.Arrival))
    }
}
