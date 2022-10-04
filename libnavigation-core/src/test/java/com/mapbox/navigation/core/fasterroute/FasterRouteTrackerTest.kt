@file:OptIn(ExperimentalMapboxNavigationAPI::class)

package com.mapbox.navigation.core.fasterroute

import com.mapbox.navigation.base.ExperimentalMapboxNavigationAPI
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FasterRouteTrackerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Test
    fun `no faster route is available from Munich to Nuremberg moving by the slowest road`() = runBlocking<Unit> {
        val fasterRoutes = createFasterRoutesTracker()
        val recordedRoutesUpdates = readRouteObserverResults("com.mapbox.navigation.core.internal.fasterroute.munichnuremberg")
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
    fun `faster route is available Munich to Nuremberg moving by the slowest road`() = runBlocking<Unit> {
        val fasterRoutes = createFasterRoutesTracker()
        val preparationUpdates = readRouteObserverResults("com.mapbox.navigation.core.internal.fasterroute.munichnuremberg").take(24)
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
        val updateWithArtificialFasterRoute = readRouteObserverResults("com.mapbox.navigation.core.internal.fasterroute.munichnuremberg-with-artificial-faster-route-22").single()

        val result = fasterRoutes.routesUpdated(updateWithArtificialFasterRoute.update, updateWithArtificialFasterRoute.alternativeMetadata.values.toList())

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