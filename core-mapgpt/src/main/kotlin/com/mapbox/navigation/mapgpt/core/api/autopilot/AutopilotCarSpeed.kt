package com.mapbox.navigation.mapgpt.core.api.autopilot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object AutopilotCarSpeedSerializer: AndroidEnumSerialNameSerializer<AutopilotCarSpeed>(
    AutopilotCarSpeed.values(),
    AutopilotCarSpeed.UNKNOWN
)

/**
 * Enum used to serialize the speed of the vehicle.
 */
@Serializable(with = AutopilotCarSpeedSerializer::class)
enum class AutopilotCarSpeed {
    @SerialName("accelerate")
    ACCELERATE,
    @SerialName("decelerate")
    DECELERATE,
    UNKNOWN
}
