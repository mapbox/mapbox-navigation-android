package com.mapbox.navigation.ui.maps.guidance.junction

import com.mapbox.common.HttpRequest

internal sealed class JunctionResult {

    data class JunctionAvailable(
        val junctionUrl: String
    ) : JunctionResult()

    object JunctionUnavailable : JunctionResult()

    data class JunctionRequest(
        val request: HttpRequest
    ) : JunctionResult()

    sealed class Junction : JunctionResult() {
        object Empty : Junction()
        data class Failure(val error: String?) : Junction()
        data class Success(val data: ByteArray) : Junction()
    }
}
