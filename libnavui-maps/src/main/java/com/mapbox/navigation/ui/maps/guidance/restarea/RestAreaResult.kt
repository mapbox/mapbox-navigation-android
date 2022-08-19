package com.mapbox.navigation.ui.maps.guidance.restarea

import android.graphics.Bitmap
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest

internal sealed class RestAreaResult {
    data class RestAreaMapAvailable(
        val sapaMapUrl: String
    ) : RestAreaResult()

    object RestAreaMapUnavailable : RestAreaResult()

    data class RestAreaMapRequest(
        val request: ResourceLoadRequest
    ) : RestAreaResult()

    sealed class RestAreaMapSvg : RestAreaResult() {
        object Empty : RestAreaMapSvg()
        data class Failure(val error: String?) : RestAreaMapSvg()
        data class Success(val data: ByteArray) : RestAreaMapSvg() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Success

                if (!data.contentEquals(other.data)) return false

                return true
            }

            override fun hashCode(): Int {
                return data.contentHashCode()
            }
        }
    }

    sealed class RestAreaBitmap : RestAreaResult() {
        data class Success(val restAreaGuideMap: Bitmap) : RestAreaBitmap()
        data class Failure(val error: String?, val throwable: Throwable?) : RestAreaBitmap()
    }
}
