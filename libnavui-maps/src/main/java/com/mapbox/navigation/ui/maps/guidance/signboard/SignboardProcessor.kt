package com.mapbox.navigation.ui.maps.guidance.signboard

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.common.ResourceLoadStatus
import com.mapbox.navigation.ui.maps.guidance.signboard.api.SvgToBitmapParser
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardOptions
import com.mapbox.navigation.ui.utils.internal.extensions.getBannerComponents
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoadRequest

internal object SignboardProcessor {

    /**
     * The function takes [SignboardAction] performs business logic and returns [SignboardResult]
     * @param action SignboardAction user specific commands
     * @return SignboardResult
     */
    fun process(action: SignboardAction): SignboardResult {
        return when (action) {
            is SignboardAction.CheckSignboardAvailability -> {
                isSignboardAvailable(action.instructions)
            }
            is SignboardAction.PrepareSignboardRequest -> {
                prepareRequest(action.signboardUrl)
            }
            is SignboardAction.ProcessSignboardResponse -> {
                processResponse(action.response)
            }
            is SignboardAction.ParseSvgToBitmap -> {
                processSvg(action.svg, action.parser, action.options)
            }
        }
    }

    private fun isSignboardAvailable(instruction: BannerInstructions): SignboardResult {
        return ifNonNull(getSignboardUrl(instruction)) { signboardUrl ->
            SignboardResult.SignboardAvailable(signboardUrl)
        } ?: SignboardResult.SignboardUnavailable
    }

    private fun getSignboardUrl(
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
                it.subType() == BannerComponents.SIGNBOARD
        }
        return component?.imageUrl()
    }

    private fun prepareRequest(url: String): SignboardResult {
        val loadRequest = ResourceLoadRequest(url)
        return SignboardResult.SignboardRequest(loadRequest)
    }

    private fun processResponse(
        response: Expected<ResourceLoadError, ResourceLoadResult>
    ): SignboardResult {
        return response.fold(
            { error ->
                SignboardResult.SignboardSvg.Failure(error.message)
            },
            { responseData ->
                when (responseData.status) {
                    ResourceLoadStatus.AVAILABLE -> {
                        val blob: ByteArray = responseData.data?.data ?: byteArrayOf()
                        if (blob.isEmpty()) {
                            SignboardResult.SignboardSvg.Empty
                        } else {
                            SignboardResult.SignboardSvg.Success(blob)
                        }
                    }
                    ResourceLoadStatus.UNAUTHORIZED -> {
                        SignboardResult.SignboardSvg.Failure(
                            "Your token cannot access this " +
                                "resource, contact support"
                        )
                    }
                    ResourceLoadStatus.NOT_FOUND -> {
                        SignboardResult.SignboardSvg.Failure("Resource is missing")
                    }
                    else -> {
                        SignboardResult.SignboardSvg.Failure("Unknown error")
                    }
                }
            }
        )
    }

    private fun processSvg(
        svg: ByteArray,
        parser: SvgToBitmapParser,
        options: MapboxSignboardOptions
    ): SignboardResult {
        return parser.parse(svg, options).fold(
            { error ->
                SignboardResult.SignboardBitmap.Failure(error)
            },
            { value ->
                SignboardResult.SignboardBitmap.Success(value)
            }
        )
    }
}
