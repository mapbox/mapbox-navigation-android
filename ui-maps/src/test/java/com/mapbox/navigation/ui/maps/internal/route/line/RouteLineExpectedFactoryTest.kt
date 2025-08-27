package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.geojson.FeatureCollection
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.internal.route.callout.model.RouteCalloutData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteLineExpectedFactoryTest {

    @Before
    fun setUp() {
        mockkStatic("com.mapbox.navigation.ui.maps.internal.route.line.RouteLineDataConverterKt")
    }

    @After
    fun tearDown() {
        unmockkStatic("com.mapbox.navigation.ui.maps.internal.route.line.RouteLineDataConverterKt")
    }

    @Test
    fun routeSetValueEmpty() {
        val expectedPrimary = mockk<RouteLineData>()
        val primary = mockk<RouteLineEventData>(relaxed = true) {
            every { toRouteLineData() } returns expectedPrimary
        }
        val waypointsSource = mockk<FeatureCollection>(relaxed = true)

        val actual = RouteLineExpectedFactory.routeSetValue(
            primary,
            emptyList(),
            waypointsSource,
            null,
        )

        assertEquals(
            RouteSetValue(
                expectedPrimary,
                emptyList(),
                waypointsSource,
                RouteCalloutData(emptyList()),
                null,
            ),
            actual,
        )
    }

    @Test
    fun routeSetValueFilled() {
        val expectedPrimary = mockk<RouteLineData>()
        val expectedAlt1 = mockk<RouteLineData>()
        val expectedAlt2 = mockk<RouteLineData>()
        val expectedMasking = mockk<RouteLineDynamicData>()
        val primary = mockk<RouteLineEventData>(relaxed = true) {
            every { toRouteLineData() } returns expectedPrimary
        }
        val alt1 = mockk<RouteLineEventData>(relaxed = true) {
            every { toRouteLineData() } returns expectedAlt1
        }
        val alt2 = mockk<RouteLineEventData>(relaxed = true) {
            every { toRouteLineData() } returns expectedAlt2
        }
        val masking = mockk<RouteLineDynamicEventData>(relaxed = true) {
            every { toRouteLineDynamicData() } returns expectedMasking
        }

        val waypointsSource = mockk<FeatureCollection>(relaxed = true)

        val actual = RouteLineExpectedFactory.routeSetValue(
            primary,
            listOf(alt1, alt2),
            waypointsSource,
            masking,
        )

        assertEquals(
            RouteSetValue(
                expectedPrimary,
                listOf(expectedAlt1, expectedAlt2),
                waypointsSource,
                RouteCalloutData(emptyList()),
                expectedMasking,
            ),
            actual,
        )
    }

    @Test
    fun routeLineUpdateValueEmpty() {
        val actual = RouteLineExpectedFactory.routeLineUpdateValue(
            null,
            emptyList(),
            null,
        )

        assertEquals(
            RouteLineUpdateValue(null, emptyList(), null),
            actual,
        )
    }

    @Test
    fun routeLineUpdateValueFilled() {
        val expectedPrimary = mockk<RouteLineDynamicData>()
        val expectedAlt1 = mockk<RouteLineDynamicData>()
        val expectedAlt2 = mockk<RouteLineDynamicData>()
        val expectedMasking = mockk<RouteLineDynamicData>()
        val primary = mockk<RouteLineDynamicEventData>(relaxed = true) {
            every { toRouteLineDynamicData() } returns expectedPrimary
        }
        val alt1 = mockk<RouteLineDynamicEventData>(relaxed = true) {
            every { toRouteLineDynamicData() } returns expectedAlt1
        }
        val alt2 = mockk<RouteLineDynamicEventData>(relaxed = true) {
            every { toRouteLineDynamicData() } returns expectedAlt2
        }
        val masking = mockk<RouteLineDynamicEventData>(relaxed = true) {
            every { toRouteLineDynamicData() } returns expectedMasking
        }

        val actual = RouteLineExpectedFactory.routeLineUpdateValue(
            primary,
            listOf(alt1, alt2),
            masking,
        )

        assertEquals(
            RouteLineUpdateValue(
                expectedPrimary,
                listOf(expectedAlt1, expectedAlt2),
                expectedMasking,
            ),
            actual,
        )
    }

    @Test
    fun routeLineClearValue() {
        val primary = mockk<FeatureCollection>()
        val alt1 = mockk<FeatureCollection>()
        val alt2 = mockk<FeatureCollection>()
        val waypointSource = mockk<FeatureCollection>()
        val actual = RouteLineExpectedFactory.routeLineClearValue(
            primary,
            listOf(alt1, alt2),
            waypointSource,
        )

        assertEquals(
            RouteLineClearValue(
                primary,
                listOf(alt1, alt2),
                waypointSource,
                RouteCalloutData(emptyList()),
            ),
            actual,
        )
    }
}
