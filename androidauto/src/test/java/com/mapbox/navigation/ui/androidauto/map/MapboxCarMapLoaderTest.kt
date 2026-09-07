package com.mapbox.navigation.ui.androidauto.map

import com.mapbox.maps.Style
import com.mapbox.maps.extension.androidauto.MapboxCarMapSurface
import com.mapbox.maps.extension.style.StyleContract
import com.mapbox.maps.module.TelemetryEvent
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.NavigationStyles
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
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
