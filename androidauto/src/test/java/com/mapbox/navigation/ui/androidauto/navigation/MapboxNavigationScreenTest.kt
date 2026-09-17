@file:OptIn(com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.TravelEstimate
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.navigation.ui.androidauto.testing.TestOnDoneCallback
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MapboxNavigationScreenTest : MapboxRobolectricTestRunner() {

    @Test
    fun `free drive uses navigation template without content card`() {
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Search")
                    .setOnClickListener {}
                    .build(),
            )
            .build()
        val mapActionStrip = ActionStrip.Builder()
            .addAction(Action.PAN)
            .build()

        val template = createFreeDriveTemplate(
            actionStrip = actionStrip,
            mapActionStrip = mapActionStrip,
        )

        assertNull(template.navigationInfo)
        assertNull(template.destinationTravelEstimate)
        assertSame(actionStrip, template.actionStrip)
        assertSame(mapActionStrip, template.mapActionStrip)
    }

    @Test
    fun `active guidance uses native navigation and travel estimate card`() {
        val navigationInfo = mockk<NavigationTemplate.NavigationInfo>()
        val travelEstimate = mockk<TravelEstimate> {
            every { remainingTimeSeconds } returns 60
        }
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle("Stop")
                    .setOnClickListener {}
                    .build(),
            )
            .build()
        val mapActionStrip = ActionStrip.Builder()
            .addAction(Action.PAN)
            .build()

        val template = createActiveGuidanceTemplate(
            navigationInfo = CarNavigationInfo(
                navigationInfo = navigationInfo,
                destinationTravelEstimate = travelEstimate,
            ),
            actionStrip = actionStrip,
            mapActionStrip = mapActionStrip,
        )

        assertSame(navigationInfo, template.navigationInfo)
        assertSame(travelEstimate, template.destinationTravelEstimate)
        assertSame(actionStrip, template.actionStrip)
        assertSame(mapActionStrip, template.mapActionStrip)
    }

    @Test
    fun `route preview is loading when zero results arrived`() {
        val routesPreview = mockk<RoutesPreview> {
            every { originalRoutesList } returns emptyList()
        }

        val template = createRoutePreviewTemplate(
            routesPreview = routesPreview,
            title = "Route preview",
            navigateActionTitle = "Navigate",
            formatDistance = { error("No route should be formatted") },
            onRouteSelected = {},
            onNavigate = {},
        )

        assertTrue(template.isLoading)
        assertNull(template.singleList)
        assertTrue(template.actions.isEmpty())
    }

    @Test
    fun `route preview navigate action is attached to each row`() {
        val navigationRoute = mockk<NavigationRoute> {
            every { directionsRoute } returns mockk {
                every { duration() } returns 60.0
                every { distance() } returns 1_000.0
                every { legs() } returns listOf(
                    mockk { every { summary() } returns "Test route" },
                )
            }
        }
        val routesPreview = mockk<RoutesPreview> {
            every { originalRoutesList } returns listOf(navigationRoute)
            every { primaryRouteIndex } returns 0
        }
        var navigateCalls = 0

        val template = createRoutePreviewTemplate(
            routesPreview = routesPreview,
            title = "Route preview",
            navigateActionTitle = "Navigate",
            formatDistance = { "1 km" },
            onRouteSelected = {},
            onNavigate = { navigateCalls++ },
        )

        assertTrue(template.actions.isEmpty())
        val routeRow = template.singleList!!.items.single() as Row
        val navigateAction = routeRow.actions.single()
        assertEquals("Navigate", navigateAction.title?.toString())

        val callback = TestOnDoneCallback()
        navigateAction.onClickDelegate!!.sendClick(callback)
        callback.assertSuccess()
        assertEquals(1, navigateCalls)
    }
}
