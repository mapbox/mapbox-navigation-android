package com.mapbox.navigation.ui.maps.route.callout.api

import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.MapView
import com.mapbox.maps.ViewAnnotationOptions
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.testing.factories.createDirectionsRoute
import com.mapbox.navigation.testing.factories.createNavigationRoute
import com.mapbox.navigation.ui.maps.internal.route.callout.model.RouteCallout
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCalloutData
import io.mockk.Called
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class MapboxRouteCalloutViewTest {

    private lateinit var mockContext: Context
    private lateinit var mapView: MapView
    private lateinit var mockViewAnnotationManager: ViewAnnotationManager
    private lateinit var viewsSlot: MutableList<View>

    @Before
    fun setUp() {
        viewsSlot = mutableListOf()
        mockContext = ApplicationProvider.getApplicationContext()
        mockViewAnnotationManager = mockk {
            every { addOnViewAnnotationUpdatedListener(any()) } just runs
            every { getViewAnnotationOptions(any<View>()) } returns null
            every { addViewAnnotation(capture(viewsSlot), any()) } just Runs
            every { removeViewAnnotation(capture(viewsSlot)) } returns true
        }
        excludeRecords { mockViewAnnotationManager.addOnViewAnnotationUpdatedListener(any()) }
        excludeRecords { mockViewAnnotationManager.getViewAnnotationOptions(any<View>()) }
        mapView = mockk {
            every { context } returns mockContext
            every { resources } returns mockk(relaxed = true) {
                every { displayMetrics } returns DisplayMetrics().apply { density = 1f }
            }
            every { viewAnnotationManager } returns mockViewAnnotationManager
            every { generateLayoutParams(any()) } returns FrameLayout.LayoutParams(10, 10)
        }
    }

    @Test
    fun `render empty callout should not call ViewAnnotationManager`() {
        val calloutView = MapboxRouteCalloutView(mapView)

        val routeCalloutData = RouteCalloutData(emptyList())
        calloutView.renderCallouts(routeCalloutData)

        verify { mockViewAnnotationManager wasNot Called }

        confirmVerified(mockViewAnnotationManager)
    }

    @Test
    fun `render eta callouts`() {
        val calloutView = MapboxRouteCalloutView(mapView)
        val routePrimary = createMockRoute(id = "primary")
        val routeAlternative = createMockRoute(id = "alternative")

        val routeCalloutData = RouteCalloutData(
            listOf(
                RouteCallout.Eta(routePrimary, mockk(), true),
                RouteCallout.Eta(routeAlternative, mockk(), false),
            ),
        )
        calloutView.renderCallouts(routeCalloutData)
        val viewPrimary = viewsSlot.first()
        val viewAlternative = viewsSlot.last()

        verify(exactly = 2) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }
        assertEquals(routePrimary.id, viewPrimary.tag)
        assertEquals(routeAlternative.id, viewAlternative.tag)
        assertTrue(viewPrimary.isSelected)
        assertFalse(viewAlternative.isSelected)

        confirmVerified(mockViewAnnotationManager)
    }

    @Test
    fun `remove one eta callout after rendering`() {
        val calloutView = MapboxRouteCalloutView(mapView)
        val routePrimary = createMockRoute(id = "primary")
        val routeAlternative = createMockRoute(id = "alternative")
        excludeRecords { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }

        val routeCalloutData = RouteCalloutData(
            listOf(
                RouteCallout.Eta(routePrimary, mockk(), true),
                RouteCallout.Eta(routeAlternative, mockk(), false),
            ),
        )
        calloutView.renderCallouts(routeCalloutData)

        val routeCalloutData2 = RouteCalloutData(
            listOf(
                RouteCallout.Eta(routeAlternative, mockk(), false),
            ),
        )
        every { mockViewAnnotationManager.getViewAnnotationOptions(any<View>()) } returns
            ViewAnnotationOptions.Builder().build()

        calloutView.renderCallouts(routeCalloutData2)

        verify(exactly = 1) { mockViewAnnotationManager.removeViewAnnotation(any()) }
        assertEquals(routePrimary.id, viewsSlot.last().tag)

        confirmVerified(mockViewAnnotationManager)
    }

    @Test
    fun `change eta callouts order`() {
        val calloutView = MapboxRouteCalloutView(mapView)
        val routePrimary = createMockRoute(id = "primary")
        val routeAlternative = createMockRoute(id = "alternative")

        val routeCalloutData = RouteCalloutData(
            listOf(
                RouteCallout.Eta(routePrimary, mockk(), true),
                RouteCallout.Eta(routeAlternative, mockk(), false),
            ),
        )
        calloutView.renderCallouts(routeCalloutData)

        val routeCalloutData2 = RouteCalloutData(
            listOf(
                RouteCallout.Eta(routeAlternative, mockk(), true),
                RouteCallout.Eta(routePrimary, mockk(), false),
            ),
        )
        every { mockViewAnnotationManager.getViewAnnotationOptions(any<View>()) } returns
            ViewAnnotationOptions.Builder().build()
        calloutView.renderCallouts(routeCalloutData2)

        verify(exactly = 2) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }

        confirmVerified(mockViewAnnotationManager)
    }

    private fun createMockRoute(id: String): NavigationRoute {
        return createNavigationRoute(
            createDirectionsRoute(
                requestUuid = id,
//                geometry = "_jajfAhauzgFqNoEh@}ChFyXr_@cpArAqIdAiKVoE}I_B",
            ),
        )
    }
}
