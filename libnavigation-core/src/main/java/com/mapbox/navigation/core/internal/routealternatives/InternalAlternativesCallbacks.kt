package com.mapbox.navigation.core.internal.routealternatives

import com.mapbox.navigation.core.MapboxNavigation

// TODO: try to get rid of this API: https://mapbox.atlassian.net/browse/NAVAND-1768
fun MapboxNavigation.setRouteAlternativesObserver(
    routeAlternativesObserver: NavigationRouteAlternativesObserver,
) {
    this.setRouteAlternativesObserver(routeAlternativesObserver)
}

// TODO: try to get rid of this API: https://mapbox.atlassian.net/browse/NAVAND-1768
fun MapboxNavigation.restoreDefaultRouteAlternativesObserver() {
    this.restoreDefaultRouteAlternativesObserver()
}
