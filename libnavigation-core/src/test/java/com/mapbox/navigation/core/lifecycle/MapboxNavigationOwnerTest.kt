@file:Suppress("NoMockkVerifyImport")

package com.mapbox.navigation.core.lifecycle

import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.utils.internal.LoggerProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalPreviewMapboxNavigationAPI
class MapboxNavigationOwnerTest {

    private val mapboxNavigation: MapboxNavigation = mockk()
    private val navigationOptions: NavigationOptions = mockk {
        every { accessToken } returns "test_access_token"
    }

    private val mapboxNavigationOwner = MapboxNavigationOwner()

    @Before
    fun setup() {
        mockkStatic(MapboxNavigationProvider::class)
        mockkObject(LoggerProvider)
        every { LoggerProvider.logger } returns mockk(relaxUnitFun = true)
        every { MapboxNavigationProvider.create(navigationOptions) } returns mapboxNavigation
        every { MapboxNavigationProvider.retrieve() } returns mapboxNavigation
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
        mapboxNavigationOwner.setup(navigationOptions)
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
        mapboxNavigationOwner.setup(navigationOptions)
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
        mapboxNavigationOwner.setup(navigationOptions)
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
        mapboxNavigationOwner.setup(navigationOptions)
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
        mapboxNavigationOwner.setup(navigationOptions)

        val observer = mockk<MapboxNavigationObserver>(relaxUnitFun = true)
        mapboxNavigationOwner.register(observer)
        mapboxNavigationOwner.unregister(observer)

        verify(exactly = 0) { observer.onAttached(any()) }
        verify(exactly = 0) { observer.onDetached(any()) }
    }
}
