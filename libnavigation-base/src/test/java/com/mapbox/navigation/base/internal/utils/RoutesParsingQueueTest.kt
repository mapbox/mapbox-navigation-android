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
        val result = queue.parseRouteResponse {
            "test"
        }
        assertEquals("test", result)
    }

    @Test
    fun `parse alternatives`() = runBlocking {
        val queue = createParsingQueue()
        val result = queue.parseAlternatives {
            "test"
        }
        assertEquals(AlternativesParsingResult.Parsed("test"), result)
    }

    @Test
    fun `parse routes in parallel`() = runBlockingTest {
        val queue = createParsingQueue()
        val rerouteResponseParsing = ParsingTask<String>()
        val newRoutesRequestParsing = ParsingTask<String>()

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

        rerouteResponseParsing.complete("reroute")
        assertEquals("reroute", rerouteParsingResult.await())

        assertTrue(newRoutesRequestParsing.isStarted)
        newRoutesRequestParsing.complete("new routes")
        assertEquals("new routes", routesParsingResult.await())
    }

    @Test
    fun `parse alternatives in parallel to reroute`() = runBlockingTest {
        val queue = createParsingQueue()
        val rerouteResponseParsing = ParsingTask<String>()
        val alternativesRouteParsing = ParsingTask<String>()

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

        rerouteResponseParsing.complete("reroute")
        assertEquals("reroute", rerouteParsingResult.await())

        assertFalse(alternativesRouteParsing.isStarted)
        assertEquals(AlternativesParsingResult.NotActual, alternativesParsingResult.await())
    }

    @Test
    fun `parse routes in parallel to alternatives`() = runBlockingTest {
        val queue = createParsingQueue()
        val rerouteResponseParsing = ParsingTask<String>()
        val alternativesRouteParsing = ParsingTask<String>()

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

        alternativesRouteParsing.complete("alternatives")
        assertEquals(AlternativesParsingResult.Parsed("alternatives"), alternativesParsingResult.await())

        assertTrue(rerouteResponseParsing.isStarted)
        rerouteResponseParsing.complete("reroute")
        assertEquals("reroute", rerouteParsingResult.await())
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