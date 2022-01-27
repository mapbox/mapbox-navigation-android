package com.mapbox.navigation.core.infra.factories

import android.location.Location
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.factory.RoadFactory
import com.mapbox.navigation.base.road.model.Road
import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigator.Shield

fun createLocationMatcherResult(
    enhancedLocation: Location = createLocation(),
    keyPoints: List<Location> = emptyList(),
    isOffRoad: Boolean = false,
    offRoadProbability: Float = 0.0f,
    isTeleport: Boolean = false,
    speedLimit: SpeedLimit? = null,
    roadEdgeMatchProbability: Float = 1.0f,
    zLevel: Int? = 1,
    road: Road = createRoad(),
    isDegradedMapMatching: Boolean = false,
) = LocationMatcherResult(
    enhancedLocation = enhancedLocation,
    keyPoints = keyPoints,
    isOffRoad = isOffRoad,
    offRoadProbability = offRoadProbability,
    isTeleport = isTeleport,
    speedLimit = speedLimit,
    roadEdgeMatchProbability = roadEdgeMatchProbability,
    zLevel = zLevel,
    road = road,
    isDegradedMapMatching = isDegradedMapMatching,
)

@OptIn(ExperimentalMapboxNavigationAPI::class)
fun createRoad(
    text: String = "test",
    imageBaseUlr: String? = null,
    shield: Shield? = null
) = RoadFactory.buildRoadObject(
    createNavigationStatus(
        roads = listOf(
            com.mapbox.navigator.Road(
                text,
                imageBaseUlr,
                shield
            )
        )
    )
)
