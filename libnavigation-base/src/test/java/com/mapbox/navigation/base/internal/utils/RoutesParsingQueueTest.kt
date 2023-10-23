@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.utils

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.LongRoutesOptimisationOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class RoutesParsingQueueTest {
    @Test
    fun `parse long routes with optimisations`() = runBlocking {
        val queue = createParsingQueue(optimiseLongRoutesConfig())
        var preparedForParsing = false
        var parseArguments: ParseArguments? = null

        queue.setPrepareForParsingAction {
            preparedForParsing = true
        }
        val result = queue.parseRouteResponse(longRoutesResponseInfo()) {
            parseArguments = it
            "test"
        }

        assertTrue(preparedForParsing)
        assertEquals("test", result)
        assertEquals(true, parseArguments?.optimiseDirectionsResponseStructure)
    }

    @Test
    fun `parse alternatives`() = runBlocking {
        val queue = createParsingQueue(optimiseLongRoutesConfig())
        var preparedForParsing = false
        var parseArguments: ParseArguments? = null

        queue.setPrepareForParsingAction {
            preparedForParsing = true
        }
        val result = queue.parseAlternatives(longRoutesAlternativesInfo()) {
            parseArguments = it
            "test"
        }

        assertTrue(preparedForParsing)
        assertEquals(AlternativesParsingResult.Parsed("test"), result)
        assertEquals(true, parseArguments?.optimiseDirectionsResponseStructure)
    }

    @Test
    fun `parse immediate alternatives without optimisations`() = runBlocking {
        val queue = createParsingQueueWithOptimisationsDisabled()
        var preparedForParsing = false
        var parsingArguments: ParseArguments? = null

        queue.setPrepareForParsingAction {
            preparedForParsing = true
        }
        val args = longRoutesAlternativesInfo(userTriggeredImmediateRefresh = true)
        val result = queue.parseAlternatives(args) {
            parsingArguments = it
            "test"
        }

        assertFalse(preparedForParsing)
        assertEquals(AlternativesParsingResult.Parsed("test"), result)
        assertEquals(false, parsingArguments?.optimiseDirectionsResponseStructure)
    }

    @Test
    fun `parse immediate alternatives with optimisations`() = runBlocking {
        val queue = createParsingQueue(optimiseLongRoutesConfig())
        val parsingTask = ParsingTask<String>().apply {
            complete("test")
        }
        var preparedForParsing = false

        queue.setPrepareForParsingAction {
            preparedForParsing = true
        }
        val args = longRoutesAlternativesInfo(userTriggeredImmediateRefresh = true)
        val result = queue.parseAlternatives(args) {
            parsingTask.parse(it)
        }

        assertFalse(preparedForParsing)
        assertFalse(parsingTask.isStarted)
        assertEquals(AlternativesParsingResult.NotActual, result)
    }

    @Test
    fun `parse long routes in parallel with optimisations`() = runBlockingTest {
        val queue = createParsingQueue(optimiseLongRoutesConfig())
        val rerouteResponseParsing = ParsingTask<String>()
        val newRoutesResponseParsing = ParsingTask<String>()
        val newRoutesResponseParsing2 = ParsingTask<String>()
        var preparedForParsingTimes = 0

        queue.setPrepareForParsingAction {
            preparedForParsingTimes++
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                rerouteResponseParsing.parse(it)
            }
        }
        val newRoutesParsingResult = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                newRoutesResponseParsing.parse(it)
            }
        }
        val newRoutesParsingResult2 = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                newRoutesResponseParsing2.parse(it)
            }
        }

        assertTrue(rerouteResponseParsing.isStarted)
        assertFalse(newRoutesResponseParsing.isStarted)
        assertFalse(newRoutesResponseParsing2.isStarted)
        assertEquals(1, preparedForParsingTimes)

        rerouteResponseParsing.complete("reroute")
        assertEquals("reroute", rerouteParsingResult.await())
        assertEquals(2, preparedForParsingTimes)

        assertTrue(newRoutesResponseParsing.isStarted)
        assertFalse(newRoutesResponseParsing2.isStarted)
        newRoutesResponseParsing.complete("new routes")
        assertEquals("new routes", newRoutesParsingResult.await())
        assertEquals(3, preparedForParsingTimes)

        assertTrue(newRoutesResponseParsing2.isStarted)
        newRoutesResponseParsing2.complete("new routes 2")
        assertEquals("new routes 2", newRoutesParsingResult2.await())
        assertEquals(3, preparedForParsingTimes)
    }

    @Test
    fun `parse short routes in parallel with optimisations enabled`() = runBlockingTest {
        val queue = createParsingQueue(
            LongRoutesOptimisationOptions.OptimiseNavigationForLongRoutes(
                currentRouteLengthMeters = 0,
                responseToParseSizeBytes = 5_000
            )
        )
        val rerouteResponseParsing = ParsingTask<String>()
        val newRoutesResponseParsing = ParsingTask<String>()
        val newRoutesResponseParsing2 = ParsingTask<String>()
        var preparedForParsingTimes = 0
        val shortRoutesResponseInfo = createRoutesResponseInfo(100)

        queue.setPrepareForParsingAction {
            preparedForParsingTimes++
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse(shortRoutesResponseInfo) {
                rerouteResponseParsing.parse(it)
            }
        }
        val newRoutesParsingResult = async {
            queue.parseRouteResponse(shortRoutesResponseInfo) {
                newRoutesResponseParsing.parse(it)
            }
        }
        val newRoutesParsingResult2 = async {
            queue.parseRouteResponse(shortRoutesResponseInfo) {
                newRoutesResponseParsing2.parse(it)
            }
        }

        assertTrue(rerouteResponseParsing.isStarted)
        assertEquals(
            true,
            rerouteResponseParsing.parsingArgs?.optimiseDirectionsResponseStructure
        )
        assertTrue(newRoutesResponseParsing.isStarted)
        assertEquals(
            true,
            newRoutesResponseParsing.parsingArgs?.optimiseDirectionsResponseStructure
        )
        assertTrue(newRoutesResponseParsing2.isStarted)
        assertEquals(
            true,
            newRoutesResponseParsing2.parsingArgs?.optimiseDirectionsResponseStructure
        )
        rerouteResponseParsing.complete("reroute")
        newRoutesResponseParsing.complete("new routes")
        newRoutesResponseParsing2.complete("new routes 2")

        assertEquals("reroute", rerouteParsingResult.await())
        assertEquals("new routes", newRoutesParsingResult.await())
        assertEquals("new routes 2", newRoutesParsingResult2.await())
        assertEquals(0, preparedForParsingTimes)
    }

    @Test
    fun `parse routes in parallel without optimisations`() = runBlockingTest {
        val queue = createParsingQueueWithOptimisationsDisabled()
        val rerouteResponseParsing = ParsingTask<String>()
        val newRoutesRequestParsing = ParsingTask<String>()
        var preparedForParsingTimes = 0

        queue.setPrepareForParsingAction {
            preparedForParsingTimes++
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                rerouteResponseParsing.parse(it)
            }
        }
        val routesParsingResult = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                newRoutesRequestParsing.parse(it)
            }
        }

        assertTrue(rerouteResponseParsing.isStarted)
        assertTrue(newRoutesRequestParsing.isStarted)
        assertEquals(0, preparedForParsingTimes)

        rerouteResponseParsing.complete("reroute")
        assertEquals("reroute", rerouteParsingResult.await())
        assertEquals(0, preparedForParsingTimes)

        newRoutesRequestParsing.complete("new routes")
        assertEquals("new routes", routesParsingResult.await())
        assertEquals(0, preparedForParsingTimes)
    }

    @Test
    fun `parse alternatives in parallel to reroute with optimisations`() = runBlockingTest {
        val queue = createParsingQueue(optimiseLongRoutesConfig())
        val rerouteResponseParsing = ParsingTask<String>()
        val alternativesRouteParsing = ParsingTask<String>()
        var preparedForParsingTimes = 0

        queue.setPrepareForParsingAction {
            preparedForParsingTimes++
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                rerouteResponseParsing.parse(it)
            }
        }
        val alternativesParsingResult = async {
            queue.parseAlternatives(longRoutesAlternativesInfo()) {
                alternativesRouteParsing.parse(it)
            }
        }

        assertTrue(rerouteResponseParsing.isStarted)
        assertFalse(alternativesRouteParsing.isStarted)
        assertEquals(1, preparedForParsingTimes)

        rerouteResponseParsing.complete("reroute")
        assertEquals("reroute", rerouteParsingResult.await())

        assertFalse(alternativesRouteParsing.isStarted)
        assertEquals(AlternativesParsingResult.NotActual, alternativesParsingResult.await())
        assertEquals(1, preparedForParsingTimes)
    }

    @Test
    fun `parse alternatives in parallel to reroute without optimisations`() = runBlockingTest {
        val queue = createParsingQueueWithOptimisationsDisabled()
        val rerouteResponseParsing = ParsingTask<String>()
        val alternativesRouteParsing = ParsingTask<String>()
        var preparedForParsingTimes = 0

        queue.setPrepareForParsingAction {
            preparedForParsingTimes++
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                rerouteResponseParsing.parse(it)
            }
        }
        val alternativesParsingResult = async {
            queue.parseAlternatives(longRoutesAlternativesInfo()) {
                alternativesRouteParsing.parse(it)
            }
        }

        assertTrue(rerouteResponseParsing.isStarted)
        assertTrue(alternativesRouteParsing.isStarted)
        assertEquals(0, preparedForParsingTimes)

        rerouteResponseParsing.complete("reroute")
        assertEquals("reroute", rerouteParsingResult.await())

        alternativesRouteParsing.complete("alternatives")
        assertEquals(
            AlternativesParsingResult.Parsed("alternatives"),
            alternativesParsingResult.await()
        )
        assertEquals(0, preparedForParsingTimes)
    }

    @Test
    fun `parse routes in parallel to alternatives with optimisations`() = runBlockingTest {
        val queue = createParsingQueue(optimiseLongRoutesConfig())
        val rerouteResponseParsing = ParsingTask<String>()
        val alternativesRouteParsing = ParsingTask<String>()
        var preparedForParsingTimes = 0

        queue.setPrepareForParsingAction {
            preparedForParsingTimes++
        }
        val alternativesParsingResult = async {
            queue.parseAlternatives(longRoutesAlternativesInfo()) {
                alternativesRouteParsing.parse(it)
            }
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                rerouteResponseParsing.parse(it)
            }
        }

        assertTrue(alternativesRouteParsing.isStarted)
        assertFalse(rerouteResponseParsing.isStarted)
        assertEquals(1, preparedForParsingTimes)

        alternativesRouteParsing.complete("alternatives")
        assertEquals(
            AlternativesParsingResult.Parsed("alternatives"),
            alternativesParsingResult.await()
        )

        assertTrue(rerouteResponseParsing.isStarted)
        assertEquals(2, preparedForParsingTimes)
        rerouteResponseParsing.complete("new routes")
        assertEquals("new routes", rerouteParsingResult.await())
    }

    @Test
    fun `parse routes in parallel to alternatives without optimisations`() = runBlockingTest {
        val queue = createParsingQueueWithOptimisationsDisabled()
        val rerouteResponseParsing = ParsingTask<String>()
        val alternativesRouteParsing = ParsingTask<String>()
        var preparedForParsingTimes = 0

        queue.setPrepareForParsingAction {
            preparedForParsingTimes++
        }
        val alternativesParsingResult = async {
            queue.parseAlternatives(longRoutesAlternativesInfo()) {
                alternativesRouteParsing.parse(it)
            }
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                rerouteResponseParsing.parse(it)
            }
        }

        assertTrue(alternativesRouteParsing.isStarted)
        assertTrue(rerouteResponseParsing.isStarted)
        assertEquals(0, preparedForParsingTimes)

        alternativesRouteParsing.complete("alternatives")
        assertEquals(
            AlternativesParsingResult.Parsed("alternatives"),
            alternativesParsingResult.await()
        )

        assertEquals(0, preparedForParsingTimes)
        rerouteResponseParsing.complete("new routes")
        assertEquals("new routes", rerouteParsingResult.await())
    }

    @Test
    fun `parse routes then alternatives`() = runBlocking {
        val queue = createParsingQueue(optimiseLongRoutesConfig())
        var preparedForParsingTimes = 0

        queue.setPrepareForParsingAction {
            preparedForParsingTimes++
        }
        val routesParstingResult = queue.parseRouteResponse(longRoutesResponseInfo()) {
            "test"
        }
        assertEquals("test", routesParstingResult)

        val alternativesParsingResult = queue.parseAlternatives(longRoutesAlternativesInfo()) {
            "test"
        }
        assertEquals(AlternativesParsingResult.Parsed("test"), alternativesParsingResult)
        assertEquals(2, preparedForParsingTimes)
    }
}

private fun createParsingQueue(
    config: LongRoutesOptimisationOptions.OptimiseNavigationForLongRoutes
) =
    RoutesParsingQueue(
        config
    )


fun createParsingQueueWithOptimisationsDisabled() =
    RoutesParsingQueue(LongRoutesOptimisationOptions.NoOptimisations)

class ParsingTask<T>() {
    private val completableDeferred = CompletableDeferred<T>()
    var isStarted = false
        private set

    var parsingArgs: ParseArguments? = null

    fun complete(result: T) {
        completableDeferred.complete(result)
    }

    suspend fun parse(args: ParseArguments): T {
        isStarted = true
        parsingArgs = args
        return completableDeferred.await()
    }
}

private fun optimiseLongRoutesConfig() =
    LongRoutesOptimisationOptions.OptimiseNavigationForLongRoutes(
        currentRouteLengthMeters = 4000 * 1000,
        responseToParseSizeBytes = 20 * 1024 * 1024
    )

private fun longRoutesAlternativesInfo(
    userTriggeredImmediateRefresh: Boolean = false
) =
    AlternativesInfo(
        longRoutesResponseInfo(),
        userTriggeredAlternativesRefresh = userTriggeredImmediateRefresh
    )

private fun longRoutesResponseInfo() =
    RouteResponseInfo(optimiseLongRoutesConfig().responseToParseSizeBytes + 1)

private fun createRoutesResponseInfo(sizeBytes: Int = 1_000_000) =
    RouteResponseInfo(sizeBytes = sizeBytes)
