package com.mapbox.navigation.core.replay.history

import android.util.Log
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap

typealias CustomEventMapper = (String, LinkedTreeMap<*, *>) -> ReplayEventBase?

/**
 * This class is responsible for creating [ReplayEvents] from history data.
 *
 * If you have added custom events to the replay history, include your own [CustomEventMapper]
 */
class ReplayHistoryMapper(
    private val gson: Gson = Gson(),
    private val customEventMapper: CustomEventMapper? = null
) {

    /**
     * Given raw json string return [ReplayEvents] that can be given to a [ReplayHistoryPlayer]
     */
    fun mapToReplayEvents(historyData: String): List<ReplayEventBase> {
        val exampleHistoryData = gson.fromJson(historyData, ReplayHistoryDTO::class.java)
        return mapToReplayEvents(exampleHistoryData)
    }

    /**
     * Given [ReplayHistoryDTO] return [ReplayEvents] that can be given to a [ReplayHistoryPlayer]
     */
    fun mapToReplayEvents(historyDTO: ReplayHistoryDTO): List<ReplayEventBase> {
        return historyDTO.events
            .mapIndexed { index, _ ->
                val event = historyDTO.events[index] as LinkedTreeMap<*, *>
                return@mapIndexed try {
                    val eventType: String = event["type"] as String
                    mapToEvent(eventType, event)
                } catch (t: Throwable) {
                    Log.e("ReplayHistory", "Failed to read index $index: $event", t)
                    throw t
                }
            }
            .filterNotNull()
    }

    private fun mapToEvent(eventType: String, event: LinkedTreeMap<*, *>): ReplayEventBase? {
        return when (eventType) {
            "updateLocation" -> gson.fromJson(event.toString(), ReplayEventUpdateLocation::class.java)
            "getStatus" -> {
                val eventTimestamp = if (event.contains("event_timestamp")) {
                    event["event_timestamp"]
                } else {
                    event["timestamp"]
                } as Double
                ReplayEventGetStatus(
                    eventTimestamp = eventTimestamp)
            }
            else -> {
                val replayEvent = customEventMapper?.invoke(eventType, event)
                if (replayEvent == null) {
                    Log.e("ReplayHistory", "Unsupported $eventType")
                }
                replayEvent
            }
        }
    }
}
