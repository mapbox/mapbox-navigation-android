@file:Suppress("NoMockkVerifyImport")

package com.mapbox.navigation.core.lifecycle

import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
class MapboxNavigationOwnerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val navigationOptionsProvider = mockk<NavigationOptionsProvider> {
        every { createNavigationOptions() } returns mockk {
            every { accessToken } returns "test_access_token"
        }
    }

    private val mapboxNavigationOwner = MapboxNavigationOwner()

    @Before
    fun setup() {
        mockkStatic(MapboxNavigationProvider::class)
        every { MapboxNavigationProvider.create(any()) } answers {
            mockk {
                every { navigationOptions } returns firstArg()
            }
        }
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test(expected = Throwable::class)
    fun `crashes when navigationOptions have not been setup`() {
        val mapboxNavigationObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationOwner.register(mapboxNavigationObserver)

        val lifecycleOwner: LifecycleOwner = mockk()
        mapboxNavigationOwner.carAppLifecycleObserver.onCreate(lifecycleOwner)
        mapboxNavigationOwner.carAppLifecycleObserver.onStart(lifecycleOwner)
        mapboxNavigationOwner.carAppLifecycleObserver.onResume(lifecycleOwner)
    }

    @Test
    fun `full lifecycle will attach and detach MapboxNavigation`() {
        mapboxNavigationOwner.setup(navigationOptionsProvider)
        val mapboxNavigationObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationOwner.register(mapboxNavigationObserver)

        val lifecycleOwner: LifecycleOwner = mockk()
        mapboxNavigationOwner.carAppLifecycleObserver.onCreate(lifecycleOwner)
        mapboxNavigationOwner.carAppLifecycleObserver.onStart(lifecycleOwner)
        mapboxNavigationOwner.carAppLifecycleObserver.onResume(lifecycleOwner)
        mapboxNavigationOwner.carAppLifecycleObserver.onPause(lifecycleOwner)
        mapboxNavigationOwner.carAppLifecycleObserver.onStop(lifecycleOwner)
        mapboxNavigationOwner.carAppLifecycleObserver.onDestroy(lifecycleOwner)

        verifyOrder {
            mapboxNavigationObserver.onAttached(any())
            mapboxNavigationObserver.onDetached(any())
        }
    }

    @Test
    fun `attach and detach multiple times`() {
        mapboxNavigationOwner.setup(navigationOptionsProvider)
        val mapboxNavigationObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationOwner.register(mapboxNavigationObserver)

        val lifecycleOwner: LifecycleOwner = mockk()
        mapboxNavigationOwner.carAppLifecycleObserver.onStart(lifecycleOwner)
        mapboxNavigationOwner.carAppLifecycleObserver.onStop(lifecycleOwner)
        mapboxNavigationOwner.carAppLifecycleObserver.onStart(lifecycleOwner)
        mapboxNavigationOwner.carAppLifecycleObserver.onStop(lifecycleOwner)

        verifyOrder {
            mapboxNavigationObserver.onAttached(any())
            mapboxNavigationObserver.onDetached(any())
            mapboxNavigationObserver.onAttached(any())
            mapboxNavigationObserver.onDetached(any())
        }
    }

    @Test
    fun `notify multiple observers in the order they were registered`() {
        mapboxNavigationOwner.setup(navigationOptionsProvider)
        val firstObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        val secondObserver = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationOwner.register(firstObserver)
        mapboxNavigationOwner.register(secondObserver)

        val lifecycleOwner: LifecycleOwner = mockk()
        mapboxNavigationOwner.carAppLifecycleObserver.onStart(lifecycleOwner)
        mapboxNavigationOwner.carAppLifecycleObserver.onStop(lifecycleOwner)

        verifyOrder {
            firstObserver.onAttached(any())
            secondObserver.onAttached(any())
            firstObserver.onDetached(any())
            secondObserver.onDetached(any())
        }
    }

    @Test
    fun `attach and detach observer when navigation is started`() {
        mapboxNavigationOwner.setup(navigationOptionsProvider)
        val lifecycleOwner: LifecycleOwner = mockk()
        mapboxNavigationOwner.carAppLifecycleObserver.onStart(lifecycleOwner)

        val observer = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationOwner.register(observer)
        mapboxNavigationOwner.unregister(observer)

        verifyOrder {
            observer.onAttached(any())
            observer.onDetached(any())
        }
    }

    @Test
    fun `do not attach and detach when navigation is not started`() {
        mapboxNavigationOwner.setup(navigationOptionsProvider)

        val observer = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationOwner.register(observer)
        mapboxNavigationOwner.unregister(observer)

        verify(exactly = 0) { observer.onAttached(any()) }
        verify(exactly = 0) { observer.onDetached(any()) }
    }

    @Test
    fun `navigation options are not created before car app lifecycle is started`() {
        mapboxNavigationOwner.setup(navigationOptionsProvider)

        verify(exactly = 0) { navigationOptionsProvider.createNavigationOptions() }

        mapboxNavigationOwner.carAppLifecycleObserver.onStart(mockk())

        verify(exactly = 1) { navigationOptionsProvider.createNavigationOptions() }
    }

    @Test
    fun `navigation options are not reused for different mapbox navigation instances`() {
        mapboxNavigationOwner.setup(navigationOptionsProvider)
        val lifecycleOwner = mockk<LifecycleOwner>()
        val navigationOptionsA = mockk<NavigationOptions>()
        every { navigationOptionsProvider.createNavigationOptions() } returns navigationOptionsA
        mapboxNavigationOwner.carAppLifecycleObserver.onStart(lifecycleOwner)

        assertEquals(navigationOptionsA, mapboxNavigationOwner.current()?.navigationOptions)

        mapboxNavigationOwner.carAppLifecycleObserver.onStop(lifecycleOwner)
        val navigationOptionsB = mockk<NavigationOptions>()
        every { navigationOptionsProvider.createNavigationOptions() } returns navigationOptionsB
        mapboxNavigationOwner.carAppLifecycleObserver.onStart(lifecycleOwner)

        assertEquals(navigationOptionsB, mapboxNavigationOwner.current()?.navigationOptions)
    }
}
