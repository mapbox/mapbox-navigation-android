@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.base.internal.utils

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RouteParsingQueueTest {

    @get:Rule
    val logRule = LoggingFrontendTestRule()

    @Test
    fun `parse long routes with optimisations`() = runBlocking {
        var preparedForParsing = false
        val queue = createTestParsingQueue {
            preparedForParsing = true
        }
        val result = queue.parseRouteResponse(longRoutesResponseInfo()) {
            "test"
        }

        assertTrue(preparedForParsing)
        assertEquals("test", result)
    }

    @Test
    fun `parse alternatives`() = runBlocking {
        var preparedForParsing = false
        val queue = createTestParsingQueue {
            preparedForParsing = true
        }
        val result = queue.parseAlternatives(longRoutesAlternativesInfo()) {
            "test"
        }

        assertTrue(preparedForParsing)
        assertEquals(AlternativesParsingResult.Parsed("test"), result)
    }

    @Test
    fun `parse long routes in parallel`() = runBlockingTest {
        val rerouteResponseParsing = ParsingTask<String>()
        val newRoutesResponseParsing = ParsingTask<String>()
        val newRoutesResponseParsing2 = ParsingTask<String>()
        var preparedForParsingTimes = 0
        val queue = createTestParsingQueue {
            preparedForParsingTimes++
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                rerouteResponseParsing.parse()
            }
        }
        val newRoutesParsingResult = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                newRoutesResponseParsing.parse()
            }
        }
        val newRoutesParsingResult2 = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                newRoutesResponseParsing2.parse()
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
    fun `parse short routes in parallel with`() = runBlockingTest {
        val rerouteResponseParsing = ParsingTask<String>()
        val newRoutesResponseParsing = ParsingTask<String>()
        val newRoutesResponseParsing2 = ParsingTask<String>()
        var preparedForParsingTimes = 0
        val shortRoutesResponseInfo = createRoutesResponseInfo(100)
        val queue = createTestParsingQueue {
            preparedForParsingTimes++
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse(shortRoutesResponseInfo) {
                rerouteResponseParsing.parse()
            }
        }
        val newRoutesParsingResult = async {
            queue.parseRouteResponse(shortRoutesResponseInfo) {
                newRoutesResponseParsing.parse()
            }
        }
        val newRoutesParsingResult2 = async {
            queue.parseRouteResponse(shortRoutesResponseInfo) {
                newRoutesResponseParsing2.parse()
            }
        }

        assertTrue(rerouteResponseParsing.isStarted)
        assertTrue(newRoutesResponseParsing.isStarted)
        assertTrue(newRoutesResponseParsing2.isStarted)
        rerouteResponseParsing.complete("reroute")
        newRoutesResponseParsing.complete("new routes")
        newRoutesResponseParsing2.complete("new routes 2")

        assertEquals("reroute", rerouteParsingResult.await())
        assertEquals("new routes", newRoutesParsingResult.await())
        assertEquals("new routes 2", newRoutesParsingResult2.await())
        assertEquals(0, preparedForParsingTimes)
    }

    @Test
    fun `parse mix of short and long routes in parallel`() = runBlockingTest {
        val rerouteResponseParsing = ParsingTask<String>()
        val newLongRoutesResponseParsingTask = ParsingTask<String>()
        val newRoutesResponseParsing2 = ParsingTask<String>()
        var preparedForParsingTimes = 0
        val shortRoutesResponseInfo = createRoutesResponseInfo(100)
        val longRoutesResponseInfo = longRoutesResponseInfo()
        val queue = createTestParsingQueue {
            preparedForParsingTimes++
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse(shortRoutesResponseInfo) {
                rerouteResponseParsing.parse()
            }
        }
        val newLongRoutesParsingResult = async {
            queue.parseRouteResponse(longRoutesResponseInfo) {
                newLongRoutesResponseParsingTask.parse()
            }
        }
        val newRoutesParsingResult2 = async {
            queue.parseRouteResponse(shortRoutesResponseInfo) {
                newRoutesResponseParsing2.parse()
            }
        }

        assertTrue(rerouteResponseParsing.isStarted)
        assertTrue(newLongRoutesResponseParsingTask.isStarted)
        assertTrue(newRoutesResponseParsing2.isStarted)
        rerouteResponseParsing.complete("reroute")
        newLongRoutesResponseParsingTask.complete("new routes")
        newRoutesResponseParsing2.complete("new routes 2")

        assertEquals("reroute", rerouteParsingResult.await())
        assertEquals("new routes", newLongRoutesParsingResult.await())
        assertEquals("new routes 2", newRoutesParsingResult2.await())
        assertEquals(1, preparedForParsingTimes)
    }

    @Test
    fun `parse alternatives in parallel to reroute with optimisations`() = runBlockingTest {
        val rerouteResponseParsing = ParsingTask<String>()
        val alternativesRouteParsing = ParsingTask<String>()
        var preparedForParsingTimes = 0
        val queue = createTestParsingQueue {
            preparedForParsingTimes++
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                rerouteResponseParsing.parse()
            }
        }
        val alternativesParsingResult = async {
            queue.parseAlternatives(longRoutesAlternativesInfo()) {
                alternativesRouteParsing.parse()
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
    fun `parse routes in parallel to alternatives with optimisations`() = runBlockingTest {
        val rerouteResponseParsing = ParsingTask<String>()
        val alternativesRouteParsing = ParsingTask<String>()
        var preparedForParsingTimes = 0
        val queue = createTestParsingQueue {
            preparedForParsingTimes++
        }
        val alternativesParsingResult = async {
            queue.parseAlternatives(longRoutesAlternativesInfo()) {
                alternativesRouteParsing.parse()
            }
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse(longRoutesResponseInfo()) {
                rerouteResponseParsing.parse()
            }
        }

        assertTrue(alternativesRouteParsing.isStarted)
        assertFalse(rerouteResponseParsing.isStarted)
        assertEquals(1, preparedForParsingTimes)

        alternativesRouteParsing.complete("alternatives")
        assertEquals(
            AlternativesParsingResult.Parsed("alternatives"),
            alternativesParsingResult.await(),
        )

        assertTrue(rerouteResponseParsing.isStarted)
        assertEquals(2, preparedForParsingTimes)
        rerouteResponseParsing.complete("new routes")
        assertEquals("new routes", rerouteParsingResult.await())
    }

    @Test
    fun `parse routes then alternatives`() = runBlocking {
        var preparedForParsingTimes = 0
        val queue = createTestParsingQueue {
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

fun createTestParsingQueue(
    prepareAction: () -> Unit = { },
) = createOptimizedRoutesParsingQueue(prepareAction)

class ParsingTask<T>() {
    private val completableDeferred = CompletableDeferred<T>()
    var isStarted = false
        private set

    fun complete(result: T) {
        completableDeferred.complete(result)
    }

    suspend fun parse(): T {
        isStarted = true
        return completableDeferred.await()
    }
}

private fun longRoutesAlternativesInfo() =
    AlternativesInfo(
        longRoutesResponseInfo(),
    )

private fun longRoutesResponseInfo() =
    RouteResponseInfo(20 * 1024 * 1024 + 1)

private fun createRoutesResponseInfo(sizeBytes: Int = 1_000_000) =
    RouteResponseInfo(sizeBytes = sizeBytes)
