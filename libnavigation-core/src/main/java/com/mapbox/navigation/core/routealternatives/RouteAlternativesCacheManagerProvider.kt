package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.core.trip.session.NavigationSession

internal object RouteAlternativesCacheManagerProvider {

    fun create(navigationSession: NavigationSession): RouteAlternativesCacheManager =
        RouteAlternativesCacheManager(navigationSession)
}
