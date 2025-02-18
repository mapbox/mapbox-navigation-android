package com.mapbox.navigation.mapgpt.core.api.window

import com.mapbox.navigation.mapgpt.core.api.EnumSerialNameSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object WindowPositionSerializer: EnumSerialNameSerializer<WindowPosition>(
    WindowPosition.values(),
    WindowPosition.UNKNOWN
)

@Serializable(with = WindowPositionSerializer::class)
enum class WindowPosition {
    @SerialName("OPEN")
    OPEN,
    @SerialName("CLOSED")
    CLOSED,
    @SerialName("HALF")
    HALF,
    @SerialName("INCREMENT_UP")
    INCREMENT_UP,
    @SerialName("INCREMENT_DOWN")
    INCREMENT_DOWN,
    UNKNOWN;
}
