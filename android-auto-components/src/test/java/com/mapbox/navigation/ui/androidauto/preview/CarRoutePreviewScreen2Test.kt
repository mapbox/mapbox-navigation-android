package com.mapbox.navigation.ui.androidauto.preview

import android.text.SpannableString
import androidx.activity.OnBackPressedDispatcher
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.preview.RoutesPreview
import com.mapbox.navigation.core.preview.RoutesPreviewObserver
import com.mapbox.navigation.core.preview.RoutesPreviewUpdate
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.navigation.CarDistanceFormatter
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.navigation.ui.androidauto.testing.TestOnDoneCallback
import com.mapbox.navigation.voice.api.MapboxAudioGuidance
import com.mapbox.navigation.voice.api.MapboxAudioGuidanceState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@Suppress("DEPRECATION")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
class CarRoutePreviewScreen2Test : MapboxRobolectricTestRunner() {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private var routeLimit = 100
    private var carApiLevel = 6
    private val appManager: AppManager = mockk(relaxed = true)
    private val carContext: CarContext = mockk(relaxed = true) {
        every { getString(any()) } returns "text"
        every { getCarService(AppManager::class.java) } returns appManager
        every { getCarService(ConstraintManager::class.java) } returns mockk {
            every {
                getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST)
            } answers { routeLimit }
        }
        every { onBackPressedDispatcher } returns OnBackPressedDispatcher()
        every { carAppApiLevel } answers { carApiLevel }
    }
    private val mapboxCarContext: MapboxCarContext = mockk(relaxed = true) {
        every { carContext } returns this@CarRoutePreviewScreen2Test.carContext
        every { options.actionStripProvider.getActionStrip(any(), any()) } returns
            ActionStrip.Builder().addAction(Action.Builder().setTitle("action").build()).build()
    }
    private val routesObserver = slot<RoutesPreviewObserver>()
    private val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
        every { registerRoutesPreviewObserver(capture(routesObserver)) } just runs
    }

    @Before
    fun setUp() {
        mockkObject(MapboxAudioGuidance.Companion)
        every { MapboxAudioGuidance.getRegisteredInstance() } returns mockk(relaxed = true) {
            every { stateFlow() } returns MutableStateFlow(
                mockk<MapboxAudioGuidanceState> { every { isMuted } returns false },
            )
        }
        mockkObject(MapboxScreenManager.Companion)
        every { MapboxScreenManager.replaceTop(any()) } just runs
        mockkStatic(CarDistanceFormatter::class)
        every { CarDistanceFormatter.formatDistance(any()) } returns SpannableString("1 km")
        carAppTestRule.onAttached(mapboxNavigation)
        every { MapboxNavigationApp.current() } returns mapboxNavigation
    }

    @After
    fun tearDown() {
        unmockkObject(MapboxAudioGuidance.Companion, MapboxScreenManager.Companion)
        unmockkStatic(CarDistanceFormatter::class)
    }

    @Test
    fun `an equal routes preview does not invalidate the screen`() {
        resumedScreen()
        val preview = routesPreview(routes("first", "second"), primaryRouteIndex = 0)

        emit(preview)
        emit(preview)

        verify(exactly = 1) { appManager.invalidate() }
    }

    @Test
    fun `a new routes preview invalidates the screen`() {
        resumedScreen()

        emit(routesPreview(routes("first", "second"), primaryRouteIndex = 0))
        emit(routesPreview(routes("first", "second"), primaryRouteIndex = 1))

        verify(exactly = 2) { appManager.invalidate() }
    }

    @Test
    fun `route list keeps the primary route beyond the route list limit`() {
        routeLimit = 2
        val screen = resumedScreen()
        val routes = routes("first", "second", "third")
        val preview = routesPreview(routes, primaryRouteIndex = 2)
        every { mapboxNavigation.getRoutesPreview() } returns preview
        emit(preview)

        val template = screen.onGetTemplate() as RoutePreviewNavigationTemplate
        val itemList = template.itemList!!
        val callback = TestOnDoneCallback()
        template.navigateAction!!.onClickDelegate!!.sendClick(callback)

        callback.assertSuccess()
        assertEquals(2, itemList.items.size)
        assertEquals(1, itemList.selectedIndex)
        verify { mapboxNavigation.setNavigationRoutes(match { it.first() == routes[2] }) }
    }

    @Test
    fun `header uses only the legacy setters below Car API 5`() {
        listOf(3, 4).forEach { level ->
            carApiLevel = level

            val template = previewTemplateWithRoutes()

            assertNull(template.header)
            assertEquals("text", template.title.toString())
            assertEquals(Action.BACK, template.headerAction)
            assertRoutesAndActions(template)
        }
    }

    // The template is deprecated as a whole, so hosts may read its legacy fields on any level.
    @Test
    fun `header uses the header and the legacy setters from Car API 5`() {
        listOf(5, 6, 7, 8).forEach { level ->
            carApiLevel = level

            val template = previewTemplateWithRoutes()

            assertEquals("text", template.header!!.title.toString())
            assertEquals(Action.BACK, template.header!!.startHeaderAction)
            assertEquals("text", template.title.toString())
            assertEquals(Action.BACK, template.headerAction)
            assertRoutesAndActions(template)
        }
    }

    @Test
    fun `a loading preview keeps its header`() {
        carApiLevel = 7

        val template = resumedScreen().onGetTemplate() as RoutePreviewNavigationTemplate

        assertTrue(template.isLoading)
        assertEquals("text", template.header!!.title.toString())
        assertEquals(Action.BACK, template.headerAction)
    }

    private fun previewTemplateWithRoutes(): RoutePreviewNavigationTemplate {
        val screen = resumedScreen()
        val preview = routesPreview(routes("first", "second"), primaryRouteIndex = 0)
        every { mapboxNavigation.getRoutesPreview() } returns preview
        emit(preview)
        return screen.onGetTemplate() as RoutePreviewNavigationTemplate
    }

    private fun assertRoutesAndActions(template: RoutePreviewNavigationTemplate) {
        assertEquals(2, template.itemList!!.items.size)
        assertEquals("text", template.navigateAction!!.title.toString())
        assertEquals("action", template.actionStrip!!.actions.single().title.toString())
    }

    private fun resumedScreen(): CarRoutePreviewScreen2 {
        val screen = CarRoutePreviewScreen2(mapboxCarContext)
        (screen.lifecycle as LifecycleRegistry).currentState = Lifecycle.State.RESUMED
        return screen
    }

    private fun emit(preview: RoutesPreview) {
        routesObserver.captured.routesPreviewUpdated(
            mockk<RoutesPreviewUpdate> { every { routesPreview } returns preview },
        )
    }

    private fun routes(vararg ids: String): List<NavigationRoute> = ids.map { routeId ->
        mockk {
            every { id } returns routeId
            every { directionsRoute } returns mockk {
                every { duration() } returns 60.0
                every { distance() } returns 1_000.0
                every { legs() } returns emptyList()
            }
        }
    }

    private fun routesPreview(
        routes: List<NavigationRoute>,
        primaryRouteIndex: Int,
    ): RoutesPreview = mockk {
        every { originalRoutesList } returns routes
        every { routesList } returns
            listOf(routes[primaryRouteIndex]) + routes.filterIndexed { index, _ ->
                index != primaryRouteIndex
            }
        every { this@mockk.primaryRouteIndex } returns primaryRouteIndex
    }
}
