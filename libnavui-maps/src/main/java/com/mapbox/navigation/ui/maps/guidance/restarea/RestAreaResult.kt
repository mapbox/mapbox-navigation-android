package com.mapbox.navigation.ui.maps.guidance.restarea

import android.graphics.Bitmap
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest

internal sealed class RestAreaResult {
    data class RestAreaMapAvailable(
        val sapaMapUrl: String,
    ) : RestAreaResult()

    object RestAreaMapUnavailable : RestAreaResult()

    data class RestAreaMapRequest(
        val request: ResourceLoadRequest,
    ) : RestAreaResult()

    sealed class RestAreaMapSvg : RestAreaResult() {
        object Empty : RestAreaMapSvg()
        data class Failure(val error: String?) : RestAreaMapSvg()
        data class Success(val data: DataRef) : RestAreaMapSvg()
    }

    sealed class RestAreaBitmap : RestAreaResult() {
        data class Success(val restAreaGuideMap: Bitmap) : RestAreaBitmap()
        data class Failure(val error: String?, val throwable: Throwable?) : RestAreaBitmap()
    }
}
