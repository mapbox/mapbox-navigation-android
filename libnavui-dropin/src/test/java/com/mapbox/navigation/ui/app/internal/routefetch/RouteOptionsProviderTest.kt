package com.mapbox.navigation.ui.app.internal.routefetch

import android.content.Context
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import com.mapbox.navigation.core.MapboxNavigation
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class RouteOptionsProviderTest {

    private val routeOptionsProvider = RouteOptionsProvider()
    private val mapboxNavigation by lazy {
        mockk<MapboxNavigation> {
            every { getZLevel() } returns Z_LEVEL
            every { navigationOptions } returns mockk {
                every { applicationContext } returns mockk {
                    every { inferDeviceLocale() } returns Locale.ENGLISH
                }
            }
        }
    }
    private val origin = Point.fromLngLat(1.0, 2.0)
    private val destination = Point.fromLngLat(3.0, 4.0)
    private val routeOptions = mockk<RouteOptions>()
    private val optionsBuilder = mockk<RouteOptions.Builder> {
        every { build() } returns routeOptions
    }
    private val interceptor: (RouteOptions.Builder) -> RouteOptions.Builder = { optionsBuilder }

    @Before
    fun `set up`() {
        mockkStatic(Context::inferDeviceLocale)
    }

    @After
    fun `tear down`() {
        unmockkStatic(Context::inferDeviceLocale)
    }

    @Test
    fun `provider returns default options if interceptor is not set`() {
        val options = routeOptionsProvider.getOptions(mapboxNavigation, origin, destination)

        assertEquals(listOf(Z_LEVEL, null), options.layersList())
        assertEquals(listOf(origin, destination), options.coordinatesList())
        assertEquals(true, options.alternatives())
    }

    @Test
    fun `provider returns options from interceptor if set`() {
        routeOptionsProvider.setInterceptor(interceptor)

        val options = routeOptionsProvider.getOptions(mapboxNavigation, origin, destination)

        assertEquals(options, routeOptions)
    }

    private companion object {
        private const val Z_LEVEL = 9
    }
}
