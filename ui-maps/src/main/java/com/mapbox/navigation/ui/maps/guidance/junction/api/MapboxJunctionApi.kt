package com.mapbox.navigation.ui.maps.guidance.junction.api

import android.net.Uri
import androidx.annotation.UiThread
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createError
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.common.MapboxServices
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.utils.MapboxOptionsUtil
import com.mapbox.navigation.base.internal.utils.toByteArray
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.base.util.internal.resource.ResourceLoaderFactory
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionAction
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionProcessor
import com.mapbox.navigation.ui.maps.guidance.junction.JunctionResult
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionError
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionValue
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionViewData
import com.mapbox.navigation.ui.maps.guidance.junction.model.JunctionViewFormat
import com.mapbox.navigation.ui.utils.internal.resource.load
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Mapbox Junction Api allows you to generate junction for select maneuvers.
 */
@UiThread
class MapboxJunctionApi {

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
        consumer: MapboxNavigationConsumer<Expected<JunctionError, JunctionValue>>,
    ) {
        val action = JunctionAction.CheckJunctionAvailability(instructions)
        when (val result = JunctionProcessor.process(action)) {
            is JunctionResult.JunctionAvailable -> {
                makeJunctionRequest(result.junctionUrl, format = null) { loadResult ->
                    processResponse(loadResult).onError {
                        consumer.accept(createError(it))
                    }.onValue { (dataRef, _) ->
                        consumer.accept(processRawJunctionView(dataRef))
                    }
                }
            }
            is JunctionResult.JunctionUnavailable -> {
                consumer.accept(
                    createJunctionErrorResult("No junction available for current maneuver."),
                )
            }
            else -> {
                consumer.accept(
                    createJunctionErrorResult("Inappropriate $result emitted for $action."),
                )
            }
        }
    }

    /**
     * The method takes in [BannerInstructions] and generates a junction based on the presence of
     * specific banner components.
     *
     * @param instructions object representing [BannerInstructions]
     * @param format the desired format for the junction view image (e.g., "png" or "svg").
     * @param consumer informs about the state of junction.
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun generateJunction(
        instructions: BannerInstructions,
        @JunctionViewFormat format: String,
        consumer: MapboxNavigationConsumer<Expected<JunctionError, JunctionViewData>>,
    ) {
        val action = JunctionAction.CheckJunctionAvailability(instructions)
        when (val result = JunctionProcessor.process(action)) {
            is JunctionResult.JunctionAvailable -> {
                makeJunctionRequest(result.junctionUrl, format = format) { loadResult ->
                    processResponse(loadResult).onError {
                        consumer.accept(createError(it))
                    }.onValue { (dataRef, contentType) ->
                        consumer.accept(
                            createValue(
                                JunctionViewData(
                                    dataRef.toByteArray(),
                                    JunctionViewData.ResponseFormat.createFromContentType(
                                        contentType,
                                    ),
                                ),
                            ),
                        )
                    }
                }
            }
            is JunctionResult.JunctionUnavailable -> {
                consumer.accept(
                    createJunctionErrorResult("No junction available for current maneuver."),
                )
            }
            else -> {
                consumer.accept(
                    createJunctionErrorResult("Inappropriate $result emitted for $action."),
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

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun makeJunctionRequest(
        junctionUrl: String,
        @JunctionViewFormat format: String?,
        callback: (Expected<ResourceLoadError, ResourceLoadResult>) -> Unit,
    ) {
        val accessToken = MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS)
        val url = Uri.parse(junctionUrl).buildUpon()
            .appendQueryParameter(ACCESS_TOKEN, accessToken)
            .apply {
                // For backwards compatibility we should not pass format parameter
                // if request is made from a function which doesn't accept format
                if (format != null) {
                    appendQueryParameter(IMAGE_FORMAT, format)
                }
            }
            .build().toString()

        val requestAction = JunctionAction.PrepareJunctionRequest(url)
        val junctionRequest = JunctionProcessor.process(requestAction)
        val loadRequest = (junctionRequest as JunctionResult.JunctionRequest).request
        mainJobController.scope.launch {
            val loadResult = resourceLoader.load(loadRequest)
            callback(loadResult)
        }
    }

    private fun processResponse(
        loadResult: Expected<ResourceLoadError, ResourceLoadResult>,
    ): Expected<JunctionError, Pair<DataRef, String>> {
        val action = JunctionAction.ProcessJunctionResponse(loadResult)
        return when (val result = JunctionProcessor.process(action)) {
            is JunctionResult.JunctionRaster.Success -> {
                createValue(result.dataRef to result.contentType)
            }
            is JunctionResult.JunctionRaster.Failure -> {
                createJunctionErrorResult(result.error)
            }
            is JunctionResult.JunctionRaster.Empty -> {
                createJunctionErrorResult("No junction available for current maneuver.")
            }
            else -> {
                createJunctionErrorResult("Inappropriate $result emitted for $action.")
            }
        }
    }

    private fun processRawJunctionView(data: DataRef): Expected<JunctionError, JunctionValue> {
        val action = JunctionAction.ParseRasterToBitmap(data)
        return when (val result = JunctionProcessor.process(action)) {
            is JunctionResult.JunctionBitmap.Success -> {
                createValue(JunctionValue(result.junction))
            }
            is JunctionResult.JunctionBitmap.Failure -> {
                createJunctionErrorResult(result.message)
            }
            else -> {
                createJunctionErrorResult("Inappropriate $result emitted for $action.")
            }
        }
    }

    private companion object {

        private const val ACCESS_TOKEN = "access_token"
        private const val IMAGE_FORMAT = "image_format"

        private fun <R> createJunctionErrorResult(
            errorMessage: String?,
            throwable: Throwable? = null,
        ): Expected<JunctionError, R> {
            return createError(JunctionError(errorMessage, throwable))
        }
    }
}
