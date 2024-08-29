package com.mapbox.navigation.base.internal.factory

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.toMapboxShield
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.road.model.RoadComponent
import com.mapbox.navigator.NavigationStatus

/**
 * Internal factory to build [Road] objects
 */
@ExperimentalMapboxNavigationAPI
object RoadFactory {

    fun buildRoadObject(navigationStatus: NavigationStatus): Road {
        val components = navigationStatus.roads.map { road ->
            RoadComponent(
                text = road.text,
                shield = road.shield.toMapboxShield(),
                imageBaseUrl = road.imageBaseUrl,
                language = road.language,
            )
        }
        return Road(components)
    }
}
