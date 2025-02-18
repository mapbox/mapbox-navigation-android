package com.mapbox.navigation.mapgpt.core.api.climate

import com.mapbox.navigation.mapgpt.core.api.EnumSerialNameSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object OccupantSerializer: EnumSerialNameSerializer<Occupant>(
    Occupant.values(),
    Occupant.UNKNOWN
)

@Serializable(with = OccupantSerializer::class)
enum class Occupant {
    @SerialName("ALL")
    ALL,
    @SerialName("DRIVER")
    DRIVER,
    @SerialName("FRONT_PASSENGER")
    FRONT_PASSENGER,
    @SerialName("FRONT_ALL")
    FRONT_ALL,
    @SerialName("BACK_ALL")
    BACK_ALL,
    @SerialName("BACK_LEFT")
    BACK_LEFT,
    @SerialName("BACK_RIGHT")
    BACK_RIGHT,
    @SerialName("BACK_PASSENGER")
    BACK_PASSENGER,
    UNKNOWN
}
