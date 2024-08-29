package com.mapbox.navigation.ui.maps.guidance.restarea.api

import android.net.Uri
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.MapboxServices
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.navigation.base.internal.utils.MapboxOptionsUtil
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.roadobject.reststop.RestStop
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.guidance.restarea.RestAreaAction
import com.mapbox.navigation.ui.maps.guidance.restarea.RestAreaProcessor
import com.mapbox.navigation.ui.maps.guidance.restarea.RestAreaResult
import com.mapbox.navigation.ui.maps.guidance.restarea.model.MapboxRestAreaOptions
import com.mapbox.navigation.ui.maps.guidance.restarea.model.RestAreaGuideMapError
import com.mapbox.navigation.ui.maps.guidance.restarea.model.RestAreaGuideMapValue
import com.mapbox.navigation.ui.utils.internal.resource.ResourceLoaderFactory
import com.mapbox.navigation.ui.utils.internal.resource.load
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

/**
 * Mapbox Rest Area Api allows you to generate service area and parking area information
 * for select maneuvers.
 * @property options MapboxRestAreaOptions
 */
class MapboxRestAreaApi @JvmOverloads constructor(
    val options: MapboxRestAreaOptions = MapboxRestAreaOptions.Builder().build(),
) {

    private companion object {
        private const val KEY: String = "access_token"
    }

    private val mainJobController by lazy { InternalJobControlFactory.createMainScopeJobControl() }
    private val resourceLoader by lazy { ResourceLoaderFactory.getInstance() }

    /**
     * The method takes in [BannerInstructions] and generates rest area guide map based on the presence of
     * [BannerComponents] of type [BannerComponents.GUIDANCE_VIEW] and subType [BannerComponents.SAPAGUIDEMAP]
     * @param instructions object representing [BannerInstructions]
     * @param consumer informs about the state of sapa guide map.
     */
    fun generateRestAreaGuideMap(
        instructions: BannerInstructions,
        consumer: MapboxNavigationConsumer<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>,
    ) {
        val action = RestAreaAction.CheckRestAreaMapAvailability(instructions)
        when (val result = RestAreaProcessor.process(action)) {
            is RestAreaResult.RestAreaMapAvailable -> {
                requestGuideMap(result, consumer)
            }
            is RestAreaResult.RestAreaMapUnavailable -> {
                onError(
                    message = "No service/parking area guide map available.",
                    consumer = consumer,
                )
            }
            else -> {
                onError(
                    message = "Inappropriate $result emitted for $action.",
                    consumer = consumer,
                )
            }
        }
    }

    /**
     * The method takes in [RouteProgress] and generates rest area guide map based on the presence
     * of [RestStop.guideMapUri].
     * @param routeProgress object representing [RouteProgress]
     * @param consumer informs about the state of sapa guide map.
     */
    fun generateUpcomingRestAreaGuideMap(
        routeProgress: RouteProgress,
        consumer: MapboxNavigationConsumer<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>,
    ) {
        val action = RestAreaAction.CheckUpcomingRestStop(routeProgress)
        when (val result = RestAreaProcessor.process(action)) {
            is RestAreaResult.RestAreaMapAvailable -> {
                requestGuideMap(result, consumer)
            }
            is RestAreaResult.RestAreaMapUnavailable -> {
                onError(
                    message = "No service/parking area guide map available.",
                    consumer = consumer,
                )
            }
            else -> {
                onError(
                    message = "Inappropriate $result emitted for $action.",
                    consumer = consumer,
                )
            }
        }
    }

    /**
     * Invoke the method to cancel all ongoing requests to generate a sapa guide map.
     */
    fun cancelAll() {
        mainJobController.job.cancelChildren()
    }

    private fun requestGuideMap(
        result: RestAreaResult.RestAreaMapAvailable,
        consumer: MapboxNavigationConsumer<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>,
    ) {
        val accessToken = MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS)
        val url = Uri.parse(result.sapaMapUrl).buildUpon()
            .appendQueryParameter(KEY, accessToken)
            .build().toString()
        val requestAction = RestAreaAction.PrepareRestAreaMapRequest(url)
        val signboardRequest = RestAreaProcessor.process(requestAction)
        val loadRequest = (signboardRequest as RestAreaResult.RestAreaMapRequest).request
        mainJobController.scope.launch {
            val loadResult = resourceLoader.load(loadRequest)
            onGuideMapResponse(loadResult, consumer)
        }
    }

    private fun onGuideMapResponse(
        loadResult: Expected<ResourceLoadError, ResourceLoadResult>,
        consumer: MapboxNavigationConsumer<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>,
    ) {
        val action = RestAreaAction.ProcessRestAreaMapResponse(loadResult)
        when (val result = RestAreaProcessor.process(action)) {
            is RestAreaResult.RestAreaMapSvg.Success -> {
                onSvgAvailable(result.data, consumer)
            }
            is RestAreaResult.RestAreaMapSvg.Failure -> {
                onError(message = result.error, consumer = consumer)
            }
            is RestAreaResult.RestAreaMapSvg.Empty -> {
                onError(
                    message = "No service/parking area guide map available.",
                    consumer = consumer,
                )
            }
            else -> {
                onError(
                    message = "Inappropriate $result emitted for $action.",
                    consumer = consumer,
                )
            }
        }
    }

    private fun onSvgAvailable(
        svg: DataRef,
        consumer: MapboxNavigationConsumer<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>,
    ) {
        val action = RestAreaAction.ParseSvgToBitmap(svg, options)
        when (val result = RestAreaProcessor.process(action)) {
            is RestAreaResult.RestAreaBitmap.Success -> {
                consumer.accept(
                    ExpectedFactory.createValue(
                        RestAreaGuideMapValue(result.restAreaGuideMap),
                    ),
                )
            }
            is RestAreaResult.RestAreaBitmap.Failure -> {
                onError(message = result.error, consumer = consumer)
            }
            else -> {
                onError(
                    message = "Inappropriate $result emitted for $action.",
                    consumer = consumer,
                )
            }
        }
    }

    private fun onError(
        message: String?,
        consumer: MapboxNavigationConsumer<Expected<RestAreaGuideMapError, RestAreaGuideMapValue>>,
    ) {
        consumer.accept(
            ExpectedFactory.createError(
                RestAreaGuideMapError(
                    message = message,
                    throwable = null,
                ),
            ),
        )
    }
}
