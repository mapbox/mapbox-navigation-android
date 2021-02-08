package com.mapbox.navigation.ui.maps.signboard

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpResponseData

internal sealed class SignboardAction {

    data class CheckSignboardAvailability(
        val instructions: BannerInstructions
    ) : SignboardAction()

    data class PrepareSignboardRequest(
        val signboardUrl: String
    ) : SignboardAction()

    data class ProcessSignboardResponse(
        val response: Expected<HttpResponseData?, HttpRequestError?>
    ) : SignboardAction()
}
