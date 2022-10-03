package com.mapbox.navigation.core.fasterroute

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
    fun `track routes from Munich to Nuremberg moving by the slowest route`() {
        val rejectedRoutesTracker = createRejectedRoutesTracker()
        val recordedRoutesUpdates =
            readRouteObserverResults("com.mapbox.navigation.core.internal.fasterroute.munichnuremberg")
        val untrackedRoutesIds = mutableListOf<String>()
        recordedRoutesUpdates.forEachIndexed { index, recordedRoutesUpdateResult ->
            val routesUpdate = recordedRoutesUpdateResult.update
            if (routesUpdate.reason == RoutesExtra.ROUTES_UPDATE_REASON_NEW) {
                val alternatives = createAlternativesMap(routesUpdate, recordedRoutesUpdateResult)
                rejectedRoutesTracker.clean()
                rejectedRoutesTracker.addRejectedRoutes(alternatives)
            }
            if (routesUpdate.reason == RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE) {
                val alternatives = createAlternativesMap(routesUpdate, recordedRoutesUpdateResult)
                val result = rejectedRoutesTracker.checkAlternatives(alternatives)
                untrackedRoutesIds.addAll(result.untracked.map { it.id })
            }
        }
        assertEquals(
            listOf(
                "UOIW_1UUIDFfyICssWNnKB2o4cANnHc5pQ4WjsBOKW694GD7ZFwG5Q==#1", // this one feels like similar to initial alternative because they both go through A9
                "EX782LX4SasgliEDWdrBLajhSdfTR4DzqjPvoQf-GJOqXJEijULtgw==#1",
                "qsbHcSTKmGlcgMc9w4wrj2Uz_IZhbVuuhHqxuU_4e51RXsroy1proA==#1",
                "Rs8ocnvnO9AY584Sd1GLWcSeV6ENJ34phLpjgTS_R2MZBNRUiWiwQg==#1",
                "2fRI3oZgP9QIffbtczCQl-FsWWdgLirxzAQL_4x8WtoB05ATMs2obA==#1",
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