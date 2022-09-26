package com.mapbox.navigation.ui.app.internal.routefetch

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.core.MapboxNavigation

class RouteOptionsProvider {

    private var interceptor: (RouteOptions.Builder) -> RouteOptions.Builder = { it }

    fun setInterceptor(interceptor: (RouteOptions.Builder) -> RouteOptions.Builder) {
        this.interceptor = interceptor
    }

    fun getOptions(
        mapboxNavigation: MapboxNavigation,
        origin: Point,
        destination: Point,
    ): RouteOptions {
        return RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(mapboxNavigation.navigationOptions.applicationContext)
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .coordinatesList(listOf(origin, destination))
            .alternatives(true)
            .let(interceptor)
            .build()
    }
}
