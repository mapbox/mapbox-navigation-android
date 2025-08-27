package com.mapbox.navigation.ui.maps.internal.route.callout.api

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun interface RoutesSetToRouteLineObserver {
    fun onSet(routes: List<NavigationRoute>, alternativeMetadata: List<AlternativeRouteMetadata>)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface RoutesSetToRouteLineDataProvider {

    fun registerRoutesSetToRouteLineObserver(observer: RoutesSetToRouteLineObserver)

    fun unregisterRoutesSetToRouteLineObserver(observer: RoutesSetToRouteLineObserver)
}
