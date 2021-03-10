package com.mapbox.navigation.ui.maps.signboard.api

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.common.HttpResponse
import com.mapbox.common.core.module.CommonSingletonModuleProvider
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.signboard.SignboardAction
import com.mapbox.navigation.ui.maps.signboard.SignboardProcessor
import com.mapbox.navigation.ui.maps.signboard.SignboardResult
import com.mapbox.navigation.ui.maps.signboard.model.MapboxSignboardRequest
import com.mapbox.navigation.ui.maps.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.signboard.model.SignboardValue
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.launch

/**
 * Implementation of [SignboardApi] allowing you to generate signboard for select maneuvers.
 * @property accessToken String
 */
class MapboxSignboardApi(
    private val accessToken: String
) {

    companion object {
        private const val ACCESS_TOKEN = "?access_token="
    }

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private val requestList: MutableList<MapboxSignboardRequest> = mutableListOf()

    /**
     * The method takes in [BannerInstructions] and generates a signboard based on the presence of
     * [BannerComponents] of type [BannerComponents.GUIDANCE_VIEW] and subType [BannerComponents.SIGNBOARD]
     * @param instructions object representing [BannerInstructions]
     * @param consumer informs about the state of signboard.
     */
    fun generateSignboard(
        instructions: BannerInstructions,
        consumer: MapboxNavigationConsumer<Expected<SignboardValue, SignboardError>>
    ) {
        val action = SignboardAction.CheckSignboardAvailability(instructions)
        val result = SignboardProcessor.process(action)
        when (result) {
            is SignboardResult.SignboardAvailable -> {
                val requestAction = SignboardAction.PrepareSignboardRequest(
                    result.signboardUrl.plus(ACCESS_TOKEN.plus(accessToken))
                )
                val signboardRequest = SignboardProcessor.process(requestAction)
                val httpRequest = (signboardRequest as SignboardResult.SignboardRequest).request
                val requestId = CommonSingletonModuleProvider.httpServiceInstance.request(
                    httpRequest
                ) { httpResponse ->
                    mainJobController.scope.launch {
                        onSignboardResponse(httpResponse, consumer)
                    }
                }
                requestList.add(MapboxSignboardRequest(requestId, httpRequest))
            }
            is SignboardResult.SignboardUnavailable -> {
                consumer.accept(
                    Expected.Failure(
                        SignboardError("No signboard available for current maneuver.", null)
                    )
                )
            }
            else -> {
                consumer.accept(
                    Expected.Failure(
                        SignboardError("Inappropriate $result emitted for $action.", null)
                    )
                )
            }
        }
    }

    /**
     * Invoke the method to cancel all ongoing requests to generate a signboard.
     */
    fun cancelAll() {
        requestList.forEach {
            CommonSingletonModuleProvider.httpServiceInstance.cancelRequest(it.requestId) {
            }
        }
        requestList.clear()
    }

    private fun onSignboardResponse(
        httpResponse: HttpResponse,
        consumer: MapboxNavigationConsumer<Expected<SignboardValue, SignboardError>>
    ) {
        val filteredList = requestList.filter {
            it.httpRequest != httpResponse.request
        }
        requestList.clear()
        requestList.addAll(filteredList)
        val response = httpResponse.result
        val action = SignboardAction.ProcessSignboardResponse(response)
        val result = SignboardProcessor.process(action)
        when (result) {
            is SignboardResult.Signboard.Success -> {
                consumer.accept(Expected.Success(SignboardValue(result.data)))
            }
            is SignboardResult.Signboard.Failure -> {
                consumer.accept(
                    Expected.Failure(SignboardError(result.error, null))
                )
            }
            is SignboardResult.Signboard.Empty -> {
                consumer.accept(
                    Expected.Failure(
                        SignboardError("No signboard available for current maneuver.", null)
                    )
                )
            }
            else -> {
                consumer.accept(
                    Expected.Failure(
                        SignboardError("Inappropriate $result emitted for $action.", null)
                    )
                )
            }
        }
    }
}
