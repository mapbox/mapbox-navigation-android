package com.mapbox.navigation.dropin.map

import android.content.Context
import android.content.res.Configuration
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.navigation.dropin.navigationview.NavigationViewOptions
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
internal class MapStyleLoaderTest {

    private lateinit var mapboxMap: MapboxMap
    lateinit var context: Context

    private lateinit var mapStyleUriDayFlow: MutableStateFlow<String>
    private lateinit var mapStyleUriNightFlow: MutableStateFlow<String>
    private lateinit var routeLineView: MapboxRouteLineView
    private lateinit var mockRouteLineOptions: MapboxRouteLineOptions

    lateinit var sut: MapStyleLoader

    private lateinit var listenerSlot: CapturingSlot<OnStyleLoadedListener>

    @Before
    fun setUp() {
        mockkConstructor(MapboxRouteLineView::class)
        mockRouteLineOptions = mockk(relaxed = true)
        routeLineView = mockk(relaxed = true) {
            every { initializeLayers(any()) } returns Unit
        }

        mapStyleUriDayFlow = MutableStateFlow(NavigationStyles.NAVIGATION_DAY_STYLE)
        mapStyleUriNightFlow = MutableStateFlow(NavigationStyles.NAVIGATION_NIGHT_STYLE)

        listenerSlot = slot()
        context = mockk(relaxed = true)
        mapboxMap = mockk(relaxed = true) {
            every { addOnStyleLoadedListener(capture(listenerSlot)) } returns Unit
        }
        val options = mockk<NavigationViewOptions> {
            every { mapStyleUriDay } returns mapStyleUriDayFlow
            every { mapStyleUriNight } returns mapStyleUriNightFlow
            every { routeLineOptions } returns MutableStateFlow(mockRouteLineOptions)
        }

        sut = MapStyleLoader(context, options)
        sut.mapboxMap = mapboxMap
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `loadInitialStyle should load initial DAY style`() {
        givenNightModeEnabled(false)

        sut.loadInitialStyle()

        verify { mapboxMap.loadStyleUri(mapStyleUriDayFlow.value) }
    }

    @Test
    fun `loadInitialStyle should load initial NIGHT style`() {
        givenNightModeEnabled(true)

        sut.loadInitialStyle()

        verify { mapboxMap.loadStyleUri(mapStyleUriNightFlow.value) }
    }

    @Test
    fun `observeAndReloadNewStyles observe options and load custom style`() = runBlockingTest {
        givenNightModeEnabled(false)

        mapStyleUriDayFlow.value = Style.LIGHT
        val job = launch {
            sut.observeAndReloadNewStyles()
        }
        advanceUntilIdle()
        job.cancel()

        verify { mapboxMap.loadStyleUri(Style.LIGHT) }
    }

    @Test
    fun `OnStyleLoadedListener should update loadedMapStyle value`() {
        givenNightModeEnabled(false)
        val loadedStyle = mockk<Style>()
        every { mapboxMap.getStyle() } returns loadedStyle
        every { anyConstructed<MapboxRouteLineView>().initializeLayers(loadedStyle) } returns Unit

        listenerSlot.captured.onStyleLoaded(mockk())

        assertEquals(loadedStyle, sut.loadedMapStyle.value)
    }

    @Test
    fun `OnStyleLoadedListener should initialize route line layers`() {
        givenNightModeEnabled(false)
        val loadedStyle = mockk<Style>()
        every { mapboxMap.getStyle() } returns loadedStyle
        every { anyConstructed<MapboxRouteLineView>().initializeLayers(loadedStyle) } returns Unit

        listenerSlot.captured.onStyleLoaded(mockk())

        verify { anyConstructed<MapboxRouteLineView>().initializeLayers(loadedStyle) }
    }

    private fun givenNightModeEnabled(enabled: Boolean) {
        val mode = if (enabled) {
            Configuration.UI_MODE_NIGHT_YES
        } else {
            Configuration.UI_MODE_NIGHT_NO
        }

        every { context.resources } returns mockk(relaxed = true) {
            every { configuration } returns Configuration().apply {
                uiMode = mode
            }
        }
    }
}
