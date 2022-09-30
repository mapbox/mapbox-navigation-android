package com.mapbox.navigation.core.internal.fasterroute

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RejectedRoutesTrackerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @Test
    fun `track faster route from Munich to Nuremberg moving`() {
        val fasterRoutes = FasterRouteTracker(
            maximumAcceptedSimilarity = 0.5
        )
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
    fun `track routes from Munich to Nuremberg moving by the slowest route`() {
        val rejectedRoutesTracker = createRejectedRoutesTracker()
        val recordedRoutesUpdates = readRouteObserverResults("com.mapbox.navigation.core.internal.fasterroute.munichnuremberg")
        val untrackedRoutesIds = mutableListOf<String>()
        recordedRoutesUpdates.forEachIndexed { index, recordedRoutesUpdateResult ->
            val routesUpdate = recordedRoutesUpdateResult.update
            if (routesUpdate.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW) {
                val alternatives = createAlternativesMap(routesUpdate, recordedRoutesUpdateResult)
                rejectedRoutesTracker.trackAlternatives(alternatives)
            }
            if (routesUpdate.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE) {
                val alternatives = createAlternativesMap(routesUpdate, recordedRoutesUpdateResult)
                val result = rejectedRoutesTracker.trackAlternatives(alternatives)
                untrackedRoutesIds.addAll(result.untracked.map { it.id })
            }
        }
        assertEquals(
            listOf(
                "qsbHcSTKmGlcgMc9w4wrj2Uz_IZhbVuuhHqxuU_4e51RXsroy1proA==#1",
                "ffXOOuMdvPd1V3gfe-UOoOzITorRiWD84zuynFpMyM0VsILlHDOALA==#1",
                "h-pdR2s9gIcYG4_HHLLKHMvHvXT0DVx18Qk5pBkJZUcds-HDrik5oA==#1",
                "TBf3zrsyBxcfFDdUZzijAffv7jRt1RK34S_950--1mQ7GfIVOc_mxw==#1"
            ),
            untrackedRoutesIds
        )
    }

    private fun createAlternativesMap(
        routesUpdate: RoutesUpdatedResult,
        recordedRoutesUpdateResult: RecordedRoutesUpdateResult
    ): MutableMap<Int, NavigationRoute> {
        val alternatives = mutableMapOf<Int, NavigationRoute>()
        routesUpdate.navigationRoutes.drop(1).forEach {
            val metadata = recordedRoutesUpdateResult.alternativeMetadata[it.id]!!
            alternatives[metadata.alternativeId] = it
        }
        return alternatives
    }

    private fun createRejectedRoutesTracker() = RejectedRoutesTracker(
        minimumGeometrySimilarity = 0.5
    )
}