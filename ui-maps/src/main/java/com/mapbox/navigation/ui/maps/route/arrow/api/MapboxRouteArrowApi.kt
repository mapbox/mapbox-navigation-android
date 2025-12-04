package com.mapbox.navigation.ui.maps.route.arrow.api

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ARROW_BEARING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MAX_DEGREES
import com.mapbox.navigation.ui.maps.route.arrow.RouteArrowUtils
import com.mapbox.navigation.ui.maps.route.arrow.model.ArrowAddedValue
import com.mapbox.navigation.ui.maps.route.arrow.model.ArrowVisibilityChangeValue
import com.mapbox.navigation.ui.maps.route.arrow.model.ClearArrowsValue
import com.mapbox.navigation.ui.maps.route.arrow.model.InvalidPointError
import com.mapbox.navigation.ui.maps.route.arrow.model.ManeuverArrow
import com.mapbox.navigation.ui.maps.route.arrow.model.RemoveArrowValue
import com.mapbox.navigation.ui.maps.route.arrow.model.UpdateManeuverArrowValue
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.turf.TurfMeasurement
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Responsible for displaying a maneuver arrow representing the next maneuver.
 * The maneuver arrow is calculated based on the route progress and the data returned should
 * be rendered on the map using the [MapboxRouteArrowView] class. Generally this class should be called
 * on each route progress update in order to ensure the arrow displayed is kept consistent
 * with the state of navigation.
 *
 * The two principal classes for the maneuver arrow are the [MapboxRouteArrowApi] and the
 * [MapboxRouteArrowView].
 *
 * Like the route line components the [MapboxRouteArrowApi] consumes data from the Navigation SDK,
 * specifically the [RouteProgress], and produces data for rendering on the map by the
 * [MapboxRouteArrowView]. Simple usage of the maneuver arrows would look like:
 *
 * ```java
 * RouteArrowOptions routeArrowOptions = new RouteArrowOptions.Builder(context)
 *  .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
 *  .build()
 * MapboxRouteArrowApi routeArrow = new MapboxRouteArrowApi()
 * MapboxRouteArrowView routeArrowView = new MapboxRouteArrowView(routeArrowOptions)
 * ```
 *
 * or
 *
 * ```kotlin
 * val routeArrowOptions = RouteArrowOptions.Builder(context)
 *      .withAboveLayerId(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
 *      .build()
 * val routeArrow = MapboxRouteArrowApi()
 * val routeArrowView = MapboxRouteArrowView(routeArrowOptions)
 * ```
 * NOTE: the above example is setting the layer above which the arrow(s) should be located. This
 * constant is a good starting point but you may have a use case that requires setting the
 * layer elevation for the arrows differently. In addition, if using this constant it is important
 * that the route line related layers be initialized before any rendering of the arrows is done.
 * The route line related layers can be created either by calling [MapboxRouteLineView.initializeLayers]
 * or by calling one of the render methods on [MapboxRouteLineView]. In most cases it is not
 * necessary to explicitly call [MapboxRouteLineView.initializeLayers] as calling any of the
 * render methods of [MapboxRouteLineView] will initialize the layers and typically a route line
 * will be drawn before a maneuver arrow.
 *
 * In order for the [MapboxRouteArrowApi] to function it needs route progress updates.
 * An application should register a [RouteProgressObserver] with the [MapboxNavigation] class
 * instance and pass the route progress updates to the [MapboxRouteArrowApi] class. Be sure to
 * unregister this listener appropriately according to the lifecycle of your activity or Fragment
 * in order to prevent resource leaks.
 *
 * At a minimum an application should do the following with route progress updates:
 *
 * ```kotlin
 * override fun onRouteProgressChanged(routeProgress: RouteProgress) {
 * val updateState = routeArrow.updateUpcomingManeuverArrow(routeProgress)
 * routeArrowView.render(mapboxMap.getStyle(), updateState)
 * }
 * ```
 *
 */

class MapboxRouteArrowApi {
    private var maneuverPoints = ManeuverArrow(listOf())
    private val arrows: CopyOnWriteArrayList<ManeuverArrow> = CopyOnWriteArrayList()

    /**
     * @return all of the maneuver arrows currently in use
     */
    fun getArrows(): List<ManeuverArrow> {
        return arrows.toList()
    }

    /**
     * Returns a state containing visibility modifications for hiding the maneuver arrow.
     *
     * @return the [ArrowVisibilityChangeValue] for rendering by the view.
     */
    fun hideManeuverArrow(): ArrowVisibilityChangeValue {
        return ArrowVisibilityChangeValue(getHideArrowModifications())
    }

    /**
     * Returns a state containing visibility modifications for showing the maneuver arrow.
     *
     * @return the UpdateRouteArrowVisibilityState for rendering by the view.
     */
    fun showManeuverArrow(): ArrowVisibilityChangeValue {
        return ArrowVisibilityChangeValue(getShowArrowModifications())
    }

    /**
     * Returns the data necessary to re-render or redraw the arrow(s).
     *
     * @return the [ArrowAddedValue] for rendering by the view.
     */
    fun redraw(): ArrowAddedValue {
        val shaftFeatureCollection = getShaftFeatureCollection()
        val arrowHeadFeatureCollection = getArrowHeadFeatureCollection()
        return ArrowAddedValue(
            shaftFeatureCollection,
            arrowHeadFeatureCollection,
        )
    }

    /**
     * Calculates a maneuver arrow based on the route progress and returns a state that can
     * be used to render the arrow on the map.
     *
     * @param routeProgress a [RouteProgress] generated by the core navigation system.
     *
     * @return the [Expected<InvalidPointError, UpdateManeuverArrowValue>] for rendering by the view.
     */
    fun addUpcomingManeuverArrow(routeProgress: RouteProgress):
        Expected<InvalidPointError, UpdateManeuverArrowValue> {
        val visibilityChanges = getVisibilityChanges(routeProgress)
        removeArrow(maneuverPoints)
        maneuverPoints = getManeuverArrow(routeProgress)

        return addArrow(maneuverPoints)
            .fold(
                {
                    UpdateManeuverArrowValue(
                        visibilityChanges,
                        null,
                        null,
                    )
                },
                { value ->
                    UpdateManeuverArrowValue(
                        visibilityChanges,
                        value
                            .arrowShaftFeatureCollection
                            .features()
                            ?.firstOrNull(),
                        value
                            .arrowHeadFeatureCollection
                            .features()
                            ?.firstOrNull(),
                    )
                },
            ).let {
                ExpectedFactory.createValue(it)
            }
    }

    private fun getManeuverArrow(routeProgress: RouteProgress): ManeuverArrow {
        val points = RouteArrowUtils.obtainArrowPointsFrom(routeProgress)
        return ManeuverArrow(points)
    }

    private fun getVisibilityChanges(routeProgress: RouteProgress): List<Pair<String, Visibility>> {
        val invalidUpcomingStepPoints = (
            routeProgress.upcomingStepPoints == null ||
                routeProgress.upcomingStepPoints!!.size < RouteLayerConstants.TWO_POINTS
            )

        val invalidCurrentStepPoints = routeProgress.currentLegProgress == null ||
            routeProgress.currentLegProgress!!.currentStepProgress == null ||
            routeProgress.currentLegProgress!!.currentStepProgress!!.stepPoints == null ||
            routeProgress.currentLegProgress!!.currentStepProgress!!.stepPoints!!.size <
            RouteLayerConstants.TWO_POINTS

        return if (invalidUpcomingStepPoints ||
            invalidCurrentStepPoints ||
            RouteArrowUtils.isArrivalStep(routeProgress)
        ) {
            getHideArrowModifications()
        } else {
            getShowArrowModifications()
        }
    }

    /**
     * Adds an arrow to the map. An arrow is made up of at least
     * two points. The direction of the arrow head is determined by calculating the bearing
     * between the last two points submitted. Each call will add a new arrow.
     *
     * @param arrow contains the points for the maneuver arrow that should be added
     *
     * @return an Expected<InvalidPointError, ArrowAddedValue>
     */
    fun addArrow(arrow: ManeuverArrow): Expected<InvalidPointError, ArrowAddedValue> {
        if (arrow.points.size < RouteLayerConstants.TWO_POINTS) {
            return ExpectedFactory.createError(
                InvalidPointError(
                    "An arrow must have at least 2 points.",
                    null,
                ),
            )
        }

        arrows.add(arrow)
        return ExpectedFactory.createValue(
            ArrowAddedValue(
                getShaftFeatureCollection(),
                getArrowHeadFeatureCollection(),
            ),
        )
    }

    /**
     * Will remove the arrow and return updated data for rendering the arrows.
     *
     * @param arrow a maneuver arrow that should be removed from the map
     *
     * @return a [RemoveArrowValue]
     */
    fun removeArrow(arrow: ManeuverArrow): RemoveArrowValue {
        arrows.remove(arrow)
        return RemoveArrowValue(
            getShaftFeatureCollection(),
            getArrowHeadFeatureCollection(),
        )
    }

    /**
     * Clears all arrows from the map.
     *
     * @return a [ClearArrowsValue]
     */
    fun clearArrows(): ClearArrowsValue {
        arrows.clear()
        maneuverPoints = ManeuverArrow(listOf())
        return ClearArrowsValue(
            getShaftFeatureCollection(),
            getArrowHeadFeatureCollection(),
        )
    }

    private fun getShaftFeatureCollection(): FeatureCollection {
        val shaftFeatures = arrows.map {
            Feature.fromGeometry(LineString.fromLngLats(it.points))
        }
        return FeatureCollection.fromFeatures(shaftFeatures)
    }

    private fun getArrowHeadFeatureCollection(): FeatureCollection {
        val arrowHeadFeatures = arrows.map {
            val azimuth = TurfMeasurement.bearing(
                it.points[it.points.size - 2],
                it.points[it.points.size - 1],
            )
            Feature.fromGeometry(it.points[it.points.size - 1]).also { feature ->
                feature.addNumberProperty(
                    ARROW_BEARING,
                    wrap(azimuth, 0.0, MAX_DEGREES),
                )
            }
        }
        return FeatureCollection.fromFeatures(arrowHeadFeatures)
    }

    private fun getHideArrowModifications(): List<Pair<String, Visibility>> {
        return listOf(
            Pair(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, Visibility.NONE),
            Pair(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID, Visibility.NONE),
            Pair(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID, Visibility.NONE),
            Pair(RouteLayerConstants.ARROW_HEAD_LAYER_ID, Visibility.NONE),
        )
    }

    private fun getShowArrowModifications(): List<Pair<String, Visibility>> {
        return listOf(
            Pair(RouteLayerConstants.ARROW_SHAFT_LINE_LAYER_ID, Visibility.VISIBLE),
            Pair(RouteLayerConstants.ARROW_SHAFT_CASING_LINE_LAYER_ID, Visibility.VISIBLE),
            Pair(RouteLayerConstants.ARROW_HEAD_CASING_LAYER_ID, Visibility.VISIBLE),
            Pair(RouteLayerConstants.ARROW_HEAD_LAYER_ID, Visibility.VISIBLE),
        )
    }

    // This came from MathUtils in the Maps SDK which may have been removed.
    private fun wrap(value: Double, min: Double, max: Double): Double {
        val delta = max - min
        val firstMod = (value - min) % delta
        val secondMod = (firstMod + delta) % delta
        return secondMod + min
    }
}
