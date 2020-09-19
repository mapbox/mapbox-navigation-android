package com.mapbox.navigation.ui.maps.route.routearrow.internal

import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.internal.route.RouteConstants.*
import com.mapbox.navigation.ui.internal.utils.CompareUtils.areEqualContentsIgnoreOrder
import com.mapbox.navigation.ui.maps.route.routearrow.api.RouteArrowActions
import com.mapbox.navigation.ui.maps.route.routearrow.internal.RouteArrowUtils.obtainArrowPointsFrom
import com.mapbox.navigation.ui.maps.route.routearrow.model.RouteArrowState
import com.mapbox.turf.TurfMeasurement

class MapboxRouteArrowActions: RouteArrowActions {
    private val maneuverPoints = mutableListOf<Point>()

    override fun getUpdateViewStyleState(style: Style): RouteArrowState.UpdateViewStyleState {
        return RouteArrowState.UpdateViewStyleState(style)
    }

    override fun hideRouteArrowState(): RouteArrowState.UpdateRouteArrowVisibilityState {
        val layerModifications = getHideArrowModifications()
        return RouteArrowState.UpdateRouteArrowVisibilityState(layerModifications)
    }

    override fun showRouteArrowState(): RouteArrowState.UpdateRouteArrowVisibilityState {
        val layerModifications = getShowArrowModifications()
        return RouteArrowState.UpdateRouteArrowVisibilityState(layerModifications)
    }

    override fun redraw(): RouteArrowState.UpdateManeuverArrowState {
        return when (maneuverPoints.isEmpty()) {
            true -> RouteArrowState.UpdateManeuverArrowState(listOf(), null, null)
            false -> {
                val arrowShaftFeature = getFeatureForArrowShaft(maneuverPoints)
                val arrowHeadFeature = getFeatureForArrowHead(maneuverPoints)
                RouteArrowState.UpdateManeuverArrowState(listOf(), arrowShaftFeature, arrowHeadFeature)
            }
        }
    }

    override fun getAddUpcomingManeuverArrowState(routeProgress: RouteProgress): RouteArrowState.UpdateManeuverArrowState {
        val invalidUpcomingStepPoints = (routeProgress.upcomingStepPoints == null
            || routeProgress.upcomingStepPoints!!.size < TWO_POINTS)
        val invalidCurrentStepPoints = routeProgress.currentLegProgress == null
            || routeProgress.currentLegProgress!!.currentStepProgress == null
            || routeProgress.currentLegProgress!!.currentStepProgress!!.stepPoints == null
            || routeProgress.currentLegProgress!!.currentStepProgress!!.stepPoints!!.size < TWO_POINTS

        val visibilityChanges = if (invalidUpcomingStepPoints || invalidCurrentStepPoints) {
            getHideArrowModifications()
        } else {
            getShowArrowModifications()
        }

        val newManeuverPoints = obtainArrowPointsFrom(routeProgress)
        return if (!areEqualContentsIgnoreOrder<Point>(maneuverPoints, newManeuverPoints)) {
            maneuverPoints.clear()
            maneuverPoints.addAll(newManeuverPoints)

            val arrowShaftFeature = getFeatureForArrowShaft(maneuverPoints)
            val arrowHeadFeature = getFeatureForArrowHead(maneuverPoints)

            RouteArrowState.UpdateManeuverArrowState(visibilityChanges, arrowShaftFeature, arrowHeadFeature)
        } else {
            RouteArrowState.UpdateManeuverArrowState(visibilityChanges, null, null)
        }
    }

    private fun getFeatureForArrowShaft(points: List<Point>): Feature {
        val shaft = LineString.fromLngLats(points)
        return Feature.fromGeometry(shaft)
    }

    private fun getFeatureForArrowHead(points: List<Point>): Feature {
        val azimuth = TurfMeasurement.bearing(points[points.size - 2], points[points.size - 1])
        return Feature.fromGeometry(points[points.size - 1]).also {
            it.addNumberProperty(ARROW_BEARING, wrap(azimuth, 0.0, MAX_DEGREES))
        }
    }

    private fun getHideArrowModifications(): List<Pair<String, Visibility>> {
        return listOf(
            Pair(ARROW_SHAFT_LINE_LAYER_ID, Visibility.NONE),
            Pair(ARROW_SHAFT_CASING_LINE_LAYER_ID, Visibility.NONE),
            Pair(ARROW_HEAD_CASING_LAYER_ID, Visibility.NONE),
            Pair(ARROW_HEAD_LAYER_ID, Visibility.NONE)
        )
    }

    private fun getShowArrowModifications(): List<Pair<String, Visibility>> {
        return listOf(
            Pair(ARROW_SHAFT_LINE_LAYER_ID, Visibility.VISIBLE),
            Pair(ARROW_SHAFT_CASING_LINE_LAYER_ID, Visibility.VISIBLE),
            Pair(ARROW_HEAD_CASING_LAYER_ID, Visibility.VISIBLE),
            Pair(ARROW_HEAD_LAYER_ID, Visibility.VISIBLE)
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
