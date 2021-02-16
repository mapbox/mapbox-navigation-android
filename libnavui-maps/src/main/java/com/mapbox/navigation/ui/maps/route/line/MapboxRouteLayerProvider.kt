package com.mapbox.navigation.ui.maps.route.line

import android.graphics.drawable.Drawable
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.switchCase
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getRouteLineColorExpressions
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap

internal class MapboxRouteLayerProvider(
    val routeStyleDescriptors: List<RouteStyleDescriptor>,
    private val routeLineScaleExpression: Expression,
    private val routeCasingLineScaleExpression: Expression,
    private val routeTrafficLineScaleExpression: Expression
) {

    fun buildPrimaryRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): LineLayer {
        val routeLineColorExpressions =
            getRouteLineColorExpressions(
                color,
                routeStyleDescriptors,
                RouteStyleDescriptor::lineColorResourceId
            )
        return initializeRouteLayer(
            style,
            roundedLineCap,
            RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
            RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
            routeLineScaleExpression,
            routeLineColorExpressions
        )
    }

    fun buildPrimaryRouteTrafficLayer(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): LineLayer {
        val routeLineColorExpressions =
            getRouteLineColorExpressions(
                color,
                routeStyleDescriptors,
                RouteStyleDescriptor::lineColorResourceId
            )
        return initializeRouteLayer(
            style,
            roundedLineCap,
            RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
            RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
            routeTrafficLineScaleExpression,
            routeLineColorExpressions
        )
    }

    fun buildPrimaryRouteCasingLayer(
        style: Style,
        color: Int
    ): LineLayer {
        val routeLineColorExpressions =
            getRouteLineColorExpressions(
                color,
                routeStyleDescriptors,
                RouteStyleDescriptor::lineCasingColorResourceId
            )
        return initializeRouteLayer(
            style,
            true,
            RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
            RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
            routeCasingLineScaleExpression,
            routeLineColorExpressions
        )
    }

    fun buildAlternativeRouteLayers(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): List<LineLayer> {
        val routeLineColorExpressions =
            getRouteLineColorExpressions(
                color,
                routeStyleDescriptors,
                RouteStyleDescriptor::lineColorResourceId
            )

        return listOf(
            initializeRouteLayer(
                style,
                roundedLineCap,
                RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID,
                RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                routeTrafficLineScaleExpression,
                routeLineColorExpressions
            ),
            initializeRouteLayer(
                style,
                roundedLineCap,
                RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID,
                RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                routeTrafficLineScaleExpression,
                routeLineColorExpressions
            )
        )
    }

    fun buildAlternativeRouteCasingLayers(
        style: Style,
        color: Int
    ): List<LineLayer> {
        val routeLineColorExpressions =
            getRouteLineColorExpressions(
                color,
                routeStyleDescriptors,
                RouteStyleDescriptor::lineCasingColorResourceId
            )

        return listOf(
            initializeRouteLayer(
                style,
                true,
                RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID,
                RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                routeCasingLineScaleExpression,
                routeLineColorExpressions
            ),
            initializeRouteLayer(
                style,
                true,
                RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID,
                RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                routeCasingLineScaleExpression,
                routeLineColorExpressions
            )
        )
    }

    fun buildAlternativeRouteTrafficLayers(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): List<LineLayer> {
        val routeLineColorExpressions =
            getRouteLineColorExpressions(
                color,
                routeStyleDescriptors,
                RouteStyleDescriptor::lineColorResourceId
            )

        return listOf(
            initializeRouteLayer(
                style,
                roundedLineCap,
                RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID,
                RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                routeTrafficLineScaleExpression,
                routeLineColorExpressions
            ),
            initializeRouteLayer(
                style,
                roundedLineCap,
                RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID,
                RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                routeTrafficLineScaleExpression,
                routeLineColorExpressions
            )
        )
    }

    fun buildWayPointLayer(
        style: Style,
        originIcon: Drawable,
        destinationIcon: Drawable
    ): SymbolLayer {
        if (style.styleLayerExists(RouteLayerConstants.WAYPOINT_LAYER_ID)) {
            style.removeStyleLayer(RouteLayerConstants.WAYPOINT_LAYER_ID)
        }

        if (style.getStyleImage(RouteConstants.ORIGIN_MARKER_NAME) != null) {
            style.removeStyleImage(RouteConstants.ORIGIN_MARKER_NAME)
        }
        originIcon.getBitmap().let {
            style.addImage(RouteConstants.ORIGIN_MARKER_NAME, it)
        }

        if (style.getStyleImage(RouteConstants.DESTINATION_MARKER_NAME) != null) {
            style.removeStyleImage(RouteConstants.DESTINATION_MARKER_NAME)
        }
        destinationIcon.getBitmap().let {
            style.addImage(RouteConstants.DESTINATION_MARKER_NAME, it)
        }

        return SymbolLayer(RouteLayerConstants.WAYPOINT_LAYER_ID, RouteConstants.WAYPOINT_SOURCE_ID)
            .iconImage(
                match {
                    toString {
                        get { literal(RouteConstants.WAYPOINT_PROPERTY_KEY) }
                    }
                    literal(RouteConstants.ORIGIN_MARKER_NAME)
                    stop {
                        RouteConstants.WAYPOINT_ORIGIN_VALUE
                        literal(RouteConstants.ORIGIN_MARKER_NAME)
                    }
                    stop {
                        RouteConstants.WAYPOINT_DESTINATION_VALUE
                        literal(RouteConstants.DESTINATION_MARKER_NAME)
                    }
                }
            )
            .iconSize(
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
            .iconPitchAlignment(IconPitchAlignment.MAP)
            .iconAllowOverlap(true)
            .iconIgnorePlacement(true)
    }

    private fun initializeRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        layerId: String,
        layerSourceId: String,
        lineWidthExpression: Expression,
        colorExpressions: List<Expression>
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
            .lineColor(
                switchCase(*colorExpressions.toTypedArray())
            )
    }
}
