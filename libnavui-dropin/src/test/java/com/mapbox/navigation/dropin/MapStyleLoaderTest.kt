package com.mapbox.navigation.dropin

import android.content.Context
import android.content.res.Configuration
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.navigation.ui.maps.NavigationStyles
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
internal class MapStyleLoaderTest {

    lateinit var mapboxMap: MapboxMap
    lateinit var context: Context

    lateinit var mapStyleUriDayFlow: MutableStateFlow<String>
    lateinit var mapStyleUriNightFlow: MutableStateFlow<String>

    lateinit var sut: MapStyleLoader

    lateinit var listenerSlot: CapturingSlot<OnStyleLoadedListener>

    @Before
    fun setUp() {
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
        }

        sut = MapStyleLoader(context, options)
        sut.mapboxMap = mapboxMap
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

        listenerSlot.captured.onStyleLoaded(mockk())

        assertEquals(loadedStyle, sut.loadedMapStyle.value)
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
