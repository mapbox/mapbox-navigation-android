package com.mapbox.navigation.ui.maps.route.callout.api

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.mapbox.bindgen.Value
import com.mapbox.common.Cancelable
import com.mapbox.maps.AnnotatedLayerFeature
import com.mapbox.maps.EventTimeInterval
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.SourceDataLoaded
import com.mapbox.maps.SourceDataLoadedCallback
import com.mapbox.maps.SourceDataLoadedType
import com.mapbox.maps.Style
import com.mapbox.maps.StylePropertyValue
import com.mapbox.maps.StylePropertyValueKind
import com.mapbox.maps.ViewAnnotationAnchorConfig
import com.mapbox.maps.viewannotation.OnViewAnnotationUpdatedListener
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.internal.route.callout.api.MapboxRouteCalloutsView
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.callout.model.CalloutViewHolder
import com.mapbox.navigation.ui.maps.route.callout.model.RouteCallout
import io.mockk.Called
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@RunWith(RobolectricTestRunner::class)
class MapboxRouteCalloutViewTest {

    @get:Rule
    val loggingRule = LoggingFrontendTestRule()

    private lateinit var mockContext: Context
    private lateinit var mockViewAnnotationManager: ViewAnnotationManager
    private lateinit var viewsSlot: MutableList<View>
    private lateinit var removedViewsSlot: MutableList<View>
    private lateinit var mockMap: MapboxMap

    @Before
    fun setUp() {
        viewsSlot = mutableListOf()
        removedViewsSlot = mutableListOf()
        mockContext = ApplicationProvider.getApplicationContext()
        mockViewAnnotationManager = mockk {
            every { addOnViewAnnotationUpdatedListener(any()) } just runs
            every { removeOnViewAnnotationUpdatedListener(any()) } just runs
            every { getViewAnnotationOptions(any<View>()) } returns null
            every { getViewAnnotation(any<AnnotatedLayerFeature>()) } returns null
            every { addViewAnnotation(capture(viewsSlot), any()) } just Runs
            every { removeViewAnnotation(capture(removedViewsSlot)) } returns true
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
        )
        excludeRecords { mockViewAnnotationManager.getViewAnnotation(any<AnnotatedLayerFeature>()) }

        val routeCalloutData = RouteCalloutUiStateData(emptyList())
        calloutView.renderCallouts(routeCalloutData)

        verify { mockViewAnnotationManager wasNot Called }

        confirmVerified(mockViewAnnotationManager)
    }

    @Test
    fun `providing a map subscribes to source data loaded`() {
        val map = mockk<MapboxMap>(relaxed = true)
        every { map.subscribeSourceDataLoaded(any()) } returns mockk<Cancelable>(relaxed = true)

        MapboxRouteCalloutsView(mockViewAnnotationManager, mockAdapter(), map)

        verify(exactly = 1) { map.subscribeSourceDataLoaded(any()) }
    }

    @Test
    fun `route source data loaded re-attaches callouts awaiting placement without style access`() {
        val (calloutView, callbackSlot) = calloutViewWithMap()

        calloutView.renderCallouts(singleCalloutData())
        verify(exactly = 1) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }

        callbackSlot.captured.run(sourceDataLoaded(SourceDataLoadedType.METADATA))

        verify(exactly = 2) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }
        verify(exactly = 0) { mockMap.style }
    }

    @Test
    fun `tile source data loaded does not re-attach callouts`() {
        val (calloutView, callbackSlot) = calloutViewWithMap()

        calloutView.renderCallouts(singleCalloutData())

        callbackSlot.captured.run(sourceDataLoaded(SourceDataLoadedType.TILE))

        verify(exactly = 1) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }
    }

    @Test
    fun `no re-attach after a callout anchor has been placed`() {
        val listenerSlot = slot<OnViewAnnotationUpdatedListener>()
        every {
            mockViewAnnotationManager.addOnViewAnnotationUpdatedListener(capture(listenerSlot))
        } just runs
        val (calloutView, callbackSlot) = calloutViewWithMap()

        calloutView.renderCallouts(singleCalloutData())

        listenerSlot.captured.onViewAnnotationAnchorUpdated(
            viewsSlot.last(),
            mockk<ViewAnnotationAnchorConfig>(relaxed = true),
        )
        callbackSlot.captured.run(sourceDataLoaded(SourceDataLoadedType.METADATA))

        verify(exactly = 1) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }
    }

    @Test
    fun `only the callouts still awaiting placement are re-attached`() {
        val listenerSlot = slot<OnViewAnnotationUpdatedListener>()
        every {
            mockViewAnnotationManager.addOnViewAnnotationUpdatedListener(capture(listenerSlot))
        } just runs
        val (calloutView, callbackSlot) = calloutViewWithMap()

        calloutView.renderCallouts(twoCalloutsData())
        verify(exactly = 2) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }
        val placedView = viewsSlot[0]
        val pendingView = viewsSlot[1]

        // only the first callout gets an anchor, the second one is still unplaced
        listenerSlot.captured.onViewAnnotationAnchorUpdated(
            placedView,
            mockk<ViewAnnotationAnchorConfig>(relaxed = true),
        )
        callbackSlot.captured.run(
            sourceDataLoaded(SourceDataLoadedType.METADATA, sourceId = SOURCE_1),
        )
        callbackSlot.captured.run(
            sourceDataLoaded(SourceDataLoadedType.METADATA, sourceId = SOURCE_2),
        )

        // one more attach, and only the unplaced callout was torn down
        verify(exactly = 3) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }
        assertEquals(listOf(pendingView), removedViewsSlot)
    }

    @Test
    fun `a callout placed on a re-attach is not re-attached again`() {
        val listenerSlot = slot<OnViewAnnotationUpdatedListener>()
        every {
            mockViewAnnotationManager.addOnViewAnnotationUpdatedListener(capture(listenerSlot))
        } just runs
        val (calloutView, callbackSlot) = calloutViewWithMap()

        calloutView.renderCallouts(singleCalloutData())
        callbackSlot.captured.run(sourceDataLoaded(SourceDataLoadedType.METADATA, dataId = "1"))
        verify(exactly = 2) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }

        // the view created by the re-attach gets an anchor
        listenerSlot.captured.onViewAnnotationAnchorUpdated(
            viewsSlot.last(),
            mockk<ViewAnnotationAnchorConfig>(relaxed = true),
        )
        callbackSlot.captured.run(sourceDataLoaded(SourceDataLoadedType.METADATA, dataId = "2"))

        verify(exactly = 2) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }
    }

    @Test
    fun `every new version of the source data re-attaches pending callouts`() {
        val (calloutView, callbackSlot) = calloutViewWithMap()

        calloutView.renderCallouts(singleCalloutData())

        // the map emits a monotonically increasing dataId per source: each event is a new
        // version of the data, and every version is a new chance to be placed
        callbackSlot.captured.run(sourceDataLoaded(SourceDataLoadedType.METADATA, dataId = "1"))
        callbackSlot.captured.run(sourceDataLoaded(SourceDataLoadedType.METADATA, dataId = "2"))

        verify(exactly = 3) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }
    }

    @Test
    fun `updates of unrelated sources are ignored`() {
        val (calloutView, callbackSlot) = calloutViewWithMap()

        calloutView.renderCallouts(singleCalloutData())
        repeat(5) {
            callbackSlot.captured.run(
                sourceDataLoaded(SourceDataLoadedType.METADATA, sourceId = OTHER_SOURCE),
            )
        }
        verify(exactly = 1) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }

        // the route source finally loads, and the callout is still healed
        callbackSlot.captured.run(sourceDataLoaded(SourceDataLoadedType.METADATA))
        verify(exactly = 2) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }
    }

    @Test
    fun `a callout on a layer with an unknown source is not re-attached`() {
        val (calloutView, callbackSlot) = calloutViewWithMap()

        // not a route line layer, and the style cannot resolve it either (style is null)
        calloutView.renderCallouts(singleCalloutData(layerId = CUSTOM_LAYER))

        callbackSlot.captured.run(sourceDataLoaded(SourceDataLoadedType.METADATA))
        callbackSlot.captured.run(
            sourceDataLoaded(SourceDataLoadedType.METADATA, sourceId = CUSTOM_SOURCE),
        )

        verify(exactly = 1) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }
    }

    @Test
    fun `the source of a layer outside the route line is resolved from the style`() {
        val (calloutView, callbackSlot) = calloutViewWithMap()
        givenStyleResolvesLayerSourcesTo(CUSTOM_SOURCE)

        calloutView.renderCallouts(singleCalloutData(layerId = CUSTOM_LAYER))

        callbackSlot.captured.run(sourceDataLoaded(SourceDataLoadedType.METADATA))
        verify(exactly = 1) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }

        callbackSlot.captured.run(
            sourceDataLoaded(SourceDataLoadedType.METADATA, sourceId = CUSTOM_SOURCE),
        )
        verify(exactly = 2) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }
    }

    @Test
    fun `an anchor update for a foreign view does not stop the re-attach`() {
        val listenerSlot = slot<OnViewAnnotationUpdatedListener>()
        every {
            mockViewAnnotationManager.addOnViewAnnotationUpdatedListener(capture(listenerSlot))
        } just runs
        val (calloutView, callbackSlot) = calloutViewWithMap()

        calloutView.renderCallouts(singleCalloutData())

        // a view annotation the app attached to the same manager, not one of our callouts
        listenerSlot.captured.onViewAnnotationAnchorUpdated(
            mockk(relaxed = true),
            mockk<ViewAnnotationAnchorConfig>(relaxed = true),
        )
        callbackSlot.captured.run(sourceDataLoaded(SourceDataLoadedType.METADATA))

        verify(exactly = 2) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }
    }

    @Test
    fun `release cancels the source data loaded subscription`() {
        val cancelable = mockk<Cancelable>(relaxed = true)
        val map = mockk<MapboxMap>(relaxed = true)
        every { map.subscribeSourceDataLoaded(any()) } returns cancelable
        val calloutView = MapboxRouteCalloutsView(mockViewAnnotationManager, mockAdapter(), map)

        calloutView.release()

        verify(exactly = 1) { cancelable.cancel() }
    }

    @Test
    fun `a source data loaded event delivered after release does not re-attach callouts`() {
        val (calloutView, callbackSlot) = calloutViewWithMap()

        calloutView.renderCallouts(singleCalloutData())
        calloutView.release()

        callbackSlot.captured.run(sourceDataLoaded(SourceDataLoadedType.METADATA))

        verify(exactly = 1) { mockViewAnnotationManager.addViewAnnotation(any<View>(), any()) }
    }

    private fun calloutViewWithMap():
        Pair<MapboxRouteCalloutsView, CapturingSlot<SourceDataLoadedCallback>> {
        val map = mockk<MapboxMap>(relaxed = true)
        mockMap = map
        // route line layers resolve their sources statically, any other layer needs the style
        every { map.style } returns null
        val callbackSlot = slot<SourceDataLoadedCallback>()
        every { map.subscribeSourceDataLoaded(capture(callbackSlot)) } returns
            mockk<Cancelable>(relaxed = true)
        val calloutView = MapboxRouteCalloutsView(mockViewAnnotationManager, mockAdapter(), map)
        return calloutView to callbackSlot
    }

    private fun givenStyleResolvesLayerSourcesTo(sourceId: String) {
        val style = mockk<Style>(relaxed = true) {
            every { getStyleLayerProperty(any(), "source") } returns StylePropertyValue(
                Value.valueOf(sourceId),
                StylePropertyValueKind.CONSTANT,
            )
        }
        every { mockMap.style } returns style
    }

    private fun mockAdapter(): MapboxRouteCalloutAdapter {
        // a distinct view per callout, so that an anchor update can be attributed to one of them
        return mockk(relaxed = true) {
            every { onCreateViewHolder(any()) } answers {
                CalloutViewHolder.Builder(mockk<View>(relaxed = true)).build()
            }
        }
    }

    private fun singleCalloutData(layerId: String = LAYER_1): RouteCalloutUiStateData {
        val uiState = RouteCalloutUiState(mockk<RouteCallout>(relaxed = true), layerId)
        return RouteCalloutUiStateData(listOf(uiState))
    }

    private fun twoCalloutsData(): RouteCalloutUiStateData {
        return RouteCalloutUiStateData(
            listOf(
                RouteCalloutUiState(mockk<RouteCallout>(relaxed = true), LAYER_1),
                RouteCalloutUiState(mockk<RouteCallout>(relaxed = true), LAYER_2),
            ),
        )
    }

    private fun sourceDataLoaded(
        type: SourceDataLoadedType,
        sourceId: String = SOURCE_1,
        dataId: String = "1",
    ): SourceDataLoaded =
        SourceDataLoaded(
            sourceId,
            type,
            null,
            null,
            dataId,
            EventTimeInterval(Date(), Date()),
        )

    private companion object {

        private val LAYER_1 = RouteLayerConstants.LAYER_GROUP_1_MAIN
        private val LAYER_2 = RouteLayerConstants.LAYER_GROUP_2_MAIN
        private val SOURCE_1 = RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
        private val SOURCE_2 = RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID
        private const val CUSTOM_LAYER = "custom-layer"
        private const val CUSTOM_SOURCE = "custom-source"
        private const val OTHER_SOURCE = "some-other-source"
    }
}
