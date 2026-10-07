package com.mapbox.navigation.ui.androidauto.map

import androidx.car.app.CarContext
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.extension.style.StyleContract
import com.mapbox.maps.module.TelemetryEvent
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.NavigationStyles
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapboxCarMapLoaderTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private lateinit var sut: MapboxCarMapLoader

    @Before
    fun setUp() {
        mockkObject(TelemetryEvent.Companion) {
            every { TelemetryEvent.Companion.create(any()) } returns mockk(relaxed = true)
            sut = MapboxCarMapLoader()
        }
    }

    @Test
    fun `functions can be called while map is detached`() {
        sut.setLightStyleOverride(mockk())
        sut.setDarkStyleOverride(mockk())
        sut.onCarConfigurationChanged(mockk())
    }

    @Test
    fun `onAttached will load the day map style when isDarkMode is false`() {
        val styleExtensionSlot = slot<StyleContract.StyleExtension>()
        val mapSurface: MapboxCarMapSurface = mockMapboxCarMapSurface(styleExtensionSlot)
        every { mapSurface.carContext } returns mockk {
            every { isDarkMode } returns false
        }

        sut.onAttached(mapSurface)

        assertEquals(NavigationStyles.NAVIGATION_DAY_STYLE, styleExtensionSlot.captured.style)
    }

    @Test
    fun `onAttached will load the night map style when isDarkMode is true`() {
        val styleExtensionSlot = slot<StyleContract.StyleExtension>()
        val mapSurface: MapboxCarMapSurface = mockMapboxCarMapSurface(styleExtensionSlot)
        every { mapSurface.carContext } returns mockk {
            every { isDarkMode } returns true
        }

        sut.onAttached(mapSurface)

        assertEquals(NavigationStyles.NAVIGATION_NIGHT_STYLE, styleExtensionSlot.captured.style)
    }

    @Test
    fun `onAttached will load the light style override when isDarkMode is false`() {
        val styleExtensionSlot = slot<StyleContract.StyleExtension>()
        val mapSurface: MapboxCarMapSurface = mockMapboxCarMapSurface(styleExtensionSlot)
        every { mapSurface.carContext } returns mockk {
            every { isDarkMode } returns false
        }
        val darkOverride: StyleContract.StyleExtension = mockk {
            every { style } returns "test-light-override"
        }

        sut.setLightStyleOverride(darkOverride).onAttached(mapSurface)

        assertEquals("test-light-override", styleExtensionSlot.captured.style)
    }

    @Test
    fun `onAttached will load the dark style override when isDarkMode is true`() {
        val styleExtensionSlot = slot<StyleContract.StyleExtension>()
        val mapSurface: MapboxCarMapSurface = mockMapboxCarMapSurface(styleExtensionSlot)
        every { mapSurface.carContext } returns mockk {
            every { isDarkMode } returns true
        }
        val darkOverride: StyleContract.StyleExtension = mockk {
            every { style } returns "test-dark-override"
        }

        sut.setDarkStyleOverride(darkOverride).onAttached(mapSurface)

        assertEquals("test-dark-override", styleExtensionSlot.captured.style)
    }

    @Test
    fun `getStyleExtension will return the default styles`() {
        assertEquals(
            NavigationStyles.NAVIGATION_DAY_STYLE,
            sut.getStyleExtension(false).style,
        )
        assertEquals(
            NavigationStyles.NAVIGATION_NIGHT_STYLE,
            sut.getStyleExtension(true).style,
        )
    }

    @Test
    fun `getStyleExtension will return the overridden styles`() {
        sut.setLightStyleOverride(mockk { every { style } returns "test-light-override" })
        sut.setDarkStyleOverride(mockk { every { style } returns "test-dark-override" })

        assertEquals(
            "test-light-override",
            sut.getStyleExtension(false).style,
        )
        assertEquals(
            "test-dark-override",
            sut.getStyleExtension(true).style,
        )
    }

    @Test
    fun `configuration change without a dark mode change keeps the loaded style`() {
        val mapboxMap = mockMapboxMap()
        val carContext = carContext(isDarkMode = false)

        sut.onAttached(mapSurface(mapboxMap, carContext))
        sut.onCarConfigurationChanged(carContext)
        sut.onCarConfigurationChanged(carContext)

        verify(exactly = 1) {
            mapboxMap.loadStyle(
                any<StyleContract.StyleExtension>(),
                any<Style.OnStyleLoaded>(),
                any<OnMapLoadErrorListener>(),
            )
        }
    }

    @Test
    fun `configuration change with a dark mode change loads the other style`() {
        val styles = mutableListOf<StyleContract.StyleExtension>()
        val mapboxMap = mockMapboxMap(styles)

        sut.onAttached(mapSurface(mapboxMap, carContext(isDarkMode = false)))
        sut.onCarConfigurationChanged(carContext(isDarkMode = true))

        assertEquals(
            listOf(NavigationStyles.NAVIGATION_DAY_STYLE, NavigationStyles.NAVIGATION_NIGHT_STYLE),
            styles.map { it.style },
        )
    }

    @Test
    fun `configuration change applies a new style override`() {
        val styles = mutableListOf<StyleContract.StyleExtension>()
        val mapboxMap = mockMapboxMap(styles)
        val carContext = carContext(isDarkMode = false)

        sut.onAttached(mapSurface(mapboxMap, carContext))
        sut.setLightStyleOverride(mockk { every { style } returns "test-light-override" })
        sut.onCarConfigurationChanged(carContext)

        assertEquals(
            listOf(NavigationStyles.NAVIGATION_DAY_STYLE, "test-light-override"),
            styles.map { it.style },
        )
    }

    @Test
    fun `configuration change retries a style that failed to load`() {
        val errorListener = slot<OnMapLoadErrorListener>()
        val mapboxMap = mockMapboxMap(errorListener = errorListener)
        val carContext = carContext(isDarkMode = false)

        sut.onAttached(mapSurface(mapboxMap, carContext))
        errorListener.captured.onMapLoadError(mockk<MapLoadingErrorEventData>(relaxed = true))
        sut.onCarConfigurationChanged(carContext)

        verify(exactly = 2) {
            mapboxMap.loadStyle(
                any<StyleContract.StyleExtension>(),
                any<Style.OnStyleLoaded>(),
                any<OnMapLoadErrorListener>(),
            )
        }
    }

    @Test
    fun `attaching again loads the style`() {
        val mapboxMap = mockMapboxMap()
        val surface = mapSurface(mapboxMap, carContext(isDarkMode = false))

        sut.onAttached(surface)
        sut.onDetached(surface)
        sut.onAttached(surface)

        verify(exactly = 2) {
            mapboxMap.loadStyle(
                any<StyleContract.StyleExtension>(),
                any<Style.OnStyleLoaded>(),
                any<OnMapLoadErrorListener>(),
            )
        }
    }

    private fun carContext(isDarkMode: Boolean): CarContext = mockk {
        every { this@mockk.isDarkMode } returns isDarkMode
    }

    private fun mockMapboxMap(
        styles: MutableList<StyleContract.StyleExtension> = mutableListOf(),
        errorListener: CapturingSlot<OnMapLoadErrorListener> = slot(),
    ): MapboxMap = mockk {
        every {
            loadStyle(capture(styles), any<Style.OnStyleLoaded>(), capture(errorListener))
        } answers {}
    }

    private fun mapSurface(mapboxMap: MapboxMap, carContext: CarContext): MapboxCarMapSurface =
        mockk {
            every { mapSurface } returns mockk {
                every { getMapboxMap() } returns mapboxMap
            }
            every { this@mockk.carContext } returns carContext
        }

    private fun mockMapboxCarMapSurface(
        styleExtensionSlot: CapturingSlot<StyleContract.StyleExtension>,
    ): MapboxCarMapSurface {
        return mockk {
            every { mapSurface } returns mockk {
                every { getMapboxMap() } returns mockk {
                    every {
                        loadStyle(
                            capture(styleExtensionSlot),
                            any<Style.OnStyleLoaded>(),
                            any(),
                        )
                    } answers {
                        secondArg<Style.OnStyleLoaded>().onStyleLoaded(mockk(relaxed = true))
                    }
                }
            }
        }
    }
}
