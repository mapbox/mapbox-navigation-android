package com.mapbox.navigation.core.location.replay

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.util.Date

internal data class ReplayLocationDto(
    @SerializedName("lng")
    var longitude: Double = 0.0,
    @SerializedName("horizontalAccuracy")
    var horizontalAccuracyMeters: Float = 0.0f,
    @SerializedName("course")
    var bearing: Double = 0.0,
    @SerializedName("verticalAccuracy")
    var verticalAccuracyMeters: Float = 0.0f,
    var speed: Double = 0.0,
    @SerializedName("lat")
    var latitude: Double = 0.0,
    @SerializedName("altitude")
    var altitude: Double = 0.0,
    @SerializedName("timestamp")
    @JsonAdapter(TimestampAdapter::class)
    var date: Date? = null
)
