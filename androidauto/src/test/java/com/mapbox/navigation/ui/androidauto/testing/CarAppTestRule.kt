package com.mapbox.navigation.ui.androidauto.testing

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.ui.androidauto.internal.AndroidAutoLog
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Top level test rule for the android auto library.
 */
class CarAppTestRule : TestWatcher() {

    private var attachedMapboxNavigation: MapboxNavigation? = null
    private val registeredMapboxNavigationObservers = mutableListOf<MapboxNavigationObserver>()

    override fun starting(description: Description) {
        mockkObject(AndroidAutoLog)
        every { AndroidAutoLog.logAndroidAuto(any()) } just runs
        every { AndroidAutoLog.logAndroidAutoFailure(any(), any()) } just runs

        mockkObject(MapboxNavigationApp)
        mockkStatic(MapboxNavigationApp::class)
        every {
            MapboxNavigationApp.registerObserver(any())
        } answers {
            attachedMapboxNavigation?.let {
                firstArg<MapboxNavigationObserver>().onAttached(it)
            }
            registeredMapboxNavigationObservers.add(firstArg())
            MapboxNavigationApp
        }
        every {
            MapboxNavigationApp.unregisterObserver(any())
        } answers {
            attachedMapboxNavigation?.let {
                firstArg<MapboxNavigationObserver>().onDetached(it)
            }
            registeredMapboxNavigationObservers.remove(firstArg())
            MapboxNavigationApp
        }
    }

    override fun finished(description: Description) {
        unmockkAll()
    }

    /**
     * Simulate the mapboxNavigation is attached, all registered observers are notified.
     */
    fun onAttached(mapboxNavigation: MapboxNavigation) {
        registeredMapboxNavigationObservers.forEach { it.onAttached(mapboxNavigation) }
        this.attachedMapboxNavigation = mapboxNavigation
    }

    /**
     * Simulate the mapboxNavigation is detached, all registered observers are notified.
     */
    fun onDetached(mapboxNavigation: MapboxNavigation) {
        registeredMapboxNavigationObservers.forEach { it.onDetached(mapboxNavigation) }
        this.attachedMapboxNavigation = null
    }
}
