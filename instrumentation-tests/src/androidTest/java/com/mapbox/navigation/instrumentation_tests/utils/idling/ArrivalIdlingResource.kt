package com.mapbox.navigation.instrumentation_tests.utils.idling

import androidx.test.espresso.IdlingResource
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.testing.ui.idling.NavigationIdlingResource

/**
 * Becomes idle when [ArrivalObserver.onFinalDestinationArrival] gets invoked.
 *
 * This is detected automatically when `mapboxNavigation` is provided.
 * Otherwise, it should be invoked manually with [ArrivalIdlingResource.onFinalDestinationArrival].
 */
class ArrivalIdlingResource(
    mapboxNavigation: MapboxNavigation? = null
) : NavigationIdlingResource(), ArrivalObserver {

    private var arrived = false
    private var callback: IdlingResource.ResourceCallback? = null

    init {
        mapboxNavigation?.registerArrivalObserver(
            object : ArrivalObserver {
                override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
                    // do nothing
                }

                override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
                    arrived = true
                    callback?.onTransitionToIdle()
                }
            }
        )
    }

    override fun getName() = "ArrivalIdlingResource"

    override fun isIdleNow() = arrived

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
        if (isIdleNow) {
            callback?.onTransitionToIdle()
        }
    }

    override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
        // do nothing
    }

    override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
        arrived = true
        callback?.onTransitionToIdle()
    }
}
