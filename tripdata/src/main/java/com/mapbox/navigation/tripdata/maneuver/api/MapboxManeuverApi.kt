package com.mapbox.navigation.tripdata.maneuver.api

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.maneuver.ManeuverAction
import com.mapbox.navigation.tripdata.maneuver.ManeuverProcessor
import com.mapbox.navigation.tripdata.maneuver.ManeuverResult
import com.mapbox.navigation.tripdata.maneuver.ManeuverState
import com.mapbox.navigation.tripdata.maneuver.model.Component
import com.mapbox.navigation.tripdata.maneuver.model.Maneuver
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverError
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverOptions
import com.mapbox.navigation.tripdata.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.tripdata.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.tripdata.maneuver.model.SubManeuver
import com.mapbox.navigation.tripdata.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.tripdata.shield.internal.api.getRouteShieldsFromModels
import com.mapbox.navigation.tripdata.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.tripdata.shield.internal.model.ShieldSpriteToDownload
import com.mapbox.navigation.tripdata.shield.model.RouteShieldCallback
import com.mapbox.navigation.tripdata.shield.model.RouteShieldError
import com.mapbox.navigation.tripdata.shield.model.RouteShieldResult
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.launch

/**
 * Mapbox Maneuver Api allows you to request [Maneuver] instructions given
 * a [DirectionsRoute] (to get all maneuvers for the provided route)
 * or [RouteProgress] (to get remaining maneuvers for the provided route).
 */
class MapboxManeuverApi internal constructor(
    private val distanceFormatter: DistanceFormatter,
    private val maneuverOptions: ManeuverOptions,
    private val processor: ManeuverProcessor,
    private val routeShieldApi: MapboxRouteShieldApi,
) {

    private val mainJobController by lazy { InternalJobControlFactory.createMainScopeJobControl() }
    private val maneuverState = ManeuverState()

    /**
     * Mapbox Maneuver Api allows you to request [Maneuver] instructions given
     * a [DirectionsRoute] (to get all maneuvers for the provided route)
     * or [RouteProgress] (to get remaining maneuvers for the provided route).
     */
    @JvmOverloads
    constructor(
        formatter: DistanceFormatter,
        maneuverOptions: ManeuverOptions = ManeuverOptions.Builder().build(),
        routeShieldApi: MapboxRouteShieldApi = MapboxRouteShieldApi(),
    ) : this(
        formatter,
        maneuverOptions,
        ManeuverProcessor,
        routeShieldApi,
    )

    /**
     * Returns a list of [Maneuver]s which are wrappers on top of [BannerInstructions] that are in the provided route.
     *
     * If a [RouteLeg] param is provided, the returned list will only contain [Maneuver]s for the [RouteLeg] provided as param,
     * otherwise, the returned list will only contain [Maneuver]s for the first [RouteLeg] in a [DirectionsRoute].
     *
     * @param route route for which to generate maneuver objects
     * @param routeLegIndex specify to inform the API of the index of [RouteLeg] you wish to get the list of [Maneuver].
     * By default the API returns the list of maneuvers for the first [RouteLeg] in a [DirectionsRoute].
     * @return Expected with [Maneuver]s if success and an error if failure.
     * @see MapboxManeuverView.renderManeuvers
     * @see getRoadShields
     */
    @JvmOverloads
    fun getManeuvers(
        route: NavigationRoute,
        routeLegIndex: Int? = null,
    ): Expected<ManeuverError, List<Maneuver>> {
        val action = ManeuverAction.GetManeuverListWithRoute(
            route.directionsRoute,
            routeLegIndex,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        return when (val result = processor.process(action)) {
            is ManeuverResult.GetManeuverList.Success -> {
                val allManeuvers = result.maneuvers
                ExpectedFactory.createValue(allManeuvers)
            }
            is ManeuverResult.GetManeuverList.Failure -> {
                ExpectedFactory.createError(ManeuverError(result.error))
            }
            else -> {
                throw IllegalArgumentException("Inappropriate $result emitted for $action.")
            }
        }
    }

    /**
     * Returns a list of [Maneuver]s which are wrappers on top of [BannerInstructions] that are in the provided route.
     *
     * Given [RouteProgress] the function prepares a list of remaining [Maneuver]s on the currently active route leg ([RouteLegProgress]).
     *
     * The first upcoming [Maneuver] object will also contain an up-to-date distance remaining
     * from the current user location to the maneuver point, based on [RouteProgress].
     *
     * @param routeProgress current route progress
     * @return Expected with [Maneuver]s if success and an error if failure.
     * @see MapboxManeuverView.renderManeuvers
     * @see getRoadShields
     */
    fun getManeuvers(
        routeProgress: RouteProgress,
    ): Expected<ManeuverError, List<Maneuver>> {
        val action = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter,
        )
        return when (val result = processor.process(action)) {
            is ManeuverResult.GetManeuverListWithProgress.Success -> {
                val allManeuvers = result.maneuvers
                ExpectedFactory.createValue(allManeuvers)
            }
            is ManeuverResult.GetManeuverListWithProgress.Failure -> {
                ExpectedFactory.createError(ManeuverError(result.error))
            }
            else -> {
                throw IllegalArgumentException("Inappropriate $result emitted for $action.")
            }
        }
    }

    /**
     * Given a list of [Maneuver] the function requests legacy road shields (if available) using
     * [BannerComponents.imageBaseUrl] associated in [RoadShieldComponentNode].
     *
     * If you do not wish to download all of the shields at once,
     * make sure to pass in only a list of maneuvers that you'd like download the road shields.
     *
     * The function is safe to be called repeatably, all the results are cached in-memory
     * and requests are managed to avoid duplicating network bandwidth usage.
     *
     * The function returns list of either [RouteShieldError] or [RouteShieldResult] in
     * [RouteShieldCallback.onRoadShields] and can be used when displaying [PrimaryManeuver],
     * [SecondaryManeuver] and [SubManeuver].
     *
     * @param maneuvers list of maneuvers
     * @param shieldCallback invoked with appropriate result
     * @see MapboxManeuverView.renderManeuverWith
     */
    fun getRoadShields(
        maneuvers: List<Maneuver>,
        shieldCallback: RouteShieldCallback,
    ) {
        getRoadShields(
            null,
            null,
            maneuvers,
            shieldCallback,
        )
    }

    /**
     * Given a list of [Maneuver] the function requests mapbox designed road shields (if available)
     * using [BannerComponents.mapboxShield] associated in [RoadShieldComponentNode]. If for any
     * reason the API fails to download the mapbox designed shields, it fallbacks to use legacy
     * [BannerComponents.imageBaseUrl] if available.
     *
     * If you do not wish to download all of the shields at once,
     * make sure to pass in only a list of maneuvers that you'd like download the road shields.
     *
     * The function is safe to be called repeatably, all the results are cached in-memory
     * and requests are managed to avoid duplicating network bandwidth usage.
     *
     * The function returns list of either [RouteShieldError] or [RouteShieldResult] in
     * [RouteShieldCallback.onRoadShields] and can be used when displaying [PrimaryManeuver],
     * [SecondaryManeuver] and [SubManeuver].
     *
     * @param maneuvers list of maneuvers
     * @param shieldCallback invoked with appropriate result
     */
    fun getRoadShields(
        userId: String?,
        styleId: String?,
        maneuvers: List<Maneuver>,
        shieldCallback: RouteShieldCallback,
    ) {
        mainJobController.scope.launch {
            val shields = mutableListOf<Expected<RouteShieldError, RouteShieldResult>>()
            maneuvers.forEach { maneuver ->
                routeShieldApi.getRouteShieldsFromModels(
                    maneuver.primary.componentList.findShieldsToDownload(
                        userId = userId,
                        styleId = styleId,
                    ),
                ).let { results ->
                    shields.addAll(results)
                }
                maneuver.secondary?.let { secondary ->
                    routeShieldApi.getRouteShieldsFromModels(
                        secondary.componentList.findShieldsToDownload(
                            userId = userId,
                            styleId = styleId,
                        ),
                    ).let { results ->
                        shields.addAll(results)
                    }
                }
                maneuver.sub?.let { sub ->
                    routeShieldApi.getRouteShieldsFromModels(
                        sub.componentList.findShieldsToDownload(
                            userId = userId,
                            styleId = styleId,
                        ),
                    ).let { results ->
                        shields.addAll(results)
                    }
                }
            }
            shieldCallback.onRoadShields(shields = shields)
        }
    }

    /**
     * Invoke the function to cancel any job invoked through other APIs
     */
    fun cancel() {
        routeShieldApi.cancel()
    }

    private fun List<Component>.findShieldsToDownload(
        userId: String? = null,
        styleId: String? = null,
    ): List<RouteShieldToDownload> {
        return mapNotNull { component ->
            if (component.node is RoadShieldComponentNode) {
                val legacyShieldUrl = component.node.shieldUrl
                val legacy = if (legacyShieldUrl != null) {
                    RouteShieldToDownload.MapboxLegacy(legacyShieldUrl)
                } else {
                    null
                }
                val mapboxShield = component.node.mapboxShield
                val designed = if (userId != null && styleId != null && mapboxShield != null) {
                    RouteShieldToDownload.MapboxDesign(
                        ShieldSpriteToDownload(
                            userId = userId,
                            styleId = styleId,
                        ),
                        mapboxShield = mapboxShield,
                        legacyFallback = legacy,
                    )
                } else {
                    null
                }

                designed ?: legacy
            } else {
                null
            }
        }
    }
}
