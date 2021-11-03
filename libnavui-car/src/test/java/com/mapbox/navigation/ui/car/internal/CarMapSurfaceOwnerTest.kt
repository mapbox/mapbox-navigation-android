package com.mapbox.navigation.ui.car.internal

import android.graphics.Rect
import android.os.Build
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapSurface
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.ui.car.map.MapboxCarMapObserver
import com.mapbox.navigation.ui.car.map.MapboxCarMapSurface
import com.mapbox.navigation.ui.car.map.internal.CarMapSurfaceOwner
import com.mapbox.navigation.utils.internal.LoggerProvider
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalMapboxNavigationAPI
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.N])
class CarMapSurfaceOwnerTest {

    private val carMapSurfaceOwner = CarMapSurfaceOwner()

    @Before
    fun setup() {
        mockkObject(LoggerProvider)
        every { LoggerProvider.logger } returns mockk(relaxUnitFun = true)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should not notify observer loaded when there is no surface`() {
        val firstObserver: MapboxCarMapObserver = mockk(relaxed = true)
        val secondObserver: MapboxCarMapObserver = mockk(relaxed = true)
        carMapSurfaceOwner.registerObserver(firstObserver)
        carMapSurfaceOwner.registerObserver(secondObserver)
        carMapSurfaceOwner.unregisterObserver(firstObserver)
        carMapSurfaceOwner.clearObservers()

        verify(exactly = 0) { firstObserver.loaded(any()) }
        verify(exactly = 0) { secondObserver.loaded(any()) }
    }

    @Test
    fun `should not notify observer detached when there is no surface`() {
        val firstObserver: MapboxCarMapObserver = mockk(relaxed = true)
        val secondObserver: MapboxCarMapObserver = mockk(relaxed = true)
        carMapSurfaceOwner.registerObserver(firstObserver)
        carMapSurfaceOwner.registerObserver(secondObserver)
        carMapSurfaceOwner.unregisterObserver(firstObserver)
        carMapSurfaceOwner.clearObservers()

        verify(exactly = 0) { firstObserver.detached(any()) }
        verify(exactly = 0) { secondObserver.detached(any()) }
    }

    @Test
    fun `surfaceAvailable should notify observers that map is loaded`() {
        val firstObserver: MapboxCarMapObserver = mockk(relaxed = true)
        val secondObserver: MapboxCarMapObserver = mockk(relaxed = true)
        carMapSurfaceOwner.registerObserver(firstObserver)
        carMapSurfaceOwner.registerObserver(secondObserver)

        val mapboxCarMapSurface: MapboxCarMapSurface = mockk()
        carMapSurfaceOwner.surfaceAvailable(mapboxCarMapSurface)

        verify(exactly = 1) { firstObserver.loaded(mapboxCarMapSurface) }
        verify(exactly = 1) { secondObserver.loaded(mapboxCarMapSurface) }
    }

    @Test
    fun `surfaceAvailable should not notify visibleAreaChanged when visible area is null`() {
        val firstObserver: MapboxCarMapObserver = mockk(relaxed = true)
        val secondObserver: MapboxCarMapObserver = mockk(relaxed = true)
        carMapSurfaceOwner.registerObserver(firstObserver)
        carMapSurfaceOwner.registerObserver(secondObserver)

        val mapboxCarMapSurface: MapboxCarMapSurface = mockk()
        carMapSurfaceOwner.surfaceAvailable(mapboxCarMapSurface)

        verify(exactly = 0) { firstObserver.visibleAreaChanged(any(), any()) }
        verify(exactly = 0) { secondObserver.visibleAreaChanged(any(), any()) }
    }

    @Test
    fun `surfaceVisibleAreaChanged should notify visibleAreaChanged when surface is available`() {
        val firstObserver: MapboxCarMapObserver = mockk(relaxed = true)
        val secondObserver: MapboxCarMapObserver = mockk(relaxed = true)
        carMapSurfaceOwner.registerObserver(firstObserver)
        carMapSurfaceOwner.registerObserver(secondObserver)

        val mapboxCarMapSurface: MapboxCarMapSurface = mockk {
            every { surfaceContainer } returns mockk {
                every { width } returns 800
                every { height } returns 400
            }
        }
        carMapSurfaceOwner.surfaceAvailable(mapboxCarMapSurface)
        val visibleRect: Rect = mockk()
        carMapSurfaceOwner.surfaceVisibleAreaChanged(visibleRect)

        verify(exactly = 1) { firstObserver.visibleAreaChanged(any(), any()) }
        verify(exactly = 1) { secondObserver.visibleAreaChanged(any(), any()) }
    }

    @Test
    fun `surfaceVisibleAreaChanged should not notify visibleAreaChanged when surface is not available`() {
        val firstObserver: MapboxCarMapObserver = mockk(relaxed = true)
        val secondObserver: MapboxCarMapObserver = mockk(relaxed = true)
        carMapSurfaceOwner.registerObserver(firstObserver)
        carMapSurfaceOwner.registerObserver(secondObserver)

        val visibleRect: Rect = mockk()
        carMapSurfaceOwner.surfaceVisibleAreaChanged(visibleRect)

        verify(exactly = 0) { firstObserver.visibleAreaChanged(any(), any()) }
        verify(exactly = 0) { secondObserver.visibleAreaChanged(any(), any()) }
    }

    @Test
    fun `surfaceDestroyed should stop and destroy map before notifying observers`() {
        val observer: MapboxCarMapObserver = mockk(relaxed = true)
        carMapSurfaceOwner.registerObserver(observer)
        val testMapSurface = mockk<MapSurface>(relaxed = true)
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk {
            every { surfaceContainer } returns mockk {
                every { width } returns 800
                every { height } returns 400
            }
            every { mapSurface } returns testMapSurface
        }

        carMapSurfaceOwner.surfaceAvailable(mapboxCarMapSurface)
        carMapSurfaceOwner.surfaceDestroyed()

        verifyOrder {
            testMapSurface.onStop()
            testMapSurface.surfaceDestroyed()
            testMapSurface.onDestroy()
            observer.detached(mapboxCarMapSurface)
        }
    }

    @Test
    fun `should notify destroy and detached old surface when new surface is available`() {
        val observer: MapboxCarMapObserver = mockk(relaxed = true)
        carMapSurfaceOwner.registerObserver(observer)
        val firstMapSurface = mockk<MapSurface>(relaxed = true)
        val firstSurface = mockk<MapboxCarMapSurface> {
            every { surfaceContainer } returns mockk {
                every { width } returns 800
                every { height } returns 400
            }
            every { mapSurface } returns firstMapSurface
        }
        val secondSurface = mockk<MapboxCarMapSurface> {
            every { surfaceContainer } returns mockk {
                every { width } returns 800
                every { height } returns 400
            }
        }

        carMapSurfaceOwner.surfaceAvailable(firstSurface)
        carMapSurfaceOwner.surfaceVisibleAreaChanged(mockk())
        carMapSurfaceOwner.surfaceAvailable(secondSurface)

        verifyOrder {
            observer.loaded(firstSurface)
            observer.visibleAreaChanged(any(), any())
            observer.detached(firstSurface)
            observer.loaded(secondSurface)
            observer.visibleAreaChanged(any(), any())
        }
        // Map style changes should not destroy the map.
        verify(exactly = 0) { firstMapSurface.onStop() }
        verify(exactly = 0) { firstMapSurface.surfaceDestroyed() }
        verify(exactly = 0) { firstMapSurface.onDestroy() }
    }

    @Test
    fun `surfaceVisibleAreaChanged should notify visibleAreaChanged with edgeInsets`() {
        val observer: MapboxCarMapObserver = mockk(relaxed = true)
        carMapSurfaceOwner.registerObserver(observer)
        val visibleAreaSlot = slot<Rect>()
        val edgeInsets = slot<EdgeInsets>()
        every {
            observer.visibleAreaChanged(capture(visibleAreaSlot), capture(edgeInsets))
        } just Runs
        val mapboxCarMapSurface: MapboxCarMapSurface = mockk {
            every { surfaceContainer } returns mockk {
                every { width } returns 800
                every { height } returns 400
            }
        }

        carMapSurfaceOwner.surfaceAvailable(mapboxCarMapSurface)
        val visibleRect = Rect(30, 112, 779, 381)
        carMapSurfaceOwner.surfaceVisibleAreaChanged(visibleRect)

        assertEquals(visibleRect, visibleAreaSlot.captured)
        // edgeInset.left = visibleRect.left = 30
        assertEquals(30.0, edgeInsets.captured.left, 0.0001)
        // edgeInset.top = visibleRect.top = 112
        assertEquals(112.0, edgeInsets.captured.top, 0.0001)
        // edgeInset.right = surfaceContainer.width - visibleRect.right = 800 - 779 = 21
        assertEquals(21.0, edgeInsets.captured.right, 0.0001)
        // edgeInsets.bottom = surfaceContainer.height - visibleRect.bottom = 400 - 381 = 19
        assertEquals(19.0, edgeInsets.captured.bottom, 0.0001)
    }
}
