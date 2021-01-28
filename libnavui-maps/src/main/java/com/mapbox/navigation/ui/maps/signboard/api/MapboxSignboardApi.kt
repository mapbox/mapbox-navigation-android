package com.mapbox.navigation.ui.maps.signboard.api

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.common.core.module.CommonSingletonModuleProvider
import com.mapbox.navigation.ui.base.api.signboard.SignboardApi
import com.mapbox.navigation.ui.base.api.signboard.SignboardReadyCallback
import com.mapbox.navigation.ui.base.model.signboard.SignboardState
import com.mapbox.navigation.ui.maps.signboard.SignboardAction
import com.mapbox.navigation.ui.maps.signboard.SignboardProcessor
import com.mapbox.navigation.ui.maps.signboard.SignboardResult
import com.mapbox.navigation.ui.maps.signboard.model.MapboxSignboardRequest
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.launch

/**
 * Implementation of [SignboardApi] allowing you to generate signboard for select maneuvers.
 * @property accessToken String
 */
class MapboxSignboardApi(
    private val accessToken: String
) : SignboardApi {

    companion object {
        private const val ACCESS_TOKEN = "?access_token="
    }

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private val requestList: MutableList<MapboxSignboardRequest> = mutableListOf()

    /**
     * The method takes in [BannerInstructions] and generates a signboard based on the presence of
     * [BannerComponents] of type [BannerComponents.GUIDANCE_VIEW] and subType [BannerComponents.SIGNBOARD]
     * @param instructions object representing [BannerInstructions]
     * @param callback informs about the state of signboard.
     */
    override fun generateSignboard(
        instructions: BannerInstructions,
        callback: SignboardReadyCallback
    ) {
        val action = SignboardAction.CheckSignboardAvailability(instructions)
        val result = SignboardProcessor.process(action)
        when (result) {
            is SignboardResult.SignboardUnavailable -> {
                callback.onFailure(SignboardState.SignboardFailure.SignboardUnavailable)
            }
            is SignboardResult.SignboardAvailable -> {
                val signboardRequest = SignboardProcessor.process(
                    SignboardAction.PrepareSignboardRequest(
                        result.signboardUrl.plus(ACCESS_TOKEN.plus(accessToken))
                    )
                )
                val httpRequest = (signboardRequest as SignboardResult.SignboardRequest).request
                val requestId = CommonSingletonModuleProvider.httpServiceInstance.request(
                    httpRequest
                ) { httpResponse ->
                    mainJobController.scope.launch {
                        val filteredList = requestList.filter {
                            it.httpRequest != httpResponse.request
                        }
                        requestList.clear()
                        requestList.addAll(filteredList)
                        val response = httpResponse.result
                        val signboardAction = SignboardAction.ProcessSignboardResponse(response)
                        val res = SignboardProcessor.process(signboardAction)
                        when (res) {
                            is SignboardResult.Signboard.Success -> {
                                callback.onSignboardReady(SignboardState.SignboardReady(res.data))
                            }
                            is SignboardResult.Signboard.Failure -> {
                                callback.onFailure(
                                    SignboardState.SignboardFailure.SignboardError(res.error)
                                )
                            }
                            is SignboardResult.Signboard.Empty -> {
                                callback.onFailure(
                                    SignboardState.SignboardFailure.SignboardUnavailable
                                )
                            }
                            else -> {
                                callback.onFailure(
                                    SignboardState.SignboardFailure.SignboardError(
                                        "Inappropriate result $result emitted for " +
                                            "$action processed."
                                    )
                                )
                            }
                        }
                    }
                }
                requestList.add(MapboxSignboardRequest(requestId, httpRequest))
            }
            else -> {
                callback.onFailure(
                    SignboardState.SignboardFailure.SignboardError(
                        "Inappropriate result $result emitted for " +
                            "$action processed."
                    )
                )
            }
        }
    }

    /**
     * Invoke the method to cancel all ongoing requests to generate a signboard.
     */
    override fun cancelAll() {
        requestList.forEach {
            CommonSingletonModuleProvider.httpServiceInstance.cancelRequest(it.requestId) {
            }
        }
        requestList.clear()
    }
}
