package com.mapbox.navigation.core.replay.history

import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.core.history.model.HistoryEvent
import com.mapbox.navigation.core.history.model.HistoryEventGetStatus
import com.mapbox.navigation.core.history.model.HistoryEventPushHistoryRecord
import com.mapbox.navigation.core.history.model.HistoryEventSetRoute
import com.mapbox.navigation.core.history.model.HistoryEventUpdateLocation
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayHistoryEventStreamTest {

    private val mapboxHistoryReader: MapboxHistoryReader = mockk()

    @Test
    fun `next will return mapped event when mapboxHistoryReader#hasNext is true`() {
        every { mapboxHistoryReader.hasNext() } returns true
        every { mapboxHistoryReader.next() } returns mockk()
        val sut = ReplayHistoryEventStream(mapboxHistoryReader, mockHistoryEventMapper())

        val next = sut.next()

        assertNotNull(next)
    }

    @Test
    fun `hasNext is false when mapboxHistoryReader#hasNext returns false`() {
        every { mapboxHistoryReader.hasNext() } returns false
        val sut = ReplayHistoryEventStream(mapboxHistoryReader, mockHistoryEventMapper())

        val hasNext = sut.hasNext()

        assertFalse(hasNext)
    }

    @Test(expected = NullPointerException::class)
    fun `next will crash when mapboxHistoryReader#hasNext is false`() {
        every { mapboxHistoryReader.hasNext() } returns false
        val sut = ReplayHistoryEventStream(mapboxHistoryReader, mockHistoryEventMapper())

        // Should crash
        sut.next()
    }

    @Test
    fun `will emit events in the same order as the mapboxHistoryReader`() {
        every { mapboxHistoryReader.hasNext() } returns true
        every { mapboxHistoryReader.next() } returnsMany listOf(
            mockk<HistoryEventGetStatus>(),
            mockk<HistoryEventUpdateLocation>(),
            mockk<HistoryEventSetRoute>(),
            mockk<HistoryEventPushHistoryRecord>(),
        )
        val sut = ReplayHistoryEventStream(mapboxHistoryReader, mockHistoryEventMapper())

        assertTrue(sut.next() is ReplayEventGetStatus)
        assertTrue(sut.next() is ReplayEventUpdateLocation)
        assertTrue(sut.next() is ReplaySetNavigationRoute)
        assertNotNull(sut.next())
    }

    @Test
    fun `customized mapper can be used to ignore or change events`() {
        class CustomPushHistoryEvent(override val eventTimestamp: Double) : ReplayEventBase
        val customizedMapper: ReplayHistoryMapper = mockk {
            every { mapToReplayEvent(any()) } answers {
                when (firstArg<HistoryEvent>()) {
                    is HistoryEventGetStatus -> null
                    is HistoryEventUpdateLocation -> mockk<ReplayEventUpdateLocation>()
                    is HistoryEventSetRoute -> null
                    else -> mockk<CustomPushHistoryEvent>()
                }
            }
        }
        every { mapboxHistoryReader.hasNext() } returns true
        every { mapboxHistoryReader.next() } returnsMany listOf(
            mockk<HistoryEventGetStatus>(),
            mockk<HistoryEventUpdateLocation>(),
            mockk<HistoryEventSetRoute>(),
            mockk<HistoryEventPushHistoryRecord>(),
        )
        val sut = ReplayHistoryEventStream(mapboxHistoryReader, customizedMapper)

        assertTrue(sut.next() is ReplayEventUpdateLocation)
        assertTrue(sut.next() is CustomPushHistoryEvent)
    }

    private fun mockHistoryEventMapper(): ReplayHistoryMapper = mockk {
        every { mapToReplayEvent(any()) } answers {
            when (firstArg<HistoryEvent>()) {
                is HistoryEventGetStatus -> mockk<ReplayEventGetStatus>()
                is HistoryEventUpdateLocation -> mockk<ReplayEventUpdateLocation>()
                is HistoryEventSetRoute -> mockk<ReplaySetNavigationRoute>()
                else -> mockk()
            }
        }
    }
}
