package com.mapbox.navigation.route.onboard.model

import com.google.gson.annotations.SerializedName

internal data class OfflineRouteError(
    val status: String,
    @SerializedName("status_code") val statusCode: Int,
    val error: String,
    @SerializedName("error_code") val errorCode: Int
)
