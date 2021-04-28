package com.mapbox.navigation.ui.maps.guidance.junction

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpResponseData

internal sealed class JunctionAction {

    data class CheckJunctionAvailability(
        val instructions: BannerInstructions
    ) : JunctionAction()

    data class PrepareJunctionRequest(
        val junctionUrl: String
    ) : JunctionAction()

    data class ProcessJunctionResponse(
        val response: Expected<HttpResponseData?, HttpRequestError?>
    ) : JunctionAction()

    data class ParseRasterToBitmap(
        val data: ByteArray
    ) : JunctionAction() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ParseRasterToBitmap

            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }
}
