package com.mapbox.navigation.core.replay.history

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.core.history.model.HistoryEventSetRoute

/**
 * This class is used to replay the set route events from history files.
 *
 * When reading history from the [MapboxHistoryReader], there will be [HistoryEventSetRoute]
 * included. To convert [HistoryEventSetRoute] into [ReplayEventBase], add an
 * instance of this class to the [ReplayHistoryMapper.Builder.setRouteMapper].
 *
 * @param accessToken when reconstructing the [DirectionsRoute] an accessToken is needed
 */
class ReplayHistorySetRouteMapper(
    val accessToken: String
) : ReplayHistoryEventMapper<HistoryEventSetRoute> {
    override fun map(event: HistoryEventSetRoute): ReplayEventBase {
        return ReplaySetRoute(
            eventTimestamp = event.eventTimestamp,
            route = mapDirectionsRoute(event.directionsRoute)
        )
    }

    private fun mapDirectionsRoute(routeResponse: String?): DirectionsRoute? {
        return routeResponse?.let { DirectionsRoute.fromJson(routeResponse, accessToken) }
    }
}
