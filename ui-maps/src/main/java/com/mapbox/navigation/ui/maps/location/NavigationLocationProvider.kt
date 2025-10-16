package com.mapbox.navigation.ui.maps.location

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationComponentConstants
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.maps.plugin.locationcomponent.LocationProvider
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.internal.location.PuckAnimationEvaluatorInterpolator
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
 *     puckBearingEnabled = true
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
class NavigationLocationProvider : LocationProvider {

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
     * Configures the puck animation duration in milliseconds.
     * Defaults to 1000ms.
     */
    var puckAnimationDuration: Long = LocationComponentConstants.DEFAULT_INTERVAL_MILLIS

    /**
     * Register the location consumer to the Location Provider.
     *
     * The Location Consumer will get location and bearing updates from the Location Provider.
     *
     * @param locationConsumer
     */
    @SuppressLint("MissingPermission")
    override fun registerLocationConsumer(locationConsumer: LocationConsumer) {
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
                    },
                )
            }
        }
    }

    /**
     * Unregister the location consumer from the Location Provider.
     *
     * @param locationConsumer
     */
    override fun unRegisterLocationConsumer(locationConsumer: LocationConsumer) {
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
    fun changePosition(
        location: Location,
        keyPoints: List<Location> = emptyList(),
        latLngTransitionOptions: (ValueAnimator.() -> Unit)? = null,
        bearingTransitionOptions: (ValueAnimator.() -> Unit)? = null,
    ) {
        locationConsumers.forEach {
            it.notifyLocationUpdates(
                location,
                keyPoints,
                latLngTransitionOptions,
                bearingTransitionOptions,
            )
        }
        lastLocation = location
        lastKeyPoints = keyPoints
    }

    private fun LocationConsumer.notifyLocationUpdates(
        location: Location,
        keyPoints: List<Location>,
        latLngTransitionOptions: (ValueAnimator.() -> Unit)? = null,
        bearingTransitionOptions: (ValueAnimator.() -> Unit)? = null,
    ) {
        val latLngUpdates = if (keyPoints.isNotEmpty()) {
            keyPoints.map { Point.fromLngLat(it.longitude, it.latitude) }.toTypedArray()
        } else {
            arrayOf(Point.fromLngLat(location.longitude, location.latitude))
        }
        val bearingUpdates = if (keyPoints.isNotEmpty()) {
            keyPoints.map { it.bearing }
        } else {
            listOf(location.bearing)
        }.filterNotNull().toDoubleArray()

        val puckAnimationEvaluatorInterpolator = PuckAnimationEvaluatorInterpolator(latLngUpdates)
        this.onLocationUpdated(
            location = latLngUpdates,
            options = {
                this.duration = puckAnimationDuration
                this.interpolator = puckAnimationEvaluatorInterpolator
                this.setEvaluator(puckAnimationEvaluatorInterpolator)
                latLngTransitionOptions?.also { this.apply(it) }
            },
        )
        if (bearingUpdates.isNotEmpty()) {
            this.onBearingUpdated(
                bearing = bearingUpdates,
                options = {
                    // keeping animation frames in sync between bearing and position animation
                    this.interpolator = puckAnimationEvaluatorInterpolator
                    this.duration = puckAnimationDuration
                    bearingTransitionOptions?.also { this.apply(it) }
                },
            )
        }
    }
}
