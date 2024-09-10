package com.mapbox.navigation.ui.maps.guidance.junction

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult

internal sealed class JunctionAction {

    data class CheckJunctionAvailability(
        val instructions: BannerInstructions,
    ) : JunctionAction()

    data class PrepareJunctionRequest(
        val junctionUrl: String,
    ) : JunctionAction()

    data class ProcessJunctionResponse(
        val response: Expected<ResourceLoadError, ResourceLoadResult>,
    ) : JunctionAction()

    data class ParseRasterToBitmap(
        val data: DataRef,
    ) : JunctionAction()
}
