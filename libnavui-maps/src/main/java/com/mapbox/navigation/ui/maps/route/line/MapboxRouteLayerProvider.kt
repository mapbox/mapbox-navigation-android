package com.mapbox.navigation.ui.maps.route.line

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap

internal class MapboxRouteLayerProvider(
    private val routeLineScaleExpression: Expression,
    private val routeCasingLineScaleExpression: Expression,
    private val routeTrafficLineScaleExpression: Expression,
    private val alternativeRouteLineScaleExpression: Expression,
    private val alternativeRouteCasingLineScaleExpression: Expression,
    private val alternativeRouteTrafficLineScaleExpression: Expression
) {

    fun buildAccessRestrictionsLayer(
        lineDashArray: List<Double>,
        lineOpacity: Double,
        @ColorInt color: Int,
        lineWidth: Double
    ): LineLayer {
        return LineLayer(
            RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID,
            RouteLayerConstants.PRIMARY_ROUTE_SOURCE_ID
        )
            .lineWidth(lineWidth)
            .lineJoin(LineJoin.ROUND)
            .lineOpacity(lineOpacity)
            .lineColor(color)
            .lineDasharray(lineDashArray)
            .lineCap(LineCap.ROUND)
    }

    fun buildPrimaryRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): LineLayer {
        return buildPrimaryRouteRelatedLayer(
            RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
            style,
            roundedLineCap,
            color
        )
    }

    fun buildPrimaryRouteTrailLayer(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): LineLayer {
        return buildPrimaryRouteRelatedLayer(
            RouteLayerConstants.PRIMARY_ROUTE_TRAIL_LAYER_ID,
            style,
            roundedLineCap,
            color
        )
    }

    private fun buildPrimaryRouteRelatedLayer(
        layerId: String,
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): LineLayer {
        return initializeRouteLayer(
            style,
            roundedLineCap,
            layerId,
            RouteLayerConstants.PRIMARY_ROUTE_SOURCE_ID,
            routeLineScaleExpression,
            color
        )
    }

    fun buildPrimaryRouteTrafficLayer(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): LineLayer {
        return initializeRouteLayer(
            style,
            roundedLineCap,
            RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
            RouteLayerConstants.PRIMARY_ROUTE_SOURCE_ID,
            routeTrafficLineScaleExpression,
            color
        )
    }

    fun buildPrimaryRouteCasingLayer(
        style: Style,
        color: Int
    ): LineLayer {
        return buildPrimaryRouteCasingRelatedLayer(
            RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
            style,
            color
        )
    }

    fun buildPrimaryRouteCasingTrailLayer(
        style: Style,
        color: Int
    ): LineLayer {
        return buildPrimaryRouteCasingRelatedLayer(
            RouteLayerConstants.PRIMARY_ROUTE_CASING_TRAIL_LAYER_ID,
            style,
            color
        )
    }

    private fun buildPrimaryRouteCasingRelatedLayer(
        layerId: String,
        style: Style,
        color: Int
    ): LineLayer {
        return initializeRouteLayer(
            style,
            true,
            layerId,
            RouteLayerConstants.PRIMARY_ROUTE_SOURCE_ID,
            routeCasingLineScaleExpression,
            color
        )
    }

    fun buildAlternativeRouteLayers(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): List<LineLayer> {
        return listOf(
            initializeRouteLayer(
                style,
                roundedLineCap,
                RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID,
                RouteLayerConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                alternativeRouteLineScaleExpression,
                color
            ),
            initializeRouteLayer(
                style,
                roundedLineCap,
                RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID,
                RouteLayerConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                alternativeRouteLineScaleExpression,
                color
            )
        )
    }

    fun buildAlternativeRouteCasingLayers(
        style: Style,
        color: Int
    ): List<LineLayer> {
        return listOf(
            initializeRouteLayer(
                style,
                true,
                RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID,
                RouteLayerConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                alternativeRouteCasingLineScaleExpression,
                color
            ),
            initializeRouteLayer(
                style,
                true,
                RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID,
                RouteLayerConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                alternativeRouteCasingLineScaleExpression,
                color
            )
        )
    }

    fun buildAlternativeRouteTrafficLayers(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): List<LineLayer> {
        return listOf(
            initializeRouteLayer(
                style,
                roundedLineCap,
                RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID,
                RouteLayerConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                alternativeRouteTrafficLineScaleExpression,
                color
            ),
            initializeRouteLayer(
                style,
                roundedLineCap,
                RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID,
                RouteLayerConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                alternativeRouteTrafficLineScaleExpression,
                color
            )
        )
    }

    fun buildWayPointLayer(
        style: Style,
        originIcon: Drawable,
        destinationIcon: Drawable,
        iconOffset: List<Double>,
        iconAnchor: IconAnchor,
        iconPitchAlignment: IconPitchAlignment
    ): SymbolLayer {
        if (style.styleLayerExists(RouteLayerConstants.WAYPOINT_LAYER_ID)) {
            style.removeStyleLayer(RouteLayerConstants.WAYPOINT_LAYER_ID)
        }

        if (style.getStyleImage(RouteLayerConstants.ORIGIN_MARKER_NAME) != null) {
            style.removeStyleImage(RouteLayerConstants.ORIGIN_MARKER_NAME)
        }
        originIcon.getBitmap().let {
            style.addImage(RouteLayerConstants.ORIGIN_MARKER_NAME, it)
        }

        if (style.getStyleImage(RouteLayerConstants.DESTINATION_MARKER_NAME) != null) {
            style.removeStyleImage(RouteLayerConstants.DESTINATION_MARKER_NAME)
        }
        destinationIcon.getBitmap().let {
            style.addImage(RouteLayerConstants.DESTINATION_MARKER_NAME, it)
        }

        return SymbolLayer(
            RouteLayerConstants.WAYPOINT_LAYER_ID,
            RouteLayerConstants.WAYPOINT_SOURCE_ID
        )
            .iconOffset(iconOffset)
            .iconAnchor(iconAnchor)
            .iconImage(
                match {
                    toString {
                        get { literal(RouteLayerConstants.WAYPOINT_PROPERTY_KEY) }
                    }
                    literal(RouteLayerConstants.WAYPOINT_ORIGIN_VALUE)
                    stop {
                        RouteLayerConstants.WAYPOINT_ORIGIN_VALUE
                        literal(RouteLayerConstants.ORIGIN_MARKER_NAME)
                    }
                    stop {
                        RouteLayerConstants.WAYPOINT_DESTINATION_VALUE
                        literal(RouteLayerConstants.DESTINATION_MARKER_NAME)
                    }
                }
            ).iconSize(
                interpolate {
                    exponential { literal(1.5) }
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(10.0)
                        literal(0.8)
                    }
                    stop {
                        literal(12.0)
                        literal(1.3)
                    }
                    stop {
                        literal(22.0)
                        literal(2.8)
                    }
                }
            )
            .iconPitchAlignment(iconPitchAlignment)
            .iconAllowOverlap(true)
            .iconIgnorePlacement(true)
            .iconKeepUpright(true)
    }

    private fun initializeRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        layerId: String,
        layerSourceId: String,
        lineWidthExpression: Expression,
        lineColor: Int
    ): LineLayer {
        if (style.styleLayerExists(layerId)) {
            style.removeStyleLayer(layerId)
        }

        val lineCapValue = when (roundedLineCap) {
            true -> LineCap.ROUND
            false -> LineCap.BUTT
        }

        val lineJoinValue = when (roundedLineCap) {
            true -> LineJoin.ROUND
            false -> LineJoin.BEVEL
        }

        return LineLayer(layerId, layerSourceId)
            .lineCap(lineCapValue)
            .lineJoin(lineJoinValue)
            .lineWidth(lineWidthExpression)
            .lineColor(lineColor)
    }
}
