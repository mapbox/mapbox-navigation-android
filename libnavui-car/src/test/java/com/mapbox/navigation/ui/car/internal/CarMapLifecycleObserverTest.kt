package com.mapbox.navigation.ui.car.internal

import android.graphics.Rect
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.SurfaceCallback
import androidx.lifecycle.LifecycleOwner
import com.mapbox.maps.MapSurface
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.ui.car.map.MapboxCarMapSurface
import com.mapbox.navigation.ui.car.map.MapboxCarOptions
import com.mapbox.navigation.ui.car.map.internal.CarMapLifecycleObserver
import com.mapbox.navigation.ui.car.map.internal.CarMapSurfaceOwner
import com.mapbox.navigation.ui.car.map.internal.MapSurfaceProvider
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalMapboxNavigationAPI
class CarMapLifecycleObserverTest {

    private val carContext: CarContext = mockk(relaxed = true)
    private val carMapSurfaceOwner: CarMapSurfaceOwner = mockk()
    private val mapboxCarOptions: MapboxCarOptions = MapboxCarOptions
        .Builder(mockk())
        .build()
    private val testMapSurface: MapSurface = mockk(relaxed = true)

    private val carMapLifecycleObserver = CarMapLifecycleObserver(
        carContext,
        carMapSurfaceOwner,
        mapboxCarOptions
    )

    @Before
    fun setup() {
        mockkObject(LoggerProvider)
        every { LoggerProvider.logger } returns mockk(relaxUnitFun = true)

        mockkObject(MapSurfaceProvider)
        every { MapSurfaceProvider.create(any(), any(), any()) } returns testMapSurface
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `onCreate should request the map surface with the SurfaceCallback`() {
        val lifecycleOwner = mockk<LifecycleOwner>()
        val surfaceCallback = slot<SurfaceCallback>()
        every { carContext.getCarService(AppManager::class.java) } returns mockk {
            every { setSurfaceCallback(capture(surfaceCallback)) } just Runs
        }

        carMapLifecycleObserver.onCreate(lifecycleOwner)

        assertTrue(surfaceCallback.isCaptured)
        assertEquals(surfaceCallback.captured, carMapLifecycleObserver)
    }

    @Test
    fun `onSurfaceAvailable should load the MapboxMap`() {
        val mapboxMap = mockk<MapboxMap>(relaxed = true)
        every { testMapSurface.getMapboxMap() } returns mapboxMap

        carMapLifecycleObserver.onSurfaceAvailable(
            mockk {
                every { surface } returns mockk()
            }
        )

        verifyOrder {
            testMapSurface.onStart()
            testMapSurface.surfaceCreated()
            testMapSurface.getMapboxMap()
            mapboxMap.loadStyleUri(any(), any(), any())
        }
    }

    @Test
    fun `onSurfaceAvailable should notify surfaceAvailable when style is loaded`() {
        every { testMapSurface.getMapboxMap() } returns mockk(relaxed = true) {
            every { loadStyleUri(any(), any(), any()) } answers {
                secondArg<Style.OnStyleLoaded>().onStyleLoaded(mockk())
            }
        }
        val carMapSurfaceSlot = slot<MapboxCarMapSurface>()
        every { carMapSurfaceOwner.surfaceAvailable(capture(carMapSurfaceSlot)) } just Runs

        carMapLifecycleObserver.onSurfaceAvailable(
            mockk {
                every { surface } returns mockk()
                every { width } returns 800
                every { height } returns 400
            }
        )

        verifyOrder {
            testMapSurface.surfaceChanged(800, 400)
            carMapSurfaceOwner.surfaceAvailable(any())
        }
    }

    @Test
    fun `onVisibleAreaChanged should notify carMapSurfaceOwner surfaceVisibleAreaChanged`() {
        val visibleRect = mockk<Rect>()
        every { carMapSurfaceOwner.surfaceVisibleAreaChanged(any()) } just Runs

        carMapLifecycleObserver.onVisibleAreaChanged(visibleRect)

        verify(exactly = 1) { carMapSurfaceOwner.surfaceVisibleAreaChanged(visibleRect) }
    }

    @Test
    fun `onStableAreaChanged should not do anything`() {
        carMapLifecycleObserver.onStableAreaChanged(mockk())

        verify(exactly = 0) { carMapSurfaceOwner.surfaceVisibleAreaChanged(any()) }
        verify(exactly = 0) { carMapSurfaceOwner.surfaceDestroyed() }
    }

    @Test
    fun `onSurfaceDestroyed should notify carMapSurfaceOwner surfaceDestroyed`() {
        every { carMapSurfaceOwner.surfaceDestroyed() } just Runs

        carMapLifecycleObserver.onSurfaceDestroyed(mockk())

        verify(exactly = 1) { carMapSurfaceOwner.surfaceDestroyed() }
    }

    @Test
    fun `updateMapStyle should notify surfaceAvailable when style is loaded`() {
        val previousMapSurface = mockk<MapboxCarMapSurface> {
            every { mapSurface } returns mockk {
                every { getMapboxMap() } returns mockk(relaxed = true) {
                    every { loadStyleUri(any(), any(), any()) } answers {
                        secondArg<Style.OnStyleLoaded>().onStyleLoaded(
                            mockk { every { styleURI } returns "test-map-style-loaded" }
                        )
                    }
                }
            }
            every { surfaceContainer } returns mockk()
        }
        every { carMapSurfaceOwner.mapboxCarMapSurface } returns previousMapSurface
        val carMapSurfaceSlot = slot<MapboxCarMapSurface>()
        every { carMapSurfaceOwner.surfaceAvailable(capture(carMapSurfaceSlot)) } just Runs

        carMapLifecycleObserver.updateMapStyle("test-map-style")

        val mapSurface = previousMapSurface.mapSurface
        verify(exactly = 0) { mapSurface.surfaceChanged(any(), any()) }
        verify(exactly = 1) { carMapSurfaceOwner.surfaceAvailable(any()) }
        assertEquals("test-map-style-loaded", carMapSurfaceSlot.captured.style.styleURI)
        assertEquals(previousMapSurface.mapSurface, carMapSurfaceSlot.captured.mapSurface)
    }
}
