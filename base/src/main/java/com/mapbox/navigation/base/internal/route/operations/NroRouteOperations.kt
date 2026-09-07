package com.mapbox.navigation.base.internal.route.operations

import androidx.annotation.WorkerThread
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.DirectionsWaypoint
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.bindgen.DataRef
import com.mapbox.directions.route.DirectionsRouteContext
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.internal.NotSupportedForNativeRouteObjectException
import com.mapbox.navigation.base.internal.route.NavigationRouteData
import com.mapbox.navigation.base.internal.route.parsing.models.DirectionsParsedRouteData
import com.mapbox.navigation.base.internal.route.parsing.parser.directions.toRouteModelsParsingResult
import com.mapbox.navigation.base.internal.utils.refreshTtl
import com.mapbox.navigation.base.route.RouteRefreshMetadata
import com.mapbox.navigation.utils.internal.logD

private val LOG_CATEGORY = "NRO-ROUTE-OPERATIONS"

internal class NroRouteOperations(
    val parsedRouteData: DirectionsParsedRouteData,
) : RouteOperations {

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @WorkerThread
    override fun refresh(
        refreshResponse: DataRef,
        legIndex: Int,
        legGeometryIndex: Int,
        responseTimeElapsedSeconds: Long,
        refreshedDirectionsRouteContext: DirectionsRouteContext,
    ): Result<RouteUpdate> {
        return Result.runCatching {
            buildRefreshedRouteUpdate(refreshedDirectionsRouteContext, responseTimeElapsedSeconds)
        }
    }

    /**
     * Builds the [RouteUpdate] from [refreshedDirectionsRouteContext], which [NavigationRoute]
     * already obtained by calling [DirectionsRouteContext.refreshRoute] itself before invoking
     * [refresh].
     */
    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @WorkerThread
    private fun buildRefreshedRouteUpdate(
        refreshedDirectionsRouteContext: DirectionsRouteContext,
        responseTimeElapsedSeconds: Long,
    ): RouteUpdate {
        logD(LOG_CATEGORY) {
            "Refreshing native route model"
        }
        val updatedRouteModel = refreshedDirectionsRouteContext.toRouteModelsParsingResult(
            routeOptions = parsedRouteData.routeOptions,
            routerOrigin = parsedRouteData.routerOrigin,
            responseOriginApi = parsedRouteData.responseOriginAPI,
        )
        val ttlUpdate = updatedRouteModel.data.route.refreshTtl()?.let {
            OptionallyRefreshedData.Updated<Long?>(it.plus(responseTimeElapsedSeconds))
        } ?: OptionallyRefreshedData.NoUpdates()
        return RouteUpdate(
            routeModelsParsingResult = updatedRouteModel,
            routeRefreshMetadata = RouteRefreshMetadata(isUpToDate = true),
            newExpirationTimeElapsedSeconds = ttlUpdate,
            overriddenTraffic = OptionallyRefreshedData.NoUpdates(),
        )
    }

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    override fun clientSideRouteUpdate(
        directionsRouteBlock: DirectionsRoute.() -> DirectionsRoute,
        waypointsBlock: List<DirectionsWaypoint>?.() -> List<DirectionsWaypoint>?,
        overriddenTraffic: CongestionNumericOverride?,
        routeRefreshMetadata: RouteRefreshMetadata?,
    ): Result<RouteUpdate> {
        return Result.failure(NotSupportedForNativeRouteObjectException("client side route update"))
    }

    override fun toDirectionsRefreshResponse(): Result<DirectionsRefreshResponse> {
        return Result.failure(
            NotSupportedForNativeRouteObjectException(
                "toDirectionsRefreshResponse",
            ),
        )
    }

    override fun serialize(navigationRouteData: NavigationRouteData): Result<String> {
        // TODO: https://mapbox.atlassian.net/browse/NAVAND-6775
        return Result.failure(
            NotSupportedForNativeRouteObjectException("serialization"),
        )
    }
}
