package com.mapbox.navigation.voicefeedback.internal

import android.location.Location
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DefaultContextProviderTest {

    @Test
    fun `getContext without location uses empty user fields`() {
        val provider = DefaultContextProvider(Locale.getDefault()) { null }

        val ctx = provider.getContext()

        assertEquals("", ctx.userContext.lat)
        assertEquals("", ctx.userContext.lon)
        assertEquals("", ctx.userContext.placeName)
    }

    @Test
    fun `getContext maps enhanced location`() {
        val provider = DefaultContextProvider(Locale.getDefault()) {
            makeLocationMatcherResult(-74.0, 40.0, 180f)
        }

        val ctx = provider.getContext()

        assertEquals("40.0", ctx.userContext.lat)
        assertEquals("-74.0", ctx.userContext.lon)
        assertEquals("180.0", ctx.userContext.heading)
    }

    @Test
    fun `getContext app locale uses language tag`() {
        val provider = DefaultContextProvider(Locale.forLanguageTag("de-DE")) { null }

        val ctx = provider.getContext()

        assertEquals("de-DE", ctx.appContext?.locale)
        assertNotNull(ctx.appContext?.clientTime)
    }

    @Test
    fun `getContext imperial locale uses Fahrenheit and miles`() {
        val provider = DefaultContextProvider(Locale.US) { null }

        val app = provider.getContext().appContext!!

        assertEquals("Fahrenheit", app.temperatureUnits)
        assertEquals("mi", app.distanceUnits)
    }

    @Test
    fun `getContext metric locale uses Celsius and km`() {
        val provider = DefaultContextProvider(Locale.GERMANY) { null }

        val app = provider.getContext().appContext!!

        assertEquals("Celsius", app.temperatureUnits)
        assertEquals("km", app.distanceUnits)
    }

    @Test
    fun `client time is non-empty`() {
        val provider = DefaultContextProvider(Locale.US) { null }
        val time = provider.getContext().appContext!!.clientTime!!

        assertTrue(time.isNotBlank())
    }

    fun makeLocation(lat: Double, lon: Double, heading: Float = 0f) =
        mockk<Location> {
            every { latitude } returns lat
            every { longitude } returns lon
            every { bearing } returns heading
        }

    fun makeLocationMatcherResult(lon: Double, lat: Double, bearing: Float) =
        mockk<LocationMatcherResult> {
            val location = makeLocation(lat, lon, bearing)
            every { enhancedLocation } returns location
        }
}
