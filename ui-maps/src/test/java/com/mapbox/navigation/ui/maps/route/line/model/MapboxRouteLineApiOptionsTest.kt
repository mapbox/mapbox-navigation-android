package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.BuilderTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.reflect.KClass

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class MapboxRouteLineApiOptionsTest :
    BuilderTest<MapboxRouteLineApiOptions, MapboxRouteLineApiOptions.Builder>() {

    override fun getImplementationClass(): KClass<MapboxRouteLineApiOptions> =
        MapboxRouteLineApiOptions::class

    override fun getFilledUpBuilder(): MapboxRouteLineApiOptions.Builder {
        return MapboxRouteLineApiOptions.Builder()
            .vanishingRouteLineEnabled(true)
            .vanishingRouteLineUpdateIntervalNano(7878)
            .styleInactiveRouteLegsIndependently(true)
            .calculateRestrictedRoadSections(true)
            .isRouteCalloutsEnabled(true)
            .trafficBackfillRoadClasses(listOf("a", "b"))
            .lowCongestionRange(0..29)
            .moderateCongestionRange(42..49)
            .heavyCongestionRange(63..71)
            .severeCongestionRange(88..100)
    }

    override fun trigger() {
        //
    }

    @Test
    fun `verify congestion ranges if not specified`() {
        val options = MapboxRouteLineApiOptions.Builder().build()

        assertEquals(0..39, options.lowCongestionRange)
        assertEquals(40..59, options.moderateCongestionRange)
        assertEquals(60..79, options.heavyCongestionRange)
        assertEquals(80..100, options.severeCongestionRange)
    }

    @Test
    fun `when low congestion range first value is negative then throw exception`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .lowCongestionRange(-10..39)
                .build()
        }
        assertEquals(
            "Ranges containing values less than 0 or greater than 100 are invalid.",
            exception.message,
        )
    }

    @Test
    fun `when low congestion range last value is greater than 100 then throw exception`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .lowCongestionRange(0..110)
                .build()
        }
        assertEquals(
            "Ranges containing values less than 0 or greater than 100 are invalid.",
            exception.message,
        )
    }

    @Test
    fun `when moderate congestion range first value is negative then throw exception`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .moderateCongestionRange(-10..39)
                .build()
        }
        assertEquals(
            "Ranges containing values less than 0 or greater than 100 are invalid.",
            exception.message,
        )
    }

    @Test
    fun `when moderate congestion range last value is greater than 100 then throw exception`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .moderateCongestionRange(80..110)
                .build()
        }
        assertEquals(
            "Ranges containing values less than 0 or greater than 100 are invalid.",
            exception.message,
        )
    }

    @Test
    fun `when heavy congestion range first value is negative then throw exception`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .heavyCongestionRange(-10..39)
                .build()
        }
        assertEquals(
            "Ranges containing values less than 0 or greater than 100 are invalid.",
            exception.message,
        )
    }

    @Test
    fun `when heavy congestion range last value is greater than 100 then throw exception`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .heavyCongestionRange(80..101)
                .build()
        }
        assertEquals(
            "Ranges containing values less than 0 or greater than 100 are invalid.",
            exception.message,
        )
    }

    @Test
    fun `when severe congestion range first value is negative then throw exception`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .severeCongestionRange(-10..39)
                .build()
        }
        assertEquals(
            "Ranges containing values less than 0 or greater than 100 are invalid.",
            exception.message,
        )
    }

    @Test
    fun `when severe congestion range last value is greater than 100 then throw exception`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .lowCongestionRange(0..39)
                .moderateCongestionRange(40..59)
                .heavyCongestionRange(60..79)
                .severeCongestionRange(80..105)
                .build()
        }
        assertEquals(
            "Ranges containing values less than 0 or greater than 100 are invalid.",
            exception.message,
        )
    }

    @Test
    fun `when low and moderate overlap then throw exception`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .lowCongestionRange(40..59)
                .moderateCongestionRange(0..52)
                .heavyCongestionRange(60..79)
                .severeCongestionRange(80..100)
                .build()
        }
        assertEquals(
            "Traffic congestion ranges should not overlap each other.",
            exception.message,
        )
    }

    @Test
    fun `when low and heavy overlap then throw exception`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .lowCongestionRange(0..39)
                .moderateCongestionRange(60..79)
                .heavyCongestionRange(20..59)
                .severeCongestionRange(80..100)
                .build()
        }
        assertEquals(
            "Traffic congestion ranges should not overlap each other.",
            exception.message,
        )
    }

    @Test
    fun `when low and severe overlap then throw exception`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .lowCongestionRange(0..39)
                .moderateCongestionRange(60..79)
                .heavyCongestionRange(80..100)
                .severeCongestionRange(20..59)
                .build()
        }
        assertEquals(
            "Traffic congestion ranges should not overlap each other.",
            exception.message,
        )
    }

    @Test
    fun `when moderate and heavy overlap then throw exception`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .lowCongestionRange(0..39)
                .moderateCongestionRange(40..59)
                .heavyCongestionRange(55..79)
                .severeCongestionRange(80..100)
                .build()
        }
        assertEquals(
            "Traffic congestion ranges should not overlap each other.",
            exception.message,
        )
    }

    @Test
    fun `when moderate and severe overlap then throw exception`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .lowCongestionRange(0..39)
                .moderateCongestionRange(40..59)
                .heavyCongestionRange(80..100)
                .severeCongestionRange(55..79)
                .build()
        }
        assertEquals(
            "Traffic congestion ranges should not overlap each other.",
            exception.message,
        )
    }

    @Test
    fun `when heavy and severe overlap then throw exception`() {
        val exception = assertThrows(IllegalStateException::class.java) {
            MapboxRouteLineApiOptions
                .Builder()
                .lowCongestionRange(0..39)
                .moderateCongestionRange(40..59)
                .heavyCongestionRange(60..79)
                .severeCongestionRange(75..100)
                .build()
        }
        assertEquals(
            "Traffic congestion ranges should not overlap each other.",
            exception.message,
        )
    }
}
