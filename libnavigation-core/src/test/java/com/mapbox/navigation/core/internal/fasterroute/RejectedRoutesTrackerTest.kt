package com.mapbox.navigation.core.internal.fasterroute

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.internal.utils.RecordedRoutesUpdateResult
import com.mapbox.navigation.core.internal.utils.readRouteObserverResults
import org.junit.Assert.assertEquals
import org.junit.Test

class RejectedRoutesTrackerTest {
    @Test
    fun `from Munich to Nuremberg by the slowest route`() {
        val rejectedRoutesTracker = createRejectedRoutesTracker()
        val recordedRoutesUpdates = readRouteObserverResults("com.mapbox.navigation.core.internal.fasterroute.munichnuremberg")
        recordedRoutesUpdates.forEachIndexed { index, recordedRoutesUpdateResult ->
            val routesUpdate = recordedRoutesUpdateResult.update
            if (routesUpdate.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW) {
                val alternatives = createAlternativesMap(routesUpdate, recordedRoutesUpdateResult)
                rejectedRoutesTracker.trackAlternatives(alternatives)
            }
            if (routesUpdate.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE) {
                val alternatives = createAlternativesMap(routesUpdate, recordedRoutesUpdateResult)
                val result = rejectedRoutesTracker.trackAlternatives(alternatives)
                if (index == 26) {
                    assertEquals(listOf("sZQg3spAjVPQn7DmpgNYzVRIiPdPSwwY9P3cMI6tajmNMjpwfUexjg==#1"), result.untracked.map { it.id })
                } else if (index == 28) {
                    assertEquals(listOf("E0WrBAlBb9OhP3e1SxePpciACvyTTgtUwbr4VCXgq1I4XMc_kU6ezQ==#1"), result.untracked.map { it.id })
                } else {
                    assertEquals(emptyList<NavigationRoute>(), result.untracked)
                }
            }
        }
    }

    private fun createAlternativesMap(
        routesUpdate: RoutesUpdatedResult,
        recordedRoutesUpdateResult: RecordedRoutesUpdateResult
    ): MutableMap<Int, NavigationRoute> {
        val alternatives = mutableMapOf<Int, NavigationRoute>()
        routesUpdate.navigationRoutes.drop(1).forEach {
            val alternativeId = recordedRoutesUpdateResult.alternativeIdsMap[it.id]!!
            alternatives[alternativeId] = it
        }
        return alternatives
    }

    private fun createRejectedRoutesTracker() = RejectedRoutesTracker(
        minimumGeometrySimilarity = 0.5
    )
}