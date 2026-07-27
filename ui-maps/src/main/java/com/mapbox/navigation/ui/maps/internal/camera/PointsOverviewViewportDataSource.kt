package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.camera.data.debugger.MapboxNavigationViewportDataSourceDebugger

/**
 * Produces the overview camera frame for the **points overview** feature: it frames only the
 * configured points (provided via [additionalPointsToFrame]) and, unlike
 * [RouteOverviewViewportDataSource], does not track the route geometry or the puck.
 *
 * Shares the framing logic with route overview via [BaseOverviewViewportDataSource].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class PointsOverviewViewportDataSource(
    mapboxMap: MapboxMap,
) : BaseOverviewViewportDataSource(mapboxMap) {

    override fun getPointsToFrame(): List<Point> = emptyList()

    override fun updateDebuggerPoints(
        debugger: MapboxNavigationViewportDataSourceDebugger,
        pointsForOverview: List<Point>,
    ) {
        debugger.pointsOnlyForOverview = pointsForOverview
    }
}
