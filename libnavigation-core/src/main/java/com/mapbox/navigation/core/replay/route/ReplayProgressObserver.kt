package com.mapbox.navigation.core.replay.route

import android.util.Log
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

/**
 * Register this to [MapboxNavigation.registerRouteProgressObserver].
 * This class will feed locations to your [MapboxReplayer] and simulate
 * your active route for you.
 */
class ReplayProgressObserver(
    /**
     * As navigation receives [RouteProgress], this will push events to your
     * replay history player to be played.
     */
    private val mapboxReplayer: MapboxReplayer
) : RouteProgressObserver {

    private val replayRouteMapper = ReplayRouteMapper()
    private var currentRouteLeg: RouteLeg? = null

    /**
     * @param options allow you to control the driver and car behavior.
     * @return [ReplayProgressObserver]
     */
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
        val routeProgressRouteLeg = routeProgress.currentLegProgress?.routeLeg
        if (routeProgressRouteLeg != currentRouteLeg) {
            this.currentRouteLeg = routeProgressRouteLeg
            onRouteLegChanged(routeProgressRouteLeg)
        }
    }

    private fun onRouteLegChanged(routeProgressRouteLeg: RouteLeg?) {
        if (routeProgressRouteLeg != null) {
            val replayEvents = replayRouteMapper.mapRouteLegGeometry(routeProgressRouteLeg)
            if (replayEvents.isNotEmpty()) {
                Log.i("location_debug", "location_debug onRouteLegChanged replayEvents:${replayEvents.size}")
                mapboxReplayer.pushEvents(replayEvents)
                mapboxReplayer.seekTo(replayEvents.first())
            }
        }
    }
}
