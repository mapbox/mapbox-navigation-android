package com.mapbox.navigation.ui.maps.guidance.junction

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.common.HttpMethod
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpResponseData
import com.mapbox.common.UAComponents
import com.mapbox.navigation.ui.maps.guidance.junction.api.MapboxRasterToBitmapParser
import com.mapbox.navigation.ui.utils.internal.extensions.getBannerComponents
import com.mapbox.navigation.ui.utils.internal.ifNonNull

internal object JunctionProcessor {

    private const val USER_AGENT_KEY = "User-Agent"
    private const val USER_AGENT_VALUE = "MapboxJava/"
    private const val SDK_IDENTIFIER = "mapbox-navigation-ui-android"
    private const val CODE_200 = 200L
    private const val CODE_401 = 401L
    private const val CODE_404 = 404L

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
        bannerInstructions: BannerInstructions
    ): String? {
        val bannerComponents = bannerInstructions.getBannerComponents()
        return when {
            bannerComponents != null -> {
                findSignboardComponent(bannerComponents)
            }
            else -> {
                null
            }
        }
    }

    private fun findSignboardComponent(
        componentList: MutableList<BannerComponents>
    ): String? {
        val component = componentList.find {
            it.type() == BannerComponents.GUIDANCE_VIEW &&
                it.subType() == BannerComponents.JCT
        }
        return component?.imageUrl()
    }

    private fun prepareRequest(url: String): JunctionResult {
        val request = HttpRequest.Builder()
            .url(url)
            .body(byteArrayOf())
            .method(HttpMethod.GET)
            .headers(hashMapOf(Pair(USER_AGENT_KEY, USER_AGENT_VALUE)))
            .uaComponents(
                UAComponents.Builder()
                    .sdkIdentifierComponent(SDK_IDENTIFIER)
                    .build()
            )
            .build()
        return JunctionResult.JunctionRequest(request)
    }

    private fun processResponse(
        response: Expected<HttpRequestError, HttpResponseData>
    ): JunctionResult {
        return response.fold(
            { error ->
                JunctionResult.JunctionRaster.Failure(error.message)
            },
            { responseData ->
                when (responseData.code) {
                    CODE_200 -> {
                        if (responseData.data.isEmpty()) {
                            JunctionResult.JunctionRaster.Empty
                        } else {
                            JunctionResult.JunctionRaster.Success(responseData.data)
                        }
                    }
                    CODE_401 -> {
                        JunctionResult.JunctionRaster.Failure(
                            "Your token cannot access this " +
                                "resource, contact support"
                        )
                    }
                    CODE_404 -> {
                        JunctionResult.JunctionRaster.Failure("Resource is missing")
                    }
                    else -> {
                        JunctionResult.JunctionRaster.Failure("Unknown error")
                    }
                }
            }
        )
    }

    private fun processRaster(
        raster: ByteArray
    ): JunctionResult {
        return MapboxRasterToBitmapParser.parse(raster).fold(
            { error ->
                JunctionResult.JunctionBitmap.Failure(error)
            },
            { value ->
                JunctionResult.JunctionBitmap.Success(value)
            }
        )
    }
}
