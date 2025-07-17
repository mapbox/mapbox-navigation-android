package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.bindgen.Expected
import com.mapbox.maps.Style
import com.mapbox.maps.StylePropertyValue
import com.mapbox.maps.StylePropertyValueKind
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.line.api.LightRouteLineValueProvider
import com.mapbox.navigation.ui.maps.route.line.api.LineGradientCommandApplier
import com.mapbox.navigation.ui.maps.route.line.api.LineTrimCommandApplier
import com.mapbox.navigation.ui.maps.route.line.api.RouteLineCommandApplier
import com.mapbox.navigation.ui.maps.route.line.api.RouteLineValueCommandHolder
import com.mapbox.navigation.ui.maps.route.line.api.unsupportedRouteLineCommandHolder
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import kotlin.coroutines.CoroutineContext

internal fun RouteLineEventData.toRouteLineData(): RouteLineData {
    return RouteLineData(
        featureCollection,
        dynamicData?.toRouteLineDynamicData(),
    )
}

internal fun RouteLineDynamicEventData.toRouteLineDynamicData(): RouteLineDynamicData {
    return RouteLineDynamicData(
        baseExpressionData.toHolder(),
        casingExpressionData.toHolder(),
        trafficExpressionData?.toHolder(),
        restrictedSectionExpressionData?.toHolder(),
        trimOffset,
        trailExpressionData?.toHolder(),
        trailCasingExpressionData?.toHolder(),
    )
}

internal suspend fun <V> Expected<RouteLineError, V>.toInput(
    toEventValue: suspend V.() -> RouteLineViewExpectedInput,
): RouteLineViewExpectedInput {
    return if (isError) {
        error!!.toEventError()
    } else {
        value!!.toEventValue()
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal fun MapboxRouteLineViewOptions.toData(): RouteLineViewOptionsData {
    return RouteLineViewOptionsData(
        routeLineColorResources,
        scaleExpressions,
        restrictedRoadDashArray,
        restrictedRoadOpacity,
        restrictedRoadLineWidth,
        displaySoftGradientForTraffic,
        softGradientTransition,
        originIconId,
        destinationIconId,
        waypointLayerIconOffset,
        waypointLayerIconAnchor,
        iconPitchAlignment,
        displayRestrictedRoadSections,
        routeLineBelowLayerId,
        tolerance,
        shareLineGeometrySources,
        lineDepthOcclusionFactor,
        slotName,
        fadeOnHighZoomsConfig,
        routeLineBlurWidth,
        routeLineBlurEnabled,
        applyTrafficColorsToRouteLineBlur,
        routeLineBlurOpacity,
    )
}

internal fun RouteLineClearValue.toEventValue(): RouteLineViewRenderRouteLineClearDataValue {
    return RouteLineViewRenderRouteLineClearDataValue(
        primaryRouteSource,
        alternativeRoutesSources,
        waypointsSource,
    )
}

internal suspend fun RouteSetValue.toEventValue(
    workerCoroutineContext: CoroutineContext,
    data: RouteLineViewOptionsData,
): RouteLineViewRenderRouteDrawDataInputValue {
    return RouteLineViewRenderRouteDrawDataInputValue(
        primaryRouteLineData.toData(workerCoroutineContext, data),
        alternativeRouteLinesData.map { it.toData(workerCoroutineContext, data) },
        waypointsSource,
        routeLineMaskingLayerDynamicData?.toData(workerCoroutineContext, data),
    )
}

internal suspend fun RouteLineUpdateValue.toEventValue(
    workerCoroutineContext: CoroutineContext,
    data: RouteLineViewOptionsData,
): RouteLineViewRenderRouteLineUpdateDataValue {
    return RouteLineViewRenderRouteLineUpdateDataValue(
        primaryRouteLineDynamicData?.toData(workerCoroutineContext, data),
        alternativeRouteLinesDynamicData.map { it.toData(workerCoroutineContext, data) },
        routeLineMaskingLayerDynamicData?.toData(workerCoroutineContext, data),
    )
}

private fun RouteLineError.toEventError(): RouteLineViewDataError {
    return RouteLineViewDataError(errorMessage)
}

private suspend fun RouteLineData.toData(
    workerCoroutineContext: CoroutineContext,
    data: RouteLineViewOptionsData,
): RouteLineEventData {
    return RouteLineEventData(
        featureCollection,
        dynamicData?.toData(workerCoroutineContext, data),
    )
}

private suspend fun RouteLineDynamicData.toData(
    workerCoroutineContext: CoroutineContext,
    data: RouteLineViewOptionsData,
): RouteLineDynamicEventData {
    return RouteLineDynamicEventData(
        baseExpressionCommandHolder.toRouteLineExpressionEventData(workerCoroutineContext, data),
        casingExpressionCommandHolder.toRouteLineExpressionEventData(workerCoroutineContext, data),
        trafficExpressionCommandHolder?.toRouteLineExpressionEventData(
            workerCoroutineContext,
            data,
        ),
        restrictedSectionExpressionCommandHolder?.toRouteLineExpressionEventData(
            workerCoroutineContext,
            data,
        ),
        trimOffset,
        trailExpressionCommandHolder?.toRouteLineExpressionEventData(workerCoroutineContext, data),
        trailCasingExpressionCommandHolder?.toRouteLineExpressionEventData(
            workerCoroutineContext,
            data,
        ),
        blurExpressionCommandHolder?.toRouteLineExpressionEventData(workerCoroutineContext, data),
    )
}

private fun RouteLineExpressionEventData.toHolder(): RouteLineValueCommandHolder {
    return when (this) {
        is RouteLineNoOpExpressionEventData -> unsupportedRouteLineCommandHolder()
        is RouteLineProviderBasedExpressionEventData -> {
            when (property) {
                // deprecated
                "line-trim-offset" -> {
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider {
                            StylePropertyValue(expression!!, StylePropertyValueKind.EXPRESSION)
                        },
                        object : RouteLineCommandApplier<StylePropertyValue>() {
                            override fun applyCommand(
                                style: Style,
                                layerId: String,
                                command: StylePropertyValue,
                            ) {
                                style.setStyleLayerProperty(layerId, getProperty(), command.value)
                            }

                            override fun getProperty(): String {
                                return "line-trim-offset"
                            }
                        },
                    )
                }

                // deprecated
                "line-trim-end" -> {
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider { value!! },
                        object : RouteLineCommandApplier<StylePropertyValue>() {
                            override fun applyCommand(
                                style: Style,
                                layerId: String,
                                command: StylePropertyValue,
                            ) {
                                style.setStyleLayerProperty(layerId, getProperty(), command.value)
                            }

                            override fun getProperty(): String {
                                return "line-trim-end"
                            }
                        },
                    )
                }

                "line-trim-start" -> {
                    RouteLineValueCommandHolder(
                        LightRouteLineValueProvider { value!! },
                        LineTrimCommandApplier(),
                    )
                }

                "line-gradient" -> {
                    RouteLineValueCommandHolder(
                        if (value != null) {
                            LightRouteLineValueProvider { value!! }
                        } else {
                            LightRouteLineValueProvider {
                                StylePropertyValue(expression!!, StylePropertyValueKind.EXPRESSION)
                            }
                        },
                        LineGradientCommandApplier(),
                    )
                }

                else -> throw IllegalStateException()
            }
        }

        else -> throw IllegalStateException()
    }
}
