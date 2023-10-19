package com.mapbox.navigation.base.internal.utils

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
    fun `parse routes`() = runBlocking {
        val queue = createParsingQueue()
        var preparedForParsing = false

        queue.setPrepareForParsingAction {
            preparedForParsing = true
        }
        val result = queue.parseRouteResponse {
            "test"
        }

        assertTrue(preparedForParsing)
        assertEquals("test", result)
    }

    @Test
    fun `parse alternatives`() = runBlocking {
        val queue = createParsingQueue()
        var preparedForParsing = false

        queue.setPrepareForParsingAction {
            preparedForParsing = true
        }
        val result = queue.parseAlternatives {
            "test"
        }

        assertTrue(preparedForParsing)
        assertEquals(AlternativesParsingResult.Parsed("test"), result)
    }

    @Test
    fun `parse routes in parallel`() = runBlockingTest {
        val queue = createParsingQueue()
        val rerouteResponseParsing = ParsingTask<String>()
        val newRoutesRequestParsing = ParsingTask<String>()
        var preparedForParsingTimes = 0

        queue.setPrepareForParsingAction {
            preparedForParsingTimes++
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse {
                rerouteResponseParsing.parse()
            }
        }
        val routesParsingResult = async {
            queue.parseRouteResponse {
                newRoutesRequestParsing.parse()
            }
        }

        assertTrue(rerouteResponseParsing.isStarted)
        assertFalse(newRoutesRequestParsing.isStarted)
        assertEquals(1, preparedForParsingTimes)

        rerouteResponseParsing.complete("reroute")
        assertEquals("reroute", rerouteParsingResult.await())
        assertEquals(2, preparedForParsingTimes)

        assertTrue(newRoutesRequestParsing.isStarted)
        newRoutesRequestParsing.complete("new routes")
        assertEquals("new routes", routesParsingResult.await())
        assertEquals(2, preparedForParsingTimes)
    }

    @Test
    fun `parse alternatives in parallel to reroute`() = runBlockingTest {
        val queue = createParsingQueue()
        val rerouteResponseParsing = ParsingTask<String>()
        val alternativesRouteParsing = ParsingTask<String>()
        var preparedForParsingTimes = 0

        queue.setPrepareForParsingAction {
            preparedForParsingTimes++
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse {
                rerouteResponseParsing.parse()
            }
        }
        val alternativesParsingResult = async {
            queue.parseAlternatives {
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
    fun `parse routes in parallel to alternatives`() = runBlockingTest {
        val queue = createParsingQueue()
        val rerouteResponseParsing = ParsingTask<String>()
        val alternativesRouteParsing = ParsingTask<String>()
        var preparedForParsingTimes = 0

        queue.setPrepareForParsingAction {
            preparedForParsingTimes++
        }
        val alternativesParsingResult = async {
            queue.parseAlternatives {
                alternativesRouteParsing.parse()
            }
        }
        val rerouteParsingResult = async {
            queue.parseRouteResponse {
                rerouteResponseParsing.parse()
            }
        }

        assertTrue(alternativesRouteParsing.isStarted)
        assertFalse(rerouteResponseParsing.isStarted)
        assertEquals(1, preparedForParsingTimes)

        alternativesRouteParsing.complete("alternatives")
        assertEquals(AlternativesParsingResult.Parsed("alternatives"), alternativesParsingResult.await())

        assertTrue(rerouteResponseParsing.isStarted)
        assertEquals(2, preparedForParsingTimes)
        rerouteResponseParsing.complete("new routes")
        assertEquals("new routes", rerouteParsingResult.await())
    }

    @Test
    fun `parse routes then alterantives`() = runBlocking {
        val queue = createParsingQueue()
        var preparedForParsingTimes = 0

        queue.setPrepareForParsingAction {
            preparedForParsingTimes++
        }
        val routesParstingResult = queue.parseRouteResponse {
            "test"
        }
        assertEquals("test", routesParstingResult)

        val alternativesParsingResult = queue.parseAlternatives {
            "test"
        }
        assertEquals(AlternativesParsingResult.Parsed("test"), alternativesParsingResult)
        assertEquals(2, preparedForParsingTimes)
    }
}

fun createParsingQueue() = RoutesParsingQueue()

class ParsingTask<T>() {
    private val completableDeferred = CompletableDeferred<T>()
    var isStarted = false
        private set
    fun complete(result: T) {
        completableDeferred.complete(result)
    }
    suspend fun parse():T {
        isStarted = true
        return completableDeferred.await()
    }
}