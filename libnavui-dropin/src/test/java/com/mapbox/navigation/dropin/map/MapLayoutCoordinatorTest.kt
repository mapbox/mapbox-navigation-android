package com.mapbox.navigation.dropin.map

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
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
        every { styles } returns mockk(relaxed = true) {
            every { mapScalebarParams } returns MutableStateFlow(mockk(relaxed = true))
        }
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
        verify { anyConstructed<MapboxMapViewBinder>().registerMapboxMapObserver(any()) }
        // 1 constructed binder
        verify(exactly = 1) { observer.onAttached(mapboxNavigation) }
    }

    @Test
    fun `should use custom binder`() = coroutineRule.runBlockingTest {
        clearMocks(observer, answers = false)
        mapViewBinderFlow.value = customBinder
        verify { customBinder.context = context }
        verify { customBinder.navigationViewBinding = binding }
        verify { customBinder.registerMapboxMapObserver(any()) }
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
        verify { newCustomBinder.registerMapboxMapObserver(any()) }
        verify(exactly = 0) { observer.onAttached(any()) }
    }

    @Test
    fun `should not interact with styles if policy is NEVER`() = coroutineRule.runBlockingTest {
        every { customBinder.getMapStyleLoadPolicy() } returns MapStyleLoadPolicy.NEVER
        clearMocks(mapStyleLoader, answers = false)
        mapViewBinderFlow.value = customBinder
        verify(exactly = 0) {
            mapStyleLoader.loadInitialStyle()
        }
        coVerify(exactly = 0) {
            mapStyleLoader.observeAndReloadNewStyles()
        }
    }

    @Test
    fun `should only load style once if policy is ONCE`() = coroutineRule.runBlockingTest {
        every { customBinder.getMapStyleLoadPolicy() } returns MapStyleLoadPolicy.ONCE
        clearMocks(mapStyleLoader, answers = false)
        mapViewBinderFlow.value = customBinder
        verify(exactly = 1) {
            mapStyleLoader.loadInitialStyle()
        }
        coVerify(exactly = 0) {
            mapStyleLoader.observeAndReloadNewStyles()
        }
    }

    @Test
    fun `should load and reload style if policy is ON_CONFIGURATION_CHANGE`() =
        coroutineRule.runBlockingTest {
            every {
                customBinder.getMapStyleLoadPolicy()
            } returns MapStyleLoadPolicy.ON_CONFIGURATION_CHANGE
            clearMocks(mapStyleLoader, answers = false)
            mapViewBinderFlow.value = customBinder
            verify(exactly = 1) {
                mapStyleLoader.loadInitialStyle()
            }
            coVerify(exactly = 1) {
                mapStyleLoader.observeAndReloadNewStyles()
            }
        }

    @Test
    fun `new binder should cancel previous load style job`() = coroutineRule.runBlockingTest {
        every {
            customBinder.getMapStyleLoadPolicy()
        } returns MapStyleLoadPolicy.ON_CONFIGURATION_CHANGE
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
    fun `listener should update mapbox map`() = coroutineRule.runBlockingTest {
        val map = mockk<MapboxMap>()
        val listeners = mutableListOf<MapboxMapObserver>()
        verify {
            anyConstructed<MapboxMapViewBinder>().registerMapboxMapObserver(capture(listeners))
        }
        listeners.first().onMapboxMapReady(map)
        verify { mapStyleLoader.mapboxMap = map }
    }

    @Test
    fun `onDetached sets map to null`() {
        coordinator.onDetached(mapboxNavigation)
        verify { mapStyleLoader.mapboxMap = null }
    }
}
