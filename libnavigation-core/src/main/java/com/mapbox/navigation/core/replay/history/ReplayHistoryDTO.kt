package com.mapbox.navigation.core.replay.history

import com.google.gson.annotations.SerializedName
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Data representation of the string given by [MapboxNavigation.retrieveHistory]. If you'd like to
 * add a custom event, add it to [events] and pass a [CustomEventMapper] to the [ReplayHistoryMapper].
 *
 * @param historyVersion version of the events supported: 1.0.0
 * @param version version of the navigator library
 * @param events navigation events to replay
 */
data class ReplayHistoryDTO(
    @SerializedName("history_version")
    val historyVersion: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("events")
    val events: List<Any>
)
