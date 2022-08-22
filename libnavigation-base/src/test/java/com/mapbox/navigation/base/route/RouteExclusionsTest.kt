package com.mapbox.navigation.base.route

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.internal.NativeRouteParserWrapper
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigator.RouteInterface
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class RouteExclusionsTest {

    @Before
    fun setup() {
        mockkObject(NativeRouteParserWrapper)
        every {
            NativeRouteParserWrapper.parseDirectionsResponse(any(), any(), any())
        } answers {
            val response = JSONObject(this.firstArg<String>())
            val routesCount = response.getJSONArray("routes").length()
            val idBase = if (response.has("uuid")) {
                response.getString("uuid")
            } else {
                "local@${UUID.randomUUID()}"
            }
            val nativeRoutes = mutableListOf<RouteInterface>().apply {
                repeat(routesCount) {
                    add(
                        mockk {
                            every { routeInfo } returns mockk(relaxed = true)
                            every { routeId } returns "$idBase#$it"
                            every { routerOrigin } returns com.mapbox.navigator.RouterOrigin.ONBOARD
                        }
                    )
                }
            }
            ExpectedFactory.createValue(nativeRoutes)
        }
    }

    @After
    fun tearDown() {
        unmockkObject(NativeRouteParserWrapper)
    }

    @Test
    fun `empty exclusion violations if no exclude RouteOptions added`() {
        val origin = Point.fromLngLat(14.75513115258181, 55.19464648744247)
        val destination = Point.fromLngLat(12.54071010365584, 55.68521471271404)
        val routeOptionsWithoutExclusions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(listOf(origin, destination))
            .build()
        val directionsRoute = DirectionsRoute.builder()
            .routeOptions(routeOptionsWithoutExclusions)
            .routeIndex("0")
            .distance(183888.609)
            .duration(10697.573)
            .build()

        val exclusionViolations = directionsRoute.exclusionViolations()

        assertEquals(0, exclusionViolations.size)
    }

    @Test
    fun `toll and ferry exclusion violations - size`() {
        val directionsRoute = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("toll_and_ferry_directions_route.json")
        )

        val exclusionViolations = directionsRoute.exclusionViolations()

        assertEquals(77, exclusionViolations.size)
    }

    @Test
    fun `toll and ferry exclusion violations - type`() {
        val directionsRoute = DirectionsRoute.fromJson(
            FileUtils.loadJsonFixture("toll_and_ferry_directions_route.json")
        )

        val tollAndFerryExclusionViolations = directionsRoute.exclusionViolations()
            .groupBy { it.type }

        assertEquals(2, tollAndFerryExclusionViolations.size)
        assertTrue(tollAndFerryExclusionViolations.containsKey("toll"))
        assertTrue(tollAndFerryExclusionViolations.containsKey("ferry"))
    }
}
