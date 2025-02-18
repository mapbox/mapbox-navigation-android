package com.mapbox.navigation.mapgpt.core.api.camera

import com.mapbox.navigation.mapgpt.core.api.EnumSerialNameSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal object CameraTrackingSerializer: EnumSerialNameSerializer<CameraTracking>(
    CameraTracking.values(),
    CameraTracking.UNKNOWN
)

@Serializable(with = CameraTrackingSerializer::class)
enum class CameraTracking {
    @SerialName("RECENTER")
    RECENTER,
    @SerialName("TRACKING_PUCK")
    TRACKING_PUCK,
    @SerialName("TRACKING_NORTH")
    TRACKING_NORTH,
    @SerialName("OVERVIEW")
    OVERVIEW,
    @SerialName("UNKNOWN")
    UNKNOWN,
}
