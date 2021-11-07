package com.mapbox.navigation.base.internal.factory

import com.mapbox.api.directions.v5.models.MapboxShield
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigator.NavigationStatus

/**
 * Internal factory to build [Road] objects
 */
@ExperimentalMapboxNavigationAPI
object RoadFactory {

    fun buildRoadObject(navigationStatus: NavigationStatus): Road {
        val mapboxShields = mutableListOf<MapboxShield>()
        navigationStatus.shields.forEach { shield ->
            mapboxShields.add(
                MapboxShield
                    .builder()
                    .name(shield.name)
                    .baseUrl(shield.baseUrl)
                    .textColor(shield.textColor)
                    .displayRef(shield.displayRef)
                    .build()
            )
        }
        return Road(
            name = navigationStatus.roadName,
            shieldUrl = navigationStatus.imageBaseurl,
            shieldName = navigationStatus.shieldName,
            mapboxShield = mapboxShields
        )
    }
}
