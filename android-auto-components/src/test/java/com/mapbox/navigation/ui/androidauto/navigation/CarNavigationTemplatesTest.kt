@file:OptIn(com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI::class)

package com.mapbox.navigation.ui.androidauto.navigation

import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.TravelEstimate
import com.mapbox.api.directions.v5.models.RouteLeg
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

class CarNavigationTemplatesTest : MapboxRobolectricTestRunner() {

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

        val template = CarNavigationTemplates.freeDrive(
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

        val template = CarNavigationTemplates.activeGuidance(
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

        val template = CarNavigationTemplates.routePreview(
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
            every { id } returns "route-0"
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
        val navigatedRouteIds = mutableListOf<String>()

        val template = CarNavigationTemplates.routePreview(
            routesPreview = routesPreview,
            title = "Route preview",
            navigateActionTitle = "Navigate",
            formatDistance = { "1 km" },
            onRouteSelected = {},
            onNavigate = { navigatedRouteIds.add(it) },
        )

        assertTrue(template.actions.isEmpty())
        val routeRow = template.singleList!!.items.single() as Row
        val navigateAction = routeRow.actions.single()
        assertEquals("Navigate", navigateAction.title?.toString())

        val callback = TestOnDoneCallback()
        navigateAction.onClickDelegate!!.sendClick(callback)
        callback.assertSuccess()
        assertEquals(listOf("route-0"), navigatedRouteIds)
    }

    @Test
    fun `route preview navigate action on each row starts that row's route`() {
        val routesPreview = mockk<RoutesPreview> {
            every { originalRoutesList } returns listOf(
                routeWithLegs("first", listOf(mockk { every { summary() } returns "First" })),
                routeWithLegs("second", listOf(mockk { every { summary() } returns "Second" })),
            )
            every { primaryRouteIndex } returns 0
        }
        val navigatedRouteIds = mutableListOf<String>()

        val template = CarNavigationTemplates.routePreview(
            routesPreview = routesPreview,
            title = "Route preview",
            navigateActionTitle = "Navigate",
            formatDistance = { "1 km" },
            onRouteSelected = {},
            onNavigate = { navigatedRouteIds.add(it) },
        )

        val rows = template.singleList!!.items.map { it as Row }
        listOf(1, 0).forEach { rowIndex ->
            val callback = TestOnDoneCallback()
            rows[rowIndex].actions.single().onClickDelegate!!.sendClick(callback)
            callback.assertSuccess()
        }
        assertEquals(listOf("second", "first"), navigatedRouteIds)
    }

    @Test
    fun `route preview selection reports the selected route id`() {
        val routesPreview = mockk<RoutesPreview> {
            every { originalRoutesList } returns listOf(
                routeWithLegs("first", emptyList()),
                routeWithLegs("second", emptyList()),
            )
            every { primaryRouteIndex } returns 0
        }
        val selectedRouteIds = mutableListOf<String>()

        val template = CarNavigationTemplates.routePreview(
            routesPreview = routesPreview,
            title = "Route preview",
            navigateActionTitle = "Navigate",
            formatDistance = { "1 km" },
            onRouteSelected = { selectedRouteIds.add(it) },
            onNavigate = {},
        )

        val callback = TestOnDoneCallback()
        template.singleList!!.onSelectedDelegate!!.sendSelected(1, callback)
        callback.assertSuccess()
        assertEquals(listOf("second"), selectedRouteIds)
    }

    @Test
    fun `route preview row is built for a route without legs`() {
        val routesPreview = mockk<RoutesPreview> {
            every { originalRoutesList } returns listOf(routeWithLegs("route", emptyList()))
            every { primaryRouteIndex } returns 0
        }

        val template = CarNavigationTemplates.routePreview(
            routesPreview = routesPreview,
            title = "Route preview",
            navigateActionTitle = "Navigate",
            formatDistance = { "1 km" },
            onRouteSelected = {},
            onNavigate = {},
        )

        assertEquals(1, template.singleList!!.items.size)
    }

    @Test
    fun `route preview selects the primary route`() {
        val routesPreview = mockk<RoutesPreview> {
            every { originalRoutesList } returns listOf(
                routeWithLegs("first", emptyList()),
                routeWithLegs("second", emptyList()),
            )
            every { primaryRouteIndex } returns 1
        }

        val template = CarNavigationTemplates.routePreview(
            routesPreview = routesPreview,
            title = "Route preview",
            navigateActionTitle = "Navigate",
            formatDistance = { "1 km" },
            onRouteSelected = {},
            onNavigate = {},
        )

        val itemList = template.singleList!!
        assertEquals(2, itemList.items.size)
        assertEquals(1, itemList.selectedIndex)
    }

    @Test
    fun `route preview shows no more routes than the route list limit`() {
        val routesPreview = mockk<RoutesPreview> {
            every { originalRoutesList } returns listOf(
                routeWithLegs("first", emptyList()),
                routeWithLegs("second", emptyList()),
                routeWithLegs("third", emptyList()),
            )
            every { primaryRouteIndex } returns 0
        }
        var navigatedRouteId: String? = null

        val template = CarNavigationTemplates.routePreview(
            routesPreview = routesPreview,
            title = "Route preview",
            navigateActionTitle = "Navigate",
            formatDistance = { "1 km" },
            onRouteSelected = {},
            onNavigate = { navigatedRouteId = it },
            navigateActionIcon = null,
            maxRoutes = 2,
        )

        val rows = template.singleList!!.items.map { it as Row }
        assertEquals(2, rows.size)
        val callback = TestOnDoneCallback()
        rows.last().actions.single().onClickDelegate!!.sendClick(callback)
        callback.assertSuccess()
        assertEquals("second", navigatedRouteId)
    }

    @Test
    fun `route preview keeps a primary route beyond the limit in the list`() {
        val routesPreview = mockk<RoutesPreview> {
            every { originalRoutesList } returns listOf(
                routeWithLegs("first", emptyList()),
                routeWithLegs("second", emptyList()),
                routeWithLegs("third", emptyList()),
            )
            every { primaryRouteIndex } returns 2
        }
        val navigatedRouteIds = mutableListOf<String>()

        val template = CarNavigationTemplates.routePreview(
            routesPreview = routesPreview,
            title = "Route preview",
            navigateActionTitle = "Navigate",
            formatDistance = { "1 km" },
            onRouteSelected = {},
            onNavigate = { navigatedRouteIds.add(it) },
            navigateActionIcon = null,
            maxRoutes = 2,
        )

        val itemList = template.singleList!!
        val rows = itemList.items.map { it as Row }
        rows.forEach { row ->
            val callback = TestOnDoneCallback()
            row.actions.single().onClickDelegate!!.sendClick(callback)
            callback.assertSuccess()
        }
        assertEquals(listOf("first", "third"), navigatedRouteIds)
        assertEquals(1, itemList.selectedIndex)
    }

    private fun routeWithLegs(
        routeId: String,
        routeLegs: List<RouteLeg>,
    ): NavigationRoute = mockk {
        every { id } returns routeId
        every { directionsRoute } returns mockk {
            every { duration() } returns 60.0
            every { distance() } returns 1_000.0
            every { legs() } returns routeLegs
        }
    }
}
