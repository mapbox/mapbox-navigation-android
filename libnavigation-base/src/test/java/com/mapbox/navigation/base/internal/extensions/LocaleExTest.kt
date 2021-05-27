package com.mapbox.navigation.base.internal.extensions

import android.content.Context
import com.mapbox.navigation.base.formatter.UnitType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class LocaleExTest {

    @Before
    fun setup() {
        mockkStatic(Locale::getUnitTypeForLocale)
        mockkStatic(Context::inferDeviceLocale)
    }

    @Test
    fun getUnitTypeForLocaleCountryUS() {
        val locale = mockk<Locale>()
        every { locale.country } returns "us"
        every { locale.language } returns "spanglish"

        val result = locale.getUnitTypeForLocale()

        assertEquals(UnitType.IMPERIAL, result)
    }

    @Test
    fun getUnitTypeForLocaleCountryLR() {
        val locale = mockk<Locale>()
        every { locale.country } returns "lr"
        every { locale.language } returns "spanglish"

        val result = locale.getUnitTypeForLocale()

        assertEquals(UnitType.IMPERIAL, result)
    }

    @Test
    fun getUnitTypeForLocaleCountryMM() {
        val locale = mockk<Locale>()
        every { locale.country } returns "mm"
        every { locale.language } returns "spanglish"

        val result = locale.getUnitTypeForLocale()

        assertEquals(UnitType.IMPERIAL, result)
    }

    @Test
    fun getUnitTypeForLocaleCountryDefault() {
        val locale = mockk<Locale>()
        every { locale.country } returns "zz"
        every { locale.language } returns "spanglish"

        val result = locale.getUnitTypeForLocale()

        assertEquals(UnitType.METRIC, result)
    }

    @After
    fun teardown() {
        unmockkStatic(Locale::getUnitTypeForLocale)
        unmockkStatic(Context::inferDeviceLocale)
    }
}
