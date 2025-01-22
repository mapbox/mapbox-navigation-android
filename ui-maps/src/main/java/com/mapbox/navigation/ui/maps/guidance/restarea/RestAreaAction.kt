package com.mapbox.navigation.ui.maps.guidance.restarea

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maps.guidance.restarea.model.MapboxRestAreaOptions

internal sealed class RestAreaAction {

    data class CheckRestAreaMapAvailability(
        val instructions: BannerInstructions,
    ) : RestAreaAction()

    data class CheckUpcomingRestStop(
        val routeProgress: RouteProgress,
    ) : RestAreaAction()

    data class PrepareRestAreaMapRequest(
        val sapaMapUrl: String,
    ) : RestAreaAction()

    data class ProcessRestAreaMapResponse(
        val response: Expected<ResourceLoadError, ResourceLoadResult>,
    ) : RestAreaAction()

    data class ParseSvgToBitmap(
        val svg: DataRef,
        val options: MapboxRestAreaOptions,
    ) : RestAreaAction()
}
