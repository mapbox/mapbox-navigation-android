package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.observable.eventdata.SourceDataLoadedEventData
import com.mapbox.maps.extension.observable.model.SourceDataType
import com.mapbox.maps.plugin.delegates.listeners.OnSourceDataLoadedListener
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoutesExpectorTest {

    private val expector = RoutesExpector()
    private val map = mockk<MapboxMap>(relaxed = true)
    private val callback = mockk<RoutesRenderedCallback>(relaxed = true)
    private val callbackWrapper = RoutesRenderedCallbackWrapper(map, callback)

    @Test
    fun expectRoutes_everythingIsEmpty() {
        expector.expectRoutes(emptySet(), emptySet(), ExpectedRoutesToRenderData(), callbackWrapper)

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(emptySet(), emptySet(), emptySet(), emptySet())
            )
        }
        verify(exactly = 0) { map.addOnSourceDataLoadedListener(any()) }
    }

    @Test
    fun expectRoutes_expectedDataIsEmpty() {
        val renderedRoutesToNotify = setOf("id#0", "id#1")
        val clearedRoutesToNotify = setOf("id#0", "id#1")
        expector.expectRoutes(
            renderedRoutesToNotify,
            clearedRoutesToNotify,
            ExpectedRoutesToRenderData(),
            callbackWrapper
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(
                    renderedRoutesToNotify,
                    emptySet(),
                    clearedRoutesToNotify,
                    emptySet()
                )
            )
        }
        verify(exactly = 0) { map.addOnSourceDataLoadedListener(any()) }
    }

    @Test
    fun expectRoutes_singleRenderedRoute_success() {
        val routesToNotify = setOf("id#0")
        val expectedRoutesToRender = ExpectedRoutesToRenderData().apply {
            addRenderedRoute("source1", 2, "id#0")
        }
        expector.expectRoutes(routesToNotify, emptySet(), expectedRoutesToRender, callbackWrapper)

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            map.removeOnSourceDataLoadedListener(any())
        }

        val listener = captureMapListener()
        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.TILE, "2")
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            map.removeOnSourceDataLoadedListener(any())
        }

        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.METADATA, "1")
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            map.removeOnSourceDataLoadedListener(any())
        }

        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source2", SourceDataType.METADATA, "2")
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            map.removeOnSourceDataLoadedListener(any())
        }

        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.METADATA, "2")
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(setOf("id#0"), emptySet(), emptySet(), emptySet())
            )
            map.removeOnSourceDataLoadedListener(listener)
        }
    }

    @Test
    fun expectRoutes_singleClearedRoute_success() {
        val routesToNotify = setOf("id#0")
        val expectedRoutesToRender = ExpectedRoutesToRenderData().apply {
            addClearedRoute("source1", 2, "id#0")
        }
        expector.expectRoutes(emptySet(), routesToNotify, expectedRoutesToRender, callbackWrapper)

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            map.removeOnSourceDataLoadedListener(any())
        }

        val listener = captureMapListener()
        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.TILE, "2")
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            map.removeOnSourceDataLoadedListener(any())
        }

        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.METADATA, "1")
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            map.removeOnSourceDataLoadedListener(any())
        }

        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source2", SourceDataType.METADATA, "2")
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            map.removeOnSourceDataLoadedListener(any())
        }

        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.METADATA, "2")
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(emptySet(), emptySet(), setOf("id#0"), emptySet())
            )
            map.removeOnSourceDataLoadedListener(listener)
        }
    }

    @Test
    fun expectRoutes_singleRenderedRoute_cancelled() {
        val routesToNotify = setOf("id#0")
        val expectedRoutesToRender = ExpectedRoutesToRenderData().apply {
            addRenderedRoute("source1", 2, "id#0")
        }
        expector.expectRoutes(routesToNotify, emptySet(), expectedRoutesToRender, callbackWrapper)

        val listener = captureMapListener()
        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.METADATA, "3")
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(emptySet(), setOf("id#0"), emptySet(), emptySet())
            )
            map.removeOnSourceDataLoadedListener(listener)
        }
    }

    @Test
    fun expectRoutes_singleClearedRoute_cancelled() {
        val routesToNotify = setOf("id#0")
        val expectedRoutesToRender = ExpectedRoutesToRenderData().apply {
            addClearedRoute("source1", 2, "id#0")
        }
        expector.expectRoutes(emptySet(), routesToNotify, expectedRoutesToRender, callbackWrapper)

        val listener = captureMapListener()
        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.METADATA, "3")
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(emptySet(), emptySet(), emptySet(), setOf("id#0"))
            )
            map.removeOnSourceDataLoadedListener(listener)
        }
    }

    @Test
    fun expectRoutes_moreRoutesToNotifyThanToExpect() {
        val renderedRoutesToNotify = setOf("id#0", "id#1", "id#2", "id#3")
        val clearedRoutesToNotify = setOf("id#4", "id#5", "id#6", "id#7")
        val expectedRoutesToRender = ExpectedRoutesToRenderData().apply {
            addRenderedRoute("source1", 1, "id#0")
            addRenderedRoute("source2", 2, "id#2")
            addRenderedRoute("source3", 1, "id#3")
            addClearedRoute("source1", 1, "id#4")
            addClearedRoute("source2", 2, "id#5")
            addClearedRoute("source3", 1, "id#6")
        }
        expector.expectRoutes(
            renderedRoutesToNotify,
            clearedRoutesToNotify,
            expectedRoutesToRender,
            callbackWrapper
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            map.removeOnSourceDataLoadedListener(any())
        }

        val listener = captureMapListener()

        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.METADATA, "1")
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            map.removeOnSourceDataLoadedListener(any())
        }

        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source2", SourceDataType.METADATA, "2")
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            map.removeOnSourceDataLoadedListener(any())
        }

        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source3", SourceDataType.METADATA, "1")
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(
                    setOf("id#0", "id#1", "id#2", "id#3"),
                    emptySet(),
                    setOf("id#4", "id#5", "id#6", "id#7"),
                    emptySet()
                )
            )
            map.removeOnSourceDataLoadedListener(listener)
        }
    }

    @Test
    fun expectRoutes_multipleRoutes_partiallyCancelled() {
        val renderedRoutesToNotify = setOf("id#0", "id#1", "id#2")
        val clearedRoutesToNotify = setOf("id#4", "id#5", "id#6")
        val expectedRoutesToRender = ExpectedRoutesToRenderData().apply {
            addRenderedRoute("source1", 2, "id#0")
            addRenderedRoute("source2", 3, "id#1")
            addRenderedRoute("source3", 3, "id#2")
            addClearedRoute("source1", 2, "id#4")
            addClearedRoute("source2", 3, "id#5")
            addClearedRoute("source3", 3, "id#6")
        }
        expector.expectRoutes(
            renderedRoutesToNotify,
            clearedRoutesToNotify,
            expectedRoutesToRender,
            callbackWrapper
        )

        val listener = captureMapListener()
        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.METADATA, "2")
        )
        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source2", SourceDataType.METADATA, "4")
        )
        listener.onSourceDataLoaded(
            sourceDataLoadedEventData("source3", SourceDataType.METADATA, "7")
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(
                    setOf("id#0"),
                    setOf("id#1", "id#2"),
                    setOf("id#4"),
                    setOf("id#5", "id#6")
                )
            )
            map.removeOnSourceDataLoadedListener(listener)
        }
    }

    @Test
    fun expectRoutes_multipleTimes() {
        val callback2 = mockk<RoutesRenderedCallback>(relaxed = true)
        val callbackWrapper2 = RoutesRenderedCallbackWrapper(map, callback2)
        val renderedRoutesToNotify1 = setOf("id#0", "id#1", "id#2")
        val clearedRoutesToNotify1 = setOf("id#4", "id#5", "id#6")
        val expectedRoutesToRender1 = ExpectedRoutesToRenderData().apply {
            addRenderedRoute("source1", 2, "id#0")
            addRenderedRoute("source2", 3, "id#1")
            addRenderedRoute("source3", 3, "id#2")
            addClearedRoute("source1", 2, "id#4")
            addClearedRoute("source2", 3, "id#5")
            addClearedRoute("source3", 3, "id#6")
        }
        val renderedRoutesToNotify2 = setOf("id#0", "id#5", "id#7")
        val clearedRoutesToNotify2 = setOf("id#1", "id#8", "id#6")
        val expectedRoutesToRender2 = ExpectedRoutesToRenderData().apply {
            addRenderedRoute("source1", 3, "id#0")
            addRenderedRoute("source2", 4, "id#5")
            addRenderedRoute("source3", 4, "id#7")
            addClearedRoute("source1", 3, "id#1")
            addClearedRoute("source2", 4, "id#8")
            addClearedRoute("source3", 4, "id#6")
        }
        expector.expectRoutes(
            renderedRoutesToNotify1,
            clearedRoutesToNotify1,
            expectedRoutesToRender1,
            callbackWrapper
        )
        val listener1 = captureMapListener()
        clearAllMocks(answers = false)

        listener1.onSourceDataLoaded(
            sourceDataLoadedEventData("source3", SourceDataType.METADATA, "3")
        )

        expector.expectRoutes(
            renderedRoutesToNotify2,
            clearedRoutesToNotify2,
            expectedRoutesToRender2,
            callbackWrapper2
        )
        val listener2 = captureMapListener()

        listener1.onSourceDataLoaded(
            sourceDataLoadedEventData("source2", SourceDataType.METADATA, "4")
        )
        listener2.onSourceDataLoaded(
            sourceDataLoadedEventData("source2", SourceDataType.METADATA, "4")
        )
        listener1.onSourceDataLoaded(
            sourceDataLoadedEventData("source3", SourceDataType.METADATA, "4")
        )
        listener2.onSourceDataLoaded(
            sourceDataLoadedEventData("source3", SourceDataType.METADATA, "4")
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            callback2.onRoutesRendered(any())
        }

        listener1.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.METADATA, "3")
        )
        listener2.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.METADATA, "3")
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(
                    emptySet(),
                    setOf("id#0", "id#1", "id#2"),
                    emptySet(),
                    setOf("id#4", "id#5", "id#6")
                )
            )
            callback2.onRoutesRendered(
                RoutesRenderedResult(
                    setOf("id#0", "id#5", "id#7"),
                    emptySet(),
                    setOf("id#1", "id#8", "id#6"),
                    emptySet()
                )
            )
            map.removeOnSourceDataLoadedListener(listener1)
            map.removeOnSourceDataLoadedListener(listener2)
        }
        clearAllMocks(answers = false)

        listener1.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.METADATA, "3")
        )
        listener2.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.METADATA, "3")
        )
        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            callback2.onRoutesRendered(any())
        }
    }

    @Test
    fun expectRoutes_multipleTimes_differentSources() {
        val callback2 = mockk<RoutesRenderedCallback>(relaxed = true)
        val callbackWrapper2 = RoutesRenderedCallbackWrapper(map, callback2)
        val renderedRoutesToNotify1 = setOf("id#0", "id#1", "id#2")
        val clearedRoutesToNotify1 = setOf("id#3", "id#4", "id#5")
        val expectedRoutesToRender1 = ExpectedRoutesToRenderData().apply {
            addRenderedRoute("source1", 2, "id#0")
            addRenderedRoute("source2", 3, "id#1")
            addRenderedRoute("source3", 3, "id#2")
            addClearedRoute("source1", 2, "id#3")
            addClearedRoute("source2", 3, "id#4")
            addClearedRoute("source3", 3, "id#5")
        }
        val renderedRoutesToNotify2 = setOf("id#0", "id#3", "id#7")
        val clearedRoutesToNotify2 = setOf("id#1", "id#5", "id#8")
        val expectedRoutesToRender2 = ExpectedRoutesToRenderData().apply {
            addRenderedRoute("source4", 1, "id#0")
            addRenderedRoute("source2", 4, "id#3")
            addRenderedRoute("source6", 2, "id#7")
            addClearedRoute("source4", 1, "id#1")
            addClearedRoute("source2", 4, "id#5")
            addClearedRoute("source6", 2, "id#8")
        }
        expector.expectRoutes(
            renderedRoutesToNotify1,
            clearedRoutesToNotify1,
            expectedRoutesToRender1,
            callbackWrapper
        )
        val listener1 = captureMapListener()
        clearAllMocks(answers = false)

        listener1.onSourceDataLoaded(
            sourceDataLoadedEventData("source1", SourceDataType.METADATA, "3")
        )
        listener1.onSourceDataLoaded(
            sourceDataLoadedEventData("source2", SourceDataType.METADATA, "3")
        )

        expector.expectRoutes(
            renderedRoutesToNotify2,
            clearedRoutesToNotify2,
            expectedRoutesToRender2,
            callbackWrapper2
        )
        val listener2 = captureMapListener()

        listener1.onSourceDataLoaded(
            sourceDataLoadedEventData("source2", SourceDataType.METADATA, "4")
        )
        listener2.onSourceDataLoaded(
            sourceDataLoadedEventData("source2", SourceDataType.METADATA, "4")
        )
        listener1.onSourceDataLoaded(
            sourceDataLoadedEventData("source4", SourceDataType.METADATA, "2")
        )
        listener2.onSourceDataLoaded(
            sourceDataLoadedEventData("source4", SourceDataType.METADATA, "2")
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            callback2.onRoutesRendered(any())
        }

        listener1.onSourceDataLoaded(
            sourceDataLoadedEventData("source3", SourceDataType.METADATA, "3")
        )
        listener2.onSourceDataLoaded(
            sourceDataLoadedEventData("source3", SourceDataType.METADATA, "3")
        )
        listener1.onSourceDataLoaded(
            sourceDataLoadedEventData("source6", SourceDataType.METADATA, "2")
        )
        listener2.onSourceDataLoaded(
            sourceDataLoadedEventData("source6", SourceDataType.METADATA, "2")
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(
                    setOf("id#2"),
                    setOf("id#0", "id#1"),
                    setOf("id#5"),
                    setOf("id#3", "id#4")
                )
            )
            callback2.onRoutesRendered(
                RoutesRenderedResult(
                    setOf("id#3", "id#7"),
                    setOf("id#0"),
                    setOf("id#5", "id#8"),
                    setOf("id#1")
                )
            )
            map.removeOnSourceDataLoadedListener(listener1)
            map.removeOnSourceDataLoadedListener(listener2)
        }
        clearAllMocks(answers = false)

        listener1.onSourceDataLoaded(
            sourceDataLoadedEventData("source3", SourceDataType.METADATA, "3")
        )
        listener2.onSourceDataLoaded(
            sourceDataLoadedEventData("source3", SourceDataType.METADATA, "3")
        )
        listener1.onSourceDataLoaded(
            sourceDataLoadedEventData("source6", SourceDataType.METADATA, "2")
        )
        listener2.onSourceDataLoaded(
            sourceDataLoadedEventData("source6", SourceDataType.METADATA, "2")
        )
        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            callback2.onRoutesRendered(any())
        }
    }

    private fun captureMapListener(): OnSourceDataLoadedListener {
        val slot = slot<OnSourceDataLoadedListener>()
        verify(exactly = 1) { map.addOnSourceDataLoadedListener(capture(slot)) }
        return slot.captured
    }

    private fun sourceDataLoadedEventData(
        sourceId: String,
        type: SourceDataType,
        dataId: String?
    ): SourceDataLoadedEventData {
        return SourceDataLoadedEventData(0, 0, sourceId, type, null, null, dataId)
    }
}
