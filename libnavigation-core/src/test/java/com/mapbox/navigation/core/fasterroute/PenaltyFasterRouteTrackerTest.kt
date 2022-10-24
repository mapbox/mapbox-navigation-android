@file:OptIn(ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.fasterroute.TestRoutes.Companion.FASTER_ROUTE_IN_MUNICH
import com.mapbox.navigation.core.fasterroute.TestRoutes.Companion.MUNICH_NUREMBERG
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PenaltyFasterRouteTrackerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Test
    fun `no faster route is available from Munich to Nuremberg moving by the slowest road`() =
        runBlocking<Unit> {
            val fasterRoutes = createPenaltyFasterRoutesTrackerCore()
            val recordedRoutesUpdates = readRouteObserverResults(MUNICH_NUREMBERG)
            for (recordedUpdate in recordedRoutesUpdates) {
                val result = fasterRoutes.findFasterRouteInUpdate(
                    recordedUpdate.update,
                    recordedUpdate.alternativeMetadata.values.toList()
                )
                val alternativesFromUpdate = recordedUpdate.alternativeMetadata.values
                    .map { it.alternativeId }
                    .joinToString(separator = ",") { it.toString() }
                assertEquals(
                    "incorrect result for update with alternatives $alternativesFromUpdate",
                    FasterRouteResult.NoFasterRoute,
                    result
                )
            }
        }

    @Test
    fun `faster route is available driving in Munich after picking the slowest road`() =
        runBlocking<Unit> {
            val fasterRoutesTracker = createPenaltyFasterRoutesTrackerCore()
            val routeUpdates = readRouteObserverResults(FASTER_ROUTE_IN_MUNICH)
            val fasterRoutesIds = mutableListOf<String>()
            for (recordedUpdate in routeUpdates) {
                val result = fasterRoutesTracker.findFasterRouteInUpdate(
                    recordedUpdate.update,
                    recordedUpdate.alternativeMetadata.values.toList()
                )
                if (result is FasterRouteResult.NewFasterRouteFound) {
                    fasterRoutesIds.add(result.route.id)
                }
            }
            assertEquals(
                listOf(
                    "JJXKpAxo3Yhh3rbJJmToNPPkzjh-hJHZV8u2Uksl1gwue_3sdZZ0ig==#1",
                    "b_dZe9Trx9MkOIphyCILePn4cBI6taAQmSctw1k5jaNzWz8vL-10-w==#1",
                    "b_dZe9Trx9MkOIphyCILePn4cBI6taAQmSctw1k5jaNzWz8vL-10-w==#1"
                ),
                fasterRoutesIds
            )
        }

    @Test
    fun `rejecting first faster route driving in Munich filters other similar faster routes`() =
        runBlocking<Unit> {
            val fasterRoutesTracker = createPenaltyFasterRoutesTrackerCore()
            val routeUpdates = readRouteObserverResults(FASTER_ROUTE_IN_MUNICH)
            val fasterRoutesIds = mutableListOf<String>()
            for (recordedUpdate in routeUpdates) {
                val result = fasterRoutesTracker.findFasterRouteInUpdate(
                    recordedUpdate.update,
                    recordedUpdate.alternativeMetadata.values.toList()
                )
                if (result is FasterRouteResult.NewFasterRouteFound) {
                    fasterRoutesIds.add(result.route.id)
                    fasterRoutesTracker.fasterRouteDeclined(result.alternativeId, result.route)
                }
            }
            assertEquals(
                listOf(
                    "JJXKpAxo3Yhh3rbJJmToNPPkzjh-hJHZV8u2Uksl1gwue_3sdZZ0ig==#1"
                ),
                fasterRoutesIds
            )
        }
}

internal fun createPenaltyFasterRoutesTrackerCore() = PenaltyFasterRouteTrackerCore()
