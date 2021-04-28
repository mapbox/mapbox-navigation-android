package com.mapbox.navigation.ui.maps.guidance.signboard

import android.graphics.Bitmap
import com.mapbox.common.HttpRequest

internal sealed class SignboardResult {

    data class SignboardAvailable(
        val signboardUrl: String
    ) : SignboardResult()

    object SignboardUnavailable : SignboardResult()

    data class SignboardRequest(
        val request: HttpRequest
    ) : SignboardResult()

    sealed class SignboardSvg : SignboardResult() {
        object Empty : SignboardSvg()
        data class Failure(val error: String?) : SignboardSvg()
        data class Success(val data: ByteArray) : SignboardSvg() {
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

    sealed class SignboardBitmap : SignboardResult() {
        data class Success(val signboard: Bitmap) : SignboardBitmap()
        data class Failure(val message: String) : SignboardBitmap()
    }
}
