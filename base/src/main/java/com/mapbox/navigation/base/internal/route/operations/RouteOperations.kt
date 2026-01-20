package com.mapbox.navigation.base.internal.route.operations

import androidx.annotation.WorkerThread
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.internal.route.NavigationRouteData
import com.mapbox.navigation.base.internal.route.parsing.models.RouteModelParsingResult
import com.mapbox.navigation.base.route.RouteRefreshMetadata

@OptIn(ExperimentalMapboxNavigationAPI::class)
internal data class RouteUpdate(
    val routeModelsParsingResult: RouteModelParsingResult,
    val routeRefreshMetadata: RouteRefreshMetadata?,
    val newExpirationTimeElapsedSeconds: OptionallyRefreshedData<Long?>,
    val overriddenTraffic: OptionallyRefreshedData<CongestionNumericOverride?>,
)

internal interface RouteOperations {
    @WorkerThread
    fun refresh(
        refreshResponse: DataRef,
        legIndex: Int,
        legGeometryIndex: Int,
        responseTimeElapsedSeconds: Long,
    ): Result<RouteUpdate>

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    fun clientSideRouteUpdate(
        directionsRouteBlock: DirectionsRoute.() -> DirectionsRoute,
        waypointsBlock: List<DirectionsWaypoint>?.() -> List<DirectionsWaypoint>?,
        overriddenTraffic: CongestionNumericOverride?,
        routeRefreshMetadata: RouteRefreshMetadata?,
    ): Result<RouteUpdate>

    fun toDirectionsRefreshResponse(): Result<DirectionsRefreshResponse>

    fun serialize(navigationRouteData: NavigationRouteData): Result<String>
}
