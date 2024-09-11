package com.mapbox.navigation.ui.androidauto

import androidx.car.app.CarContext
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.maps.extension.androidauto.MapboxCarMap
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
}
