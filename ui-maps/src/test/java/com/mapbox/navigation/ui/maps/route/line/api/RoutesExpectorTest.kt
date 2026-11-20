package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.common.Cancelable
import com.mapbox.maps.EventTimeInterval
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.SourceDataLoaded
import com.mapbox.maps.SourceDataLoadedCallback
import com.mapbox.maps.SourceDataLoadedType
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RoutesExpectorTest {

    private val expector = RoutesExpector()
    private val map = mockk<MapboxMap>(relaxed = true)
    private val callback = mockk<DelayedRoutesRenderedCallback>(relaxed = true)
    private val callbackWrapper = RoutesRenderedCallbackWrapper(map, callback)
    private val sourceDataLoadedTask = mockk<Cancelable>(relaxed = true)

    @Before
    fun setUp() {
        every { map.subscribeSourceDataLoaded(any()) } returns sourceDataLoadedTask
    }

    @Test
    fun expectRoutes_everythingIsEmpty() {
        expector.expectRoutes(emptySet(), emptySet(), ExpectedRoutesToRenderData(), callbackWrapper)

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(emptySet(), emptySet(), emptySet(), emptySet()),
            )
        }
        verify(exactly = 0) { map.subscribeSourceDataLoaded(any()) }
    }

    @Test
    fun expectRoutes_expectedDataIsEmpty() {
        val renderedRoutesToNotify = setOf("id#0", "id#1")
        val clearedRoutesToNotify = setOf("id#0", "id#1")
        expector.expectRoutes(
            renderedRoutesToNotify,
            clearedRoutesToNotify,
            ExpectedRoutesToRenderData(),
            callbackWrapper,
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(
                    renderedRoutesToNotify,
                    emptySet(),
                    clearedRoutesToNotify,
                    emptySet(),
                ),
            )
        }
        verify(exactly = 0) { map.subscribeSourceDataLoaded(any()) }
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
            sourceDataLoadedTask.cancel()
        }

        val listener = captureMapCallback()
        listener.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.TILE, "2"),
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            sourceDataLoadedTask.cancel()
        }

        listener.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "1"),
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            sourceDataLoadedTask.cancel()
        }

        listener.run(
            sourceDataLoadedEventData("source2", SourceDataLoadedType.METADATA, "2"),
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            sourceDataLoadedTask.cancel()
        }

        listener.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "2"),
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(setOf("id#0"), emptySet(), emptySet(), emptySet()),
            )
            sourceDataLoadedTask.cancel()
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
            sourceDataLoadedTask.cancel()
        }

        val listener = captureMapCallback()
        listener.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.TILE, "2"),
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            sourceDataLoadedTask.cancel()
        }

        listener.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "1"),
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            sourceDataLoadedTask.cancel()
        }

        listener.run(
            sourceDataLoadedEventData("source2", SourceDataLoadedType.METADATA, "2"),
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            sourceDataLoadedTask.cancel()
        }

        listener.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "2"),
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(emptySet(), emptySet(), setOf("id#0"), emptySet()),
            )
            sourceDataLoadedTask.cancel()
        }
    }

    @Test
    fun expectRoutes_singleRenderedRoute_cancelled() {
        val routesToNotify = setOf("id#0")
        val expectedRoutesToRender = ExpectedRoutesToRenderData().apply {
            addRenderedRoute("source1", 2, "id#0")
        }
        expector.expectRoutes(routesToNotify, emptySet(), expectedRoutesToRender, callbackWrapper)

        val listener = captureMapCallback()
        listener.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "3"),
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(emptySet(), setOf("id#0"), emptySet(), emptySet()),
            )
            sourceDataLoadedTask.cancel()
        }
    }

    @Test
    fun expectRoutes_singleClearedRoute_cancelled() {
        val routesToNotify = setOf("id#0")
        val expectedRoutesToRender = ExpectedRoutesToRenderData().apply {
            addClearedRoute("source1", 2, "id#0")
        }
        expector.expectRoutes(emptySet(), routesToNotify, expectedRoutesToRender, callbackWrapper)

        val listener = captureMapCallback()
        listener.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "3"),
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(emptySet(), emptySet(), emptySet(), setOf("id#0")),
            )
            sourceDataLoadedTask.cancel()
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
            callbackWrapper,
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            sourceDataLoadedTask.cancel()
        }

        val listener = captureMapCallback()

        listener.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "1"),
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            sourceDataLoadedTask.cancel()
        }

        listener.run(
            sourceDataLoadedEventData("source2", SourceDataLoadedType.METADATA, "2"),
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            sourceDataLoadedTask.cancel()
        }

        listener.run(
            sourceDataLoadedEventData("source3", SourceDataLoadedType.METADATA, "1"),
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(
                    setOf("id#0", "id#1", "id#2", "id#3"),
                    emptySet(),
                    setOf("id#4", "id#5", "id#6", "id#7"),
                    emptySet(),
                ),
            )
            sourceDataLoadedTask.cancel()
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
            callbackWrapper,
        )

        val listener = captureMapCallback()
        listener.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "2"),
        )
        listener.run(
            sourceDataLoadedEventData("source2", SourceDataLoadedType.METADATA, "4"),
        )
        listener.run(
            sourceDataLoadedEventData("source3", SourceDataLoadedType.METADATA, "7"),
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(
                    setOf("id#0"),
                    setOf("id#1", "id#2"),
                    setOf("id#4"),
                    setOf("id#5", "id#6"),
                ),
            )
            sourceDataLoadedTask.cancel()
        }
    }

    @Test
    fun expectRoutes_multipleTimes() {
        val callback2 = mockk<DelayedRoutesRenderedCallback>(relaxed = true)
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
            callbackWrapper,
        )
        val listener1 = captureMapCallback()
        clearAllMocks(answers = false)

        listener1.run(
            sourceDataLoadedEventData("source3", SourceDataLoadedType.METADATA, "3"),
        )

        val sourceDataLoadedTask2 = mockk<Cancelable>(relaxed = true)
        every { map.subscribeSourceDataLoaded(any()) } returns sourceDataLoadedTask2
        expector.expectRoutes(
            renderedRoutesToNotify2,
            clearedRoutesToNotify2,
            expectedRoutesToRender2,
            callbackWrapper2,
        )
        val listener2 = captureMapCallback()

        listener1.run(
            sourceDataLoadedEventData("source2", SourceDataLoadedType.METADATA, "4"),
        )
        listener2.run(
            sourceDataLoadedEventData("source2", SourceDataLoadedType.METADATA, "4"),
        )
        listener1.run(
            sourceDataLoadedEventData("source3", SourceDataLoadedType.METADATA, "4"),
        )
        listener2.run(
            sourceDataLoadedEventData("source3", SourceDataLoadedType.METADATA, "4"),
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            callback2.onRoutesRendered(any())
        }

        listener1.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "3"),
        )
        listener2.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "3"),
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(
                    emptySet(),
                    setOf("id#0", "id#1", "id#2"),
                    emptySet(),
                    setOf("id#4", "id#5", "id#6"),
                ),
            )
            callback2.onRoutesRendered(
                RoutesRenderedResult(
                    setOf("id#0", "id#5", "id#7"),
                    emptySet(),
                    setOf("id#1", "id#8", "id#6"),
                    emptySet(),
                ),
            )
            sourceDataLoadedTask.cancel()
            sourceDataLoadedTask2.cancel()
        }
        clearAllMocks(answers = false)

        listener1.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "3"),
        )
        listener2.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "3"),
        )
        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            callback2.onRoutesRendered(any())
        }
    }

    @Test
    fun expectRoutes_multipleTimes_differentSources() {
        val callback2 = mockk<DelayedRoutesRenderedCallback>(relaxed = true)
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
            callbackWrapper,
        )
        val listener1 = captureMapCallback()
        clearAllMocks(answers = false)

        listener1.run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "3"),
        )
        listener1.run(
            sourceDataLoadedEventData("source2", SourceDataLoadedType.METADATA, "3"),
        )

        val sourceDataLoadedTask2 = mockk<Cancelable>(relaxed = true)
        every { map.subscribeSourceDataLoaded(any()) } returns sourceDataLoadedTask2
        expector.expectRoutes(
            renderedRoutesToNotify2,
            clearedRoutesToNotify2,
            expectedRoutesToRender2,
            callbackWrapper2,
        )
        val listener2 = captureMapCallback()

        listener1.run(
            sourceDataLoadedEventData("source2", SourceDataLoadedType.METADATA, "4"),
        )
        listener2.run(
            sourceDataLoadedEventData("source2", SourceDataLoadedType.METADATA, "4"),
        )
        listener1.run(
            sourceDataLoadedEventData("source4", SourceDataLoadedType.METADATA, "2"),
        )
        listener2.run(
            sourceDataLoadedEventData("source4", SourceDataLoadedType.METADATA, "2"),
        )

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            callback2.onRoutesRendered(any())
        }

        listener1.run(
            sourceDataLoadedEventData("source3", SourceDataLoadedType.METADATA, "3"),
        )
        listener2.run(
            sourceDataLoadedEventData("source3", SourceDataLoadedType.METADATA, "3"),
        )
        listener1.run(
            sourceDataLoadedEventData("source6", SourceDataLoadedType.METADATA, "2"),
        )
        listener2.run(
            sourceDataLoadedEventData("source6", SourceDataLoadedType.METADATA, "2"),
        )

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(
                    setOf("id#2"),
                    setOf("id#0", "id#1"),
                    setOf("id#5"),
                    setOf("id#3", "id#4"),
                ),
            )
            callback2.onRoutesRendered(
                RoutesRenderedResult(
                    setOf("id#3", "id#7"),
                    setOf("id#0"),
                    setOf("id#5", "id#8"),
                    setOf("id#1"),
                ),
            )
            sourceDataLoadedTask.cancel()
            sourceDataLoadedTask2.cancel()
        }
        clearAllMocks(answers = false)

        listener1.run(
            sourceDataLoadedEventData("source3", SourceDataLoadedType.METADATA, "3"),
        )
        listener2.run(
            sourceDataLoadedEventData("source3", SourceDataLoadedType.METADATA, "3"),
        )
        listener1.run(
            sourceDataLoadedEventData("source6", SourceDataLoadedType.METADATA, "2"),
        )
        listener2.run(
            sourceDataLoadedEventData("source6", SourceDataLoadedType.METADATA, "2"),
        )
        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            callback2.onRoutesRendered(any())
        }
    }

    @Test
    fun cancelAll_nothingPending_doesNothing() {
        expector.cancelAll()

        verify(exactly = 0) {
            callback.onRoutesRendered(any())
            sourceDataLoadedTask.cancel()
        }
    }

    @Test
    fun cancelAll_pendingRoutes_cancelsSubscriptionAndReportsThemCancelled() {
        val expectedRoutesToRender = ExpectedRoutesToRenderData().apply {
            addRenderedRoute("source1", 2, "id#0")
            addClearedRoute("source2", 3, "id#1")
        }
        expector.expectRoutes(
            setOf("id#0"),
            setOf("id#1"),
            expectedRoutesToRender,
            callbackWrapper,
        )

        expector.cancelAll()

        verify(exactly = 1) {
            sourceDataLoadedTask.cancel()
            callback.onRoutesRendered(
                RoutesRenderedResult(emptySet(), setOf("id#0"), emptySet(), setOf("id#1")),
            )
        }
    }

    @Test
    fun cancelAll_partiallyRendered_keepsAlreadyRenderedRoutesSuccessful() {
        val expectedRoutesToRender = ExpectedRoutesToRenderData().apply {
            addRenderedRoute("source1", 2, "id#0")
            addRenderedRoute("source2", 3, "id#1")
        }
        expector.expectRoutes(
            setOf("id#0", "id#1"),
            emptySet(),
            expectedRoutesToRender,
            callbackWrapper,
        )
        captureMapCallback().run(
            sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "2"),
        )

        expector.cancelAll()

        verify(exactly = 1) {
            sourceDataLoadedTask.cancel()
            callback.onRoutesRendered(
                RoutesRenderedResult(setOf("id#0"), setOf("id#1"), emptySet(), emptySet()),
            )
        }
    }

    @Test
    fun cancelAll_multipleCallbacks_notifiesEachOnce() {
        val callback2 = mockk<DelayedRoutesRenderedCallback>(relaxed = true)
        expector.expectRoutes(
            setOf("id#0"),
            emptySet(),
            ExpectedRoutesToRenderData().apply { addRenderedRoute("source1", 2, "id#0") },
            callbackWrapper,
        )
        expector.expectRoutes(
            setOf("id#1"),
            emptySet(),
            ExpectedRoutesToRenderData().apply {
                addRenderedRoute("source2", 3, "id#1")
                addRenderedRoute("source3", 4, "id#1")
            },
            RoutesRenderedCallbackWrapper(map, callback2),
        )

        expector.cancelAll()

        verify(exactly = 2) { sourceDataLoadedTask.cancel() }
        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(emptySet(), setOf("id#0"), emptySet(), emptySet()),
            )
            callback2.onRoutesRendered(
                RoutesRenderedResult(emptySet(), setOf("id#1"), emptySet(), emptySet()),
            )
        }
    }

    @Test
    fun cancelAll_mapEventAfterCancel_doesNotNotifyAgain() {
        expector.expectRoutes(
            setOf("id#0"),
            emptySet(),
            ExpectedRoutesToRenderData().apply { addRenderedRoute("source1", 2, "id#0") },
            callbackWrapper,
        )
        val listener = captureMapCallback()
        expector.cancelAll()

        listener.run(sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "2"))

        verify(exactly = 1) { callback.onRoutesRendered(any()) }
    }

    @Test
    fun cancelAll_fromCallbackWhileMapEventFinishesSeveralRenders_notifiesEachOnce() {
        val callback2 = mockk<DelayedRoutesRenderedCallback>(relaxed = true)
        every { callback.onRoutesRendered(any()) } answers { expector.cancelAll() }
        expector.expectRoutes(
            setOf("id#0"),
            emptySet(),
            ExpectedRoutesToRenderData().apply { addRenderedRoute("source1", 2, "id#0") },
            callbackWrapper,
        )
        val listener = captureMapCallback()
        expector.expectRoutes(
            setOf("id#1"),
            emptySet(),
            ExpectedRoutesToRenderData().apply { addRenderedRoute("source1", 3, "id#1") },
            RoutesRenderedCallbackWrapper(map, callback2),
        )

        listener.run(sourceDataLoadedEventData("source1", SourceDataLoadedType.METADATA, "4"))

        verify(exactly = 1) {
            callback.onRoutesRendered(
                RoutesRenderedResult(emptySet(), setOf("id#0"), emptySet(), emptySet()),
            )
        }
        verify(exactly = 1) {
            callback2.onRoutesRendered(
                RoutesRenderedResult(emptySet(), setOf("id#1"), emptySet(), emptySet()),
            )
        }
        verify(exactly = 1) { callback2.onRoutesRendered(any()) }
    }

    @Test
    fun cancelAll_callbackThrows_remainingCallbacksNotifiedAndSubscriptionsCancelled() {
        val callback2 = mockk<DelayedRoutesRenderedCallback>(relaxed = true)
        var cancelsBeforeFirstCallback = -1
        var cancelCount = 0
        every { sourceDataLoadedTask.cancel() } answers { cancelCount++ }
        every { callback.onRoutesRendered(any()) } answers {
            cancelsBeforeFirstCallback = cancelCount
            throw IllegalStateException("callback failure")
        }
        expector.expectRoutes(
            setOf("id#0"),
            emptySet(),
            ExpectedRoutesToRenderData().apply { addRenderedRoute("source1", 2, "id#0") },
            callbackWrapper,
        )
        expector.expectRoutes(
            setOf("id#1"),
            emptySet(),
            ExpectedRoutesToRenderData().apply { addRenderedRoute("source2", 3, "id#1") },
            RoutesRenderedCallbackWrapper(map, callback2),
        )

        val error = runCatching { expector.cancelAll() }.exceptionOrNull()

        assertEquals("callback failure", error?.message)
        assertEquals(2, cancelsBeforeFirstCallback)
        verify(exactly = 1) {
            callback2.onRoutesRendered(
                RoutesRenderedResult(emptySet(), setOf("id#1"), emptySet(), emptySet()),
            )
        }
    }

    private fun captureMapCallback(): SourceDataLoadedCallback {
        val slot = slot<SourceDataLoadedCallback>()
        verify(exactly = 1) { map.subscribeSourceDataLoaded(capture(slot)) }
        return slot.captured
    }

    private fun sourceDataLoadedEventData(
        sourceId: String,
        type: SourceDataLoadedType,
        dataId: String?,
    ): SourceDataLoaded {
        return SourceDataLoaded(
            sourceId,
            type,
            null,
            null,
            dataId,
            EventTimeInterval(Date(), Date()),
        )
    }
}
