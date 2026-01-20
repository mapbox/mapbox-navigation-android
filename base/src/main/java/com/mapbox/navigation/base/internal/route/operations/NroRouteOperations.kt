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
import com.mapbox.navigation.base.internal.route.parsing.models.ParsedRouteData
import com.mapbox.navigation.base.internal.route.parsing.models.toRouteModelsParsingResult
import com.mapbox.navigation.base.internal.utils.refreshTtl
import com.mapbox.navigation.base.route.RouteRefreshMetadata
import com.mapbox.navigation.utils.internal.logD

private val LOG_CATEGORY = "NRO-ROUTE-OPERATIONS"

internal class NroRouteOperations(
    val directionsRouteContext: DirectionsRouteContext,
    val parsedRouteData: ParsedRouteData,
) : RouteOperations {

    @OptIn(ExperimentalMapboxNavigationAPI::class)
    @WorkerThread
    override fun refresh(
        refreshResponse: DataRef,
        legIndex: Int,
        legGeometryIndex: Int,
        responseTimeElapsedSeconds: Long,
    ): Result<RouteUpdate> {
        logD(LOG_CATEGORY) {
            "Refreshing native route model"
        }
        return directionsRouteContext
            .refreshRoute(refreshResponse, legIndex, legGeometryIndex)
            .mapValue {
                val updatedRouteModel = it.toRouteModelsParsingResult(
                    routeOptions = parsedRouteData.routeOptions,
                    routerOrigin = parsedRouteData.routerOrigin,
                    responseOriginApi = parsedRouteData.responseOriginAPI,
                )
                val ttlUpdate = updatedRouteModel.data.route.refreshTtl()?.let {
                    OptionallyRefreshedData.Updated<Long?>(
                        it.plus(
                            responseTimeElapsedSeconds,
                        ),
                    )
                } ?: OptionallyRefreshedData.NoUpdates()
                Result.success(
                    RouteUpdate(
                        routeModelsParsingResult = updatedRouteModel,
                        routeRefreshMetadata = RouteRefreshMetadata(isUpToDate = true),
                        newExpirationTimeElapsedSeconds = ttlUpdate,
                        overriddenTraffic = OptionallyRefreshedData.NoUpdates(),
                    ),
                )
            }.getValueOrElse {
                Result.failure(Throwable("error refreshing NRO: $it"))
            }
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
