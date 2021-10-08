package com.mapbox.navigation.base.internal.factory

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigator.NavigationStatus

/**
 * Internal factory to build [Road] objects
 */
@ExperimentalMapboxNavigationAPI
object RoadFactory {

    fun buildRoadObject(navigationStatus: NavigationStatus): Road =
        Road(navigationStatus.roadName, null, navigationStatus.shieldName)
}
