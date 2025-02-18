package com.mapbox.navigation.mapgpt.core.api.autopilot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object AutopilotLaneDirectionSerializer: AndroidEnumSerialNameSerializer<AutopilotLaneDirection>(
    AutopilotLaneDirection.values(),
    AutopilotLaneDirection.UNKNOWN
)

/**
 * Enum used to serialize the lane direction.
 */
@Serializable(with = AutopilotLaneDirectionSerializer::class)
enum class AutopilotLaneDirection {
    @SerialName("left")
    LEFT,
    @SerialName("right")
    RIGHT,
    UNKNOWN
}
