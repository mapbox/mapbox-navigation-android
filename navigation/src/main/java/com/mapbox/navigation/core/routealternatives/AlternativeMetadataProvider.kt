package com.mapbox.navigation.core.routealternatives

import com.mapbox.navigation.base.route.NavigationRoute

internal interface AlternativeMetadataProvider {

    fun getMetadataFor(navigationRoute: NavigationRoute): AlternativeRouteMetadata?
}
