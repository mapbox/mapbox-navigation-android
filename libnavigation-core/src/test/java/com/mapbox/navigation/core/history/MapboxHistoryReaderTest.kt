package com.mapbox.navigation.core.history

import com.mapbox.navigation.core.history.model.HistoryEventMapper
import com.mapbox.navigator.HistoryReaderInterface
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class MapboxHistoryReaderTest {

    private val historyEventMapper: HistoryEventMapper = mockk {
        every { map(any()) } returns mockk()
    }

    @Test
    fun `next is loaded when object is constructed`() {
        val nativeHistoryReader: HistoryReaderInterface = mockk {
            every { next() } returns mockk()
        }
        val sut = MapboxHistoryReader(nativeHistoryReader, historyEventMapper)

        val next = sut.next()

        assertNotNull(next)
    }

    @Test
    fun `hasNext is false when next returns null`() {
        val nativeHistoryReader: HistoryReaderInterface = mockk {
            every { next() } returns null
        }
        val sut = MapboxHistoryReader(nativeHistoryReader, historyEventMapper)

        val hasNext = sut.hasNext()

        assertFalse(hasNext)
    }

    @Test(expected = NullPointerException::class)
    fun `next is false when next returns null`() {
        val nativeHistoryReader: HistoryReaderInterface = mockk {
            every { next() } returns null
        }
        val sut = MapboxHistoryReader(nativeHistoryReader, historyEventMapper)

        // Should crash
        sut.next()
    }
}
