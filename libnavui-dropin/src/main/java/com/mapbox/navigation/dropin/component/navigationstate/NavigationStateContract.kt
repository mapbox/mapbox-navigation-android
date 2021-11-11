package com.mapbox.navigation.dropin.component.navigationstate

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal sealed class NavigationStateAction {
    object ToEmpty : NavigationStateAction()
    object ToFreeDrive : NavigationStateAction()
    object ToRoutePreview : NavigationStateAction()
    object ToActiveNavigation : NavigationStateAction()
    object ToArrival : NavigationStateAction()
}

internal sealed class NavigationStateResult {
    data class ToEmpty(val navigationState: NavigationState) : NavigationStateResult()
    data class ToFreeDrive(val navigationState: NavigationState) : NavigationStateResult()
    data class ToRoutePreview(val navigationState: NavigationState) : NavigationStateResult()
    data class ToActiveNavigation(val navigationState: NavigationState) : NavigationStateResult()
    data class ToArrival(val navigationState: NavigationState) : NavigationStateResult()
}

internal fun Flow<NavigationStateAction>.toProcessor(): Flow<NavigationStateProcessor> =
    map { action ->
        when (action) {
            is NavigationStateAction.ToEmpty -> NavigationStateProcessor.ToEmpty
            is NavigationStateAction.ToFreeDrive -> NavigationStateProcessor.ToFreeDrive
            is NavigationStateAction.ToRoutePreview -> NavigationStateProcessor.ToRoutePreview
            is NavigationStateAction.ToActiveNavigation ->
                NavigationStateProcessor.ToActiveNavigation
            is NavigationStateAction.ToArrival -> NavigationStateProcessor.ToArrival
        }
    }

internal fun Flow<NavigationStateProcessor>.toResult(): Flow<NavigationStateResult> =
    map { processor ->
        when (processor) {
            is NavigationStateProcessor.ToEmpty -> processor.process()
            is NavigationStateProcessor.ToFreeDrive -> processor.process()
            is NavigationStateProcessor.ToRoutePreview -> processor.process()
            is NavigationStateProcessor.ToActiveNavigation -> processor.process()
            is NavigationStateProcessor.ToArrival -> processor.process()
        }
    }
