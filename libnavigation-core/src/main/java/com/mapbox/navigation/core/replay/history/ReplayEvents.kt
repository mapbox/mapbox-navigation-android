package com.mapbox.navigation.core.replay.history

import com.google.gson.annotations.SerializedName

data class ReplayEvents(
    // Assumes chronological order, index 0 moves to events.size over time.
    val events: List<ReplayEventBase>
)

interface ReplayEventBase {
    val eventTimestamp: Double
}

data class ReplayEventGetStatus(
    @SerializedName("event_timestamp")
    override val eventTimestamp: Double
) : ReplayEventBase

data class ReplayEventUpdateLocation(
    @SerializedName("event_timestamp")
    override val eventTimestamp: Double,
    val location: ReplayEventLocation
) : ReplayEventBase

data class ReplayEventLocation(
    val lon: Double,
    val lat: Double,
    val provider: String?,
    val time: Double?,
    val altitude: Double?,
    val accuracyHorizontal: Double?,
    val bearing: Double?,
    val speed: Double?
)
