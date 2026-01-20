package com.mapbox.navigation.base.internal.route.parsing

import com.mapbox.navigation.base.internal.route.parsing.models.DirectionsResponseParsingResult
import com.mapbox.navigation.base.internal.route.parsing.models.JavaRouteModelsParser
import com.mapbox.navigation.base.internal.route.parsing.models.RouteModelsParser
import com.mapbox.navigation.base.internal.utils.AlternativesParsingResult
import com.mapbox.navigation.base.internal.utils.createImmediateNoOptimizationsParsingQueue
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.assertIs
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.testing.factories.createRouteInterface
import com.mapbox.navigation.utils.internal.Time
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JsonResponseOptimizedParserTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val mockTime: Time = mockk {
        every { seconds() } returns 1000L
    }

    @Test
    fun `routes from different responses `() = runTest {
        val response1 = createDirectionsResponse(uuid = "uuid1")
        val response1Json = response1.toJson()
        val response2 = createDirectionsResponse(uuid = "uuid2")
        val response2Json = response2.toJson()
        val response3 = createDirectionsResponse(uuid = "uuid3")
        val response3Json = response3.toJson()

        val parsingStrategy = JavaParserWrapper()

        val parser = createRouteInterfaceParser(modelsParser = parsingStrategy)

        // Create three routes from THREE different responses
        val routeInterface1 = createRouteInterface(
            responseUUID = response1.uuid()!!,
            routeIndex = 0,
            responseJson = response1Json,
        )

        val routeInterface2 = createRouteInterface(
            responseUUID = response2.uuid()!!,
            routeIndex = 0,
            responseJson = response2Json,
        )

        val routeInterface3 = createRouteInterface(
            responseUUID = response3.uuid()!!,
            routeIndex = 0,
            responseJson = response3Json,
        )

        val result = assertAlternativesParsed {
            parser.parserContinuousAlternatives(
                listOf(routeInterface1, routeInterface2, routeInterface3),
            )
        }
        val parsedRoutes = result.getOrThrow().routes

        assertEquals(
            listOf("uuid1#0", "uuid2#0", "uuid3#0").sorted(),
            parsedRoutes.map { it.id },
        )
        assertEquals(
            listOf("uuid1", "uuid2", "uuid3").sorted(),
            parsingStrategy.parsedResponses.sorted(),
        )
    }

    @Test
    fun `mixed scenario - some routes from lookup, some from different responses`() = runTest {
        val response1 = createDirectionsResponse(uuid = "uuid1")
        val response1Json = response1.toJson()
        val response2 = createDirectionsResponse(
            uuid = "uuid2",
            routes = listOf(
                createDirectionsRoute(),
                createDirectionsRoute(),
            ),
        )
        val response2Json = response2.toJson()
        val response3 = createDirectionsResponse(
            uuid = "uuid3",
            routes = listOf(
                createDirectionsRoute(),
                createDirectionsRoute(),
            ),
        )
        val response3Json = response3.toJson()

        val routeFromLookup1 = createNavigationRoutes(response1)[0]
        val routeFromLookup2 = createNavigationRoutes(response2)[1]
        val routeLookup: (String) -> NavigationRoute? = { routeId ->
            when (routeId) {
                "uuid1#0" -> routeFromLookup1
                "uuid2#1" -> routeFromLookup2
                else -> null
            }
        }
        val parsingStrategy = JavaParserWrapper()

        val parser = createRouteInterfaceParser(
            routeLookup = routeLookup,
            modelsParser = parsingStrategy,
        )

        val routeInterface1_0 = createRouteInterface(
            responseUUID = response1.uuid()!!,
            routeIndex = 0,
            responseJson = response1Json,
        )
        val routeInterface2_0 = createRouteInterface(
            responseUUID = response2.uuid()!!,
            routeIndex = 0,
            responseJson = response2Json,
        )
        val routeInterface2_1 = createRouteInterface(
            responseUUID = response2.uuid()!!,
            routeIndex = 1,
            responseJson = response2Json,
        )
        val routeInterface3_0 = createRouteInterface(
            responseUUID = response3.uuid()!!,
            routeIndex = 0,
            responseJson = response3Json,
        )
        val routeInterface3_1 = createRouteInterface(
            responseUUID = response3.uuid()!!,
            routeIndex = 1,
            responseJson = response3Json,
        )

        // Execute
        val result = assertAlternativesParsed {
            parser.parserContinuousAlternatives(
                listOf(
                    routeInterface1_0,
                    routeInterface2_0,
                    routeInterface3_0,
                    routeInterface2_1,
                    routeInterface3_1,
                ),
            )
        }
        val parsedRoutes = result.getOrThrow().routes

        assertEquals(
            listOf(
                "uuid1#0",
                "uuid2#0",
                "uuid2#1",
                "uuid3#0",
                "uuid3#1",
            ).sorted(),
            parsedRoutes.map { it.id }.sorted(),
        )

        assertEquals(
            listOf("uuid2", "uuid3").sorted(),
            parsingStrategy.parsedResponses.sorted(),
        )

        assertSame(routeFromLookup1, parsedRoutes.first { it.id == "uuid1#0" })
        assertSame(routeFromLookup2, parsedRoutes.first { it.id == "uuid2#1" })
    }

    @Test
    fun `parsing failure is propagated as Result failure`() = runTest {
        val routeLookup: (String) -> NavigationRoute? = { null }

        val parsingException = RuntimeException("Parsing failed")
        val modelParser = RouteModelsParser { _ ->
            Result.failure(parsingException)
        }

        val parser = createRouteInterfaceParser(
            modelsParser = modelParser,
        )

        val routeInterface = createRouteInterface(
            responseUUID = "uuid1",
            routeIndex = 0,
            responseJson = createDirectionsResponse(uuid = "uuid1").toJson(),
        )

        // Execute
        val result = assertAlternativesParsed {
            parser.parserContinuousAlternatives(listOf(routeInterface))
        }

        // Verify: should return failure
        assertTrue(result.isFailure)
        assertEquals(parsingException, result.exceptionOrNull())
    }

    @Test
    fun `empty route list returns empty result`() = runTest {
        val parsingStrategy = JavaParserWrapper()
        val parser = createRouteInterfaceParser(parsingStrategy)

        val result = assertAlternativesParsed { parser.parserContinuousAlternatives(emptyList()) }

        assertNull(result.exceptionOrNull())
        assertEquals(0, result.getOrThrow().routes.size)
        assertEquals(emptyList<String>(), parsingStrategy.parsedResponses)
    }

    private fun createRouteInterfaceParser(
        modelsParser: RouteModelsParser,
        routeLookup: (String) -> NavigationRoute? = { null },
    ): JsonResponseOptimizedRouteInterfaceParser = JsonResponseOptimizedRouteInterfaceParser(
        existingParsedRoutesLookup = routeLookup,
        parsingDispatcher = coroutineRule.testDispatcher,
        time = mockTime,
        parser = modelsParser,
        parsingQueue = createImmediateNoOptimizationsParsingQueue(),
    )

    private inline fun assertAlternativesParsed(
        block: () -> AlternativesParsingResult<
            Result<ContinuousAlternativesParsingSuccessfulResult>,
            >,
    ): Result<ContinuousAlternativesParsingSuccessfulResult> {
        return assertIs<
            AlternativesParsingResult.Parsed<
                Result<ContinuousAlternativesParsingSuccessfulResult>,
                >,
            >(
            block(),
        ).value
    }
}

internal class JavaParserWrapper : RouteModelsParser {
    val parser = JavaRouteModelsParser()
    val parsedResponses = mutableListOf<String>()

    override fun parse(
        response: DirectionsResponseToParse,
    ): Result<DirectionsResponseParsingResult> {
        return parser.parse(response).onSuccess {
            parsedResponses.add(it.responseUUID!!)
        }
    }
}
