package com.mapbox.navigation.mapgpt.core.api.map

import com.mapbox.navigation.mapgpt.core.api.EnumSerialNameSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object MapThemeSerializer: EnumSerialNameSerializer<MapTheme>(
    MapTheme.values(),
    MapTheme.UNKNOWN
)

@Serializable(with = MapThemeSerializer::class)
enum class MapTheme {
    @SerialName("DEFAULT")
    DEFAULT,
    @SerialName("FADED")
    FADED,
    @SerialName("MONO")
    MONO,
    @SerialName("UNKNOWN")
    UNKNOWN,
}
