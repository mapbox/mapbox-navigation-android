package com.mapbox.navigation.ui.internal.route

import android.graphics.drawable.Drawable
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.expressions.Expression.color
import com.mapbox.mapboxsdk.style.expressions.Expression.eq
import com.mapbox.mapboxsdk.style.expressions.Expression.exponential
import com.mapbox.mapboxsdk.style.expressions.Expression.get
import com.mapbox.mapboxsdk.style.expressions.Expression.interpolate
import com.mapbox.mapboxsdk.style.expressions.Expression.literal
import com.mapbox.mapboxsdk.style.expressions.Expression.match
import com.mapbox.mapboxsdk.style.expressions.Expression.product
import com.mapbox.mapboxsdk.style.expressions.Expression.stop
import com.mapbox.mapboxsdk.style.expressions.Expression.switchCase
import com.mapbox.mapboxsdk.style.expressions.Expression.zoom
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconPitchAlignment
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconSize
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineCap
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.DEFAULT_ROUTE_DESCRIPTOR_PLACEHOLDER
import com.mapbox.navigation.ui.internal.route.RouteConstants.DESTINATION_MARKER_NAME
import com.mapbox.navigation.ui.internal.route.RouteConstants.ORIGIN_MARKER_NAME
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_TRAFFIC_SOURCE_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_DESTINATION_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_ORIGIN_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_PROPERTY_KEY
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.internal.utils.MapImageUtils
import com.mapbox.navigation.ui.route.RouteStyleDescriptor

interface MapboxRouteLayerProvider : RouteLayerProvider {
    val routeStyleDescriptors: List<RouteStyleDescriptor>

    fun getRouteLineColorExpressions(defaultColor: Int): List<Expression> {
        val expressions = mutableListOf<Expression>(
            eq(get(DEFAULT_ROUTE_DESCRIPTOR_PLACEHOLDER), true),
            color(defaultColor)
        )
        routeStyleDescriptors.forEach {
            expressions.add(eq(get(it.routeIdentifier), true))
            expressions.add(color(it.lineColorResourceId))
        }
        return expressions.plus(color(defaultColor))
    }

    fun getRouteLineShieldColorExpressions(defaultColor: Int): List<Expression> {
        val expressions = mutableListOf<Expression>()
        routeStyleDescriptors.forEach {
            expressions.add(eq(get(it.routeIdentifier), true))
            expressions.add(color(it.lineShieldColorResourceId))
        }
        return expressions.plus(color(defaultColor))
    }

    fun getRouteLineWidthExpressions(scale: Float): Expression {
        return interpolate(
            exponential(1.5f), zoom(),
            stop(4f, product(literal(3f), literal(scale))),
            stop(10f, product(literal(4f), literal(scale))),
            stop(13f, product(literal(6f), literal(scale))),
            stop(16f, product(literal(10f), literal(scale))),
            stop(19f, product(literal(14f), literal(scale))),
            stop(22f, product(literal(18f), literal(scale)))
        )
    }

    fun getShieldLineWidthExpression(scale: Float): Expression {
        return interpolate(
            exponential(1.5f), zoom(),
            stop(10f, 7f),
            stop(14f, product(literal(10.5f), literal(scale))),
            stop(16.5f, product(literal(15.5f), literal(scale))),
            stop(19f, product(literal(24f), literal(scale))),
            stop(22f, product(literal(29f), literal(scale)))
        )
    }

    override fun initializePrimaryRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        scale: Float,
        color: Int
    ): LineLayer {
        val lineWidthScaleExpression = getRouteLineWidthExpressions(scale)
        val routeLineColorExpressions = getRouteLineColorExpressions(color)
        return initializeRouteLayer(
            style,
            roundedLineCap,
            PRIMARY_ROUTE_LAYER_ID,
            PRIMARY_ROUTE_SOURCE_ID,
            lineWidthScaleExpression,
            routeLineColorExpressions)
    }

    override fun initializePrimaryRouteTrafficLayer(
        style: Style,
        roundedLineCap: Boolean,
        scale: Float,
        color: Int
    ): LineLayer {
        val lineWidthScaleExpression = getRouteLineWidthExpressions(scale)
        val routeLineColorExpressions = getRouteLineColorExpressions(color)
        return initializeRouteLayer(
            style,
            roundedLineCap,
            PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
            PRIMARY_ROUTE_TRAFFIC_SOURCE_ID,
            lineWidthScaleExpression,
            routeLineColorExpressions)
    }

    override fun initializePrimaryRouteCasingLayer(
        style: Style,
        scale: Float,
        color: Int
    ): LineLayer {
        val lineWidthScaleExpression = getShieldLineWidthExpression(scale)
        val routeLineColorExpressions = getRouteLineShieldColorExpressions(color)
        return initializeRouteLayer(
            style,
            true,
            PRIMARY_ROUTE_CASING_LAYER_ID,
            PRIMARY_ROUTE_SOURCE_ID,
            lineWidthScaleExpression,
            routeLineColorExpressions)
    }

    override fun initializeAlternativeRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        scale: Float,
        color: Int
    ): LineLayer {
        val lineWidthExpression = getRouteLineWidthExpressions(scale)
        val routeLineColorExpressions = getRouteLineColorExpressions(color)
        return initializeRouteLayer(
            style,
            roundedLineCap,
            ALTERNATIVE_ROUTE_LAYER_ID,
            ALTERNATIVE_ROUTE_SOURCE_ID,
            lineWidthExpression,
            routeLineColorExpressions)
    }

    override fun initializeAlternativeRouteCasingLayer(
        style: Style,
        scale: Float,
        color: Int
    ): LineLayer {
        val lineWidthScaleExpression = getShieldLineWidthExpression(scale)
        val routeLineColorExpressions = getRouteLineShieldColorExpressions(color)
        return initializeRouteLayer(
            style,
            true,
            ALTERNATIVE_ROUTE_CASING_LAYER_ID,
            ALTERNATIVE_ROUTE_SOURCE_ID,
            lineWidthScaleExpression,
            routeLineColorExpressions)
    }

    override fun initializeWayPointLayer(
        style: Style,
        originIcon: Drawable,
        destinationIcon: Drawable
    ): SymbolLayer {
        style.getLayerAs<SymbolLayer>(WAYPOINT_LAYER_ID)?.let {
            style.removeLayer(WAYPOINT_LAYER_ID)
        }

        MapImageUtils.getBitmapFromDrawable(originIcon).let {
            style.addImage(ORIGIN_MARKER_NAME, it)
        }

        MapImageUtils.getBitmapFromDrawable(destinationIcon).let {
            style.addImage(DESTINATION_MARKER_NAME, it)
        }

        return SymbolLayer(WAYPOINT_LAYER_ID, WAYPOINT_SOURCE_ID).withProperties(
            iconImage(
                match(
                    Expression.toString(get(WAYPOINT_PROPERTY_KEY)), literal(ORIGIN_MARKER_NAME),
                    stop(WAYPOINT_ORIGIN_VALUE, literal(ORIGIN_MARKER_NAME)),
                    stop(WAYPOINT_DESTINATION_VALUE, literal(DESTINATION_MARKER_NAME))
                )),
            iconSize(
                interpolate(
                    exponential(1.5f), zoom(),
                    stop(0f, 0.6f),
                    stop(10f, 0.8f),
                    stop(12f, 1.3f),
                    stop(22f, 2.8f)
                )
            ),
            iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
            iconAllowOverlap(true),
            iconIgnorePlacement(true)
        )
    }

    private fun initializeRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        layerId: String,
        layerSourceId: String,
        lineWidthExpression: Expression,
        colorExpressions: List<Expression>
    ): LineLayer {
        style.getLayerAs<LineLayer>(layerId)?.let {
            style.removeLayer(it)
        }

        val lineCapValue = when (roundedLineCap) {
            true -> Property.LINE_CAP_ROUND
            false -> Property.LINE_CAP_BUTT
        }

        val lineJoinValue = when (roundedLineCap) {
            true -> Property.LINE_JOIN_ROUND
            false -> Property.LINE_JOIN_BEVEL
        }

        return LineLayer(layerId, layerSourceId).withProperties(
            lineCap(lineCapValue),
            lineJoin(lineJoinValue),
            lineWidth(lineWidthExpression),
            lineColor(
                switchCase(
                    *colorExpressions.toTypedArray()
            ))
        )
    }
}
