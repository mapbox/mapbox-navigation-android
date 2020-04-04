package com.mapbox.navigation.core.replay.history

import com.google.gson.annotations.SerializedName

data class ReplayHistoryDTO(
    @SerializedName("history_version")
    val historyVersion: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("events")
    val events: List<Any>
)
