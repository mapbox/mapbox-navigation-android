package com.mapbox.navigation.ui.maps.location

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.location.Location
import androidx.annotation.CallSuper
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.internal.location.PuckAnimationEvaluator
import com.mapbox.navigation.utils.internal.ifNonNull
import java.util.concurrent.CopyOnWriteArraySet

/**
 * This class provides a [LocationProvider] implementation that can be used together with the
 * [LocationComponentPlugin] and provides an easy integration with the
 * [LocationObserver] that produces enhanced location updates.
 *
 * #### Example usage
 * Initialize the location plugin:
 * ```
 * locationComponent = mapView.location.apply {
 *     setLocationProvider(navigationLocationProvider)
 *     addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
 *     enabled = true
 * }
 * ```
 *
 * Pass location data to transition the puck:
 * ```
 * private val locationObserver = object : LocationObserver {
 *     override fun onNewRawLocation(rawLocation: Location) {}
 *     override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
 *         val transitionOptions: (ValueAnimator.() -> Unit)? = if (locationMatcherResult.isTeleport) {
 *             {
 *                 duration = 0
 *             }
 *         } else {
 *             {
 *                 duration = 1000
 *             }
 *         }
 *         navigationLocationProvider.changePosition(
 *             locationMatcherResult.enhancedLocation,
 *             locationMatcherResult.keyPoints,
 *             latLngTransitionOptions = transitionOptions,
 *             bearingTransitionOptions = transitionOptions
 *         )
 * }
 * ```
 */
open class NavigationLocationProvider :
    LocationProvider,
    LocationObserver,
    MapboxNavigationObserver {

    private val locationConsumers = CopyOnWriteArraySet<LocationConsumer>()

    /**
     * Returns the last cached target location value.
     *
     * For precise puck's position use the [OnIndicatorPositionChangedListener].
     */
    var lastLocation: Location? = null
        private set

    /**
     * Returns the last cached key points value.
     *
     * For precise puck's position use the [OnIndicatorPositionChangedListener].
     */
    var lastKeyPoints: List<Location> = emptyList()
        private set

    /**
     * Automatically updated when this class is registered to [MapboxNavigationApp]
     */
    @CallSuper
    override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
        changePosition(
            locationMatcherResult.enhancedLocation,
            locationMatcherResult.keyPoints,
        )
    }

    /**
     * Register the location consumer to the Location Provider.
     *
     * The Location Consumer will get location and bearing updates from the Location Provider.
     *
     * @param locationConsumer
     */
    @SuppressLint("MissingPermission")
    final override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
        if (locationConsumers.add(locationConsumer)) {
            ifNonNull(lastLocation, lastKeyPoints) { location, keyPoints ->
                locationConsumer.notifyLocationUpdates(
                    location,
                    keyPoints,
                    latLngTransitionOptions = {
                        this.duration = 0
                    },
                    bearingTransitionOptions = {
                        this.duration = 0
                    }
                )
            }
        }
    }

    /**
     * Unregister the location consumer from the Location Provider.
     *
     * @param locationConsumer
     */
    final override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) {
        locationConsumers.remove(locationConsumer)
    }

    /**
     * Provide the location update that puck should transition to.
     *
     * @param location the location update
     * @param keyPoints a list (can be empty) of predicted location points
     * leading up to and including the target update.
     * The last point on the list (if not empty) should always be equal to [location].
     * @param latLngTransitionOptions lambda that style that position transition of the puck. See
     * [LocationConsumer.onLocationUpdated].
     * @param bearingTransitionOptions lambda that style that bearing transition of the puck. See
     * [LocationConsumer.onBearingUpdated].
     *
     * @see LocationObserver
     * @see MapboxNavigation.registerLocationObserver
     */
    @CallSuper
    fun changePosition(
        location: Location,
        keyPoints: List<Location> = emptyList(),
        latLngTransitionOptions: (ValueAnimator.() -> Unit)? = null,
        bearingTransitionOptions: (ValueAnimator.() -> Unit)? = null
    ) {
        locationConsumers.forEach {
            it.notifyLocationUpdates(
                location,
                keyPoints,
                latLngTransitionOptions,
                bearingTransitionOptions
            )
        }
        lastLocation = location
        lastKeyPoints = keyPoints
    }

    /**
     * Use [getRegisteredInstance] to share a single instance of the [NavigationLocationProvider].
     *
     * @see [MapboxNavigationApp]
     */
    @CallSuper
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerLocationObserver(this)
    }

    /**
     * Disabled automatically with the [MapboxNavigationApp]. You can manually disable with
     * [MapboxNavigationApp.unregisterObserver].
     *
     * @see [MapboxNavigationApp]
     */
    @CallSuper
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterLocationObserver(this)
    }

    private fun LocationConsumer.notifyLocationUpdates(
        location: Location,
        keyPoints: List<Location>,
        latLngTransitionOptions: (ValueAnimator.() -> Unit)? = null,
        bearingTransitionOptions: (ValueAnimator.() -> Unit)? = null
    ) {
        val latLngUpdates = if (keyPoints.isNotEmpty()) {
            keyPoints.map { Point.fromLngLat(it.longitude, it.latitude) }.toTypedArray()
        } else {
            arrayOf(Point.fromLngLat(location.longitude, location.latitude))
        }
        val bearingUpdates = if (keyPoints.isNotEmpty()) {
            keyPoints.map { it.bearing.toDouble() }.toDoubleArray()
        } else {
            doubleArrayOf(location.bearing.toDouble())
        }

        this.onLocationUpdated(
            location = latLngUpdates,
            options = locationAnimatorOptions(latLngUpdates, latLngTransitionOptions)
        )
        this.onBearingUpdated(bearing = bearingUpdates, options = bearingTransitionOptions)
    }

    private fun locationAnimatorOptions(
        keyPoints: Array<Point>,
        clientOptions: (ValueAnimator.() -> Unit)?
    ): (ValueAnimator.() -> Unit) {
        val evaluator = PuckAnimationEvaluator(keyPoints)
        return {
            // TODO: Remove setDuration once patched in MapsSDK https://github.com/mapbox/mapbox-maps-android/issues/1446
            duration = LocationComponentConstants.DEFAULT_INTERVAL_MILLIS
            evaluator.installIn(this)
            clientOptions?.also { apply(it) }
        }
    }

    companion object {
        /**
         * Get the registered instance or create one and register it to [MapboxNavigationApp].
         */
        @JvmStatic
        fun getRegisteredInstance(): NavigationLocationProvider = MapboxNavigationApp
            .getObservers(NavigationLocationProvider::class).firstOrNull()
            ?: NavigationLocationProvider().also { MapboxNavigationApp.registerObserver(it) }
    }
}
