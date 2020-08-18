package com.mapbox.navigation.core.telemetry.events

import com.google.gson.annotations.SerializedName

internal data class TelemetryLocation(
    @SerializedName("lat") val latitude: Double,
    @SerializedName("lng") val longitude: Double,
    @SerializedName("speed") val speed: Float,
    @SerializedName("course") val bearing: Float,
    @SerializedName("altitude") val altitude: Double,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("horizontalAccuracy") val horizontalAccuracy: Float,
    @SerializedName("verticalAccuracy") val verticalAccuracy: Float
)
