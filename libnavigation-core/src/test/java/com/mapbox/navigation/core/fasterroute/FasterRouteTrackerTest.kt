@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.core.fasterroute.TestRoutes.Companion.ARTIFICIAL_FASTER_ROUTE_FOR_MUNICH_NUREMBERG
import com.mapbox.navigation.core.fasterroute.TestRoutes.Companion.MUNICH_NUREMBERG
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FasterRouteTrackerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Test
    fun `no faster route is available from Munich to Nuremberg moving by the slowest road`() =
        runBlocking<Unit> {
            val fasterRoutes = createFasterRoutesTracker()
            val recordedRoutesUpdates = readRouteObserverResults(MUNICH_NUREMBERG)
            for (recordedUpdate in recordedRoutesUpdates) {
                val result = fasterRoutes.routesUpdated(
                    recordedUpdate.update,
                    recordedUpdate.alternativeMetadata.values.toList()
                )
                val alternativesFromUpdate = recordedUpdate.alternativeMetadata.values
                    .map { it.alternativeId }
                    .joinToString(separator = ",") { it.toString() }
                assertEquals(
                    "incorrect result for update with alternatives $alternativesFromUpdate",
                    FasterRouteResult.NoFasterRoad,
                    result
                )
            }
        }

    @Test
    fun `faster route is available Munich to Nuremberg moving by the slowest road`() =
        runBlocking<Unit> {
            val fasterRoutes = createFasterRoutesTracker()
            val preparationUpdates =
                readRouteObserverResults(MUNICH_NUREMBERG)
                    .take(24)
            for (recordedUpdate in preparationUpdates) {
                val result = fasterRoutes.routesUpdated(
                    recordedUpdate.update,
                    recordedUpdate.alternativeMetadata.values.toList()
                )
                assertEquals(
                    FasterRouteResult.NoFasterRoad,
                    result
                )
            }
            val updateWithArtificialFasterRoute =
                readRouteObserverResults(ARTIFICIAL_FASTER_ROUTE_FOR_MUNICH_NUREMBERG).single()

            val result = fasterRoutes.routesUpdated(
                updateWithArtificialFasterRoute.update,
                updateWithArtificialFasterRoute.alternativeMetadata.values.toList()
            )

            assertTrue("result is $result", result is FasterRouteResult.NewFasterRoadFound)
            val fasterRouteResult = result as FasterRouteResult.NewFasterRoadFound
            assertEquals(
                "2fRI3oZgP9QIffbtczCQl-FsWWdgLirxzAQL_4x8WtoB05ATMs2obA==#1",
                fasterRouteResult.route.id
            )
        }
}

internal fun createFasterRoutesTracker() = FasterRouteTracker(
    FasterRouteOptions(maxSimilarityToExistingRoute = 0.5)
)
