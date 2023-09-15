package com.mapbox.navigation.ui.maps.guidance.junction.api

import android.net.Uri
import androidx.annotation.UiThread
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionAction
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionProcessor
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionResult
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionError
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoaderFactory
import com.mapbox.navigation.ui.utils.internal.resource.load
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Mapbox Junction Api allows you to generate junction for select maneuvers.
 * @property accessToken String
 */
@UiThread
class MapboxJunctionApi(
    private val accessToken: String
) {

    private companion object {
        private const val ACCESS_TOKEN = "access_token"
    }

    private val mainJobController by lazy { InternalJobControlFactory.createMainScopeJobControl() }
    private val resourceLoader by lazy { ResourceLoaderFactory.getInstance() }

    /**
     * The method takes in [BannerInstructions] and generates a junction based on the presence of
     * [BannerComponents] of type [BannerComponents.GUIDANCE_VIEW] and subType
     * [BannerComponents.JCT],
     * [BannerComponents.SAPA],
     * [BannerComponents.CITYREAL],
     * [BannerComponents.AFTERTOLL],
     * [BannerComponents.SIGNBOARD],
     * [BannerComponents.TOLLBRANCH],
     * [BannerComponents.EXPRESSWAY_EXIT],
     * [BannerComponents.EXPRESSWAY_ENTRANCE]
     *
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
        mainJobController.job.cancelChildren()
    }

    private fun makeJunctionRequest(
        result: JunctionResult.JunctionAvailable,
        consumer: MapboxNavigationConsumer<Expected<JunctionError, JunctionValue>>
    ) {
        val url = Uri.parse(result.junctionUrl).buildUpon().apply {
            appendQueryParameter(ACCESS_TOKEN, accessToken)
        }.build().toString()
        val requestAction = JunctionAction.PrepareJunctionRequest(url)
        val junctionRequest = JunctionProcessor.process(requestAction)
        val loadRequest = (junctionRequest as JunctionResult.JunctionRequest).request
        mainJobController.scope.launch {
            val loadResult = resourceLoader.load(loadRequest)
            onJunctionResponse(loadResult, consumer)
        }
    }

    private fun onJunctionResponse(
        loadResult: Expected<ResourceLoadError, ResourceLoadResult>,
        consumer: MapboxNavigationConsumer<Expected<JunctionError, JunctionValue>>
    ) {
        val action = JunctionAction.ProcessJunctionResponse(loadResult)
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
