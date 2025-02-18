package com.mapbox.navigation.mapgpt.core.api.map

import com.mapbox.navigation.mapgpt.core.api.EnumSerialNameSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object MapLightingSerializer: EnumSerialNameSerializer<MapLighting>(
    MapLighting.values(),
    MapLighting.UNKNOWN
)

@Serializable(with = MapLightingSerializer::class)
enum class MapLighting {
    @SerialName("AUTO")
    AUTO,
    @SerialName("DAWN")
    DAWN,
    @SerialName("DAY")
    DAY,
    @SerialName("DUSK")
    DUSK,
    @SerialName("DARK")
    DARK,
    @SerialName("UNKNOWN")
    UNKNOWN,
}
