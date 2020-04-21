package com.mapbox.navigation.base.internal.extensions

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import com.mapbox.navigation.base.typedef.IMPERIAL
import com.mapbox.navigation.base.typedef.METRIC
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class LocaleExTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun initialize() {
            mockkStatic("com.mapbox.navigation.base.internal.extensions.ContextEx")
        }
    }

    @Test
    fun getUnitTypeForLocaleCountryUS() {
        val locale = mockk<Locale>()
        every { locale.country } returns "us"
        every { locale.language } returns "spanglish"

        val result = locale.getUnitTypeForLocale()

        assertEquals(IMPERIAL, result)
    }

    @Test
    fun getUnitTypeForLocaleCountryLR() {
        val locale = mockk<Locale>()
        every { locale.country } returns "lr"
        every { locale.language } returns "spanglish"

        val result = locale.getUnitTypeForLocale()

        assertEquals(IMPERIAL, result)
    }

    @Test
    fun getUnitTypeForLocaleCountryMM() {
        val locale = mockk<Locale>()
        every { locale.country } returns "mm"
        every { locale.language } returns "spanglish"

        val result = locale.getUnitTypeForLocale()

        assertEquals(IMPERIAL, result)
    }

    @Test
    fun getUnitTypeForLocaleCountryDefault() {
        val locale = mockk<Locale>()
        every { locale.country } returns "zz"
        every { locale.language } returns "spanglish"

        val result = locale.getUnitTypeForLocale()

        assertEquals(METRIC, result)
    }

    @Test
    fun getLocaleDirectionsRouteNonNullVoiceLanguage() {
        val context = mockk<Context>()
        val directionsRoute = mockk<DirectionsRoute>()
        every { directionsRoute.voiceLanguage() } returns "en"

        val result = LocaleEx.getLocaleDirectionsRoute(directionsRoute, context)

        assertEquals("en", result.language)
    }

    @Test
    fun getLocaleDirectionsRouteNullVoiceLanguage() {
        val context = mockk<Context>()
        val directionsRoute = mockk<DirectionsRoute>()
        every { directionsRoute.voiceLanguage() } returns null
        every { context.inferDeviceLocale() } returns Locale("en")

        val result = LocaleEx.getLocaleDirectionsRoute(directionsRoute, context)

        assertEquals("en", result.language)
    }
}
