package com.mapbox.navigation.ui.maps.guidance.signboard

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.navigation.ui.maps.guidance.signboard.api.SvgToBitmapParser
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardOptions

internal sealed class SignboardAction {

    data class CheckSignboardAvailability(
        val instructions: BannerInstructions,
    ) : SignboardAction()

    data class PrepareSignboardRequest(
        val signboardUrl: String,
    ) : SignboardAction()

    data class ProcessSignboardResponse(
        val response: Expected<ResourceLoadError, ResourceLoadResult>,
    ) : SignboardAction()

    data class ParseSvgToBitmap(
        val svg: DataRef,
        val parser: SvgToBitmapParser,
        val options: MapboxSignboardOptions,
    ) : SignboardAction()
}
