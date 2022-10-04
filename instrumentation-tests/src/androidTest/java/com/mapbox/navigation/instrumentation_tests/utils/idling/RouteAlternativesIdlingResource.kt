package com.mapbox.navigation.instrumentation_tests.utils.idling

import android.os.Looper
import androidx.test.espresso.IdlingResource
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.routealternatives.RouteAlternativesObserver
import com.mapbox.navigation.testing.ui.idling.NavigationIdlingResource
import com.mapbox.navigation.testing.ui.utils.runOnMainSync
import junit.framework.Assert.assertNotNull

/**
 * Becomes idle when [RouteAlternativesObserver.onRouteAlternatives] is invoked,
 * and then it is automatically unsubscribed. The results for the moment,
 * can be accessed via [routeProgress] and [alternatives]
 *
 * If your test needs to receive the next set of alternatives, you must create a new
 * instance of [RouteAlternativesIdlingResource] to capture the next callback.
 * @param mapboxNavigation the instance of [MapboxNavigation]
 * @param onRoutesAlternatives lambda returns boolean value: true if alternatives
 * observer must keep working, false otherwise. If false is returned resource transiting for busy
 * to idling (see [IdlingResource.ResourceCallback.onTransitionToIdle]).
 */
class RouteAlternativesIdlingResource(
    val mapboxNavigation: MapboxNavigation,
    private val onRoutesAlternatives: (
        RouteProgress,
        List<DirectionsRoute>,
        RouterOrigin
    ) -> Boolean = { _, _, _ -> true },
) : NavigationIdlingResource(), RouteAlternativesObserver {

    var routeProgress: RouteProgress? = null
        private set
    var alternatives: List<DirectionsRoute>? = null
        private set
    var calledOnMainThread = false

    private var callback: IdlingResource.ResourceCallback? = null

    init {
        runOnMainSync {
            mapboxNavigation.registerRouteAlternativesObserver(this)
        }
    }

    override fun getName() = this::class.simpleName

    override fun isIdleNow() = alternatives != null

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
        if (isIdleNow) {
            callback?.onTransitionToIdle()
        }
    }

    override fun onRouteAlternatives(
        routeProgress: RouteProgress,
        alternatives: List<DirectionsRoute>,
        routerOrigin: RouterOrigin
    ) {
        if (!onRoutesAlternatives.invoke(routeProgress, alternatives, routerOrigin)) {
            return
        }

        // Verify this happens on the main thread.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            calledOnMainThread = true
        }

        mapboxNavigation.unregisterRouteAlternativesObserver(this)
        this.routeProgress = routeProgress
        this.alternatives = alternatives

        callback?.onTransitionToIdle()
    }

    fun verifyOnRouteAlternativesAndProgressReceived() {
        assertNotNull(routeProgress)
        assertNotNull(alternatives)
    }
}
