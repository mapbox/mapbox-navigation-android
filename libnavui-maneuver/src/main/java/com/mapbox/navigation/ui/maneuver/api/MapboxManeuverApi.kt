package com.mapbox.navigation.ui.maneuver.api

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
import com.mapbox.navigation.ui.maneuver.RoadShieldContentManager
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
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
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
    private val processor: ManeuverProcessor
) {

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private val maneuverState = ManeuverState()
    private val roadShieldContentManager = RoadShieldContentManager()

    /**
     * Mapbox Maneuver Api allows you to request [Maneuver] instructions given
     * a [DirectionsRoute] (to get all maneuvers for the provided route)
     * or [RouteProgress] (to get remaining maneuvers for the provided route).
     */
    @JvmOverloads
    constructor(
        formatter: DistanceFormatter,
        maneuverOptions: ManeuverOptions = ManeuverOptions.Builder().build()
    ) : this(
        formatter,
        maneuverOptions,
        ManeuverProcessor
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
     * Given a list of [Maneuver] the function requests road shields (if available) using urls associated in
     * [RoadShieldComponentNode].
     *
     * If you do not wish to download all of the shields at once,
     * make sure to pass in only a list of maneuvers that you'd like download the road shields.
     *
     * The function is safe to be called repeatably, all the results are cached in-memory
     * and requests are managed to avoid duplicating network bandwidth usage.
     *
     * The return maps of [String] to [RoadShield] or [RoadShieldError] in [RoadShieldCallback.onRoadShields]
     * can be used when displaying [PrimaryManeuver], [SecondaryManeuver], and [SubManeuver].
     *
     * @param maneuvers list of maneuvers
     * @param callback invoked with appropriate result
     * @see MapboxManeuverView.renderManeuverShields
     */
    fun getRoadShields(
        maneuvers: List<Maneuver>,
        callback: RoadShieldCallback
    ) {
        mainJobController.scope.launch {
            val result = roadShieldContentManager.getShields(
                maneuvers
            )
            callback.onRoadShields(
                maneuvers,
                result.shields,
                result.errors
            )
        }
    }

    /**
     * Invoke the function to cancel any job invoked through other APIs
     */
    fun cancel() {
        roadShieldContentManager.cancelAll()
        mainJobController.job.children.forEach {
            it.cancel()
        }
    }
}
