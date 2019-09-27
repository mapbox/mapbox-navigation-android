package com.mapbox.services.android.navigation.v5.location.replay

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

import java.util.Date

class ReplayLocationDto {

    @SerializedName("lng")
    var longitude: Double = 0.toDouble()
    @SerializedName("horizontalAccuracy")
    var horizontalAccuracyMeters: Float = 0.toFloat()
    @SerializedName("course")
    var bearing: Double = 0.toDouble()
    @SerializedName("verticalAccuracy")
    var verticalAccuracyMeters: Float = 0.toFloat()
    var speed: Double = 0.toDouble()
    @SerializedName("lat")
    var latitude: Double = 0.toDouble()
    @SerializedName("altitude")
    var altitude: Double = 0.toDouble()
    @SerializedName("timestamp")
    @JsonAdapter(TimestampAdapter::class)
    var date: Date? = null
}
