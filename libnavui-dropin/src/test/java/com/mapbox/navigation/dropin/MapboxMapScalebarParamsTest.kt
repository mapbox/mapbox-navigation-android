package com.mapbox.navigation.dropin

import android.content.Context
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.base.internal.extensions.LocaleEx
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class MapboxMapScalebarParamsTest {

    private val locale = mockk<Locale>(relaxed = true)
    private val appContext = mockk<Context>(relaxed = true) {
        every { inferDeviceLocale() } returns locale
    }
    private val context = mockk<Context>(relaxed = true) {
        every { applicationContext } returns appContext
    }

    @Before
    fun setUp() {
        mockkStatic(LocaleEx::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(LocaleEx::class)
    }

    @Test
    fun defaultMetricUnitsImperial() {
        every { locale.getUnitTypeForLocale() } returns UnitType.IMPERIAL
        val defaultParams = MapboxMapScalebarParams.Builder(context).build()
        assertEquals(false, defaultParams.isMetricUnits)
    }

    @Test
    fun defaultMetricUnitsMetrics() {
        every { locale.getUnitTypeForLocale() } returns UnitType.METRIC
        val defaultParams = MapboxMapScalebarParams.Builder(context).build()
        assertEquals(true, defaultParams.isMetricUnits)
    }

    @Test
    fun setMetricUnitsMetrics() {
        every { locale.getUnitTypeForLocale() } returns UnitType.IMPERIAL
        val defaultParams = MapboxMapScalebarParams.Builder(context).isMetricsUnits(true).build()
        assertEquals(true, defaultParams.isMetricUnits)
        verify(exactly = 0) { appContext.inferDeviceLocale() }
        verify(exactly = 0) { locale.getUnitTypeForLocale() }
    }
}
