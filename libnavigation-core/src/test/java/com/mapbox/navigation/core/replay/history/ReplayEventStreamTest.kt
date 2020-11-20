package com.mapbox.navigation.core.replay.history

import com.google.gson.stream.JsonReader
import org.apache.commons.io.IOUtils
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.InputStreamReader

@RunWith(RobolectricTestRunner::class)
class ReplayEventStreamTest {

    @get:Rule
    val memoryTestRule = MemoryTestRule()

    private val historyFile = "history-events-file.json"

    @Test
    fun `read a couple elements from the stream`() {
        val historyEventStream = resourceAsHistoryEventStream(historyFile)

        val replayEvents = historyEventStream
            .asSequence()
            .filterIsInstance<ReplayEventUpdateLocation>()
            .take(10)
            .toList()

        println("memoryUsed: ${memoryTestRule.memoryUsedMB}")
        assertEquals(10, replayEvents.size)
    }

    @Test
    fun `read a couple elements from the string`() {
        val historyString = resourceAsString(historyFile)
        val replayHistoryMapper = ReplayHistoryMapper()

        val replayEvents = replayHistoryMapper.mapToReplayEvents(historyString)
            .asSequence()
            .filterIsInstance<ReplayEventUpdateLocation>()
            .take(10)
            .toList()

        println("memoryUsed: ${memoryTestRule.memoryUsedMB}")
        assertEquals(10, replayEvents.size)
    }

    @Test
    fun `read entire file as a stream`() {
        val historyEventStream = resourceAsHistoryEventStream(historyFile)

        var counted = 0
        while (historyEventStream.hasNext()) {
            val next = historyEventStream.next()
            counted++
        }

        println("memoryUsed: ${memoryTestRule.memoryUsedMB}")
        assertEquals(3270, counted)
    }

    @Test
    fun `read entire file as a string`() {
        val historyString = resourceAsString(historyFile)
        val replayHistoryMapper = ReplayHistoryMapper()
        val replayEvents = replayHistoryMapper.mapToReplayEvents(historyString)

        println("memoryUsed: ${memoryTestRule.memoryUsedMB}")
        assertEquals(3270, replayEvents.size)
    }

    private fun resourceAsHistoryEventStream(
        name: String,
        packageName: String = "com.mapbox.navigation.core.replay.history"
    ): ReplayEventStream {
        val inputStream = javaClass.classLoader?.getResourceAsStream("$packageName/$name")
        val jsonReader = JsonReader(InputStreamReader(inputStream!!))
        return ReplayEventStream(jsonReader)
    }

    private fun resourceAsString(
        name: String,
        packageName: String = "com.mapbox.navigation.core.replay.history"
    ): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream("$packageName/$name")
        return IOUtils.toString(inputStream, "UTF-8")
    }
}
