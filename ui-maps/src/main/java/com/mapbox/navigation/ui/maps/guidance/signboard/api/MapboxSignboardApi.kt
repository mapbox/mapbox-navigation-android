package com.mapbox.navigation.ui.maps.guidance.signboard.api

import android.content.Context
import android.net.Uri
import androidx.annotation.AnyThread
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.bindgen.DataRef
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.common.MapboxServices
import com.mapbox.common.ResourceLoadError
import com.mapbox.common.ResourceLoadResult
import com.mapbox.navigation.base.internal.utils.MapboxOptionsUtil
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.base.util.internal.resource.ResourceLoaderFactory
import com.mapbox.navigation.ui.maps.guidance.signboard.SignboardAction
import com.mapbox.navigation.ui.maps.guidance.signboard.SignboardProcessor
import com.mapbox.navigation.ui.maps.guidance.signboard.SignboardResult
import com.mapbox.navigation.ui.maps.guidance.signboard.SignboardResult.SignboardBitmap
import com.mapbox.navigation.ui.maps.guidance.signboard.model.MapboxSignboardOptions
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardError
import com.mapbox.navigation.ui.maps.guidance.signboard.model.SignboardValue
import com.mapbox.navigation.ui.utils.internal.resource.load
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Mapbox Signboard Api allows you to generate signboard for select maneuvers.
 * By default uses the [MapboxExternalFileResolver].
 * Check out [MapboxExternalFileResolver.resolveFont] to know how css style's fonts are treated
 */
class MapboxSignboardApi @JvmOverloads constructor(
    private val parser: SvgToBitmapParser,
    private val options: MapboxSignboardOptions = MapboxSignboardOptions.Builder().build(),
) {

    private companion object {
        private const val KEY: String = "access_token"
    }

    constructor(
        applicationContext: Context,
        options: MapboxSignboardOptions = MapboxSignboardOptions.Builder().build(),
    ) : this(
        MapboxSvgToBitmapParser(MapboxExternalFileResolver(applicationContext.assets)),
        options,
    )

    private val mainJobController by lazy { InternalJobControlFactory.createMainScopeJobControl() }
    private val calculationJobController by lazy {
        InternalJobControlFactory.createDefaultScopeJobControl()
    }
    private val resourceLoader by lazy { ResourceLoaderFactory.getInstance() }

    /**
     * The method takes in [BannerInstructions] and generates a signboard based on the presence of
     * [BannerComponents] of type [BannerComponents.GUIDANCE_VIEW] and subType [BannerComponents.SIGNBOARD]
     * @param instructions object representing [BannerInstructions]
     * @param consumer informs about the state of signboard.
     */
    @AnyThread
    fun generateSignboard(
        instructions: BannerInstructions,
        consumer: MapboxNavigationConsumer<Expected<SignboardError, SignboardValue>>,
    ) {
        calculationJobController.scope.launch {
            val action = SignboardAction.CheckSignboardAvailability(instructions)
            when (val result = SignboardProcessor.process(action)) {
                is SignboardResult.SignboardAvailable -> makeSignboardRequest(result, consumer)

                is SignboardResult.SignboardUnavailable -> {
                    notifyConsumerError(consumer, "No signboard available for current maneuver.")
                }

                else -> notifyConsumerError(consumer, "Inappropriate $result emitted for $action.")
            }
        }
    }

    /**
     * Invoke the method to cancel all ongoing requests to generate a signboard.
     */
    fun cancelAll() {
        calculationJobController.job.cancelChildren()
        mainJobController.job.cancelChildren()
    }

    private suspend fun makeSignboardRequest(
        result: SignboardResult.SignboardAvailable,
        consumer: MapboxNavigationConsumer<Expected<SignboardError, SignboardValue>>,
    ) {
        val accessToken = MapboxOptionsUtil.getTokenForService(MapboxServices.DIRECTIONS)
        val url = Uri.parse(result.signboardUrl).buildUpon()
            .appendQueryParameter(KEY, accessToken)
            .build().toString()
        val prepareRequestAction = SignboardAction.PrepareSignboardRequest(url)
        val signboardRequest = SignboardProcessor.process(prepareRequestAction)
        val loadRequest = (signboardRequest as SignboardResult.SignboardRequest).request
        val loadResult = resourceLoader.load(loadRequest)
        onSignboardResponse(loadResult, consumer)
    }

    private suspend fun onSignboardResponse(
        loadResult: Expected<ResourceLoadError, ResourceLoadResult>,
        consumer: MapboxNavigationConsumer<Expected<SignboardError, SignboardValue>>,
    ) {
        val action = SignboardAction.ProcessSignboardResponse(loadResult)
        when (val result = SignboardProcessor.process(action)) {
            is SignboardResult.SignboardSvg.Success -> {
                onSvgAvailable(result.data, consumer)
            }
            is SignboardResult.SignboardSvg.Failure -> {
                consumer.accept(
                    ExpectedFactory.createError(SignboardError(result.error, null)),
                )
            }
            is SignboardResult.SignboardSvg.Empty -> {
                notifyConsumerError(consumer, "No signboard available for current maneuver.")
            }
            else -> {
                notifyConsumerError(consumer, "Inappropriate $result emitted for $action.")
            }
        }
    }

    private suspend fun onSvgAvailable(
        svg: DataRef,
        consumer: MapboxNavigationConsumer<Expected<SignboardError, SignboardValue>>,
    ) {
        val action = SignboardAction.ParseSvgToBitmap(svg, parser, options)
        when (val result = SignboardProcessor.process(action)) {
            is SignboardBitmap.Success -> {
                notifyConsumer(
                    consumer,
                    ExpectedFactory.createValue(SignboardValue(result.signboard)),
                )
            }
            is SignboardBitmap.Failure -> notifyConsumerError(consumer, result.message)
            else -> notifyConsumerError(consumer, "Inappropriate $result emitted for $action.")
        }
    }

    /**
     * Notify the consumer with a [SignboardError] with the error message from the main thread.
     */
    private suspend fun notifyConsumerError(
        consumer: MapboxNavigationConsumer<Expected<SignboardError, SignboardValue>>,
        errorMessage: String,
    ) {
        notifyConsumer(consumer, ExpectedFactory.createError(SignboardError(errorMessage, null)))
    }

    /**
     * Notify the consumer with the result of the signboard generation from the main thread.
     */
    private suspend fun notifyConsumer(
        consumer: MapboxNavigationConsumer<Expected<SignboardError, SignboardValue>>,
        value: Expected<SignboardError, SignboardValue>,
    ) {
        withContext(mainJobController.scope.coroutineContext) {
            consumer.accept(value)
        }
    }
}
