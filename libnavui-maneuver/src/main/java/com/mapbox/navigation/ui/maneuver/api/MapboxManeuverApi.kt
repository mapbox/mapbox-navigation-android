package com.mapbox.navigation.ui.maneuver.api

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maneuver.ManeuverAction
import com.mapbox.navigation.ui.maneuver.ManeuverProcessor
import com.mapbox.navigation.ui.maneuver.ManeuverResult
import com.mapbox.navigation.ui.maneuver.ManeuverState
import com.mapbox.navigation.ui.maneuver.model.Component
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import com.mapbox.navigation.ui.maneuver.model.ManeuverError
import com.mapbox.navigation.ui.maneuver.model.ManeuverOptions
import com.mapbox.navigation.ui.maneuver.model.PrimaryManeuver
import com.mapbox.navigation.ui.maneuver.model.RoadShield
import com.mapbox.navigation.ui.maneuver.model.RoadShieldComponentNode
import com.mapbox.navigation.ui.maneuver.model.RoadShieldError
import com.mapbox.navigation.ui.maneuver.model.SecondaryManeuver
import com.mapbox.navigation.ui.maneuver.model.SubManeuver
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.shield.api.MapboxRouteShieldApi
import com.mapbox.navigation.ui.shield.internal.api.getRouteShieldsFromModels
import com.mapbox.navigation.ui.shield.internal.model.RouteShieldToDownload
import com.mapbox.navigation.ui.shield.internal.model.ShieldSpriteToDownload
import com.mapbox.navigation.ui.shield.model.RouteShield
import com.mapbox.navigation.ui.shield.model.RouteShieldResult
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.launch

/**
 * Mapbox Maneuver Api allows you to request [Maneuver] instructions given
 * a [DirectionsRoute] (to get all maneuvers for the provided route)
 * or [RouteProgress] (to get remaining maneuvers for the provided route).
 *
 * You can use the default [MapboxManeuverView] to render the results of the functions exposed by this API.
 */
class MapboxManeuverApi internal constructor(
    private val distanceFormatter: DistanceFormatter,
    private val maneuverOptions: ManeuverOptions,
    private val processor: ManeuverProcessor,
    private val routeShieldApi: MapboxRouteShieldApi
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
        routeShieldApi
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
        route: DirectionsRoute,
        routeLegIndex: Int? = null
    ): Expected<ManeuverError, List<Maneuver>> {
        val action = ManeuverAction.GetManeuverListWithRoute(
            route,
            routeLegIndex,
            maneuverState,
            maneuverOptions,
            distanceFormatter
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
        routeProgress: RouteProgress
    ): Expected<ManeuverError, List<Maneuver>> {
        val action = ManeuverAction.GetManeuverList(
            routeProgress,
            maneuverState,
            maneuverOptions,
            distanceFormatter
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
     * The function returns maps of [String] to [RoadShield] or [String] to [RoadShieldError] in
     * [RoadShieldCallback.onRoadShields] and can be used when displaying [PrimaryManeuver],
     * [SecondaryManeuver] and [SubManeuver].
     *
     * @param maneuvers list of maneuvers
     * @param callback invoked with appropriate result
     * @see MapboxManeuverView.renderManeuverShields
     */
    @Deprecated(
        message = "The API is incapable of associating multiple shields with a single maneuver id",
        replaceWith = ReplaceWith(
            "getRoadShields(maneuvers, shieldCallback)",
            "com.mapbox.navigation.ui.maneuver.api"
        ),
        level = DeprecationLevel.WARNING
    )
    fun getRoadShields(
        maneuvers: List<Maneuver>,
        callback: RoadShieldCallback
    ) {
        getRoadShields(
            userId = null,
            styleId = null,
            accessToken = null,
            maneuvers = maneuvers,
            callback = callback
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
     * The function returns maps of [String] to [RoadShield] or [String] to [RoadShieldError] in
     * [RoadShieldCallback.onRoadShields] and can be used when displaying [PrimaryManeuver],
     * [SecondaryManeuver] and [SubManeuver].
     *
     * @param maneuvers list of maneuvers
     * @param callback invoked with appropriate result
     * @see MapboxManeuverView.renderManeuverShields
     */
    @Deprecated(
        message = "The API is incapable of associating multiple shields with a single maneuver id",
        replaceWith = ReplaceWith(
            "getRoadShields(userId, styleId, accessToken, maneuvers, shieldCallback)",
            "com.mapbox.navigation.ui.maneuver.api"
        ),
        level = DeprecationLevel.WARNING
    )
    fun getRoadShields(
        userId: String?,
        styleId: String?,
        accessToken: String?,
        maneuvers: List<Maneuver>,
        callback: RoadShieldCallback
    ) {
        mainJobController.scope.launch {
            val shieldMap = hashMapOf<String, List<RoadShield>>()
            val errorMap = hashMapOf<String, List<RoadShieldError>>()

            maneuvers.forEach { maneuver ->
                routeShieldApi.getRouteShieldsFromModels(
                    maneuver.primary.componentList.findShieldsToDownload(
                        accessToken = accessToken,
                        userId = userId,
                        styleId = styleId
                    )
                ).let { results ->
                    val shields = mutableListOf<RoadShield>()
                    val errors = mutableListOf<RoadShieldError>()
                    results.forEach { result ->
                        result.fold(
                            { error ->
                                errors.add(
                                    RoadShieldError(url = error.url, message = error.errorMessage)
                                )
                            },
                            { routeShieldResult ->
                                shields.add(getShield(routeShieldResult))
                            }
                        )
                    }
                    shieldMap[maneuver.primary.id] = shields
                    if (errors.isNotEmpty()) {
                        errorMap[maneuver.primary.id] = errors
                    }
                }
                maneuver.secondary?.let { secondary ->
                    routeShieldApi.getRouteShieldsFromModels(
                        secondary.componentList.findShieldsToDownload(
                            accessToken = accessToken,
                            userId = userId,
                            styleId = styleId
                        )
                    ).let { results ->
                        val shields = mutableListOf<RoadShield>()
                        val errors = mutableListOf<RoadShieldError>()
                        results.forEach { result ->
                            result.fold(
                                { error ->
                                    errors.add(
                                        RoadShieldError(
                                            url = error.url,
                                            message = error.errorMessage
                                        )
                                    )
                                },
                                { routeShieldResult ->
                                    shields.add(getShield(routeShieldResult))
                                }
                            )
                        }
                        shieldMap[secondary.id] = shields
                        if (errors.isNotEmpty()) {
                            errorMap[secondary.id] = errors
                        }
                    }
                }
                maneuver.sub?.let { sub ->
                    routeShieldApi.getRouteShieldsFromModels(
                        sub.componentList.findShieldsToDownload(
                            accessToken = accessToken,
                            userId = userId,
                            styleId = styleId
                        )
                    ).let { results ->
                        val shields = mutableListOf<RoadShield>()
                        val errors = mutableListOf<RoadShieldError>()
                        results.forEach { result ->
                            result.fold(
                                { error ->
                                    errors.add(
                                        RoadShieldError(
                                            url = error.url,
                                            message = error.errorMessage
                                        )
                                    )
                                },
                                { routeShieldResult ->
                                    shields.add(getShield(routeShieldResult))
                                }
                            )
                        }
                        shieldMap[sub.id] = shields
                        if (errors.isNotEmpty()) {
                            errorMap[sub.id] = errors
                        }
                    }
                }
            }
            callback.onRoadShields(
                maneuvers = maneuvers,
                shields = shieldMap.mapValues { it.value.firstOrNull() },
                errors = errorMap.mapValues { it.value.first() }
            )
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
     * The function returns maps of [String] to [RoadShield] or [String] to [RoadShieldError] in
     * [RoadShieldsCallback.onRoadShields] and can be used when displaying [PrimaryManeuver],
     * [SecondaryManeuver] and [SubManeuver].
     *
     * @param maneuvers list of maneuvers
     * @param shieldCallback invoked with appropriate result
     * @see MapboxManeuverView.renderManeuverShields
     */
    fun getRoadShields(
        maneuvers: List<Maneuver>,
        shieldCallback: RoadShieldsCallback
    ) {
        getRoadShields(
            null,
            null,
            null,
            maneuvers,
            shieldCallback
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
     * The function returns maps of [String] to [RoadShield] or [String] to [RoadShieldError] in
     * [RoadShieldsCallback.onRoadShields] and can be used when displaying [PrimaryManeuver],
     * [SecondaryManeuver] and [SubManeuver].
     *
     * @param maneuvers list of maneuvers
     * @param shieldCallback invoked with appropriate result
     * @see MapboxManeuverView.renderManeuverShields
     */
    fun getRoadShields(
        userId: String?,
        styleId: String?,
        accessToken: String?,
        maneuvers: List<Maneuver>,
        shieldCallback: RoadShieldsCallback
    ) {
        mainJobController.scope.launch {
            val shieldMap = hashMapOf<String, List<RoadShield>>()
            val errorMap = hashMapOf<String, List<RoadShieldError>>()

            maneuvers.forEach { maneuver ->
                routeShieldApi.getRouteShieldsFromModels(
                    maneuver.primary.componentList.findShieldsToDownload(
                        accessToken = accessToken,
                        userId = userId,
                        styleId = styleId
                    )
                ).let { results ->
                    val shields = mutableListOf<RoadShield>()
                    val errors = mutableListOf<RoadShieldError>()
                    results.forEach { result ->
                        result.fold(
                            { error ->
                                errors.add(
                                    RoadShieldError(url = error.url, message = error.errorMessage)
                                )
                            },
                            { routeShieldResult ->
                                shields.add(getShield(routeShieldResult))
                            }
                        )
                    }
                    shieldMap[maneuver.primary.id] = shields
                    if (errors.isNotEmpty()) {
                        errorMap[maneuver.primary.id] = errors
                    }
                }
                maneuver.secondary?.let { secondary ->
                    routeShieldApi.getRouteShieldsFromModels(
                        secondary.componentList.findShieldsToDownload(
                            accessToken = accessToken,
                            userId = userId,
                            styleId = styleId
                        )
                    ).let { results ->
                        val shields = mutableListOf<RoadShield>()
                        val errors = mutableListOf<RoadShieldError>()
                        results.forEach { result ->
                            result.fold(
                                { error ->
                                    errors.add(
                                        RoadShieldError(
                                            url = error.url,
                                            message = error.errorMessage
                                        )
                                    )
                                },
                                { routeShieldResult ->
                                    shields.add(getShield(routeShieldResult))
                                }
                            )
                        }
                        shieldMap[secondary.id] = shields
                        if (errors.isNotEmpty()) {
                            errorMap[secondary.id] = errors
                        }
                    }
                }
                maneuver.sub?.let { sub ->
                    routeShieldApi.getRouteShieldsFromModels(
                        sub.componentList.findShieldsToDownload(
                            accessToken = accessToken,
                            userId = userId,
                            styleId = styleId
                        )
                    ).let { results ->
                        val shields = mutableListOf<RoadShield>()
                        val errors = mutableListOf<RoadShieldError>()
                        results.forEach { result ->
                            result.fold(
                                { error ->
                                    errors.add(
                                        RoadShieldError(
                                            url = error.url,
                                            message = error.errorMessage
                                        )
                                    )
                                },
                                { routeShieldResult ->
                                    shields.add(getShield(routeShieldResult))
                                }
                            )
                        }
                        shieldMap[sub.id] = shields
                        if (errors.isNotEmpty()) {
                            errorMap[sub.id] = errors
                        }
                    }
                }
            }
            shieldCallback.onRoadShields(
                maneuvers = maneuvers,
                shields = shieldMap,
                errors = errorMap
            )
        }
    }

    /**
     * Invoke the function to cancel any job invoked through other APIs
     */
    fun cancel() {
        routeShieldApi.cancel()
    }

    private fun List<Component>.findShieldsToDownload(
        accessToken: String? = null,
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
                val designed = if (
                    userId != null && styleId != null &&
                    mapboxShield != null && accessToken != null
                ) {
                    RouteShieldToDownload.MapboxDesign(
                        ShieldSpriteToDownload(
                            userId = userId,
                            styleId = styleId
                        ),
                        accessToken = accessToken,
                        mapboxShield = mapboxShield,
                        legacyFallback = legacy
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

    private fun getShield(routeShieldResult: RouteShieldResult): RoadShield {
        return when (routeShieldResult.shield) {
            is RouteShield.MapboxLegacyShield -> {
                RoadShield(
                    shieldUrl = routeShieldResult.shield.url,
                    shieldIcon = routeShieldResult.shield.byteArray,
                    mapboxShield = null
                )
            }
            else -> {
                RoadShield(
                    shieldUrl = routeShieldResult.shield.url,
                    shieldIcon = routeShieldResult.shield.byteArray,
                    mapboxShield =
                    (routeShieldResult.shield as RouteShield.MapboxDesignedShield).mapboxShield
                )
            }
        }
    }
}
