package com.mapbox.navigation.ui.maps.route.callout.api

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.mapbox.maps.AnnotatedLayerFeature
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.internal.route.callout.model.RouteCalloutData
import io.mockk.Called
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class MapboxRouteCalloutViewTest {

    private lateinit var mockContext: Context
    private lateinit var mockViewAnnotationManager: ViewAnnotationManager
    private lateinit var viewsSlot: MutableList<View>
    private val layerIdProvider = mockk<RouteLayerIdProvider>(relaxed = true)

    @Before
    fun setUp() {
        viewsSlot = mutableListOf()
        mockContext = ApplicationProvider.getApplicationContext()
        mockViewAnnotationManager = mockk {
            every { addOnViewAnnotationUpdatedListener(any()) } just runs
            every { getViewAnnotationOptions(any<View>()) } returns null
            every { getViewAnnotation(any<AnnotatedLayerFeature>()) } returns null
            every { addViewAnnotation(capture(viewsSlot), any()) } just Runs
            every { removeViewAnnotation(capture(viewsSlot)) } returns true
            every { viewAnnotationAvoidLayers } returns hashSetOf()
            every { viewAnnotationAvoidLayers = any() } just Runs
        }
        excludeRecords { mockViewAnnotationManager.addOnViewAnnotationUpdatedListener(any()) }
        excludeRecords { mockViewAnnotationManager.getViewAnnotationOptions(any<View>()) }
        excludeRecords { mockViewAnnotationManager.viewAnnotationAvoidLayers }
        excludeRecords { mockViewAnnotationManager.viewAnnotationAvoidLayers = any() }
    }

    @Test
    fun `render empty callout should not call ViewAnnotationManager`() {
        val defaultAdapter = DefaultRouteCalloutAdapter(mockContext)
        val calloutView = MapboxRouteCalloutsView(
            mockViewAnnotationManager,
            defaultAdapter,
            layerIdProvider,
        )
        excludeRecords { mockViewAnnotationManager.getViewAnnotation(any<AnnotatedLayerFeature>()) }

        val routeCalloutData = RouteCalloutData(emptyList())
        calloutView.renderCallouts(routeCalloutData)

        verify { mockViewAnnotationManager wasNot Called }

        confirmVerified(mockViewAnnotationManager)
    }
}
