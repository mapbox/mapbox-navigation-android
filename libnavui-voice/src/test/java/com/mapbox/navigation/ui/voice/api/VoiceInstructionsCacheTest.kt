package com.mapbox.navigation.ui.voice.api

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.TypeAndAnnouncement
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceInstructionsCacheTest {

    private val cache = VoiceInstructionsCache()
    private val key = TypeAndAnnouncement("ssml", "turn right")

    @Test
    fun `no value`() {
        assertNull(cache.get(key))
        assertEquals(0, cache.getEntries().size)
    }

    @Test
    fun `put new value`() {
        val key2 = TypeAndAnnouncement("ssml", "turn left")
        val value = ExpectedFactory.createValue<SpeechError, SpeechValue>(mockk())
        val value2 = ExpectedFactory.createError<SpeechError, SpeechValue>(mockk())

        cache.put(key, value)
        cache.put(key2, value2)

        assertEquals(value, cache.get(key))
        assertEquals(2, cache.getEntries().size)
        assertEquals(value, cache.getEntries()[key])
        assertEquals(value2, cache.getEntries()[key2])
    }

    @Test
    fun `put rewrites existing value`() {
        val value = ExpectedFactory.createValue<SpeechError, SpeechValue>(mockk())
        val value2 = ExpectedFactory.createError<SpeechError, SpeechValue>(mockk())

        cache.put(key, value)
        cache.put(key, value2)

        assertEquals(value2, cache.get(key))
        assertEquals(1, cache.getEntries().size)
        assertEquals(value2, cache.getEntries()[key])
    }

    @Test
    fun `remove non-existent value`() {
        cache.remove(key)

        assertNull(cache.get(key))
        assertEquals(0, cache.getEntries().size)
    }

    @Test
    fun `remove existent value`() {
        val key2 = TypeAndAnnouncement("ssml", "turn left")
        val value = ExpectedFactory.createValue<SpeechError, SpeechValue>(mockk())
        val value2 = ExpectedFactory.createError<SpeechError, SpeechValue>(mockk())
        cache.put(key, value)
        cache.put(key2, value2)

        cache.remove(key)

        assertNull(cache.get(key))
        assertEquals(value2, cache.get(key2))
        assertEquals(1, cache.getEntries().size)
        assertEquals(value2, cache.getEntries()[key2])
    }

    @Test
    fun `registerOneShotObserver has value`() {
        val key2 = TypeAndAnnouncement("ssml", "turn left")
        val observer1 = mockk<(Expected<SpeechError, SpeechValue>) -> Unit>(relaxed = true)
        val observer2 = mockk<(Expected<SpeechError, SpeechValue>) -> Unit>(relaxed = true)
        val value = ExpectedFactory.createValue<SpeechError, SpeechValue>(mockk())
        val value2 = ExpectedFactory.createError<SpeechError, SpeechValue>(mockk())
        cache.put(key, value)

        cache.registerOneShotObserver(key, observer1)
        cache.registerOneShotObserver(key2, observer2)

        verify(exactly = 1) { observer1(value) }
        verify(exactly = 0) { observer2(value) }
        clearAllMocks(answers = false)

        cache.put(key, value2)
        verify(exactly = 0) { observer1(any()) }
    }

    @Test
    fun `registerOneShotObserver no value`() {
        val key2 = TypeAndAnnouncement("ssml", "turn left")
        val observer1 = mockk<(Expected<SpeechError, SpeechValue>) -> Unit>(relaxed = true)
        val observer2 = mockk<(Expected<SpeechError, SpeechValue>) -> Unit>(relaxed = true)
        val value = ExpectedFactory.createValue<SpeechError, SpeechValue>(mockk())
        val value2 = ExpectedFactory.createError<SpeechError, SpeechValue>(mockk())

        cache.registerOneShotObserver(key, observer1)
        cache.registerOneShotObserver(key2, observer2)

        verify(exactly = 0) {
            observer1(any())
            observer2(any())
        }

        cache.put(key2, value2)

        verify(exactly = 0) { observer1(any()) }
        verify(exactly = 1) { observer2(any()) }
        clearAllMocks(answers = false)

        cache.put(key, value)

        verify(exactly = 1) { observer1(value) }
        verify(exactly = 0) { observer2(value) }
        clearAllMocks(answers = false)

        cache.put(key, value2)
        cache.put(key2, value)
        verify(exactly = 0) { observer1(any()) }
        verify(exactly = 0) { observer2(any()) }
    }

    @Test
    fun `unregisterOneShotObserver no observer`() {
        val observer = mockk<(Expected<SpeechError, SpeechValue>) -> Unit>(relaxed = true)

        cache.unregisterOneShotObserver(key, observer)
    }

    @Test
    fun `unregisterOneShotObserver has observer`() {
        val observer1 = mockk<(Expected<SpeechError, SpeechValue>) -> Unit>(relaxed = true)
        val observer2 = mockk<(Expected<SpeechError, SpeechValue>) -> Unit>(relaxed = true)
        val value = ExpectedFactory.createValue<SpeechError, SpeechValue>(mockk())

        cache.registerOneShotObserver(key, observer1)
        cache.registerOneShotObserver(key, observer2)
        cache.unregisterOneShotObserver(key, observer1)

        cache.put(key, value)

        verify(exactly = 0) { observer1(value) }
        verify(exactly = 1) { observer2(value) }
    }
}
