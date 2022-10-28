package com.mapbox.navigation.dropin.map

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MapLayoutCoordinatorTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val observer = mockk<MapboxNavigationObserver>(relaxed = true)
    private val mapboxNavigation = mockk<MapboxNavigation>()
    private val mapStyleLoader = mockk<MapStyleLoader>(relaxed = true)
    private val mapViewOwner = mockk<MapViewOwner>(relaxed = true)
    private val mapViewBinderFlow = MutableStateFlow<MapViewBinder?>(null)
    private val customBinder = mockk<MapViewBinder>(relaxed = true)
    private val context = mockk<NavigationViewContext>(relaxed = true) {
        every { mapStyleLoader } returns this@MapLayoutCoordinatorTest.mapStyleLoader
        every { mapViewOwner } returns this@MapLayoutCoordinatorTest.mapViewOwner
        every { uiBinders } returns mockk(relaxed = true) {
            every { mapViewBinder } returns mapViewBinderFlow
        }
        every { styles } returns mockk(relaxed = true)
    }
    private lateinit var binding: MapboxNavigationViewLayoutBinding
    private lateinit var coordinator: MapLayoutCoordinator

    @Before
    fun setUp() = coroutineRule.runBlockingTest {
        mockkConstructor(MapboxMapViewBinder::class)
        every { anyConstructed<MapboxMapViewBinder>().bind(any()) } returns observer
        val ctx: Context = ApplicationProvider.getApplicationContext()
        binding = MapboxNavigationViewLayoutBinding.inflate(
            LayoutInflater.from(ctx),
            FrameLayout(ctx)
        )
        coordinator = MapLayoutCoordinator(context, binding)
        coordinator.onAttached(mapboxNavigation)
    }

    @After
    fun tearDown() {
        unmockkConstructor(MapboxMapViewBinder::class)
    }

    @Test
    fun `should use default binder`() = coroutineRule.runBlockingTest {
        verify { anyConstructed<MapboxMapViewBinder>().context = context }
        verify { anyConstructed<MapboxMapViewBinder>().navigationViewBinding = binding }
        verify { mapViewOwner.registerObserver(any()) }
        // 1 constructed binder
        verify(exactly = 1) { observer.onAttached(mapboxNavigation) }
    }

    @Test
    fun `should use custom binder`() = coroutineRule.runBlockingTest {
        clearMocks(observer, answers = false)
        mapViewBinderFlow.value = customBinder
        verify { customBinder.context = context }
        verify { customBinder.navigationViewBinding = binding }
        verify { mapViewOwner.registerObserver(any()) }
        verify(exactly = 0) { observer.onAttached(any()) }
    }

    @Test
    fun `should reload binder when flow changes`() = coroutineRule.runBlockingTest {
        val newCustomBinder = mockk<MapViewBinder>(relaxed = true)
        mapViewBinderFlow.value = customBinder
        clearMocks(observer, answers = false)
        mapViewBinderFlow.value = newCustomBinder
        verify { newCustomBinder.context = context }
        verify { newCustomBinder.navigationViewBinding = binding }
        verify { mapViewOwner.registerObserver(any()) }
        verify(exactly = 0) { observer.onAttached(any()) }
    }

    @Test
    fun `should not interact with styles if shouldLoadMapStyle is false`() =
        coroutineRule.runBlockingTest {
            every { customBinder.shouldLoadMapStyle } returns false
            clearMocks(mapStyleLoader, answers = false)
            mapViewBinderFlow.value = customBinder
            attachMapView()
            verify(exactly = 0) {
                mapStyleLoader.loadInitialStyle()
            }
            coVerify(exactly = 0) {
                mapStyleLoader.observeAndReloadNewStyles()
            }
        }

    @Test
    fun `should load and reload style if shouldLoadMapStyle is true`() =
        coroutineRule.runBlockingTest {
            every { customBinder.shouldLoadMapStyle } returns true
            clearMocks(mapStyleLoader, answers = false)
            attachMapView()
            mapViewBinderFlow.value = customBinder
            verify(exactly = 1) {
                mapStyleLoader.loadInitialStyle()
            }
            coVerify(exactly = 1) {
                mapStyleLoader.observeAndReloadNewStyles()
            }
        }

    private fun attachMapView() {
        val observers = mutableListOf<MapViewObserver>()
        verify { mapViewOwner.registerObserver(capture(observers)) }
        observers.last().onAttached(mockk(relaxed = true))
    }

    @Test
    fun `new binder should cancel previous load style job`() = coroutineRule.runBlockingTest {
        every { customBinder.shouldLoadMapStyle } returns true
        var reloadFinished = false
        coEvery {
            mapStyleLoader.observeAndReloadNewStyles()
        } coAnswers {
            delay(2000)
            reloadFinished = true
        }
        mapViewBinderFlow.value = customBinder
        delay(10)
        mapViewBinderFlow.value = mockk(relaxed = true)
        coroutineRule.testDispatcher.advanceUntilIdle()
        assertFalse(reloadFinished)
    }

    @Test
    fun `new binder should set mapView to null`() = coroutineRule.runBlockingTest {
        clearMocks(mapViewOwner)

        mapViewBinderFlow.value = customBinder

        verify {
            mapViewOwner.updateMapView(null)
        }
    }

    @Test
    fun `listener should update mapbox map`() = coroutineRule.runBlockingTest {
        val map = mockk<MapboxMap>()
        val mapView = mockk<MapView> {
            every { getMapboxMap() } returns map
        }
        val listeners = mutableListOf<MapViewObserver>()
        verify {
            mapViewOwner.registerObserver(capture(listeners))
        }
        listeners.first().onAttached(mapView)
        verify { mapStyleLoader.mapboxMap = map }
        verify { mapViewOwner.unregisterObserver(listeners.first()) }
    }

    @Test
    fun `onDetached sets map to null`() {
        coordinator.onDetached(mapboxNavigation)
        verify { mapStyleLoader.mapboxMap = null }
    }
}
