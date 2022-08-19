package com.mapbox.navigation.ui.maps.guidance.restarea

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maps.guidance.restarea.model.MapboxRestAreaOptions

internal sealed class RestAreaAction {

    data class CheckRestAreaMapAvailability(
        val instructions: BannerInstructions
    ) : RestAreaAction()

    data class CheckUpcomingRestStop(
        val routeProgress: RouteProgress
    ) : RestAreaAction()

    data class PrepareRestAreaMapRequest(
        val sapaMapUrl: String
    ) : RestAreaAction()

    data class ProcessRestAreaMapResponse(
        val response: Expected<ResourceLoadError, ResourceLoadResult>
    ) : RestAreaAction()

    data class ParseSvgToBitmap(
        val svg: ByteArray,
        val options: MapboxRestAreaOptions
    ) : RestAreaAction() {

        /**
         * Regenerate whenever a change is made
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ParseSvgToBitmap

            if (!svg.contentEquals(other.svg)) return false
            if (options != other.options) return false

            return true
        }

        /**
         * Regenerate whenever a change is made
         */
        override fun hashCode(): Int {
            var result = svg.contentHashCode()
            result = 31 * result + options.hashCode()
            return result
        }

        /**
         * Regenerate whenever a change is made
         */
        override fun toString(): String {
            return "ParseSvgToBitmap(svg=${svg.contentToString()}, options=$options)"
        }
    }
}
