package com.mapbox.navigation.ui.maps.internal.route.line

import android.graphics.drawable.Drawable
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.color
import com.mapbox.maps.extension.style.expressions.dsl.generated.eq
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.exponential
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.switchCase
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ALTERNATIVE_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.DEFAULT_ROUTE_DESCRIPTOR_PLACEHOLDER
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.DESTINATION_MARKER_NAME
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ORIGIN_MARKER_NAME
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.PRIMARY_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.WAYPOINT_DESTINATION_VALUE
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.WAYPOINT_LAYER_ID
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.WAYPOINT_ORIGIN_VALUE
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.WAYPOINT_PROPERTY_KEY
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.base.internal.utils.MapImageUtils
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import kotlin.reflect.KProperty1

interface MapboxRouteLayerProvider : RouteLayerProvider {
    val routeStyleDescriptors: List<RouteStyleDescriptor>
    val routeLineScaleValues: List<RouteLineScaleValue>
    val routeLineCasingScaleValues: List<RouteLineScaleValue>
    val routeLineTrafficScaleValues: List<RouteLineScaleValue>

    fun getRouteLineColorExpressions(
        defaultColor: Int,
        routeColorProvider: KProperty1<RouteStyleDescriptor, Int>
    ): List<Expression> {
        val expressions = mutableListOf<Expression>(
            eq {
                get { literal(DEFAULT_ROUTE_DESCRIPTOR_PLACEHOLDER) }
                literal(true)
            },
            color(defaultColor)
        )
        routeStyleDescriptors.forEach {
            expressions.add(
                eq {
                    get { it.routeIdentifier }
                    literal(true)
                }
            )
            expressions.add(color(routeColorProvider.get(it)))
        }
        return expressions.plus(color(defaultColor))
    }

    fun buildScalingExpression(scalingValues: List<RouteLineScaleValue>): Expression {
        val expressionBuilder = Expression.ExpressionBuilder("interpolate")
        expressionBuilder.addArgument(exponential { literal(1.5) })
        expressionBuilder.zoom()
        scalingValues.forEach { routeLineScaleValue ->
            expressionBuilder.stop {
                this.literal(routeLineScaleValue.scaleStop.toDouble())
                product {
                    literal(routeLineScaleValue.scaleMultiplier.toDouble())
                    literal(routeLineScaleValue.scale.toDouble())
                }
            }
        }
        return expressionBuilder.build()
    }

    override fun initializePrimaryRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): LineLayer {
        val lineWidthScaleExpression = buildScalingExpression(routeLineScaleValues)
        val routeLineColorExpressions =
            getRouteLineColorExpressions(color, RouteStyleDescriptor::lineColorResourceId)
        return initializeRouteLayer(
            style,
            roundedLineCap,
            PRIMARY_ROUTE_LAYER_ID,
            PRIMARY_ROUTE_SOURCE_ID,
            lineWidthScaleExpression,
            routeLineColorExpressions
        )
    }

    override fun initializePrimaryRouteTrafficLayer(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): LineLayer {
        val lineWidthScaleExpression = buildScalingExpression(routeLineTrafficScaleValues)
        val routeLineColorExpressions =
            getRouteLineColorExpressions(color, RouteStyleDescriptor::lineColorResourceId)
        return initializeRouteLayer(
            style,
            roundedLineCap,
            PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
            PRIMARY_ROUTE_SOURCE_ID,
            lineWidthScaleExpression,
            routeLineColorExpressions
        )
    }

    override fun initializePrimaryRouteCasingLayer(
        style: Style,
        color: Int
    ): LineLayer {
        val lineWidthScaleExpression = buildScalingExpression(routeLineCasingScaleValues)
        val routeLineColorExpressions =
            getRouteLineColorExpressions(color, RouteStyleDescriptor::lineShieldColorResourceId)
        return initializeRouteLayer(
            style,
            true,
            PRIMARY_ROUTE_CASING_LAYER_ID,
            PRIMARY_ROUTE_SOURCE_ID,
            lineWidthScaleExpression,
            routeLineColorExpressions
        )
    }

    override fun initializeAlternativeRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        color: Int
    ): LineLayer {
        val lineWidthExpression = buildScalingExpression(routeLineScaleValues)
        val routeLineColorExpressions =
            getRouteLineColorExpressions(color, RouteStyleDescriptor::lineColorResourceId)
        return initializeRouteLayer(
            style,
            roundedLineCap,
            ALTERNATIVE_ROUTE_LAYER_ID,
            ALTERNATIVE_ROUTE_SOURCE_ID,
            lineWidthExpression,
            routeLineColorExpressions
        )
    }

    override fun initializeAlternativeRouteCasingLayer(
        style: Style,
        color: Int
    ): LineLayer {
        val lineWidthScaleExpression = buildScalingExpression(routeLineCasingScaleValues)
        val routeLineColorExpressions =
            getRouteLineColorExpressions(color, RouteStyleDescriptor::lineShieldColorResourceId)
        return initializeRouteLayer(
            style,
            true,
            ALTERNATIVE_ROUTE_CASING_LAYER_ID,
            ALTERNATIVE_ROUTE_SOURCE_ID,
            lineWidthScaleExpression,
            routeLineColorExpressions
        )
    }

    override fun initializeWayPointLayer(
        style: Style,
        originIcon: Drawable,
        destinationIcon: Drawable
    ): SymbolLayer {
        if (style.styleLayerExists(WAYPOINT_LAYER_ID)) {
            style.removeStyleLayer(WAYPOINT_LAYER_ID)
        }

        if (style.getStyleImage(ORIGIN_MARKER_NAME) != null) {
            style.removeStyleImage(ORIGIN_MARKER_NAME)
        }
        MapImageUtils.getBitmapFromDrawable(originIcon).let {
            style.addImage(ORIGIN_MARKER_NAME, it)
        }

        if (style.getStyleImage(DESTINATION_MARKER_NAME) != null) {
            style.removeStyleImage(DESTINATION_MARKER_NAME)
        }
        MapImageUtils.getBitmapFromDrawable(destinationIcon).let {
            style.addImage(DESTINATION_MARKER_NAME, it)
        }

        return SymbolLayer(WAYPOINT_LAYER_ID, WAYPOINT_SOURCE_ID)
            .iconImage(
                match {
                    toString {
                        get { literal(WAYPOINT_PROPERTY_KEY) }
                    }
                    literal(ORIGIN_MARKER_NAME)
                    stop {
                        WAYPOINT_ORIGIN_VALUE
                        literal(ORIGIN_MARKER_NAME)
                    }
                    stop {
                        WAYPOINT_DESTINATION_VALUE
                        literal(DESTINATION_MARKER_NAME)
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
