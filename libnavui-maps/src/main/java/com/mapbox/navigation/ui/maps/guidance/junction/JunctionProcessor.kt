package com.mapbox.navigation.ui.maps.guidance.junction

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.ui.maps.guidance.junction.api.MapboxRasterToBitmapParser
import com.mapbox.navigation.ui.utils.internal.extensions.getBannerComponents
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.utils.internal.isNotEmpty

internal object JunctionProcessor {

    /**
     * The function takes [JunctionAction], performs business logic and returns [JunctionResult]
     * @param action JunctionAction user specific commands
     * @return JunctionResult
     */
    fun process(action: JunctionAction): JunctionResult {
        return when (action) {
            is JunctionAction.CheckJunctionAvailability -> {
                isJunctionAvailable(action.instructions)
            }
            is JunctionAction.PrepareJunctionRequest -> {
                prepareRequest(action.junctionUrl)
            }
            is JunctionAction.ProcessJunctionResponse -> {
                processResponse(action.response)
            }
            is JunctionAction.ParseRasterToBitmap -> {
                processRaster(action.data)
            }
        }
    }

    private fun isJunctionAvailable(instruction: BannerInstructions): JunctionResult {
        return ifNonNull(getJunctionUrl(instruction)) { junctionUrl ->
            JunctionResult.JunctionAvailable(junctionUrl)
        } ?: JunctionResult.JunctionUnavailable
    }

    private fun getJunctionUrl(
        bannerInstructions: BannerInstructions,
    ): String? = bannerInstructions.getBannerComponents()?.findJunctionUrl()

    private fun List<BannerComponents>.findJunctionUrl(): String? {
        val component = find {
            it.type() == BannerComponents.GUIDANCE_VIEW &&
                (
                    it.subType() == BannerComponents.JCT ||
                        it.subType() == BannerComponents.SAPA ||
                        it.subType() == BannerComponents.CITYREAL ||
                        it.subType() == BannerComponents.AFTERTOLL ||
                        it.subType() == BannerComponents.TOLLBRANCH ||
                        it.subType() == BannerComponents.EXPRESSWAY_EXIT ||
                        it.subType() == BannerComponents.EXPRESSWAY_ENTRANCE
                    )
        }
        return component?.imageUrl()
    }

    private fun prepareRequest(url: String): JunctionResult {
        val loadRequest = ResourceLoadRequest(url)
        return JunctionResult.JunctionRequest(loadRequest)
    }

    private fun processResponse(
        response: Expected<ResourceLoadError, ResourceLoadResult>,
    ): JunctionResult {
        return response.fold(
            { error ->
                JunctionResult.JunctionRaster.Failure(error.message)
            },
            { responseData ->
                when (responseData.status) {
                    ResourceLoadStatus.AVAILABLE -> {
                        val dataRef = responseData.data?.data
                        if (dataRef?.isNotEmpty() == true) {
                            JunctionResult.JunctionRaster.Success(dataRef, responseData.contentType)
                        } else {
                            JunctionResult.JunctionRaster.Empty
                        }
                    }
                    ResourceLoadStatus.UNAUTHORIZED -> {
                        JunctionResult.JunctionRaster.Failure(
                            "Your token cannot access this " +
                                "resource, contact support",
                        )
                    }
                    ResourceLoadStatus.NOT_FOUND -> {
                        JunctionResult.JunctionRaster.Failure("Resource is missing")
                    }
                    else -> {
                        JunctionResult.JunctionRaster.Failure("Unknown error")
                    }
                }
            },
        )
    }

    private fun processRaster(
        raster: DataRef,
    ): JunctionResult {
        return MapboxRasterToBitmapParser.parse(raster).fold(
            { error ->
                JunctionResult.JunctionBitmap.Failure(error)
            },
            { value ->
                JunctionResult.JunctionBitmap.Success(value)
            },
        )
    }
}
