package com.mapbox.navigation.base.internal.route.parsing

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.navigation.base.internal.route.testing.toDataRefJava
import com.mapbox.navigation.base.internal.utils.AlternativesParsingResult
import com.mapbox.navigation.base.internal.utils.PrepareForParsingAction
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.testing.FileUtils
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.assertIs
import com.mapbox.navigation.testing.factories.TestSDKRouteParser
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createRouteInterface
import com.mapbox.navigation.testing.factories.createRouteOptions
import com.mapbox.navigation.testing.fakes.TestTime
import com.mapbox.navigation.utils.internal.Time
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParsingTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Test
    fun `preparation callback is called when parsing long CA`() = runTest {
        var preparationCallbackInvoked = 0
        val parser = createParsing(
            prepareForParsingAction = {
                preparationCallbackInvoked++
            },
        )
        val longResponseJson = FileUtils.loadJsonFixture("long_route_7k.json")
        val longResponse = DirectionsResponse.fromJson(longResponseJson)

        val routeInterface = createRouteInterface(
            responseUUID = longResponse.uuid()!!,
            routeIndex = 1,
            responseJson = longResponseJson,
        )

        val result = parser.parserContinuousAlternatives(listOf(routeInterface))

        assertEquals(1, preparationCallbackInvoked)
        val parsedResult = assertIs<
            AlternativesParsingResult.Parsed<
                Result<ContinuousAlternativesParsingSuccessfulResult>,
                >,
            >(
            result,
        )
        assertNull(parsedResult.value.exceptionOrNull())
        assertEquals(
            listOf("Hx9dSjQIDnHkThyjoziZBodVBvaSGynKcAZEd2Ha5O05s3pKsvYkAQ==#1"),
            parsedResult.value.getOrThrow().routes.map { it.id },
        )
    }

    @Test
    fun `preparation callback is not called when parsing short CA`() = runTest {
        var preparationCallbackInvoked = 0
        val parser = createParsing(
            prepareForParsingAction = {
                preparationCallbackInvoked++
            },
        )
        val response = createDirectionsResponse(uuid = "uuid1")
        val responseJson = response.toJson()
        val routeInterface = createRouteInterface(
            responseUUID = response.uuid()!!,
            routeIndex = 0,
            responseJson = responseJson,
        )

        val result = parser.parserContinuousAlternatives(listOf(routeInterface))

        assertEquals(
            "preparation callback should not be called for short CA",
            0,
            preparationCallbackInvoked,
        )
        val parsedResult = assertIs<
            AlternativesParsingResult.Parsed<
                Result<ContinuousAlternativesParsingSuccessfulResult>,
                >,
            >(
            result,
        )
        assertNull(parsedResult.value.exceptionOrNull())
        assertEquals(
            listOf("uuid1#0"),
            parsedResult.value.getOrThrow().routes.map { it.id },
        )
    }

    @Test
    fun `routes are the same from the same response`() = runTest {
        val parser = createParsing()

        val testRoutes = parser.parseDirectionsResponse(
            createTestResponseToParse("route_with_alternatives_response.json"),
        ).getOrThrow().routes
        val interfacesParsingResult = parser.parserContinuousAlternatives(
            testRoutes.map { it.nativeRoute },
        )
        val reparsedRoutes = assertIs<
            AlternativesParsingResult.Parsed<
                Result<ContinuousAlternativesParsingSuccessfulResult>,
                >,
            >(
            interfacesParsingResult,
        ).value.getOrThrow().routes

        assertEquals(
            testRoutes,
            reparsedRoutes,
        )
    }

    @Test
    fun `parsing time is reported`() = runTest {
        val time = TestTime().apply {
            setSeconds(9)
        }
        val tracker = mockk<RouteParsingTracking>(relaxed = true)
        val parser = createParsing(
            routeParsingTracking = tracker,
            time = time,
        )

        parser.parseDirectionsResponse(
            createTestResponseToParse("route_with_alternatives_response.json"),
        ).getOrThrow().routes

        verify { tracker.routeResponseIsParsed(match { it.createdAtElapsedMillis == 9000L }) }
    }

    @Test
    fun `parsing empty response`() = runTest {
        val parser = createParsing()

        val parsingResult = parser.parseDirectionsResponse(
            createTestResponseToParse("test_directions_response_empty.json"),
        )

        assertTrue(parsingResult.isFailure)
    }

    private fun createParsing(
        routeLookup: (String) -> NavigationRoute? = { null },
        routeParsingTracking: RouteParsingTracking = noTracking(),
        time: Time = TestTime(),
        prepareForParsingAction: PrepareForParsingAction = {},
    ): ParsingEntryPoint = setupParsing(
        parsingDispatcher = coroutineRule.testDispatcher,
        time = time,
        existingParsedRoutesLookup = routeLookup,
        routeParsingTracking = routeParsingTracking,
        nativeRoute = false, // we can test only java parsing in unit tests
        nnParser = TestSDKRouteParser(),
        prepareForParsingAction = prepareForParsingAction,
    )
}

private fun createTestResponseToParse(
    fixtureName: String,
): DirectionsResponseToParse = DirectionsResponseToParse.from(
    responseBody = FileUtils.loadJsonFixture(fixtureName)
        .toDataRefJava(),
    routeRequest = createRouteOptions().toUrl("***").toString(),
    routerOrigin = RouterOrigin.ONLINE,
)
