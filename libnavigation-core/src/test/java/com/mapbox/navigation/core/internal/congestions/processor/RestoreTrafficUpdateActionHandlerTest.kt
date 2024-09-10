package com.mapbox.navigation.core.internal.congestions.processor

import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.internal.route.overriddenTraffic
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction
import com.mapbox.navigation.testing.factories.createDirectionsResponse
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoutes
import com.mapbox.navigation.testing.factories.createRouteLeg
import com.mapbox.navigation.testing.factories.createRouteLegAnnotation
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class RestoreTrafficUpdateActionHandlerTest {
    private val actionHandler = RestoreTrafficUpdateActionHandler()

    @Test
    fun `handler should restore traffic original traffic`() {
        val originalCongestionNumeric = listOf(40, 40, 40, 40, 40, 40, 40, 40, 40)
        val expectedCongestionNumeric = listOf(40, 40, 40, 60, 60, 60, 40, 40, 40)
        val congestionOverride = CongestionNumericOverride(0, 3, 3, listOf(60, 60, 60))

        val action = createAction(originalCongestionNumeric, congestionOverride)

        val actualResult = actionHandler.handleAction(action)
        assertNull(actualResult.overriddenTraffic)
        assertEquals(expectedCongestionNumeric, actualResult.getCongestionNumeric())
    }

    private fun createAction(
        originalCongestionNumeric: List<Int>,
        congestionOverride: CongestionNumericOverride,
    ) = TrafficUpdateAction.RestoreTraffic(
        createTestRoute(originalCongestionNumeric),
        congestionOverride,
    )

    private fun createTestRoute(congestionNumeric: List<Int>) = createNavigationRoutes(
        createDirectionsResponse(
            routes = listOf(
                createDirectionsRoute(
                    legs = listOf(
                        createRouteLeg(
                            createRouteLegAnnotation(
                                congestionNumeric = congestionNumeric,
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ).first()

    private fun NavigationRoute.getCongestionNumeric() =
        directionsRoute.legs()?.first()?.annotation()?.congestionNumeric()
}
