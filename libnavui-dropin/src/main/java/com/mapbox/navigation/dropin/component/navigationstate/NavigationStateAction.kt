package com.mapbox.navigation.dropin.component.navigationstate

internal sealed class NavigationStateAction {
    object ToEmpty : NavigationStateAction()
    object ToFreeDrive : NavigationStateAction()
    object ToRoutePreview : NavigationStateAction()
    object ToActiveNavigation : NavigationStateAction()
    object ToArrival : NavigationStateAction()
}
