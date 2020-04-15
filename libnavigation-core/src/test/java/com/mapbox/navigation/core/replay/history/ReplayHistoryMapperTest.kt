package com.mapbox.navigation.core.replay.history

import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test

class ReplayHistoryMapperTest {

    private val replayHistoryMapper = ReplayHistoryMapper()

    @Test
    fun `should map events`() {
        val historyString = """{"events":[{"type":"getStatus","timestamp":1580744200.379,"event_timestamp":1580744198.879556,"delta_ms":0},{"type":"updateLocation","location":{"lat":50.1232182,"lon":8.6343946,"time":1580744199.406,"speed":0.02246818132698536,"bearing":33.55318069458008,"altitude":162.8000030517578,"accuracyHorizontal":14.710000038146973,"provider":"fused"},"event_timestamp":1580744199.407049,"delta_ms":0}],"version":"6.2.1","history_version":"1.0.0"}"""

        val historyEvents = replayHistoryMapper.mapToReplayEvents(historyString)

        assertEquals(historyEvents.size, 2)
    }

    @Test
    fun `should map get status values`() {
        val historyString = """{"events":[{"type":"getStatus","timestamp":1580744200.379,"event_timestamp":1580744198.879556,"delta_ms":0},{"type":"updateLocation","location":{"lat":50.1232182,"lon":8.6343946,"time":1580744199.406,"speed":0.02246818132698536,"bearing":33.55318069458008,"altitude":162.8000030517578,"accuracyHorizontal":14.710000038146973,"provider":"fused"},"event_timestamp":1580744199.407049,"delta_ms":0}],"version":"6.2.1","history_version":"1.0.0"}"""

        val historyEvents = replayHistoryMapper.mapToReplayEvents(historyString)

        assertEquals(historyEvents[0].eventTimestamp, 1580744198.879556)
        assertTrue(historyEvents[0] is ReplayEventGetStatus)
    }

    @Test
    fun `should map location values`() {
        val historyString = """{"events":[{"type":"getStatus","timestamp":1580744200.379,"event_timestamp":1580744198.879556,"delta_ms":0},{"type":"updateLocation","location":{"lat":50.1232182,"lon":8.6343946,"time":1580744199.406,"speed":0.02246818132698536,"bearing":33.55318069458008,"altitude":162.8000030517578,"accuracyHorizontal":14.710000038146973,"provider":"fused"},"event_timestamp":1580744199.407049,"delta_ms":0}],"version":"6.2.1","history_version":"1.0.0"}"""

        val historyEvents = replayHistoryMapper.mapToReplayEvents(historyString)

        assertEquals(historyEvents[1].eventTimestamp, 1580744199.407049)
        (historyEvents[1] as ReplayEventUpdateLocation).location.let {
            assertEquals(it.lat, 50.1232182)
            assertEquals(it.lon, 8.6343946)
            assertEquals(it.time, 1580744199.406)
            assertEquals(it.speed, 0.02246818132698536)
            assertEquals(it.bearing, 33.55318069458008)
            assertEquals(it.altitude, 162.8000030517578)
            assertEquals(it.accuracyHorizontal, 14.710000038146973)
            assertEquals(it.provider, "fused")
        }
    }

    @Test
    fun `should map custom event`() {
        val historyString = """{"events":[{"type":"getStatus","timestamp":1580744200.379,"event_timestamp":1580744198.879556,"delta_ms":0},{"type":"updateLocation","location":{"lat":50.1232182,"lon":8.6343946,"time":1580744199.406,"speed":0.02246818132698536,"bearing":33.55318069458008,"altitude":162.8000030517578,"accuracyHorizontal":14.710000038146973,"provider":"fused"},"event_timestamp":1580744199.407049,"delta_ms":0},{"type":"getStatus","timestamp":1580744213.506,"event_timestamp":1580744212.006626,"delta_ms":0},{"type":"end_transit","properties":1580744212.223,"event_timestamp":1580744212.223644}],"version":"6.2.1","history_version":"1.0.0"}"""
        val replayHistoryMapper = ReplayHistoryMapper(customEventMapper = ExampleCustomEventMapper())
        val historyEvents = replayHistoryMapper.mapToReplayEvents(historyString)
        assertEquals(historyEvents.size, 4)
    }

    @Test
    fun `old versions of history are missing event_timestamp`() {
        val historyString = """{"events":[{"type":"getStatus","timestamp":1551460823.922}],"version":"5.0.0","history_version":"1.0.0"}"""
        val replayHistoryMapper = ReplayHistoryMapper(customEventMapper = ExampleCustomEventMapper())
        val historyEvents = replayHistoryMapper.mapToReplayEvents(historyString)
        assertEquals(historyEvents.size, 1)
    }

    private data class ExampleEndTransitEvent(
        @SerializedName("event_timestamp")
        override val eventTimestamp: Double,
        val properties: Double
    ) : ReplayEventBase

    private class ExampleCustomEventMapper : CustomEventMapper {
        override fun invoke(eventType: String, parameters: LinkedTreeMap<*, *>): ReplayEventBase? {
            return when (eventType) {
                "end_transit" -> ExampleEndTransitEvent(
                    eventTimestamp = parameters["event_timestamp"] as Double,
                    properties = parameters["properties"] as Double
                )
                else -> null
            }
        }
    }
}
