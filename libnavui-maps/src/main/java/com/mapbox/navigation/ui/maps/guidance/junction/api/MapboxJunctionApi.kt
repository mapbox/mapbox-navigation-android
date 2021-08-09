package com.mapbox.navigation.ui.maps.guidance.junction.api

import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.HttpResponse
import com.mapbox.common.core.module.CommonSingletonModuleProvider
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionAction
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionProcessor
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionResult
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionError
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue
import com.mapbox.navigation.ui.maps.guidance.junction.model.MapboxJunctionRequest
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.launch

/**
 * Mapbox Junction Api allows you to generate junction for select maneuvers.
 * @property accessToken String
 */
class MapboxJunctionApi(
    private val accessToken: String
) {

    private companion object {
        private const val ACCESS_TOKEN = "&access_token="
    }

    private val mainJobController: JobControl by lazy { ThreadController.getMainScopeAndRootJob() }
    private val requestList: MutableList<MapboxJunctionRequest> = mutableListOf()

    /**
     * The method takes in [BannerInstructions] and generates a junction based on the presence of
     * [BannerComponents] of type [BannerComponents.GUIDANCE_VIEW] and subType [BannerComponents.JCT]
     * @param instructions object representing [BannerInstructions]
     * @param consumer informs about the state of junction.
     */
    fun generateJunction(
        instructions: BannerInstructions,
        consumer: MapboxNavigationConsumer<Expected<JunctionError, JunctionValue>>
    ) {
        val action = JunctionAction.CheckJunctionAvailability(instructions)
        when (val result = JunctionProcessor.process(action)) {
            is JunctionResult.JunctionAvailable -> {
                makeJunctionRequest(result, consumer)
            }
            is JunctionResult.JunctionUnavailable -> {
                consumer.accept(
                    ExpectedFactory.createError(
                        JunctionError("No junction available for current maneuver.", null)
                    )
                )
            }
            else -> {
                consumer.accept(
                    ExpectedFactory.createError(
                        JunctionError("Inappropriate $result emitted for $action.", null)
                    )
                )
            }
        }
    }

    /**
     * Invoke the method to cancel all ongoing requests to generate a junction.
     */
    fun cancelAll() {
        requestList.forEach {
            CommonSingletonModuleProvider.httpServiceInstance.cancelRequest(it.requestId) {
            }
        }
        requestList.clear()
    }

    private fun makeJunctionRequest(
        result: JunctionResult.JunctionAvailable,
        consumer: MapboxNavigationConsumer<Expected<JunctionError, JunctionValue>>
    ) {
        val requestAction = JunctionAction.PrepareJunctionRequest(
            result.junctionUrl.plus(ACCESS_TOKEN.plus(accessToken))
        )
        val junctionRequest = JunctionProcessor.process(requestAction)
        val httpRequest = (junctionRequest as JunctionResult.JunctionRequest).request
        val requestId = CommonSingletonModuleProvider.httpServiceInstance.request(
            httpRequest
        ) { httpResponse ->
            mainJobController.scope.launch {
                onJunctionResponse(httpResponse, consumer)
            }
        }
        requestList.add(MapboxJunctionRequest(requestId, httpRequest))
    }

    private fun onJunctionResponse(
        httpResponse: HttpResponse,
        consumer: MapboxNavigationConsumer<Expected<JunctionError, JunctionValue>>
    ) {
        val filteredList = requestList.filter {
            it.httpRequest != httpResponse.request
        }
        requestList.clear()
        requestList.addAll(filteredList)
        val response = httpResponse.result
        val action = JunctionAction.ProcessJunctionResponse(response)
        when (val result = JunctionProcessor.process(action)) {
            is JunctionResult.JunctionRaster.Success -> {
                onJunctionAvailable(result.data, consumer)
            }
            is JunctionResult.JunctionRaster.Failure -> {
                consumer.accept(
                    ExpectedFactory.createError(JunctionError(result.error, null))
                )
            }
            is JunctionResult.JunctionRaster.Empty -> {
                consumer.accept(
                    ExpectedFactory.createError(
                        JunctionError("No junction available for current maneuver.", null)
                    )
                )
            }
            else -> {
                consumer.accept(
                    ExpectedFactory.createError(
                        JunctionError("Inappropriate $result emitted for $action.", null)
                    )
                )
            }
        }
    }

    private fun onJunctionAvailable(
        data: ByteArray,
        consumer: MapboxNavigationConsumer<Expected<JunctionError, JunctionValue>>
    ) {
        val action = JunctionAction.ParseRasterToBitmap(data)
        when (val result = JunctionProcessor.process(action)) {
            is JunctionResult.JunctionBitmap.Success -> {
                consumer.accept(ExpectedFactory.createValue(JunctionValue(result.junction)))
            }
            is JunctionResult.JunctionBitmap.Failure -> {
                consumer.accept(ExpectedFactory.createError(JunctionError(result.message, null)))
            }
            else -> {
                consumer.accept(
                    ExpectedFactory.createError(
                        JunctionError("Inappropriate $result emitted for $action.", null)
                    )
                )
            }
        }
    }
}
