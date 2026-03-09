package com.mapbox.navigation.testing.utils.idling

import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import com.mapbox.common.location.Location
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver

/**
 * Idling resource for testing situations that require a valid location.
 *
 * @param mapboxNavigation the navigation interface used to register a [LocationObserver].
 */
class FirstLocationIdlingResource(
    private val mapboxNavigation: MapboxNavigation
) {

    var firstLocation: Location? = null
        private set
    private var callback: IdlingResource.ResourceCallback? = null

    /**
     * This will block the execution of a test while waiting for the first
     * enhanced location from the navigator.
     *
     * If this fails due to timeout, locations are not being sent to the navigator.
     */
    fun firstLocationSync(): Location {
        IdlingRegistry.getInstance().register(idlingResource)
        mapboxNavigation.registerLocationObserver(locationObserver)
        Espresso.onIdle()
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        IdlingRegistry.getInstance().unregister(idlingResource)
        return firstLocation!!
    }

    /** Used to communicate with the [IdlingRegistry] **/
    private val idlingResource = object : IdlingResource {
        override fun getName() = "FirstLocationIdlingResource"

        override fun isIdleNow(): Boolean = firstLocation != null

        override fun registerIdleTransitionCallback(
            resourceCallback: IdlingResource.ResourceCallback
        ) {
            callback = resourceCallback
            if (isIdleNow) {
                callback?.onTransitionToIdle()
            }
        }
    }

    /** Used to communicate with [MapboxNavigation.registerLocationObserver] **/
    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            // Do nothing
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            if (firstLocation == null) {
                firstLocation = locationMatcherResult.enhancedLocation
                callback?.onTransitionToIdle()
            }
        }
    }
}
