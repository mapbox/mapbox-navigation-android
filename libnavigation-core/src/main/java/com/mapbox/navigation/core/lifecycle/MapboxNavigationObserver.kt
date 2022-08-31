package com.mapbox.navigation.core.lifecycle

import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Defines an object that needs to interact with or observe [MapboxNavigation]. Use the
 * [MapboxNavigationApp] singleton to register and unregister observers with
 * [MapboxNavigationApp.registerObserver] and [MapboxNavigationApp.unregisterObserver].
 *
 * Example of observing locations with a view model
 * ```
 * class MyViewModel : ViewModel() {
 *   private val locationObserver = MyLocationObserver()
 *
 *   val location: LiveData<Location> = locationObserver.location.asLiveData()
 *
 *   init {
 *     MapboxNavigationApp.register(locationObserver)
 *   }
 *
 *   override fun onCleared() {
 *     MapboxNavigationApp.unregister(locationObserver)
 *   }
 * }
 *
 * class MyLocationObserver : MapboxNavigationObserver {
 *   private val mutableLocation = MutableStateFlow<LocationMatcherResult?>(null)
 *   val locationFlow: Flow<LocationMatcherResult?> = mutableLocation
 *
 *   private val locationObserver = object : LocationObserver {
 *     override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
 *       mutableLocation.value = locationMatcherResult
 *     }
 *
 *     override fun onNewRawLocation(rawLocation: Location) {
 *       // no op
 *     }
 *   }
 *
 *   override fun onAttached(mapboxNavigation: MapboxNavigation) {
 *     mapboxNavigation.registerLocationObserver(locationObserver)
 *   }
 *
 *   override fun onDetached(mapboxNavigation: MapboxNavigation) {
 *     mapboxNavigation.unregisterLocationObserver(locationObserver)
 *   }
 * }
 * ```
 */
interface MapboxNavigationObserver {
    /**
     * Signals that the [mapboxNavigation] instance is ready for use. Use this function to
     * register [mapboxNavigation] observers, such as [MapboxNavigation.registerRoutesObserver].
     *
     * After you have registered an observer through [MapboxNavigationApp.registerObserver],
     * the onAttached will be called when at least one [LifecycleOwner] that has been attached to
     * [MapboxNavigationApp.attach] and is at least in the [Lifecycle.State.STARTED] state.
     *
     * @param mapboxNavigation instance that is being attached.
     */
    @UiThread
    fun onAttached(mapboxNavigation: MapboxNavigation)

    /**
     * Signals that the [mapboxNavigation] instance is being detached. Use this function to
     * unregister [mapboxNavigation] observers that were registered in [onAttached].
     *
     * [onDetached] is called when [onAttached] was called at some time in the past and:
     * - All [LifecycleOwner]s that have been attached with [MapboxNavigationApp.attach] have
     * moved to the [Lifecycle.State.CREATED] or [Lifecycle.State.DESTROYED] states.
     * - The last [LifecycleOwner]s is detached [MapboxNavigationApp.detach].
     * - This observer is unregistered with [MapboxNavigationApp.unregisterObserver].
     * - [MapboxNavigationApp.disable] is called.
     *
     * @param mapboxNavigation instance that is being detached.
     */
    @UiThread
    fun onDetached(mapboxNavigation: MapboxNavigation)
}
