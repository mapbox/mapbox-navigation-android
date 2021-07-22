package com.mapbox.navigation.instrumentation_tests.utils.idling

import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver

/**
 * This class is meant to register for [RoutesObserver.onRoutesChanged] updates.
 *   1. [register]: Start expecting onRoutesChanged calls
 *   2. [next]: Wait for a onRoutesChanged call
 *   3. [unregister]: Clean up the idling resource and stop listening
 *
 * [routesObserved]: At any point, all the available results can be assessed through the list
 */
class RoutesObserverIdlingResource(
    val mapboxNavigation: MapboxNavigation
) {

    /**
     * Allow callers to access the list of routes observed at any point.
     */
    private val mutableRoutesObserved = mutableListOf<List<DirectionsRoute>>()
    var routesObserved: List<List<DirectionsRoute>> = mutableRoutesObserved
        private set

    private var expected = 0

    private var callback: IdlingResource.ResourceCallback? = null

    /**
     * Start listening for routes.
     */
    fun register() = apply {
        IdlingRegistry.getInstance().register(idlingResource)
        mapboxNavigation.registerRoutesObserver(routesObserver)
    }

    /**
     * Wait for the routes observer to find a route.
     */
    fun next(): List<DirectionsRoute> {
        expected++
        if (routesObserved.size == expected) {
            return routesObserved.last()
        }
        Espresso.onIdle()
        return routesObserved.last()
    }

    /**
     * Stop listening for routes.
     */
    fun unregister() {
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        IdlingRegistry.getInstance().unregister(idlingResource)
    }

    /** Used to communicate with **/
    private val routesObserver = RoutesObserver { routes ->
        mutableRoutesObserved.add(routes)
        callback?.onTransitionToIdle()
    }

    /** Used to communicate with the [IdlingRegistry] **/
    private val idlingResource = object : IdlingResource {
        override fun getName() = "RoutesObserverIdlingResource"

        override fun isIdleNow(): Boolean = routesObserved.size == expected

        override fun registerIdleTransitionCallback(
            resourceCallback: IdlingResource.ResourceCallback
        ) {
            callback = resourceCallback
            if (isIdleNow) {
                callback?.onTransitionToIdle()
            }
        }
    }
}
