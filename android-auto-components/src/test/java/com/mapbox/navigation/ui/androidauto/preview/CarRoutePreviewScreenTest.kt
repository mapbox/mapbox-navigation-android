package com.mapbox.navigation.ui.androidauto.preview

import android.text.SpannableString
import androidx.activity.OnBackPressedDispatcher
import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.navigation.CarDistanceFormatter
import com.mapbox.navigation.ui.androidauto.search.PlaceRecord
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import com.mapbox.navigation.voice.api.MapboxAudioGuidance
import com.mapbox.navigation.voice.api.MapboxAudioGuidanceState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@Suppress("DEPRECATION")
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CarRoutePreviewScreenTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private var carApiLevel = 7
    private val carContext: CarContext = mockk(relaxed = true) {
        every { getString(any()) } returns "text"
        every { getCarService(ConstraintManager::class.java) } returns mockk {
            every {
                getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_ROUTE_LIST)
            } returns 100
        }
        every { onBackPressedDispatcher } returns OnBackPressedDispatcher()
        every { carAppApiLevel } answers { carApiLevel }
    }
    private val mapboxCarContext: MapboxCarContext = mockk(relaxed = true) {
        every { carContext } returns this@CarRoutePreviewScreenTest.carContext
        every { options.actionStripProvider.getActionStrip(any(), any()) } returns
            ActionStrip.Builder().addAction(Action.Builder().setTitle("action").build()).build()
    }

    @Before
    fun setUp() {
        mockkObject(MapboxAudioGuidance.Companion)
        every { MapboxAudioGuidance.getRegisteredInstance() } returns mockk(relaxed = true) {
            every { stateFlow() } returns MutableStateFlow(
                mockk<MapboxAudioGuidanceState> { every { isMuted } returns false },
            )
        }
        mockkStatic(CarDistanceFormatter::class)
        every { CarDistanceFormatter.formatDistance(any()) } returns SpannableString("1 km")
    }

    @After
    fun tearDown() {
        unmockkObject(MapboxAudioGuidance.Companion)
        unmockkStatic(CarDistanceFormatter::class)
    }

    @Test
    fun `header uses only the legacy setters below Car API 5`() {
        listOf(3, 4).forEach { level ->
            carApiLevel = level

            val template = template()

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

            val template = template()

            assertEquals("text", template.header!!.title.toString())
            assertEquals(Action.BACK, template.header!!.startHeaderAction)
            assertEquals("text", template.title.toString())
            assertEquals(Action.BACK, template.headerAction)
            assertRoutesAndActions(template)
        }
    }

    private fun template(): RoutePreviewNavigationTemplate {
        val screen = CarRoutePreviewScreen(mapboxCarContext, place(), routes("first", "second"))
        return screen.onGetTemplate() as RoutePreviewNavigationTemplate
    }

    private fun assertRoutesAndActions(template: RoutePreviewNavigationTemplate) {
        assertEquals(2, template.itemList!!.items.size)
        assertEquals("text", template.navigateAction!!.title.toString())
        assertEquals("action", template.actionStrip!!.actions.single().title.toString())
    }

    private fun place() = PlaceRecord("id", "Home", null, null, listOf())

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
}
