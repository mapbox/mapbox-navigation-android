package com.mapbox.navigation.ui.maps.guidance.signboard

import android.graphics.Bitmap
import com.mapbox.bindgen.DataRef
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest

internal sealed class SignboardResult {

    data class SignboardAvailable(
        val signboardUrl: String,
    ) : SignboardResult()

    object SignboardUnavailable : SignboardResult()

    data class SignboardRequest(
        val request: ResourceLoadRequest,
    ) : SignboardResult()

    sealed class SignboardSvg : SignboardResult() {
        object Empty : SignboardSvg()
        data class Failure(val error: String?) : SignboardSvg()
        data class Success(val data: DataRef) : SignboardSvg()
    }

    sealed class SignboardBitmap : SignboardResult() {
        data class Success(val signboard: Bitmap) : SignboardBitmap()
        data class Failure(val message: String) : SignboardBitmap()
    }
}
