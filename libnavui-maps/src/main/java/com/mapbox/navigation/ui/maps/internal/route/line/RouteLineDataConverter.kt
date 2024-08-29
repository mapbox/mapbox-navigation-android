package com.mapbox.navigation.ui.maps.internal.route.line

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.route.line.api.LightRouteLineExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.api.LineGradientCommandApplier
import com.mapbox.navigation.ui.maps.route.line.api.LineTrimCommandApplier
import com.mapbox.navigation.ui.maps.route.line.api.RouteLineExpressionCommandHolder
import com.mapbox.navigation.ui.maps.route.line.api.unsupportedRouteLineCommandHolder
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue

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
    data: RouteLineViewOptionsData,
): RouteLineViewRenderRouteDrawDataInputValue {
    return RouteLineViewRenderRouteDrawDataInputValue(
        primaryRouteLineData.toData(data),
        alternativeRouteLinesData.map { it.toData(data) },
        waypointsSource,
        routeLineMaskingLayerDynamicData?.toData(data),
    )
}

internal suspend fun RouteLineUpdateValue.toEventValue(
    data: RouteLineViewOptionsData,
): RouteLineViewRenderRouteLineUpdateDataValue {
    return RouteLineViewRenderRouteLineUpdateDataValue(
        primaryRouteLineDynamicData?.toData(data),
        alternativeRouteLinesDynamicData.map { it.toData(data) },
        routeLineMaskingLayerDynamicData?.toData(data),
    )
}

private fun RouteLineError.toEventError(): RouteLineViewDataError {
    return RouteLineViewDataError(errorMessage)
}

private suspend fun RouteLineData.toData(
    data: RouteLineViewOptionsData,
): RouteLineEventData {
    return RouteLineEventData(
        featureCollection,
        dynamicData?.toData(data),
    )
}

private suspend fun RouteLineDynamicData.toData(
    data: RouteLineViewOptionsData,
): RouteLineDynamicEventData {
    return RouteLineDynamicEventData(
        baseExpressionCommandHolder.toRouteLineExpressionEventData(data),
        casingExpressionCommandHolder.toRouteLineExpressionEventData(data),
        trafficExpressionCommandHolder?.toRouteLineExpressionEventData(data),
        restrictedSectionExpressionCommandHolder?.toRouteLineExpressionEventData(data),
        trimOffset,
        trailExpressionCommandHolder?.toRouteLineExpressionEventData(data),
        trailCasingExpressionCommandHolder?.toRouteLineExpressionEventData(data),
    )
}

private fun RouteLineExpressionEventData.toHolder(): RouteLineExpressionCommandHolder {
    return when (this) {
        is RouteLineNoOpExpressionEventData -> unsupportedRouteLineCommandHolder()
        is RouteLineProviderBasedExpressionEventData -> {
            val provider = LightRouteLineExpressionProvider { expression }
            when (property) {
                "line-trim-offset" -> {
                    RouteLineExpressionCommandHolder(
                        provider,
                        LineTrimCommandApplier(),
                    )
                }

                "line-gradient" -> {
                    RouteLineExpressionCommandHolder(
                        provider,
                        LineGradientCommandApplier(),
                    )
                }

                else -> throw IllegalStateException()
            }
        }

        else -> throw IllegalStateException()
    }
}
