package com.mapbox.navigation.instrumentation_tests.utils.idling

import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.ui.http.MockWebServerRule
import org.junit.Assert.fail

/**
 * Idling resource for testing route requests with MapboxNavigation.
 *
 * @param mapboxNavigation the navigation interface used to request routes.
 * @param routeOptions the options used for the route request. Set the [RouteOptions.baseUrl]
 * using the [MockWebServerRule.baseUrl] in order to mock requests.
 */
class RouteRequestIdlingResource(
    private val mapboxNavigation: MapboxNavigation,
    private val routeOptions: RouteOptions
) {
    /**
     * Getter helper, stores the results from [requestRoutesSync]
     */
    var directionsRoutes: List<DirectionsRoute>? = null
        private set

    private lateinit var callback: IdlingResource.ResourceCallback

    /**
     * This will block the execution of your test while the directions
     * resources are being requested. Return the results upon completion.
     *
     * If the request fails, the test will fail.
     */
    fun requestRoutesSync(): List<DirectionsRoute> {
        IdlingRegistry.getInstance().register(idlingResource)
        mapboxNavigation.requestRoutes(routeOptions, routesRequestCallback)
        Espresso.onIdle()
        IdlingRegistry.getInstance().unregister(idlingResource)
        return directionsRoutes!!
    }

    /** Used to communicate with the [IdlingRegistry] **/
    private val idlingResource = object : IdlingResource {
        override fun getName() = "RouteRequestIdlingResource"

        override fun isIdleNow(): Boolean = directionsRoutes != null

        override fun registerIdleTransitionCallback(
            resourceCallback: IdlingResource.ResourceCallback
        ) {
            callback = resourceCallback
            if (isIdleNow) {
                callback.onTransitionToIdle()
            }
        }
    }

    /** Used to communicate with [MapboxNavigation.requestRoutes] **/
    private val routesRequestCallback = object : RouterCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>, routerOrigin: RouterOrigin) {
            directionsRoutes = routes
            callback.onTransitionToIdle()
        }

        override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
            fail()
        }

        override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
            fail("onRoutesRequestCanceled")
        }
    }
}
