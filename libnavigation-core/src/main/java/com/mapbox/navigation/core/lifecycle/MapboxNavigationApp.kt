package com.mapbox.navigation.core.lifecycle

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Manages a default lifecycle for [MapboxNavigation].
 *
 * 1. Call [MapboxNavigationApp.setup] to specify your [NavigationOptions].
 * 2. Register and unregister your [MapboxNavigationObserver] instances with
 * [MapboxNavigationApp.registerObserver] and [MapboxNavigationApp.unregisterObserver].
 * 3. Attach and detach [LifecycleOwner]s to create instances of [MapboxNavigation].
 *
 * ## Examples
 *
 * Register and unregister individual [LifecycleOwner]s. Below is an example of creating a
 * single activity that uses [MapboxNavigation].
 *
 * ```
 * class MyActivity : ComponentActivity() {
 *   override fun onCreate() {
 *     MapboxNavigationApp
 *       .setup(navigationOptions)
 *       .attach(this)
 *   }
 *   override fun onResume() {
 *     MapboxNavigationApp
 *       .registerObserver(locationObserver)
 *   }
 *   override fun onPause() {
 *     MapboxNavigationApp
 *       .unregisterObserver(locationObserver)
 *   }
 * }
 * ```
 *
 * Alternatively, you can enable an entire application by attaching all activities. This will
 * make a [MapboxNavigation] instance available to any activity or fragment.
 *
 * ```
 * class MyApplication : Application() {
 *   override fun onCreate() {
 *     MapboxNavigationApp.setup(this)
 *       .attachAllActivities()
 *       .registerObserver(locationObserver)
 *   }
 * }
 * ```
 */
@ExperimentalPreviewMapboxNavigationAPI
object MapboxNavigationApp {

    // The singleton MapboxNavigationApp is not good for unit testing.
    // See the unit tests in MapboxNavigationAppDelegateTest
    private val mapboxNavigationAppDelegate by lazy { MapboxNavigationAppDelegate() }

    /**
     * Get the lifecycle of the car and app. It is started when either the car or
     * app is in foreground. When both the car and app have been closed, the lifecycle
     * is stopped. The lifecycle is never destroyed.
     */
    val lifecycleOwner: LifecycleOwner by lazy { mapboxNavigationAppDelegate.lifecycleOwner }

    /**
     * Call [MapboxNavigationApp.setup] to provide the application with [NavigationOptions].
     *
     * This call is a no-op if an attached activity is changing configurations
     * [Activity.isChangingConfigurations].
     */
    fun setup(navigationOptions: NavigationOptions) = apply {
        mapboxNavigationAppDelegate.setup(navigationOptions)
    }

    /**
     * Detect when any Activity is in the foreground. Use [attach] and [detach] for
     * granular control of which lifecycle is used for creating [MapboxNavigation].
     */
    fun attachAllActivities() = apply {
        mapboxNavigationAppDelegate.attachAllActivities()
    }

    /**
     * Optional function to detach all observers and disable [MapboxNavigation] from being created.
     * You can re-enable [MapboxNavigation] by calling [MapboxNavigationApp.setup].
     */
    fun disable() = apply {
        mapboxNavigationAppDelegate.disable()
    }

    /**
     * Individually attach a lifecycle onto the [MapboxNavigationApp].
     * If the app has been [setup], then this lifecycle will cause
     * [MapboxNavigation] to be created.
     *
     * It is not required to [detach] every lifecycle. Because when
     * the [Lifecycle] reaches the terminal [Lifecycle.State.DESTROYED] state
     * it will be removed from the observers automatically.
     */
    fun attach(lifecycleOwner: LifecycleOwner) = apply {
        mapboxNavigationAppDelegate.attach(lifecycleOwner)
    }

    /**
     * Individually detach lifecycles from [MapboxNavigationApp].
     * When all LifecycleOwners have been detached, this will cause all
     * [MapboxNavigationObserver.onDetached].
     *
     * Warning: The [CarAppLifecycleOwner] assumes all [detach] calls included an [attach].
     * Detaching an owner that was never attached will create incorrect counters
     * and will potentially cause [MapboxNavigation] to never be created.
     */
    fun detach(lifecycleOwner: LifecycleOwner) = apply {
        mapboxNavigationAppDelegate.detach(lifecycleOwner)
    }

    /**
     * Register an observer to receive the [MapboxNavigation] instance.
     */
    fun registerObserver(mapboxNavigationObserver: MapboxNavigationObserver) = apply {
        mapboxNavigationAppDelegate.registerObserver(mapboxNavigationObserver)
    }

    /**
     * Unregister the observer that was registered through [registerObserver].
     */
    fun unregisterObserver(mapboxNavigationObserver: MapboxNavigationObserver) = apply {
        mapboxNavigationAppDelegate.unregisterObserver(mapboxNavigationObserver)
    }

    /**
     * [MapboxNavigation] has functions that do not require observation streams. This function
     * allows you to get the current instance to call those functions.
     *
     * For example, you do not need to [registerObserver] in order to call
     * [MapboxNavigation.postUserFeedback] or [MapboxNavigation.setRoutes].
     */
    fun current(): MapboxNavigation? = mapboxNavigationAppDelegate.current()
}
