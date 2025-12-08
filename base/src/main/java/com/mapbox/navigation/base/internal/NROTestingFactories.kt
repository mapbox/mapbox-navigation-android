package com.mapbox.navigation.base.internal

import androidx.annotation.RestrictTo
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsResponseFBWrapper
import com.mapbox.api.directions.v5.models.FBDirectionsResponse
import com.mapbox.bindgen.DataRef
import com.mapbox.directions.route.DirectionsRouteResponse
import com.mapbox.navigation.base.route.toDirectionsResponse
import org.jetbrains.annotations.TestOnly

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@TestOnly
fun createNRODirectionsResponse(
    dataRef: DataRef,
): DirectionsResponse {
    return DirectionsResponseFBWrapper(
        FBDirectionsResponse.getRootAsDirectionsResponse(
            parseToFBBuffer(dataRef).buffer,
        ),
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@TestOnly
fun createJavaDirectionsResponse(dataRef: DataRef): DirectionsResponse =
    dataRef.toDirectionsResponse(false)

private fun parseToFBBuffer(jsonBuffer: DataRef): DataRef {
    val result = DirectionsRouteResponse.parseDirectionsResponseJson(jsonBuffer)
    if (result.isError) {
        throw IllegalArgumentException("Failed to parse DirectionsResponse: ${result.error}")
    }
    return result.value!!.getData()
}
