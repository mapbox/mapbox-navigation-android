package com.mapbox.navigation.mapgpt.core.api.climate

import com.mapbox.navigation.mapgpt.core.api.EnumSerialNameSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object FanLocationSerializer: EnumSerialNameSerializer<FanLocation>(
    FanLocation.values(),
    FanLocation.UNKNOWN
)

/**
 * Enum used to serialize position of the fan.
 */
@Serializable(with = FanLocationSerializer::class)
enum class FanLocation {
    @SerialName("ALL")
    ALL,
    @SerialName("FEET")
    FEET,
    @SerialName("HEAD")
    HEAD,
    @SerialName("WINDSHIELD")
    WINDSHIELD,
    UNKNOWN
}
