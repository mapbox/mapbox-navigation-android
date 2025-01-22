package com.mapbox.navigation.ui.maps.guidance.junction

import android.graphics.Bitmap
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest

internal sealed class JunctionResult {

    data class JunctionAvailable(
        val junctionUrl: String,
    ) : JunctionResult()

    object JunctionUnavailable : JunctionResult()

    data class JunctionRequest(
        val request: ResourceLoadRequest,
    ) : JunctionResult()

    sealed class JunctionRaster : JunctionResult() {
        object Empty : JunctionRaster()
        data class Failure(val error: String?) : JunctionRaster()
        data class Success(val dataRef: DataRef, val contentType: String) : JunctionRaster()
    }

    sealed class JunctionBitmap : JunctionResult() {
        data class Success(val junction: Bitmap) : JunctionBitmap()
        data class Failure(val message: String) : JunctionBitmap()
    }
}
