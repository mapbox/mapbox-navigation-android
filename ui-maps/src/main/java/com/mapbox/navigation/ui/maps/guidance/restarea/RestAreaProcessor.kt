package com.mapbox.navigation.ui.maps.guidance.restarea

import android.graphics.Bitmap
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.RoadObjectType
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.ui.maps.guidance.restarea.model.MapboxRestAreaOptions
import com.mapbox.navigation.ui.utils.internal.SvgUtil
import com.mapbox.navigation.ui.utils.internal.extensions.getBannerComponents
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest
import com.mapbox.navigation.utils.internal.ByteBufferBackedInputStream
import com.mapbox.navigation.utils.internal.isNotEmpty

internal object RestAreaProcessor {

    /**
     * The function takes [RestAreaAction] performs business logic and returns [RestAreaResult]
     * @param action RestAreaAction user specific commands
     * @return RestAreaResult
     */
    fun process(action: RestAreaAction): RestAreaResult {
        return when (action) {
            is RestAreaAction.CheckRestAreaMapAvailability -> {
                isSapaMapAvailable(action.instructions)
            }
            is RestAreaAction.CheckUpcomingRestStop -> {
                restStopHasGuideMap(action.routeProgress)
            }
            is RestAreaAction.PrepareRestAreaMapRequest -> {
                prepareRequest(action.sapaMapUrl)
            }
            is RestAreaAction.ProcessRestAreaMapResponse -> {
                processResponse(action.response)
            }
            is RestAreaAction.ParseSvgToBitmap -> {
                processSvg(action.svg, action.options)
            }
        }
    }

    private fun isSapaMapAvailable(instruction: BannerInstructions): RestAreaResult {
        return ifNonNull(getSapaMapUrl(instruction)) { sapaMapUrl ->
            RestAreaResult.RestAreaMapAvailable(sapaMapUrl)
        } ?: RestAreaResult.RestAreaMapUnavailable
    }

    private fun getSapaMapUrl(
        bannerInstructions: BannerInstructions,
    ): String? = bannerInstructions.getBannerComponents()?.findGuideMapUrl()

    private fun List<BannerComponents>.findGuideMapUrl(): String? {
        val component = find {
            it.type() == BannerComponents.GUIDANCE_VIEW &&
                it.subType() == BannerComponents.SAPAGUIDEMAP
        }
        return component?.imageUrl()
    }

    private fun restStopHasGuideMap(routeProgress: RouteProgress): RestAreaResult {
        return routeProgress.getUpcomingRestStopOnCurrentStep()?.guideMapUri?.let { uri ->
            RestAreaResult.RestAreaMapAvailable(uri)
        } ?: RestAreaResult.RestAreaMapUnavailable
    }

    private fun prepareRequest(url: String): RestAreaResult {
        val loadRequest = ResourceLoadRequest(url)
        return RestAreaResult.RestAreaMapRequest(loadRequest)
    }

    private fun processResponse(
        response: Expected<ResourceLoadError, ResourceLoadResult>,
    ): RestAreaResult {
        return response.fold(
            { error ->
                RestAreaResult.RestAreaMapSvg.Failure(
                    error.type.name + ": " + error.message,
                )
            },
            { responseData ->
                when (responseData.status) {
                    ResourceLoadStatus.AVAILABLE -> {
                        val dataRef = responseData.data?.data
                        if (dataRef?.isNotEmpty() == true) {
                            RestAreaResult.RestAreaMapSvg.Success(dataRef)
                        } else {
                            RestAreaResult.RestAreaMapSvg.Empty
                        }
                    }
                    ResourceLoadStatus.UNAUTHORIZED -> {
                        RestAreaResult.RestAreaMapSvg.Failure(
                            "Your token cannot access this resource, contact support",
                        )
                    }
                    ResourceLoadStatus.NOT_FOUND -> {
                        RestAreaResult.RestAreaMapSvg.Failure("Resource is missing")
                    }
                    else -> {
                        RestAreaResult.RestAreaMapSvg.Failure("Unknown error")
                    }
                }
            },
        )
    }

    private fun RouteProgress.getUpcomingRestStopOnCurrentStep(): RestStop? {
        val currentStepDistanceRemaining =
            currentLegProgress?.currentStepProgress?.distanceRemaining ?: return null
        return this.upcomingRoadObjects.firstOrNull {
            if (it.roadObject.objectType == RoadObjectType.REST_STOP) {
                val distanceToStart = it.distanceToStart
                distanceToStart != null && distanceToStart - currentStepDistanceRemaining <= 0
            } else {
                false
            }
        }?.roadObject as? RestStop
    }

    private fun processSvg(
        svg: DataRef,
        options: MapboxRestAreaOptions,
    ): RestAreaResult {
        val expected: Expected<Exception, Bitmap> = try {
            val stream = ByteBufferBackedInputStream(svg.buffer)
            ExpectedFactory.createValue(
                stream.use {
                    SvgUtil.renderAsBitmapWithWidth(
                        it,
                        options.desiredGuideMapWidth,
                    )
                },
            )
        } catch (ex: Exception) {
            ExpectedFactory.createError(ex)
        }
        return expected.fold(
            { error ->
                RestAreaResult.RestAreaBitmap.Failure(error.message, error.cause)
            },
            { value ->
                RestAreaResult.RestAreaBitmap.Success(value)
            },
        )
    }
}
