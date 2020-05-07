package com.mapbox.navigation.ui.map.route.view

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.plugin.style.expressions.Expression
import com.mapbox.maps.plugin.style.expressions.dsl.interpolate
import com.mapbox.maps.plugin.style.layers.properties.LineCap
import com.mapbox.maps.plugin.style.layers.properties.LineJoin
import com.mapbox.maps.plugin.style.layers.properties.Visibility
import com.mapbox.navigation.ui.base.map.route.model.RouteLineState

internal sealed class LineLayerRenderData(
    val featureCollection: FeatureCollection,
    val visibility: Visibility,
    val lineCap: LineCap,
    val lineJoin: LineJoin,
    val routeWidthExpression: Expression,
    val shieldWidthExpression: Expression,
    val routeColor: Int,
    val shieldColor: Int
)

internal class PrimaryRouteData(state: RouteLineState) : LineLayerRenderData(
    featureCollection = state.getPrimaryRouteFeatureCollection(),
    visibility = getVisibility(state.options.primaryRouteVisible),
    lineCap = state.getLineCap(),
    lineJoin = state.getLineJoin(),
    routeWidthExpression = getRouteWidth(state.options.primaryRouteScale),
    shieldWidthExpression = getShieldWidth(state.options.primaryRouteScale),
    routeColor = state.options.primaryRouteColor,
    shieldColor = state.options.primaryShieldColor
)

internal class AlternativeRouteData(state: RouteLineState) : LineLayerRenderData(
    featureCollection = state.getAlternativeRoutesCollection(),
    visibility = getVisibility(state.options.alternativeRouteVisible),
    lineCap = state.getLineCap(),
    lineJoin = state.getLineJoin(),
    routeWidthExpression = getRouteWidth(state.options.alternativeRouteScale),
    shieldWidthExpression = getShieldWidth(state.options.alternativeRouteScale),
    routeColor = state.options.alternativeRouteColor,
    shieldColor = state.options.alternativeShieldColor
)

private fun RouteLineState.getPrimaryRouteFeatureCollection() =
    FeatureCollection.fromFeatures(
        this.features.getOrNull(0)?.let { listOf(it) } ?: emptyList()
    )

private fun RouteLineState.getAlternativeRoutesCollection() =
    FeatureCollection.fromFeatures(this.features.drop(1))

private fun RouteLineState.getLineCap() =
    if (this.options.roundedLineCap) LineCap.ROUND else LineCap.BUTT

private fun RouteLineState.getLineJoin() =
    if (this.options.roundedLineCap) LineJoin.ROUND else LineJoin.BEVEL

private fun getRouteWidth(scale: Double) = interpolate {
    exponential {
        literal(1.5)
    }
    zoom()
    stop { literal(4); product { literal(3); literal(scale) } }
    stop { literal(10); product { literal(4); literal(scale) } }
    stop { literal(13); product { literal(6); literal(scale) } }
    stop { literal(16); product { literal(10); literal(scale) } }
    stop { literal(19); product { literal(14); literal(scale) } }
    stop { literal(22); product { literal(18); literal(scale) } }
}

private fun getShieldWidth(scale: Double) = interpolate {
    exponential {
        literal(1.5)
    }
    zoom()
    stop { literal(10); literal(7) }
    stop { literal(14); product { literal(10.5); literal(scale) } }
    stop { literal(16.5); product { literal(15.5); literal(scale) } }
    stop { literal(19); product { literal(24); literal(scale) } }
    stop { literal(22); product { literal(29); literal(scale) } }
}

private fun getVisibility(isVisible: Boolean) =
    if (isVisible)
        Visibility.VISIBLE
    else
        Visibility.NONE
