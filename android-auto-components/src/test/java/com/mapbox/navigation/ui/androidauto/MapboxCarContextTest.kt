package com.mapbox.navigation.ui.androidauto

import androidx.car.app.CarContext
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.preview.CarRoutePreviewRequest
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapboxCarContextTest {

    private val session: Session = mockk()
    private val mapboxCarMap: MapboxCarMap = mockk()
    private val lifecycleRegistry = LifecycleRegistry.createUnsafe(session)
        .also { it.currentState = Lifecycle.State.INITIALIZED }
    private val carContext: CarContext = mockk(relaxed = true) {
        every { getCarService(ScreenManager::class.java) } returns mockk()
    }

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        every { session.lifecycle } returns lifecycleRegistry
        every { session.carContext } returns carContext
    }

    @After
    fun teardown() {
        // MapboxCarContext registers observers with the global MapboxNavigationApp while CREATED.
        // Destroy the lifecycle so they do not leak into later tests.
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    @Test
    fun `CarContext is accessible after lifecycle is CREATED`() {
        val mapboxCarContext = MapboxCarContext(session.lifecycle, mapboxCarMap)

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        mapboxCarContext.carContext
    }

    @Test(expected = IllegalStateException::class)
    fun `CarContext crashes if accessed before lifecycle is CREATED`() {
        val mapboxCarContext = MapboxCarContext(session.lifecycle, mapboxCarMap)

        mapboxCarContext.carContext
    }

    @Test
    fun `MapboxScreenManager is accessible before lifecycle is CREATED`() {
        val mapboxCarContext = MapboxCarContext(session.lifecycle, mapboxCarMap)

        mapboxCarContext.mapboxScreenManager
    }

    @Test
    fun `MapboxNavigationManager is accessible after lifecycle is CREATED`() {
        val mapboxCarContext = MapboxCarContext(session.lifecycle, mapboxCarMap)

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        mapboxCarContext.mapboxNavigationManager
    }

    @Test(expected = IllegalStateException::class)
    fun `MapboxNavigationManager crashes if accessed before lifecycle is CREATED`() {
        val mapboxCarContext = MapboxCarContext(session.lifecycle, mapboxCarMap)

        mapboxCarContext.mapboxNavigationManager
    }

    @Test
    fun `MapboxNotification is accessible after lifecycle is CREATED`() {
        val mapboxCarContext = MapboxCarContext(session.lifecycle, mapboxCarMap)

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        mapboxCarContext.mapboxNotification
    }

    @Test(expected = IllegalStateException::class)
    fun `MapboxNotification crashes if accessed before lifecycle is CREATED`() {
        val mapboxCarContext = MapboxCarContext(session.lifecycle, mapboxCarMap)

        mapboxCarContext.mapboxNotification
    }

    @Test
    fun `MapboxCarStorage is accessible after lifecycle is CREATED`() {
        val mapboxCarContext = MapboxCarContext(session.lifecycle, mapboxCarMap)

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        mapboxCarContext.mapboxCarStorage
    }

    @Test(expected = IllegalStateException::class)
    fun `MapboxCarStorage crashes if accessed before lifecycle is CREATED`() {
        val mapboxCarContext = MapboxCarContext(session.lifecycle, mapboxCarMap)

        mapboxCarContext.mapboxCarStorage
    }

    @Test
    fun `CarRoutePreviewRequest is accessible before lifecycle is CREATED`() {
        val mapboxCarContext = MapboxCarContext(session.lifecycle, mapboxCarMap)

        mapboxCarContext.routePreviewRequest
    }

    @Test
    fun `stopNavigationFromCarHost clears the routes`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)
        withMockedScreenManager {
            MapboxCarContext(session.lifecycle, mapboxCarMap).stopNavigationFromCarHost(
                mapboxNavigation,
            )
        }

        verify { mapboxNavigation.setNavigationRoutes(emptyList()) }
    }

    @Test
    fun `stopNavigationFromCarHost replaces a legacy guidance screen with free drive`() {
        withMockedScreenManager {
            MapboxCarContext(session.lifecycle, mapboxCarMap).stopNavigationFromCarHost(null)

            verify { MapboxScreenManager.replaceTop(MapboxScreen.FREE_DRIVE) }
        }
    }

    @Test
    fun `stopNavigationFromCarHost stays on the unified navigation screen`() {
        withMockedScreenManager {
            every { MapboxScreenManager.current() } returns mockk {
                every { key } returns MapboxScreen.NAVIGATION
            }

            MapboxCarContext(session.lifecycle, mapboxCarMap).stopNavigationFromCarHost(null)

            verify(exactly = 0) { MapboxScreenManager.replaceTop(any()) }
        }
    }

    private fun withMockedScreenManager(block: () -> Unit) {
        mockkObject(MapboxScreenManager) {
            every { MapboxScreenManager.current() } returns null
            every { MapboxScreenManager.replaceTop(any()) } just Runs
            block()
        }
    }

    @Test
    fun `CarRoutePreviewRequest is attached to MapboxNavigationApp while the lifecycle is CREATED`() {
        val mapboxCarContext = MapboxCarContext(session.lifecycle, mapboxCarMap)
        val routePreviewRequest = mapboxCarContext.routePreviewRequest
        assertFalse(isRegistered(routePreviewRequest))

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        assertTrue(isRegistered(routePreviewRequest))

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertFalse(isRegistered(routePreviewRequest))
    }

    private fun isRegistered(routePreviewRequest: CarRoutePreviewRequest) = MapboxNavigationApp
        .getObservers(CarRoutePreviewRequest::class)
        .any { it === routePreviewRequest }
}
