package com.mapbox.navigation.core.telemetry.events

import com.google.gson.annotations.SerializedName

internal data class TelemetryLocation(
    @SerializedName("lat") val latitude: Double,
    @SerializedName("lng") val longitude: Double,
    @SerializedName("speed") val speed: Double?,
    @SerializedName("course") val bearing: Double?,
    @SerializedName("altitude") val altitude: Double?,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("horizontalAccuracy") val horizontalAccuracy: Double,
    @SerializedName("verticalAccuracy") val verticalAccuracy: Double,
)
