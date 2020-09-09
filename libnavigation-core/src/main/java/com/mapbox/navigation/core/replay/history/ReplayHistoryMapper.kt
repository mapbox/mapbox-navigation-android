package com.mapbox.navigation.core.replay.history

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.navigation.core.replay.MapboxReplayer

/**
 * Additional mapper that can be used with [ReplayHistoryMapper].
 */
interface CustomEventMapper {

    /**
     * Override to map your own custom events from history files,
     * into [ReplayEventBase] for the [MapboxReplayer]
     */
    fun map(eventType: String, properties: Map<*, *>): ReplayEventBase?
}

/**
 * This class is responsible for creating [ReplayEvents] from history data.
 *
 * @param customEventMapper if you have added custom events to the replay history, include your
 * own [CustomEventMapper] (optional)
 * @param logger interface for logging any events (optional)
 */
class ReplayHistoryMapper @JvmOverloads constructor(
    private val customEventMapper: CustomEventMapper? = null,
    private val logger: Logger? = null
) {
    private val gson: Gson = Gson()

    /**
     * Given raw json string return [ReplayEvents] that can be given to a [MapboxReplayer]
     */
    fun mapToReplayEvents(historyData: String): List<ReplayEventBase> {
        val exampleHistoryData = gson.fromJson(historyData, ReplayHistoryDTO::class.java)
        return mapToReplayEvents(exampleHistoryData)
    }

    /**
     * Given [ReplayHistoryDTO] return [ReplayEvents] that can be given to a [MapboxReplayer]
     */
    fun mapToReplayEvents(historyDTO: ReplayHistoryDTO): List<ReplayEventBase> {
        return historyDTO.events
            .mapIndexed { index, _ ->
                val event = historyDTO.events[index] as LinkedTreeMap<*, *>
                return@mapIndexed try {
                    val eventType: String = event["type"] as String
                    mapToEvent(eventType, event)
                } catch (throwable: Throwable) {
                    logger?.e(
                        msg = Message("Failed to read index $index: $event"),
                        tr = throwable
                    )
                    throw throwable
                }
            }
            .filterNotNull()
    }

    private fun mapToEvent(eventType: String, event: LinkedTreeMap<*, *>): ReplayEventBase? {
        return when (eventType) {
            "updateLocation" -> gson.fromJson(
                event.toString(),
                ReplayEventUpdateLocation::class.java
            )
            "getStatus" -> {
                val eventTimestamp = if (event.contains("event_timestamp")) {
                    event["event_timestamp"]
                } else {
                    event["timestamp"]
                } as Double
                ReplayEventGetStatus(
                    eventTimestamp = eventTimestamp
                )
            }
            "setRoute" -> {
                val directionsRoute = try {
                    if (event["route"] == "{}") {
                        null
                    } else {
                        DirectionsRoute.fromJson(event["route"] as String)
                    }
                } catch (throwable: Throwable) {
                    logger?.w(
                        msg = Message("Unable to setRoute from history file"),
                        tr = throwable
                    )
                    return null
                }
                ReplaySetRoute(
                    eventTimestamp = event["event_timestamp"] as Double,
                    route = directionsRoute
                )
            }
            else -> {
                val replayEvent = customEventMapper?.map(eventType, event.toMap())
                if (replayEvent == null) {
                    logger?.e(msg = Message("Replay unsupported event $eventType"))
                }
                replayEvent
            }
        }
    }
}
