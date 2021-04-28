package com.mapbox.navigation.ui.maps.guidance.junction

import android.graphics.Bitmap
import com.mapbox.common.HttpRequest

internal sealed class JunctionResult {

    data class JunctionAvailable(
        val junctionUrl: String
    ) : JunctionResult()

    object JunctionUnavailable : JunctionResult()

    data class JunctionRequest(
        val request: HttpRequest
    ) : JunctionResult()

    sealed class JunctionRaster : JunctionResult() {
        object Empty : JunctionRaster()
        data class Failure(val error: String?) : JunctionRaster()
        data class Success(val data: ByteArray) : JunctionRaster() {
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

    sealed class JunctionBitmap : JunctionResult() {
        data class Success(val junction: Bitmap) : JunctionBitmap()
        data class Failure(val message: String) : JunctionBitmap()
    }
}
