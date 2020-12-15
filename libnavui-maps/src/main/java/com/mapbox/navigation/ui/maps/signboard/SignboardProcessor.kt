package com.mapbox.navigation.ui.maps.signboard

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.common.HttpMethod
import com.mapbox.common.HttpRequest
import com.mapbox.common.HttpRequestError
import com.mapbox.common.HttpResponseData
import com.mapbox.common.UserAgentComponents
import com.mapbox.navigation.ui.utils.extension.getBannerComponents
import com.mapbox.navigation.ui.utils.internal.ifNonNull

internal object SignboardProcessor {

    private const val USER_AGENT_KEY = "User-Agent"
    private const val USER_AGENT_VALUE = "MapboxJava/"
    private const val CODE_200 = 200L
    private const val CODE_401 = 401L
    private const val CODE_404 = 404L

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
        val request = HttpRequest.Builder()
            .url(url)
            .body(byteArrayOf())
            .method(HttpMethod.GET)
            .headers(hashMapOf(Pair(USER_AGENT_KEY, USER_AGENT_VALUE)))
            .userAgentComponents(UserAgentComponents.Builder().build())
            .build()
        return SignboardResult.SignboardRequest(request)
    }

    private fun processResponse(
        response: Expected<HttpResponseData?, HttpRequestError?>
    ): SignboardResult {
        when {
            response.isValue -> {
                return response.value?.let { responseData ->
                    when (responseData.code) {
                        CODE_200 -> {
                            SignboardResult.Signboard.Success(responseData.data)
                        }
                        CODE_401 -> {
                            SignboardResult.Signboard.Failure(
                                "Your token cannot access this " +
                                    "resource, contact support"
                            )
                        }
                        CODE_404 -> {
                            SignboardResult.Signboard.Failure("Resource is missing")
                        }
                        else -> {
                            SignboardResult.Signboard.Failure("Unknown error")
                        }
                    }
                } ?: SignboardResult.Signboard.Empty("No data available")
            }
            response.isError -> {
                return SignboardResult.Signboard.Failure(response.error?.message)
            }
            else -> {
                return SignboardResult.Signboard.Failure(response.error?.message)
            }
        }
    }
}
