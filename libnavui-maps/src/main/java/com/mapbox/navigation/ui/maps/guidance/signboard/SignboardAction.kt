package com.mapbox.navigation.ui.maps.guidance.signboard

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpResponseData
import com.mapbox.navigation.ui.maps.guidance.signboard.api.SvgToBitmapParser
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardOptions

internal sealed class SignboardAction {

    data class CheckSignboardAvailability(
        val instructions: BannerInstructions
    ) : SignboardAction()

    data class PrepareSignboardRequest(
        val signboardUrl: String
    ) : SignboardAction()

    data class ProcessSignboardResponse(
        val response: Expected<HttpRequestError, HttpResponseData>
    ) : SignboardAction()

    data class ParseSvgToBitmap(
        val svg: ByteArray,
        val parser: SvgToBitmapParser,
        val options: MapboxSignboardOptions
    ) : SignboardAction() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ParseSvgToBitmap

            if (!svg.contentEquals(other.svg)) return false
            if (parser != other.parser) return false

            return true
        }

        override fun hashCode(): Int {
            var result = svg.contentHashCode()
            result = 31 * result + parser.hashCode()
            return result
        }
    }
}
