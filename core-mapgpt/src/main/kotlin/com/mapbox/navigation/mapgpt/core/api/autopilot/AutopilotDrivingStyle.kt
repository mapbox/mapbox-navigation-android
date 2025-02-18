package com.mapbox.navigation.mapgpt.core.api.autopilot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object AutopilotDrivingStyleSerializer: AndroidEnumSerialNameSerializer<AutopilotDrivingStyle>(
    AutopilotDrivingStyle.values(),
    AutopilotDrivingStyle.UNKNOWN
)

/**
 * Enum used to serialize the driving style.
 */
@Serializable(with = AutopilotDrivingStyleSerializer::class)
enum class AutopilotDrivingStyle {
    @SerialName("aggressive")
    AGGRESSIVE,
    @SerialName("standard")
    STANDARD,
    @SerialName("relaxed")
    RELAXED,
    UNKNOWN
}
