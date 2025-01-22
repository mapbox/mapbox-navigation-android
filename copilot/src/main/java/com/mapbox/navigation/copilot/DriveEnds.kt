package com.mapbox.navigation.copilot

import androidx.annotation.Keep

@Keep
internal data class DriveEnds(
    val type: String,
    val realDuration: Long,
) : EventDTO
