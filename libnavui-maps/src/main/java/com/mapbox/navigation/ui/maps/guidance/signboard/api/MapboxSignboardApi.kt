package com.mapbox.navigation.ui.maps.guidance.signboard.api

import android.content.Context
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.HttpResponse
import com.mapbox.common.core.module.CommonSingletonModuleProvider
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.guidance.signboard.SignboardAction
import com.mapbox.navigation.ui.maps.guidance.signboard.SignboardProcessor
import com.mapbox.navigation.ui.maps.guidance.signboard.SignboardResult
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardOptions
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardRequest
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardValue
import com.mapbox.navigation.utils.internal.JobControl
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.launch

/**
 * Mapbox Signboard Api allows you to generate signboard for select maneuvers.
 * By default uses the [MapboxExternalFileResolver].
 * Check out [MapboxExternalFileResolver.resolveFont] to know how css style's fonts are treated
 * @property accessToken String
 */
class MapboxSignboardApi @JvmOverloads constructor(
    private val accessToken: String,
    private val parser: SvgToBitmapParser,
    private val options: MapboxSignboardOptions = MapboxSignboardOptions.Builder().build()
) {

    constructor(
        accessToken: String,
        applicationContext: Context,
        options: MapboxSignboardOptions = MapboxSignboardOptions.Builder().build()
    ) : this(
        accessToken,
        MapboxSvgToBitmapParser(MapboxExternalFileResolver(applicationContext.assets)),
        options
    )

    private companion object {
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
        consumer: MapboxNavigationConsumer<Expected<SignboardError, SignboardValue>>
    ) {
        val action = SignboardAction.CheckSignboardAvailability(instructions)
        when (val result = SignboardProcessor.process(action)) {
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
                    ExpectedFactory.createError(
                        SignboardError("No signboard available for current maneuver.", null)
                    )
                )
            }
            else -> {
                consumer.accept(
                    ExpectedFactory.createError(
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
        consumer: MapboxNavigationConsumer<Expected<SignboardError, SignboardValue>>
    ) {
        val filteredList = requestList.filter {
            it.httpRequest != httpResponse.request
        }
        requestList.clear()
        requestList.addAll(filteredList)
        val response = httpResponse.result
        val action = SignboardAction.ProcessSignboardResponse(response)
        when (val result = SignboardProcessor.process(action)) {
            is SignboardResult.SignboardSvg.Success -> {
                onSvgAvailable(result.data, consumer)
            }
            is SignboardResult.SignboardSvg.Failure -> {
                consumer.accept(
                    ExpectedFactory.createError(SignboardError(result.error, null))
                )
            }
            is SignboardResult.SignboardSvg.Empty -> {
                consumer.accept(
                    ExpectedFactory.createError(
                        SignboardError("No signboard available for current maneuver.", null)
                    )
                )
            }
            else -> {
                consumer.accept(
                    ExpectedFactory.createError(
                        SignboardError("Inappropriate $result emitted for $action.", null)
                    )
                )
            }
        }
    }

    private fun onSvgAvailable(
        svg: ByteArray,
        consumer: MapboxNavigationConsumer<Expected<SignboardError, SignboardValue>>
    ) {
        val action = SignboardAction.ParseSvgToBitmap(svg, parser, options)
        when (val result = SignboardProcessor.process(action)) {
            is SignboardResult.SignboardBitmap.Success -> {
                consumer.accept(ExpectedFactory.createValue(SignboardValue(result.signboard)))
            }
            is SignboardResult.SignboardBitmap.Failure -> {
                consumer.accept(ExpectedFactory.createError(SignboardError(result.message, null)))
            }
            else -> {
                consumer.accept(
                    ExpectedFactory.createError(
                        SignboardError("Inappropriate $result emitted for $action.", null)
                    )
                )
            }
        }
    }
}
