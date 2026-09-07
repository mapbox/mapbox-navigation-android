package com.mapbox.navigation.ui.androidauto.navigation.roadlabel

import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.tripdata.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.tripdata.shield.model.RouteShield
import com.mapbox.navigation.tripdata.shield.model.RouteShieldCallback
import com.mapbox.navigation.ui.androidauto.navigation.MapUserStyleObserver

internal abstract class CarRoadNameObserver(
    private val routeShieldApi: MapboxRouteShieldApi,
    private val mapUserStyleObserver: MapUserStyleObserver,
) : MapboxNavigationObserver {

    var currentRoad = emptyList<RoadComponent>()
    var currentShields = emptyList<RouteShield>()

    private var mapboxNavigation: MapboxNavigation? = null

    private val roadNameShieldsCallback = RouteShieldCallback { shieldResult ->
        val newShields = shieldResult.mapNotNull { it.value?.shield }
        if (currentShields != newShields) {
            currentShields = newShields
            onRoadUpdate(currentRoad, newShields)
        }
    }

    private val locationObserver = object : LocationObserver {

        override fun onNewRawLocation(rawLocation: com.mapbox.common.location.Location) {
            // no-op
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val newRoad = locationMatcherResult.road.components
            if (currentRoad != newRoad) {
                currentRoad = newRoad
                onRoadUpdate(newRoad, currentShields)
                routeShieldApi.getRouteShields(
                    locationMatcherResult.road,
                    mapUserStyleObserver.userId,
                    mapUserStyleObserver.styleId,
                    roadNameShieldsCallback,
                )
            }
        }
    }

    abstract fun onRoadUpdate(road: List<RoadComponent>, shields: List<RouteShield>)

    final override fun onAttached(mapboxNavigation: MapboxNavigation) {
        this.mapboxNavigation = mapboxNavigation
        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    final override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        this.mapboxNavigation = null
    }
}
