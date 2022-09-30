package com.mapbox.navigation.dropin.map

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.transition.Scene
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.databinding.MapboxNavigationViewLayoutBinding
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    private val scene = mockk<Scene>(relaxed = true)
    private val mapboxNavigation = mockk<MapboxNavigation>()
    private val mapStyleLoader = mockk<MapStyleLoader>(relaxed = true)
    private val mapViewOwner = mockk<MapViewOwner>(relaxed = true)
    private val mapViewFlow = MutableStateFlow<MapView?>(null)
    private val context = mockk<NavigationViewContext>(relaxed = true) {
        every { mapStyleLoader } returns this@MapLayoutCoordinatorTest.mapStyleLoader
        every { mapViewOwner } returns this@MapLayoutCoordinatorTest.mapViewOwner
        every { mapView } returns mapViewFlow
        every { styles } returns mockk(relaxed = true) {
            every { mapScalebarParams } returns MutableStateFlow(mockk(relaxed = true))
        }
    }
    private lateinit var binding: MapboxNavigationViewLayoutBinding
    private val mapboxMap = mockk<MapboxMap>()
    private val customMapboxMap = mockk<MapboxMap>()
    private val mapView = mockedCustomMapView {
        every { getMapboxMap() } returns mapboxMap
    }
    private val customMapView = mockedCustomMapView {
        every { getMapboxMap() } returns customMapboxMap
    }
    private lateinit var coordinator: MapLayoutCoordinator

    @Before
    fun setUp() = coroutineRule.runBlockingTest {
        mockkStatic(Scene::class)
        mockkObject(BoundMapViewProvider)
        mockkConstructor(MapBinder::class)
        every { anyConstructed<MapBinder>().bind(any()) } returns observer
        every { Scene.getSceneForLayout(any(), any(), any()) } returns scene
        every { BoundMapViewProvider.bindLayoutAndGet(any()) } returns mapView
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
        unmockkStatic(Scene::class)
        unmockkObject(BoundMapViewProvider)
        unmockkConstructor(MapBinder::class)
    }

    @Test
    fun `should use default map`() = coroutineRule.runBlockingTest {
        verify(exactly = 1) {
            Scene.getSceneForLayout(
                binding.mapViewLayout,
                R.layout.mapbox_mapview_layout,
                binding.mapViewLayout.context
            )
        }
        verify(exactly = 1) { scene.enter() }
        verify(exactly = 1) { mapStyleLoader.mapboxMap = mapboxMap }
        verify(exactly = 1) { mapStyleLoader.loadInitialStyle() }
        coVerify(exactly = 1) { mapStyleLoader.observeAndReloadNewStyles() }
        verify(exactly = 1) { mapViewOwner.updateMapView(mapView) }
        verify(exactly = 1) { BoundMapViewProvider.bindLayoutAndGet(binding.mapViewLayout) }

        // 1 constructed binder
        verify(exactly = 1) { observer.onAttached(mapboxNavigation) }
    }

    @Test
    fun `should use custom map`() = coroutineRule.runBlockingTest {
        clearAllMocks(answers = false)
        mapViewFlow.value = customMapView
        verify(exactly = 0) { Scene.getSceneForLayout(any(), any(), any()) }
        verify(exactly = 1) { mapStyleLoader.mapboxMap = customMapboxMap }
        verify(exactly = 0) { mapStyleLoader.loadInitialStyle() }
        coVerify(exactly = 0) { mapStyleLoader.observeAndReloadNewStyles() }
        verify(exactly = 1) { mapViewOwner.updateMapView(customMapView) }

        // 1 constructed mock
        verify(exactly = 1) { observer.onAttached(mapboxNavigation) }
        assertEquals(1, binding.mapViewLayout.childCount)
        assertTrue(binding.mapViewLayout.getChildAt(0) === customMapView)
    }

    @Test
    fun `should reload binder when custom map changes`() = coroutineRule.runBlockingTest {
        val customMapView2 = mockedCustomMapView {
            every { getMapboxMap() } returns mockk(relaxed = true)
        }

        clearAllMocks(answers = false)
        mapViewFlow.value = customMapView
        mapViewFlow.value = customMapView2
        // 2 constructed mocks
        verify(exactly = 2) { observer.onAttached(mapboxNavigation) }
    }

    @Test
    fun `onDetached sets map to null`() {
        coordinator.onDetached(mapboxNavigation)
        verify { mapStyleLoader.mapboxMap = null }
    }

    private fun mockedCustomMapView(initBlock: MapView.() -> Unit = {}): MapView {
        return mockk(relaxed = true) {
            every { compass } returns mockk(relaxed = true)
            every { scalebar } returns mockk(relaxed = true)
            every { parent } returns null
            initBlock()
        }
    }
}
