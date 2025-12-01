package com.mapbox.navigation.core.replay.route

import androidx.annotation.UiThread
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.performance.PerformanceTracker
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement

/**
 * Register this to [MapboxNavigation.registerRouteProgressObserver].
 * This class will feed locations to your [MapboxReplayer] and simulate
 * your active route for you.
 */
class ReplayProgressObserver @JvmOverloads constructor(
    /**
     * As navigation receives [RouteProgress], this will push events to your
     * replay history player to be played.
     */
    private val mapboxReplayer: MapboxReplayer,
    private val replayRouteMapper: ReplayRouteMapper = ReplayRouteMapper(),
) : RouteProgressObserver {

    private var currentLegIdentifier: RouteLegIdentifier? = null

    /**
     * @param options allow you to control the driver and car behavior.
     * @return [ReplayProgressObserver]
     */
    @UiThread
    fun updateOptions(options: ReplayRouteOptions): ReplayProgressObserver {
        replayRouteMapper.options = options
        return this
    }

    /**
     * Called by [MapboxNavigation] when this class has been registered. If you are handling
     * your own [RouteProgress], you can call this method directly.
     *
     * @param routeProgress from the navigation session
     */
    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        PerformanceTracker.trackPerformanceSync("ReplayProgressObserver") {
            val currentLegProgress = routeProgress.currentLegProgress
            val routeProgressRouteLeg = currentLegProgress?.routeLeg
            val legIdentifier = routeProgress.getCurrentRouteLegIdentifier()
            if (currentLegIdentifier != legIdentifier && currentLegProgress != null) {
                currentLegIdentifier = legIdentifier
                onRouteLegChanged(routeProgressRouteLeg, currentLegProgress.distanceTraveled)
            }
        }
    }

    private fun onRouteLegChanged(routeProgressRouteLeg: RouteLeg?, distanceTraveled: Float) {
        if (routeProgressRouteLeg == null) return
        val replayEvents = replayRouteMapper.mapRouteLegGeometry(routeProgressRouteLeg)
        if (replayEvents.isEmpty()) return
        val seekToIndex = indexAlong(replayEvents, distanceTraveled)
        mapboxReplayer.pushEvents(replayEvents)
        mapboxReplayer.seekTo(replayEvents[seekToIndex])
    }

    /**
     * Measures the distance between the [events] and returns the index of an event that is greater
     * than the [distanceTraveled]. If the distanceTraveled is greater than the event distance, the
     * last index is returned. If the event list is empty -1 is returned.
     *
     * events: 0---1-------2------3--4-----5------
     *         ==distanceTraveled====>||
     * returns index: 5
     */
    private fun indexAlong(events: List<ReplayEventBase>, distanceTraveled: Float): Int {
        var currentDistance = 0.0
        var index = -1
        var lastPoint: Point? = null
        for (item in events) {
            if (item is ReplayEventUpdateLocation) {
                val currentPoint = Point.fromLngLat(item.location.lon, item.location.lat)
                lastPoint?.let { point ->
                    currentDistance += TurfMeasurement.distance(
                        point,
                        currentPoint,
                        TurfConstants.UNIT_METERS,
                    )
                }
                lastPoint = currentPoint
            }
            index++
            if (currentDistance >= distanceTraveled) {
                return index
            }
        }
        return index
    }
}

private data class RouteLegIdentifier(
    val routeId: String,
    val legIndex: Int,
)

private fun RouteProgress.getCurrentRouteLegIdentifier(): RouteLegIdentifier? {
    return currentLegProgress?.let { RouteLegIdentifier(this.navigationRoute.id, it.legIndex) }
}
